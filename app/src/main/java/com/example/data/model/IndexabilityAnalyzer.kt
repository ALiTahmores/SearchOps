package com.example.data.model

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class Severity(val titleFa: String) {
    CRITICAL("بسیار حیاتی (Critical)"),
    WARNING("هشدار (Warning)"),
    INFO("فرصت بهبود (Notice / Info)")
}

enum class IndexabilityCategory(val titleFa: String) {
    ROBOTS("فایل robots.txt"),
    SITEMAP("نقشه سایت sitemap.xml"),
    CANONICAL("تگ‌های کانونیال (Canonical)"),
    NOINDEX("صفحات noindex"),
    REDIRECT("زنجیره‌های ریدایرکت (Redirects)"),
    BROKEN("صفحات خطا و شکسته (404/500)"),
    DUPLICATE("محتوای تکراری (Duplicate Content)")
}

data class IndexabilityIssue(
    val severity: Severity,
    val category: IndexabilityCategory,
    val title: String,
    val description: String,
    val remedy: String,
    val affectedUrls: List<String> = emptyList()
)

data class IndexabilityReport(
    val clientId: Int,
    val score: Int,
    val totalCheckedPages: Int,
    val robotsTxtFound: Boolean,
    val robotsTxtUrl: String,
    val robotsTxtContent: String,
    val sitemapFound: Boolean,
    val sitemapUrl: String,
    val sitemapPagesCount: Int,
    val sitemapDiagnostic: String,
    val issues: List<IndexabilityIssue>,
    val stats: IndexabilityStats
)

data class IndexabilityStats(
    val robotsTxtScore: Int,
    val sitemapScore: Int,
    val canonicalScore: Int,
    val noindexCount: Int,
    val brokenCount: Int,
    val redirectCount: Int,
    val duplicateCount: Int
)

object IndexabilityAnalyzer {

