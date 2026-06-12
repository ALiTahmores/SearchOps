package com.example.data.model

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class OpportunityImpact(val titleFa: String, val level: String) {
    HIGH("اثرگذاری بالا (High)", "HIGH"),
    MEDIUM("اثرگذاری متوسط (Medium)", "MEDIUM"),
    LOW("اثرگذاری کم (Low)", "LOW")
}

enum class OpportunityCategory(val titleFa: String) {
    QUICK_WIN("برد سریع (Quick Win)"),
    METADATA("متاداده‌ها (Metadata)"),
    HEADINGS("ساختار هدینگ‌ها (Headings)"),
    IMAGE_ALT("ویژگی Alt تصاویر (Image ALT)"),
    SCHEMA("کدهای اسکیما (Structured Schema)"),
    PERFORMANCE("سرعت و پرفورمنس (Performance)")
}

data class SeoOpportunity(
    val id: String,
    val category: OpportunityCategory,
    val impact: OpportunityImpact,
    val title: String,
    val description: String,
    val priorityScore: Int, // 0 - 100
    val remedy: String,
    val affectedUrlsCount: Int,
    val affectedUrls: List<String> = emptyList()
)

data class OpportunityReport(
    val clientId: Int,
    val totalOpportunitiesCount: Int,
    val highImpactCount: Int,
    val mediumImpactCount: Int,
    val lowImpactCount: Int,
    val opportunities: List<SeoOpportunity>
)

object OpportunityAnalyzer {

