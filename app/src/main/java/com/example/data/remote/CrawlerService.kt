package com.example.data.remote

import android.util.Log
import com.example.data.model.CrawledPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.net.URL
import java.util.concurrent.TimeUnit

object CrawlerService {
    private const val TAG = "CrawlerService"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Crawling loop with 100-page limit. Discovers sitemap URLs and recursively crawls internal links.
     */
    suspend fun crawlWebsite(clientId: Int, startUrl: String, maxPages: Int = 100): List<CrawledPage> = withContext(Dispatchers.IO) {
        val crawledPages = mutableListOf<CrawledPage>()
        val visitedUrls = mutableSetOf<String>()
        val urlQueue = ArrayDeque<String>()

        // 1. Normalize start URL
        var normalizedStart = startUrl.trim()
        if (!normalizedStart.startsWith("http://") && !normalizedStart.startsWith("https://")) {
            normalizedStart = "https://$normalizedStart"
        }

        val baseHost = try {
            URL(normalizedStart).host
        } catch (e: Exception) {
            Log.e(TAG, "Invalid startUrl host calculation", e)
            return@withContext emptyList()
        }

        // 2. Discover URLs from Sitemaps
        val sitemapDiscoveredUrls = mutableSetOf<String>()
        val sitemapUrls = discoverSitemaps(normalizedStart, baseHost)
        sitemapUrls.forEach { sitemapUrl ->
            val urls = fetchAndParseSitemap(sitemapUrl, baseHost)
            sitemapDiscoveredUrls.addAll(urls)
        }

        // 3. Setup crawl queue
        urlQueue.add(normalizedStart)
        // Add sitemap discovered urls as candidates
        sitemapDiscoveredUrls.forEach { url ->
            val normalizedUrl = url.trim()
            if (normalizeUrl(normalizedUrl) != normalizeUrl(normalizedStart)) {
                urlQueue.add(normalizedUrl)
            }
        }

        Log.d(TAG, "Starting crawl. Initial candidates count: ${urlQueue.size}")

        // 4. Recursive BFS crawling loop
        while (urlQueue.isNotEmpty() && crawledPages.size < maxPages) {
            val currentUrl = urlQueue.removeFirst()
            val cleanUrl = normalizeUrl(currentUrl)

            if (visitedUrls.contains(cleanUrl)) {
                continue
            }
            visitedUrls.add(cleanUrl)

            try {
                Log.d(TAG, "Crawling page ($currentUrl) [${crawledPages.size + 1}/$maxPages]")
                val startTime = System.currentTimeMillis()

                val request = Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", "Mozilla/5.0 (compatible; SearchOpsBot/1.0; +https://ai.studio/build)")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val loadTimeMs = System.currentTimeMillis() - startTime
                val statusCode = response.code

                if (!response.isSuccessful) {
                    val failedPage = CrawledPage(
                        clientId = clientId,
                        url = currentUrl,
                        title = "خطای بارگذاری / عدم دسترسی سرور",
                        metaDescription = "فایل یا صفحه مورد نظر با کد وضعیت خطای $statusCode پاسخ داد.",
                        headingsH1 = "",
                        headingsH2 = "",
                        headingsH3 = "",
                        statusCode = statusCode,
                        loadTimeMs = loadTimeMs,
                        wordCount = 0,
                        missingAltCount = 0,
                        totalImages = 0,
                        isSecure = currentUrl.startsWith("https"),
                        canonicalUrl = currentUrl,
                        indexable = false,
                        hasSchema = false,
                        internalLinksCount = 0,
                        externalLinksCount = 0,
                        brokenLinksCount = 1
                    )
                    crawledPages.add(failedPage)
                    continue
                }

                val html = response.body?.string() ?: ""
                val doc = Jsoup.parse(html, currentUrl)

                // Metadata parsing
                val title = doc.title().trim().ifEmpty { "بدون عنوان" }
                val metaDesc = doc.select("meta[name=description]").firstOrNull()?.attr("content")?.trim() ?: ""
                val h1s = doc.select("h1").map { it.text().trim() }.filter { it.isNotEmpty() }
                val h2s = doc.select("h2").map { it.text().trim() }.filter { it.isNotEmpty() }
                val h3s = doc.select("h3").map { it.text().trim() }.filter { it.isNotEmpty() }
                val h4s = doc.select("h4").map { it.text().trim() }.filter { it.isNotEmpty() }
                val h5s = doc.select("h5").map { it.text().trim() }.filter { it.isNotEmpty() }
                val h6s = doc.select("h6").map { it.text().trim() }.filter { it.isNotEmpty() }

                val canonicalUrl = doc.select("link[rel=canonical]").firstOrNull()?.attr("href")?.trim() ?: currentUrl
                val robotsMeta = doc.select("meta[name=robots]").firstOrNull()?.attr("content")?.trim()
                val indexable = robotsMeta?.contains("noindex", ignoreCase = true) != true

                // Images alt tags analysis
                val images = doc.select("img")
                val totalImages = images.size
                var missingAltCount = 0
                images.forEach { img ->
                    val alt = img.attr("alt")
                    if (alt.isNullOrEmpty() || alt.trim().isEmpty()) {
                        missingAltCount++
                    }
                }

                // Schema tags
                val hasSchema = doc.select("script[type=application/ld+json]").isNotEmpty()

                // Word count estimation
                val text = doc.body()?.text() ?: ""
                val wordCount = text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size

                // Links analysis
                val linksElements = doc.select("a[href]")
                var internalLinksCount = 0
                var externalLinksCount = 0

                linksElements.forEach { element ->
                    val rawHref = element.attr("href").trim()
                    if (rawHref.isEmpty() || rawHref.startsWith("#") || rawHref.startsWith("javascript:") || rawHref.startsWith("mailto:") || rawHref.startsWith("tel:")) {
                        return@forEach
                    }

                    val resolvedHref = element.absUrl("href").trim()
                    if (resolvedHref.isEmpty()) return@forEach

                    try {
                        val parsedLinkUrl = URL(resolvedHref)
                        val linkHost = parsedLinkUrl.host

                        if (linkHost.contains(baseHost) || baseHost.contains(linkHost)) {
                            internalLinksCount++
                            val cleanResolved = normalizeUrl(resolvedHref)
                            if (!visitedUrls.contains(cleanResolved)) {
                                urlQueue.add(resolvedHref)
                            }
                        } else {
                            externalLinksCount++
                        }
                    } catch (e: Exception) {
                        // Ignore malformed links
                    }
                }

                val pageResult = CrawledPage(
                    clientId = clientId,
                    url = currentUrl,
                    title = title,
                    metaDescription = metaDesc,
                    headingsH1 = h1s.joinToString(", ").take(1000),
                    headingsH2 = h2s.joinToString(", ").take(1000),
                    headingsH3 = h3s.joinToString(", ").take(1000),
                    headingsH4 = h4s.joinToString(", ").take(1000),
                    headingsH5 = h5s.joinToString(", ").take(1000),
                    headingsH6 = h6s.joinToString(", ").take(1000),
                    statusCode = statusCode,
                    loadTimeMs = loadTimeMs,
                    wordCount = wordCount,
                    missingAltCount = missingAltCount,
                    totalImages = totalImages,
                    isSecure = currentUrl.startsWith("https"),
                    canonicalUrl = canonicalUrl,
                    indexable = indexable,
                    hasSchema = hasSchema,
                    internalLinksCount = internalLinksCount,
                    externalLinksCount = externalLinksCount,
                    brokenLinksCount = 0
                )

                crawledPages.add(pageResult)

            } catch (e: Exception) {
                Log.e(TAG, "Error crawling URL: $currentUrl", e)
                val failedPage = CrawledPage(
                    clientId = clientId,
                    url = currentUrl,
                    title = "خزش ناموفق وب‌سایت",
                    metaDescription = "یک خطای فنی هنگام واکشی صفحه رخ داد: ${e.localizedMessage}",
                    headingsH1 = "",
                    headingsH2 = "",
                    headingsH3 = "",
                    statusCode = 500,
                    loadTimeMs = 0,
                    wordCount = 0,
                    missingAltCount = 0,
                    totalImages = 0,
                    isSecure = currentUrl.startsWith("https"),
                    canonicalUrl = currentUrl,
                    indexable = false,
                    hasSchema = false,
                    internalLinksCount = 0,
                    externalLinksCount = 0,
                    brokenLinksCount = 1
                )
                crawledPages.add(failedPage)
            }
        }

        return@withContext crawledPages
    }

    private fun normalizeUrl(url: String): String {
        return try {
            val parsed = URL(url)
            var path = parsed.path
            if (path.endsWith("/")) {
                path = path.substring(0, path.length - 1)
            }
            "${parsed.protocol}://${parsed.host}${path}".lowercase()
        } catch (e: Exception) {
            url.lowercase()
        }
    }

    private fun discoverSitemaps(startUrl: String, baseHost: String): List<String> {
        val resultDesc = mutableListOf<String>()
        try {
            val parsedStart = URL(startUrl)
            val robotsUrl = "${parsedStart.protocol}://${parsedStart.host}${if (parsedStart.port != -1) ":" + parsedStart.port else ""}/robots.txt"
            val robotsRequest = Request.Builder().url(robotsUrl).build()
            val robotsResponse = okHttpClient.newCall(robotsRequest).execute()
            if (robotsResponse.isSuccessful) {
                val robotsTxt = robotsResponse.body?.string() ?: ""
                robotsTxt.lineSequence().forEach { line ->
                    if (line.trim().startsWith("Sitemap:", ignoreCase = true)) {
                        val path = line.substring(8).trim()
                        if (path.isNotEmpty() && (path.startsWith("http://") || path.startsWith("https://"))) {
                            resultDesc.add(path)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering sitemaps from robots.txt", e)
        }

        try {
            val parsedStart = URL(startUrl)
            val portStr = if (parsedStart.port != -1) ":" + parsedStart.port else ""
            val standardSitemap = "${parsedStart.protocol}://${parsedStart.host}$portStr/sitemap.xml"
            val standardSitemapIndex = "${parsedStart.protocol}://${parsedStart.host}$portStr/sitemap_index.xml"

            if (!resultDesc.contains(standardSitemap)) {
                resultDesc.add(standardSitemap)
            }
            if (!resultDesc.contains(standardSitemapIndex)) {
                resultDesc.add(standardSitemapIndex)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating default sitemaps", e)
        }

        return resultDesc
    }

    /**
     * Dedicated Jsoup parser function that fetches and parses sitemap.xml files using Jsoup's XML parser
     * to extract all nested <loc> tags, ensuring proper nested index recursion.
     */
    suspend fun fetchAndParseSitemap(sitemapUrl: String, baseHost: String, depth: Int = 0): Set<String> = withContext(Dispatchers.IO) {
        if (depth > 3) return@withContext emptySet()
        val foundUrls = mutableSetOf<String>()
        try {
            Log.d(TAG, "Fetching and parsing sitemap XML: $sitemapUrl")
            val request = Request.Builder()
                .url(sitemapUrl)
                .header("User-Agent", "Mozilla/5.0 (compatible; SearchOpsBot/1.0; +https://ai.studio/build)")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val xml = response.body?.string() ?: ""
                val doc = Jsoup.parse(xml, "", Parser.xmlParser())
                
                val locs = doc.select("loc")
                Log.d(TAG, "Found ${locs.size} <loc> elements in sitemap $sitemapUrl")

                locs.forEach { element ->
                    val url = element.text().trim()
                    if (url.isNotEmpty()) {
                        val parent = element.parent()
                        val parentName = parent?.normalName()?.lowercase() ?: ""
                        
                        // Handle standard or namespaced sitemap tags
                        if (parentName == "sitemap" || parentName.endsWith(":sitemap") || url.endsWith(".xml") || url.contains("sitemap")) {
                            Log.d(TAG, "Discovered nested sitemap in index: $url")
                            // Recursive fetch of nested sitemap
                            val nestedUrls = fetchAndParseSitemap(url, baseHost, depth + 1)
                            foundUrls.addAll(nestedUrls)
                        } else {
                            // Regular standard URL entries
                            try {
                                val parsedUrl = URL(url)
                                val linkHost = parsedUrl.host
                                if (linkHost.contains(baseHost) || baseHost.contains(linkHost)) {
                                    foundUrls.add(url)
                                }
                            } catch (e: Exception) {
                                // ignore malformed URLs in locs
                            }
                        }
                    }
                }
            } else {
                Log.w(TAG, "Sitemap fetch failed with code ${response.code}: $sitemapUrl")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sitemap $sitemapUrl", e)
        }
        return@withContext foundUrls
    }
}
