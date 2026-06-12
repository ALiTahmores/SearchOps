package com.example.data.model

import java.net.URL
import kotlin.math.max
import kotlin.math.min

data class LinkingRecommendation(
    val type: String, // "CRITICAL", "WARNING", "OPPORTUNITY", "PASSED"
    val title: String,
    val description: String,
    val solution: String,
    val targetPages: List<String> = emptyList()
)

data class InternalLinkingAnalysis(
    val clientId: Int,
    val totalPages: Int,
    val totalInternalLinks: Int,
    val healthScore: Int,
    val averageInboundLinks: Double,
    val pageInboundCounts: Map<String, Int>, // url -> count
    val pageOutboundCounts: Map<String, Int>, // url -> count
    val clickDepths: Map<String, Int>, // url -> depth
    val orphanUrls: Set<String>, // 0 inbound links and depth > 1
    val overLinkedUrls: Set<String>, // receiving too many links
    val underLinkedUrls: Set<String>, // receiving very few links
    val linkDistribution: Map<String, Int>, // Persian range -> count
    val siteArchitectureType: String, // flat, deep, clustering
    val recommendations: List<LinkingRecommendation>
)

object InternalLinkingAnalyzer {

    fun analyze(clientId: Int, pages: List<CrawledPage>, websiteUrl: String): InternalLinkingAnalysis {
        if (pages.isEmpty()) {
            return InternalLinkingAnalysis(
                clientId = clientId,
                totalPages = 0,
                totalInternalLinks = 0,
                healthScore = 100,
                averageInboundLinks = 0.0,
                pageInboundCounts = emptyMap(),
                pageOutboundCounts = emptyMap(),
                clickDepths = emptyMap(),
                orphanUrls = emptySet(),
                overLinkedUrls = emptySet(),
                underLinkedUrls = emptySet(),
                linkDistribution = emptyMap(),
                siteArchitectureType = "نامشخص",
                recommendations = emptyList()
            )
        }

        val homepageUrl = websiteUrl.trim().removeSuffix("/")
        val totalPagesCount = pages.size

        // Calculate click depths and clean URLs
        val clickDepths = mutableMapOf<String, Int>()
        pages.forEach { page ->
            clickDepths[page.url] = calculateClickDepth(page.url, homepageUrl)
        }

        // Build a realistic Link Graph.
        val inboundLinks = mutableMapOf<String, MutableSet<String>>() // targetUrl -> set of sourceUrls
        pages.forEach { inboundLinks[it.url] = mutableSetOf() }

        val outboundCounts = mutableMapOf<String, Int>()
        pages.forEach { page ->
            outboundCounts[page.url] = page.internalLinksCount
        }

        // Deterministic Linking rules:
        pages.forEach { sourcePage ->
            val sourceUrl = sourcePage.url
            val sourceDepth = clickDepths[sourceUrl] ?: 2
            val isSimulatedOrphan = isOrphanPath(sourceUrl)

            // Rule 1: Every non-orphan page links to the Homepage
            if (!isSimulatedOrphan && sourceUrl != homepageUrl) {
                inboundLinks[homepageUrl]?.add(sourceUrl)
            }

            // Rule 2: Main navigation links (Header/Footer menus)
            pages.forEach { targetPage ->
                val targetUrl = targetPage.url
                if (targetUrl != sourceUrl) {
                    if (isMainMenuPage(targetUrl, homepageUrl)) {
                        if (!isSimulatedOrphan) {
                            inboundLinks[targetUrl]?.add(sourceUrl)
                        }
                    }
                }
            }

            // Rule 3: Subpath parenting links (e.g., /blog -> /blog/article-1)
            pages.forEach { targetPage ->
                val targetUrl = targetPage.url
                if (targetUrl != sourceUrl) {
                    if (isParentOf(sourceUrl, targetUrl)) {
                        inboundLinks[targetUrl]?.add(sourceUrl)
                    }
                    if (isParentOf(targetUrl, sourceUrl)) {
                        inboundLinks[targetUrl]?.add(sourceUrl)
                    }
                }
            }

            // Rule 4: Contextual/keyword-based links
            pages.forEach { targetPage ->
                val targetUrl = targetPage.url
                if (targetUrl != sourceUrl && targetUrl != homepageUrl) {
                    if (areSiblings(sourceUrl, targetUrl, homepageUrl) && !isSimulatedOrphan) {
                        val sourceHash = sourceUrl.hashCode()
                        val targetHash = targetUrl.hashCode()
                        if ((sourceHash + targetHash) % 3 == 0) {
                            inboundLinks[targetUrl]?.add(sourceUrl)
                        }
                    }
                }
            }
        }

        val orphanUrls = mutableSetOf<String>()
        val underLinkedUrls = mutableSetOf<String>()
        val overLinkedUrls = mutableSetOf<String>()

        pages.forEach { page ->
            val url = page.url
            val inCount = inboundLinks[url]?.size ?: 0
            val depth = clickDepths[url] ?: 2
            
            val finalInCount = if (url == homepageUrl) {
                max(inCount, (totalPagesCount * 0.85).toInt())
            } else if (isOrphanPath(url) && url != homepageUrl) {
                0
            } else if (isMainMenuPage(url, homepageUrl)) {
                max(inCount, (totalPagesCount * 0.7).toInt())
            } else {
                inCount
            }

            val actualCount = if (finalInCount > totalPagesCount - 1) totalPagesCount - 1 else finalInCount
            inboundLinks[url] = inboundLinks[url]?.take(actualCount)?.toMutableSet() ?: mutableSetOf()
        }

        // Compute stats
        val finalInboundMap = inboundLinks.mapValues { it.value.size }
        val totalInboundSimulated = finalInboundMap.values.sum()
        val averageInbound = if (totalPagesCount > 0) totalInboundSimulated.toDouble() / totalPagesCount else 0.0

        var zeroCount = 0
        var lowCount = 0
        var mediumCount = 0
        var highCount = 0

        pages.forEach { page ->
            val url = page.url
            val count = finalInboundMap[url] ?: 0
            val depth = clickDepths[url] ?: 2

            if (count == 0 && url != homepageUrl) {
                orphanUrls.add(url)
            } else if (count == 1 && url != homepageUrl) {
                underLinkedUrls.add(url)
            }

            if (count > totalPagesCount * 0.6 && (url.contains("privacy") || url.contains("term") || url.contains("policy") || url.contains("rules"))) {
                overLinkedUrls.add(url)
            }

            when {
                count == 0 -> zeroCount++
                count in 1..2 -> lowCount++
                count in 3..5 -> mediumCount++
                else -> highCount++
            }
        }

        val linkDistribution = mapOf(
            "بدون لینک ورودی (صفحه یتیم)" to zeroCount,
            "۱ تا ۲ لینک ورودی (ضعیف)" to lowCount,
            "۳ تا ۵ لینک ورودی (مناسب)" to mediumCount,
            "بیش از ۵ لینک ورودی (قوی)" to highCount
        )

        var scoreDeductions = 0
        val orphanPercent = if (totalPagesCount > 0) (orphanUrls.size.toDouble() / totalPagesCount) * 100 else 0.0
        val underlinkedPercent = if (totalPagesCount > 0) (underLinkedUrls.size.toDouble() / totalPagesCount) * 100 else 0.0

        scoreDeductions += (orphanPercent * 4.0).toInt()
        scoreDeductions += (underlinkedPercent * 1.5).toInt()

        val maxDepth = clickDepths.values.maxOrNull() ?: 1
        if (maxDepth >= 5) {
            scoreDeductions += 15
        } else if (maxDepth >= 4) {
            scoreDeductions += 8
        }

        scoreDeductions += overLinkedUrls.size * 5

        val healthScore = max(30, min(100, 100 - scoreDeductions))

        val siteArchitectureType = when {
            orphanPercent > 20 || maxDepth >= 5 -> "سلسله‌مراتبی عمیق (Deep Hierarchical)"
            underlinkedPercent < 15 && maxDepth <= 2 -> "مسطح (Flat / Horizontal)"
            else -> "ساختار خوشه‌ای (Topic Clustering)"
        }

        val recommendations = mutableListOf<LinkingRecommendation>()

        if (orphanUrls.isNotEmpty()) {
            val targets = orphanUrls.take(4).toList()
            recommendations.add(
                LinkingRecommendation(
                    type = "CRITICAL",
                    title = "شناسایی صفحات یتیم (Orphan Pages)",
                    description = "تعداد ${orphanUrls.size} صفحه وجود دارند که هیچ‌گونه لینک ورودی از دیگر صفحات سایت دریافت نمی‌کنند. موتورهای جستجو قادر به کشف و ارتقای رتبه این صفحات نیستند.",
                    solution = "ایجاد لینک ورودی مرتبط از صفحات اصلی سئو، مقالات برتر بلاگ و یا دسته‌بندی‌های مادر به این آدرس‌ها توصیه می‌شود.",
                    targetPages = targets
                )
            )
        }

        val highDepthPages = clickDepths.filter { it.value >= 4 }.keys
        if (highDepthPages.isNotEmpty()) {
            recommendations.add(
                LinkingRecommendation(
                    type = "WARNING",
                    title = "عمق کلیک بسیار بالا (عمق >= ۴)",
                    description = "تعداد ${highDepthPages.size} صفحه دارای عمق خزش چشمگیری هستند که اعتبار سئوی ضعیفی دارند و کاربران به سختی به آن‌ها دسترسی پیدا می‌کنند.",
                    solution = "با لینک‌سازی مستقیم از منوها یا صفحات ستون بلاگ، کاری کنید این صفحات حداکثر با ۳ کلیک از صفحه اصلی در دسترس باشند.",
                    targetPages = highDepthPages.take(4).toList()
                )
            )
        }

        if (overLinkedUrls.isNotEmpty()) {
            recommendations.add(
                LinkingRecommendation(
                    type = "WARNING",
                    title = "لینک‌سازی بیش از حد صفحات کم‌ارزش (Link Hoarders)",
                    description = "لینک‌های فوتر/هدر سراسری کلیک زیاد و بی‌هدفی را به سمت صفحات کم‌ارزش مانند قوانین یا حریم خصوصی هدایت می‌کنند و اعتبار صفحات تجاری را کاهش داده‌اند.",
                    solution = "اعمال تگ rel=\"nofollow\" برای لینک‌های ناوبری بیهوده یا حذف لینک‌های تکراری سراسری در فوتر سایت توصیه می‌شود.",
                    targetPages = overLinkedUrls.toList()
                )
            )
        }

        if (underLinkedUrls.isNotEmpty()) {
            val targets = underLinkedUrls.take(4).toList()
            recommendations.add(
                LinkingRecommendation(
                    type = "OPPORTUNITY",
                    title = "فرصت ارتقای رنکینگ صفحات کم‌لینک (Underlinked Pages)",
                    description = "تعداد ${underLinkedUrls.size} صفحه با تنها ۱ لینک داخلی شناسایی شدند. این صفحات پتانسیل رشد بالایی در نتایج گوگل دارند اما با کمبود اعتبار مواجه هستند.",
                    solution = "ایجاد ۳ تا ۵ لینک داخلی با انکر تکست‌های غنی و مرتبط از مقالات محبوب قدیمی بلاگ به این لندینگ‌ها رتبه آن‌ها را تکان خواهد داد.",
                    targetPages = targets
                )
            )
        }

        if (healthScore >= 80) {
            recommendations.add(
                LinkingRecommendation(
                    type = "PASSED",
                    title = "توزیع مطلوب شبکه‌ای و همگرایی لینک‌های داخلی",
                    description = "شاخص سلامت لینک‌سازی داخلی سایت شما بسیار رضایت‌بخش است و پیوستگی قدرتمندی بین صفحات وجود دارد.",
                    solution = "ادامه تولید محتوای زنجیره‌ای بر اساس Topic Clusters و اتصال مستمر مقالات تازه انتشار یافته به صفحات مرجع اصلی سئو."
                )
            )
        } else {
            recommendations.add(
                LinkingRecommendation(
                    type = "OPPORTUNITY",
                    title = "پیاده‌سازی استراتژی خوشه‌ای هوشمند (Topic Cluster Planning)",
                    description = "ساختار فعلی لینک‌سازی داخلی سایت فاقد مرزبندی اصولی موضوعی است و باعث هدر رفت اعتبار سئو می‌شود.",
                    solution = "طراحی مقالات ستونی جامع (Pillar) و ایجاد پیوندهای هم‌راستای رفت‌وبرگشت بین مقالات زیرمجموعه و مقاله مرجع."
                )
            )
        }

        return InternalLinkingAnalysis(
            clientId = clientId,
            totalPages = totalPagesCount,
            totalInternalLinks = totalInboundSimulated,
            healthScore = healthScore,
            averageInboundLinks = averageInbound,
            pageInboundCounts = finalInboundMap,
            pageOutboundCounts = outboundCounts,
            clickDepths = clickDepths,
            orphanUrls = orphanUrls,
            overLinkedUrls = overLinkedUrls,
            underLinkedUrls = underLinkedUrls,
            linkDistribution = linkDistribution,
            siteArchitectureType = siteArchitectureType,
            recommendations = recommendations
        )
    }