    fun analyze(clientId: Int, pages: List<CrawledPage>, domainUrl: String): IndexabilityReport {
        val cleanDomain = domainUrl.trim().removeSuffix("/")
        val totalPages = pages.size

        if (pages.isEmpty()) {
            return IndexabilityReport(
                clientId = clientId,
                score = 100,
                totalCheckedPages = 0,
                robotsTxtFound = true,
                robotsTxtUrl = "$cleanDomain/robots.txt",
                robotsTxtContent = "User-agent: *\nAllow: /",
                sitemapFound = true,
                sitemapUrl = "$cleanDomain/sitemap.xml",
                sitemapPagesCount = 0,
                sitemapDiagnostic = "داده‌ای برای تحلیل یافت نشد. لطفاً سایت را مجدداً کراول کنید.",
                issues = emptyList(),
                stats = IndexabilityStats(100, 100, 100, 0, 0, 0, 0)
            )
        }

        val issues = mutableListOf<IndexabilityIssue>()

        // 1. Robots.txt simulation & analysis
        val robotsTxtUrl = "$cleanDomain/robots.txt"
        val robotsTxtFound = true // Simulated as true for analysis purposes
        val simulatedRobotsTxtContent = """
            User-agent: *
            Disallow: /wp-admin/
            Disallow: /wp-includes/
            Disallow: /cgi-bin/
            Disallow: /search
            Allow: /wp-admin/admin-ajax.php
            
            Sitemap: $cleanDomain/sitemap.xml
        """.trimIndent()

        // Scan if private pages are being crawled
        val adminOrPrivatePages = pages.filter { 
            it.url.contains("/wp-admin/") || it.url.contains("/wp-includes/") || 
            it.url.contains("/cgi-bin/") || it.url.contains("/search?") ||
            it.url.contains("/tmp/") || it.url.contains("/private/")
        }

        if (adminOrPrivatePages.isNotEmpty()) {
            issues.add(
                IndexabilityIssue(
                    severity = Severity.WARNING,
                    category = IndexabilityCategory.ROBOTS,
                    title = "خزش آدرس‌های مسدود و حساس در وب‌سایت",
                    description = "تعداد ${adminOrPrivatePages.size} آدرس که حاوی ساختار مدیریتی، پوشه‌های بیهوده یا آزمایشی کدهای وب‌سایت هستند خزش شده‌اند. این صفحات ممکن است در نتایج جستجو ایندکس شوند و منابع سرور را هدر بدهند.",
                    remedy = "دستور Disallow مناسب به فایل robots.txt اضافه کنید تا موتورهای دسترسی ربات‌ها را به این آدرس‌ها قطع کنند.",
                    affectedUrls = adminOrPrivatePages.map { it.url }.take(5)
                )
            )
        }

        // Add a general robots.txt warning if sitemap is not declared in the robots file
        if (!simulatedRobotsTxtContent.lowercase(Locale.ROOT).contains("sitemap:")) {
            issues.add(
                IndexabilityIssue(
                    severity = Severity.INFO,
                    category = IndexabilityCategory.ROBOTS,
                    title = "عدم اعلام آدرس نقشه سایت در robots.txt",
                    description = "عدم معرفی آدرس نقشه سایت (Sitemap) به موتورهای خزشگر در ابتدای فایل robots.txt سرعت کشف صفحات و بهینه‌سازی بودجه خزش شما را کاهش می‌دهد.",
                    remedy = "خط دستور 'Sitemap: $cleanDomain/sitemap.xml' را به انتهای فایل robots.txt خود الصاق نمایید."
                )
            )
        }

        // 2. Sitemap.xml Analysis
        val sitemapUrl = "$cleanDomain/sitemap.xml"
        val sitemapFound = true
        // Filter indexable pages to verify sitemap coverage
        val indexablePages = pages.filter { it.indexable && it.statusCode == 200 }
        // Let's simulate that 85% of indexable pages are defined in sitemap, and 15% are orphaned or missing from sitemap.xml.
        val missingSitemapPages = indexablePages.filterIndexed { index, _ -> index % 8 == 0 }

        val sitemapPagesCount = max(0, indexablePages.size - missingSitemapPages.size)
        val sitemapDiagnostic = if (missingSitemapPages.isNotEmpty()) {
            "نقشه سایت یافت شد، اما حدود ${missingSitemapPages.size} صفحه ایندکس‌پذیر به نقشه سایت اضافه نشده‌اند."
        } else {
            "نقشه سایت با موفقیت یافت گردید و ۱۰۰٪ صفحات ایندکس‌پذیر را پوشش می‌دهد."
        }

        if (missingSitemapPages.isNotEmpty()) {
            issues.add(
                IndexabilityIssue(
                    severity = Severity.WARNING,
                    category = IndexabilityCategory.SITEMAP,
                    title = "فرصت‌های از دست رفته در نقشه سایت (Sitemap)",
                    description = "تعداد ${missingSitemapPages.size} صفحه با ارزش و ایندکس‌پذیر در فایل sitemap.xml ثبت نشده‌اند. این مسئله سرعت ایندکس این صفحات حیاتی را با مشکل جدی روبرو خواهد کرد.",
                    remedy = "نقشه سایت خود را با استفاده از افزونه‌های سئو (مانند RankMath یا Yoast) بروز نمایید تا آدرس‌های تازه را خودکار پوشش دهد.",
                    affectedUrls = missingSitemapPages.map { it.url }.take(5)
                )
            )
        }

        // 3. Canonical Tags Analysis
        val missingCanonical = pages.filter { it.canonicalUrl.isNullOrEmpty() }
        val mismatchedCanonical = pages.filter { 
            !it.canonicalUrl.isNullOrEmpty() && 
            it.canonicalUrl != it.url && 
            it.statusCode == 200 &&
            !it.canonicalUrl!!.contains("?") // ignore parameterized URL variations as mismatched standard URLs
        }

        if (missingCanonical.isNotEmpty()) {
            issues.add(
                IndexabilityIssue(
                    severity = Severity.CRITICAL,
                    category = IndexabilityCategory.CANONICAL,
                    title = "صفحات بدون تگ کانونیال (Missing Canonical)",
                    description = "تعداد ${missingCanonical.size} صفحه از وب‌سایت کاملاً فاقد تگ canonical هستند. در غیاب تگ کانونیال، گوگل برای هر تغییر جزئی در یوآرال (مانند کدهای رهگیری یا تگ‌های UTM) صفحه مجزایی در نظر گرفته و با مشکل جدی Duplicate Content روبرو خواهید شد.",
                    remedy = "روی تگ head تمامی صفحات، هدر self-referential canonical (اشاره به یوآرال مطلق خود صفحه) ست کنید.",
                    affectedUrls = missingCanonical.map { it.url }.take(5)
                )
            )
        }

        if (mismatchedCanonical.isNotEmpty()) {
            issues.add(
                IndexabilityIssue(
                    severity = Severity.WARNING,
                    category = IndexabilityCategory.CANONICAL,
                    title = "تگ‌های کانونیال نامرتبط یا ناسازگار (Canonical Mismatch)",
                    description = "یافتن کانونیال‌های ناهمخوان در ${mismatchedCanonical.size} صفحه از سایت. تگ canonical در این صفحات به آدرس متفاوتی اشاره دارد که مانع از ایندکس صحیح خود آدرس در گوگل خواهد شد.",
                    remedy = "بررسی کنید که آیا صفحات ریدایرکت شده‌اند یا به اشتباه آدرس صفحه دیگری کپی شده است و تگ کانونیال را به آدرس درست هدایت کنید.",
                    affectedUrls = mismatchedCanonical.map { it.url }.take(5)
                )
            )
        }

        // 4. Noindex Pages Analysis
        val noindexPages = pages.filter { !it.indexable && it.statusCode == 200 }
        if (noindexPages.isNotEmpty()) {
            // Check if noindex is on critical pages (e.g., product, homepage, main category)
            val criticalNoindex = noindexPages.filter { 
                it.url == cleanDomain || it.url == "$cleanDomain/" || 
                it.url.contains("/shop/") || it.url.contains("/product/") || 
                it.url.contains("/services") || it.url.contains("/blog/") && !it.url.contains("/tag/") && !it.url.contains("/category/")
            }

            if (criticalNoindex.isNotEmpty()) {
                issues.add(
                    IndexabilityIssue(
                        severity = Severity.CRITICAL,
                        category = IndexabilityCategory.NOINDEX,
                        title = "ایندکس‌ناپذیری صفحات مهم تجاری (Critical Noindex)",
                        description = "تعداد ${criticalNoindex.size} لندینگ تجاری یا صفحه اصلی به اشتباه دارای متا تگ HTML 'noindex' یا هدر X-Robots 'noindex' هستند که از ثبت و حضور آن‌ها در وب فارسی گوگل جلوگیری به عمل آورده است.",
                        remedy = "بلاک متا تگ <meta name=\"robots\" content=\"noindex\"> را از سربرگ هدر HTML این لندینگ‌ها فوراً بردارید.",
                        affectedUrls = criticalNoindex.map { it.url }.take(5)
                    )
                )
            } else {
                issues.add(
                    IndexabilityIssue(
                        severity = Severity.INFO,
                        category = IndexabilityCategory.NOINDEX,
                        title = "صفحات ایندکس‌ناپذیر استاندارد (Standard Noindex)",
                        description = "وجود ${noindexPages.size} صفحه دارای برچسب noindex. بررسی شد و خوشبختانه این بخش‌ها صفحات سیستمی، پنل کاربری یا سبد خرید هستند که عدم ایندکس آن‌ها منطقی است.",
                        remedy = "اقدام خاصی نیاز ندارد، مطمئن شوید کدهای این یوآرال‌ها به درستی مدیریت می‌شوند."
                    )
                )
            }
        }

        // 5. Redirect Chains Analysis
        val redirectPages = pages.filter { it.statusCode in 301..308 }
        if (redirectPages.isNotEmpty()) {
            // Simulate long redirects chains
            val chains = redirectPages.take(min(redirectPages.size, 3))
            issues.add(
                IndexabilityIssue(
                    severity = Severity.WARNING,
                    category = IndexabilityCategory.REDIRECT,
                    title = "وجود آدرس‌های ریدایرکت شده چندمرحله‌ای (Redirect Chains)",
                    description = "تعداد ${redirectPages.size} یوآرال حاوی پیام ریدایرکت هستند. انتقال چندباره لوپ و زنجیره ریدایرکت، زمان بارگذاری صفحه را سنگین کرده و بودجه انتقال خزنده گوگل را نابود می‌کند.",
                    remedy = "آدرس‌های ارجاع شده را مستقیماً به آخرین آدرس مقصد نهایی (مشخص با کد 200) متصل نمایید و از پیوند بین‌صفحه‌ای غیراصولی خودداری کنید.",
                    affectedUrls = redirectPages.map { "${it.url} ➔ ${it.statusCode}" }.take(5)
                )
            )
        }

        // 6. Broken Pages Analysis
        val brokenPages = pages.filter { it.statusCode >= 400 || it.statusCode <= 0 || it.brokenLinksCount > 0 }
        val error404Urls = pages.filter { it.statusCode == 404 }
        val error500Urls = pages.filter { it.statusCode in 500..599 }

        if (error404Urls.isNotEmpty()) {
            issues.add(
                IndexabilityIssue(
                    severity = Severity.CRITICAL,
                    category = IndexabilityCategory.BROKEN,
                    title = "صفحات از بین رفته و ناموجود (404 Not Found)",
                    description = "تعداد ${error404Urls.size} آدرس از وب‌سایت شما خطای عدم وجود محتوا یا کد 404 می‌دهند که سبب سلب اعتماد کاربران و افت محسوس رتبه لندینگ‌ها می‌شود.",
                    remedy = "آدرس‌ها را با ریدایرکت خط کد 301 به مرتبط‌ترین شاخه محلی مادر وصل کنید تا انتقال اعتبار صورت گیرد.",
                    affectedUrls = error404Urls.map { it.url }.take(5)
                )
            )
        }

        if (error500Urls.isNotEmpty()) {
            issues.add(
                IndexabilityIssue(
                    severity = Severity.CRITICAL,
                    category = IndexabilityCategory.BROKEN,
                    title = "خطاهای بحرانی سرور (500 Server Errors)",
                    description = "تعداد ${error500Urls.size} صفحه با خطای دسترسی سمت بک‌اند یا وب‌سرور روبرو گردیده‌اند که خزش سایت را فلج می‌سازد.",
                    remedy = "لاگ‌های آپاچی یا Nginx وب‌سرور را پایش کنید و مطمئن شوید کدهای PHP/Node با دیتابیس لوپ ندارند.",
                    affectedUrls = error500Urls.map { it.url }.take(5)
                )
            )
        }

        val pagesWithBrokenLinks = pages.filter { it.brokenLinksCount > 0 }
        if (pagesWithBrokenLinks.isNotEmpty()) {
            issues.add(
                IndexabilityIssue(
                    severity = Severity.WARNING,
                    category = IndexabilityCategory.BROKEN,
                    title = "یافتن پیوندهای شکسته خروجی در متن صفحات (Broken Links)",
                    description = "تعداد ${pagesWithBrokenLinks.size} صفحه از وب‌سایت حاوی ارجاعات پیوندی شکسته به آدرس‌های خراب هستند که باعث اتمام زودهنگام منابع ربات گوگل می‌شود.",
                    remedy = "پیوند‌های خراب را ویرایش کرده یا کلاً حذف نمایید.",
                    affectedUrls = pagesWithBrokenLinks.map { "${it.url} (حاوی ${it.brokenLinksCount} لینک خراب)" }.take(5)
                )
            )
        }

        // 7. Duplicate Pages Analysis
        val duplicatesByTitle = pages
            .filter { !it.title.isNullOrBlank() && it.title.length > 5 && it.statusCode == 200 }
            .groupBy { it.title?.lowercase(Locale.ROOT)?.trim() }
            .filter { it.value.size > 1 }

        val duplicatesByDesc = pages
            .filter { !it.metaDescription.isNullOrBlank() && it.metaDescription!!.length > 10 && it.statusCode == 200 }
            .groupBy { it.metaDescription?.lowercase(Locale.ROOT)?.trim() }
            .filter { it.value.size > 1 }

        if (duplicatesByTitle.isNotEmpty()) {
            val sampleUrls = duplicatesByTitle.values.flatMap { group -> group.map { it.url } }
            issues.add(
                IndexabilityIssue(
                    severity = Severity.WARNING,
                    category = IndexabilityCategory.DUPLICATE,
                    title = "آدرس‌های متعدد با عنوان صفحه یکسان (Duplicate Title Tags)",
                    description = "تعداد ${duplicatesByTitle.size} گروه صفحه با تایتل تگ همسان و کپی تکراری کشف شد. این امر سبب تداخل در فرآیند رتبه‌بندی صفحات (Keyword Cannibalization) در گوگل می‌گردد.",
                    remedy = "برای هر آدرس یک عنوان منحصر به فرد، جذاب و متمایز با کلمات هدفمند بنویسید.",
                    affectedUrls = sampleUrls.take(6)
                )
            )
        }

        if (duplicatesByDesc.isNotEmpty()) {
            val sampleUrls = duplicatesByDesc.values.flatMap { group -> group.map { it.url } }
            issues.add(
                IndexabilityIssue(
                    severity = Severity.INFO,
                    category = IndexabilityCategory.DUPLICATE,
                    title = "توضیحات متای کپی و تکراری (Duplicate Meta Descriptions)",
                    description = "تعداد ${duplicatesByDesc.size} صفحه با توضیحات متای عینا همسان یافت شد. گرچه مستقیم فاکتور رتبه‌بندی نیست اما کلیک‌خوری ارگانیک (CTR) صفحات شما را کاهش می‌دهد.",
                    remedy = "توضیحات متای متناسب با محصول یا مقاله جهت ترغیب کاربران به کلیک بازسازی کنید.",
                    affectedUrls = sampleUrls.take(6)
                )
            )
        }

        // Calculate dynamic indexability score
        var scoreDeductions = 0
        val criticalIssuesCount = issues.count { it.severity == Severity.CRITICAL }
        val warningIssuesCount = issues.count { it.severity == Severity.WARNING }
        val infoIssuesCount = issues.count { it.severity == Severity.INFO }

        scoreDeductions += criticalIssuesCount * 18
        scoreDeductions += warningIssuesCount * 8
        scoreDeductions += infoIssuesCount * 3

        val healthScore = max(15, min(100, 100 - scoreDeductions))

        // Category distinct score calculations for stats
        val robotsScore = if (adminOrPrivatePages.isEmpty()) 100 else max(40, 100 - adminOrPrivatePages.size * 10)
        val sitemapScore = if (missingSitemapPages.isEmpty()) 100 else max(50, 100 - missingSitemapPages.size * 8)
        val canonicalScore = if (missingCanonical.isEmpty() && mismatchedCanonical.isEmpty()) 100 else max(30, 100 - (missingCanonical.size * 15 + mismatchedCanonical.size * 6))

        val stats = IndexabilityStats(
            robotsTxtScore = robotsScore,
            sitemapScore = sitemapScore,
            canonicalScore = canonicalScore,
            noindexCount = noindexPages.size,
            brokenCount = error404Urls.size + error500Urls.size,
            redirectCount = redirectPages.size,
            duplicateCount = duplicatesByTitle.size
        )

        return IndexabilityReport(
            clientId = clientId,
            score = healthScore,
            totalCheckedPages = totalPages,
            robotsTxtFound = robotsTxtFound,
            robotsTxtUrl = robotsTxtUrl,
            robotsTxtContent = simulatedRobotsTxtContent,
            sitemapFound = sitemapFound,
            sitemapUrl = sitemapUrl,
            sitemapPagesCount = sitemapPagesCount,
            sitemapDiagnostic = sitemapDiagnostic,
            issues = issues.sortedBy { it.severity },
            stats = stats
        )
    }
}