    fun analyze(clientId: Int, pages: List<CrawledPage>): OpportunityReport {
        if (pages.isEmpty()) {
            return OpportunityReport(
                clientId = clientId,
                totalOpportunitiesCount = 0,
                highImpactCount = 0,
                mediumImpactCount = 0,
                lowImpactCount = 0,
                opportunities = emptyList()
            )
        }

        val opportunities = mutableListOf<SeoOpportunity>()

        // Helper - filter indexable normal pages
        val indexablePages = pages.filter { it.indexable && it.statusCode == 200 }

        // 1. QUICK WINS DETECTION
        // A) High word count pages missing Schema Markup
        val highWordCountNoSchema = indexablePages.filter { it.wordCount >= 700 && !it.hasSchema }
        if (highWordCountNoSchema.isNotEmpty()) {
            opportunities.add(
                SeoOpportunity(
                    id = "quick_win_missing_schema",
                    category = OpportunityCategory.QUICK_WIN,
                    impact = OpportunityImpact.HIGH,
                    title = "انفجار رتبه با افزودن اسکیما به صفحات پرمحتوا",
                    description = "تعداد ${highWordCountNoSchema.size} صفحه با محتوای بالا علمی/آموزشی و تجاری (بیش از ۷۰۰ واژه) فاقد هرگونه داده ساختاریافته (Schema Markup) هستند. افزودن اسکیما شانس تصاحب ستاره‌های گوگل، بخش FAQ و Rich Snippets را فوراً بالا می‌برد.",
                    priorityScore = 95,
                    remedy = "افزودن اسکیمای Article، Product یا FAQ متناسب با موضوع به این صفحات منتخب.",
                    affectedUrlsCount = highWordCountNoSchema.size,
                    affectedUrls = highWordCountNoSchema.map { "${it.url} (تعداد کلمات: ${it.wordCount})" }.take(5)
                )
            )
        }

        // B) Well-written content with missing or extremely weak Meta Descriptions
        val qualityContentWeakMeta = indexablePages.filter { it.wordCount >= 500 && (it.metaDescription.isNullOrBlank() || it.metaDescription!!.length < 40) }
        if (qualityContentWeakMeta.isNotEmpty()) {
            opportunities.add(
                SeoOpportunity(
                    id = "quick_win_weak_meta",
                    category = OpportunityCategory.QUICK_WIN,
                    impact = OpportunityImpact.HIGH,
                    title = "بهبود نرخ کلیک (CTR) صفحات غنی با بهبود متادیسکریپشن",
                    description = "تعداد ${qualityContentWeakMeta.size} صفحه با محتوای قوی (بیش از ۵۰۰ کلمه) دارای توضیحات متای ناقص، بسیار کوتاه یا خالی هستند. سئو این صفحات عالی است اما به دلیل جذاب نبودن خلاصه در گوگل، ورودی پایینی دارند.",
                    priorityScore = 90,
                    remedy = "یک متادیسکریپشن جذاب بین ۱۲۰ تا ۱۶۰ کاراکتر همراه با دکمه اقدام به عمل (CTA) جذاب برای هر صفحه بنویسید.",
                    affectedUrlsCount = qualityContentWeakMeta.size,
                    affectedUrls = qualityContentWeakMeta.map { it.url }.take(5)
                )
            )
        }


        // 2. MISSING METADATA
        // A) Missing Page Titles
        val missingTitlePages = pages.filter { it.statusCode == 200 && it.title.isNullOrBlank() }
        if (missingTitlePages.isNotEmpty()) {
            opportunities.add(
                SeoOpportunity(
                    id = "meta_missing_title",
                    category = OpportunityCategory.METADATA,
                    impact = OpportunityImpact.HIGH,
                    title = "تایتل خالی یا ناموجود صفحات سایت",
                    description = "تعداد ${missingTitlePages.size} صفحه با پاسخ ۲۰۰ فاقد تگ عنوان (H1 / Title) هستند. گوگل بدون عنوان مناسب، لندینگ شما را ایندکس یا رتبه‌بندی نمی‌کند.",
                    priorityScore = 98,
                    remedy = "برای هر صفحه یک تگ <title> یکتا، شامل کلمه کلیدی اصلی با طول ۵۰ تا ۶۰ کاراکتر تعریف کنید.",
                    affectedUrlsCount = missingTitlePages.size,
                    affectedUrls = missingTitlePages.map { it.url }.take(5)
                )
            )
        }

        // B) Too short or Duplicate Titles
        val shortTitlePages = indexablePages.filter { !it.title.isNullOrBlank() && it.title!!.length < 15 }
        if (shortTitlePages.isNotEmpty()) {
            opportunities.add(
                SeoOpportunity(
                    id = "meta_short_title",
                    category = OpportunityCategory.METADATA,
                    impact = OpportunityImpact.MEDIUM,
                    title = "عنوان صفحات بسیار کوتاه است (زیر ۱۵ کاراکتر)",
                    description = "تعداد ${shortTitlePages.size} صفحه وجود دارند که عنوان آن‌ها پتانسیل کافی برای پوشش کلمات کلیدی مترادف را از دست داده است.",
                    priorityScore = 70,
                    remedy = "عنوان صفحه را بروز کرده و بخش‌های برند، کلمه تکمیلی یا عبارات جذاب اضافه کنید.",
                    affectedUrlsCount = shortTitlePages.size,
                    affectedUrls = shortTitlePages.map { "${it.url} (عنوان فعلی: ${it.title})" }.take(5)
                )
            )
        }

        // C) Missing Meta Description Entirely
        val missingMetaDescPages = indexablePages.filter { it.metaDescription.isNullOrBlank() }
        if (missingMetaDescPages.isNotEmpty()) {
            opportunities.add(
                SeoOpportunity(
                    id = "meta_missing_desc",
                    category = OpportunityCategory.METADATA,
                    impact = OpportunityImpact.MEDIUM,
                    title = "توضیحات متای تعریف‌نشده (Missing Description)",
                    description = "تعداد ${missingMetaDescPages.size} صفحه از سایت فاقد توضیحات خلاصه یا متا دیسکریپشن هستند. گوگل در این موارد متنی تصادفی از مقاله را نشان می‌دهد که عموماً غیرجذاب است.",
                    priorityScore = 78,
                    remedy = "در تنظیمات سئو این صفحات، فیلد توضیحات متا را با توصیف مستقیم و حاوی کلمات کلیدی لندینگ پر کنید.",
                    affectedUrlsCount = missingMetaDescPages.size,
                    affectedUrls = missingMetaDescPages.map { it.url }.take(5)
                )
            )
        }


        // 3. MISSING HEADINGS
        // A) Missing H1 Heading
        val missingH1Pages = indexablePages.filter { it.headingsH1.isNullOrBlank() }
        if (missingH1Pages.isNotEmpty()) {
            opportunities.add(
                SeoOpportunity(
                    id = "headings_missing_h1",
                    category = OpportunityCategory.HEADINGS,
                    impact = OpportunityImpact.HIGH,
                    title = "فقدان هدینگ آ-یک (H1) در صفحات اصلی",
                    description = "تعداد ${missingH1Pages.size} لندینگ دارای محتوا بدون هدینگ اصلی اچ‌یک هستند. تگ H1 مهم‌ترین المان برای درک موضوع اصلی لندینگ توسط گوگل و رباتها است.",
                    priorityScore = 88,
                    remedy = "مطمئن شوید هر صفحه دقیقاً دارای یک تگ <h1> منحصربه‌فرد در ابتدای بخش بدنه خود باشد.",
                    affectedUrlsCount = missingH1Pages.size,
                    affectedUrls = missingH1Pages.map { it.url }.take(5)
                )
            )
        }

        // B) Multiple H1 Headings
        val multipleH1Pages = indexablePages.filter { 
            val h1s = it.headingsH1 ?: ""
            h1s.contains("||") || h1s.split(",").size > 1 
        }
        if (multipleH1Pages.isNotEmpty()) {
            opportunities.add(
                SeoOpportunity(
                    id = "headings_multiple_h1",
                    category = OpportunityCategory.HEADINGS,
                    impact = OpportunityImpact.MEDIUM,
                    title = "وجود بیش از یک هدینگ H1 در یک صفحه",
                    description = "تعداد ${multipleH1Pages.size} صفحه دارای ساختار نامتعارف با چندین تگ H1 هستند که سبب سردرگمی ربات‌های گوگل در تفکیک موضوع اصلی خواهد شد.",
                    priorityScore = 65,
                    remedy = "تگ‌های اضافی ۲ تا ۶ را به تگ H2 یا H3 تبدیل کنید و فقط عنوان اصلی را به عنوان تگ H1 حفظ کنید.",
                    affectedUrlsCount = multipleH1Pages.size,
                    affectedUrls = multipleH1Pages.map { it.url }.take(5)
                )
            )
        }


        // 4. MISSING IMAGE ALT ATTRIBUTES
        // Pages having images, but missing ALT attributes for some
        val pagesWithMissingAlt = indexablePages.filter { it.missingAltCount > 0 && it.totalImages > 0 }
        if (pagesWithMissingAlt.isNotEmpty()) {
            val totalMissingAltCount = pagesWithMissingAlt.sumOf { it.missingAltCount }
            opportunities.add(
                SeoOpportunity(
                    id = "images_missing_alt",
                    category = OpportunityCategory.IMAGE_ALT,
                    impact = OpportunityImpact.MEDIUM,
                    title = "تصاویر بدون متن جایگزین (Missing Image Alt)",
                    description = "کشف تعداد $totalMissingAltCount تصویر بدون صفت Alt در ${pagesWithMissingAlt.size} صفحه مجزا. متن جایگزین به موتورهای جستجو کمک می‌کند تا محتوای بصری را بفهمند و در بخش Google Images ترافیک عالی بیاورند.",
                    priorityScore = 75,
                    remedy = "به کدهای HTML تصاویر برچسب alt='توضیح کوتاه تصویر' اضافه نمایید تا مفهوم تصویر کاملاً قابل خوانش شود.",
                    affectedUrlsCount = pagesWithMissingAlt.size,
                    affectedUrls = pagesWithMissingAlt.map { "${it.url} (${it.missingAltCount} تصویر بدون Alt)" }.take(5)
                )
            )
        }


        // 5. MISSING SCHEMA MARKUP
        val pagesWithNoSchema = indexablePages.filter { !it.hasSchema }
        if (pagesWithNoSchema.isNotEmpty()) {
            opportunities.add(
                SeoOpportunity(
                    id = "schema_missing_all",
                    category = OpportunityCategory.SCHEMA,
                    impact = OpportunityImpact.MEDIUM,
                    title = "عدم پیاده‌سازی همگانی ساختار کدهای اسکیما",
                    description = "تعداد ${pagesWithNoSchema.size} صفحه ایندکس‌پذیر فاقد کدهای بهینه‌سازی خواننده سرور موسوم به نشانه گذاری معنایی Schema هستند.",
                    priorityScore = 72,
                    remedy = "افزودن اتوماتیک نشانه گذاری وب‌سایت (WebSite, WebPage, Organization) به کالبد کدهای قالب سایت به کمک افزونه‌های معتبر.",
                    affectedUrlsCount = pagesWithNoSchema.size,
                    affectedUrls = pagesWithNoSchema.map { it.url }.take(5)
                )
            )
        }


        // 6. PERFORMANCE OPPORTUNITIES
        val slowPages = indexablePages.filter { it.loadTimeMs > 1500 }
        if (slowPages.isNotEmpty()) {
            opportunities.add(
                SeoOpportunity(
                    id = "performance_slow_load",
                    category = OpportunityCategory.PERFORMANCE,
                    impact = OpportunityImpact.HIGH,
                    title = "صفحات با سرعت لود بحرانی و سنگین (بیش از ۱.۵ ثانیه)",
                    description = "تعداد ${slowPages.size} صفحه رصد شده اند که تایم لودینگ اولیه و رندر آن‌ها فراتر از استاندارد Core Web Vitals است. سرعت پایین به شکل مستقیم نرخ پرش (Bounce Rate) را افزایش می‌دهد.",
                    priorityScore = 85,
                    remedy = "کاهش بار حجم تصاویر با فرمت WebP، استفاده از سیستم کش (Cache) قوی در وب‌سرور و بهینه‌سازی کدهای JS/CSS خارجی.",
                    affectedUrlsCount = slowPages.size,
                    affectedUrls = slowPages.map { "${it.url} (${String.format(Locale.ROOT, "%.2f", it.loadTimeMs / 1000.0)} ثانیه)" }.take(5)
                )
            )
        }

        // Sort opportunities by Priority Score descending (highest first)
        val sortedOpportunities = opportunities.sortedByDescending { it.priorityScore }

        val highCount = sortedOpportunities.count { it.impact == OpportunityImpact.HIGH }
        val mediumCount = sortedOpportunities.count { it.impact == OpportunityImpact.MEDIUM }
        val lowCount = sortedOpportunities.count { it.impact == OpportunityImpact.LOW }

        return OpportunityReport(
            clientId = clientId,
            totalOpportunitiesCount = sortedOpportunities.size,
            highImpactCount = highCount,
            mediumImpactCount = mediumCount,
            lowImpactCount = lowCount,
            opportunities = sortedOpportunities
        )
    }
}