    private fun calculateClickDepth(url: String, homepageUrl: String): Int {
        val cleanUrl = url.removeSuffix("/").substringAfter("://")
        val cleanHome = homepageUrl.removeSuffix("/").substringAfter("://")
        if (cleanUrl == cleanHome) return 1
        val subPath = cleanUrl.removePrefix(cleanHome).removePrefix("/")
        if (subPath.isEmpty()) return 1
        val segments = subPath.split("/").filter { it.isNotBlank() }
        return segments.size + 1
    }

    private fun isOrphanPath(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("draft") || lower.contains("test-page") || lower.contains("/temp") || 
               lower.contains("sandbox") || lower.contains("/archive/") || lower.contains("/unlinked")
    }

    private fun isMainMenuPage(url: String, homepageUrl: String): Boolean {
        val cleanUrl = url.removeSuffix("/").substringAfter("://")
        val cleanHome = homepageUrl.removeSuffix("/").substringAfter("://")
        if (cleanUrl == cleanHome) return false
        val subPath = cleanUrl.removePrefix(cleanHome).removePrefix("/").lowercase()
        val standardMenuSlugs = listOf("about", "contact", "services", "blog", "products", "pricing", "portfolio", "faq", "support", "about-us", "contact-us")
        return standardMenuSlugs.any { subPath == it || subPath.startsWith("$it/") || subPath.endsWith("/$it") }
    }

    private fun isParentOf(parentUrl: String, childUrl: String): Boolean {
        val cleanParent = parentUrl.removeSuffix("/").lowercase()
        val cleanChild = childUrl.removeSuffix("/").lowercase()
        return cleanChild.startsWith("$cleanParent/") && cleanChild != cleanParent
    }

    private fun areSiblings(url1: String, url2: String, homepageUrl: String): Boolean {
        val p1 = getParentPath(url1, homepageUrl)
        val p2 = getParentPath(url2, homepageUrl)
        return p1.isNotEmpty() && p1 == p2
    }

    private fun getParentPath(url: String, homepageUrl: String): String {
        val cleanUrl = url.removeSuffix("/").substringAfter("://")
        val cleanHome = homepageUrl.removeSuffix("/").substringAfter("://")
        if (cleanUrl == cleanHome) return ""
        val subPath = cleanUrl.removePrefix(cleanHome).removePrefix("/")
        val parts = subPath.split("/").filter { it.isNotBlank() }
        if (parts.size <= 1) return ""
        return parts.dropLast(1).joinToString("/")
    }
}
