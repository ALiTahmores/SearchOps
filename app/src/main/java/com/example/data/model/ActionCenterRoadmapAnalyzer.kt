package com.example.data.model

import java.util.Locale

enum class RoadmapCategory(val titleFa: String, val iconName: String) {
    TECHNICAL("سئوی فنی (Technical SEO)", "Settings"),
    CONTENT("سئوی محتوا (Content SEO)", "Description"),
    PERFORMANCE("سرعت و عملکرد (Performance)", "Speed"),
    INTERNAL_LINKING("لینک‌سازی داخلی (Internal Linking)", "Link"),
    SCHEMA("کدهای اسکیما (Schema Markup)", "Code")
}

data class RoadmapItem(
    val id: String,
    val category: RoadmapCategory,
    val title: String,
    val whatIsWrong: String,
    val whyItMatters: String,
    val howToFix: String,
    val estimatedImpact: OpportunityImpact,
    val affectedPagesCount: Int,
    val priorityScore: Int, // 1-100 for sorted display
    val affectedUrls: List<String> = emptyList()
)

data class ActionCenterRoadmap(
    val clientId: Int,
    val totalItemsCount: Int,
    val technicalCount: Int,
    val contentCount: Int,
    val performanceCount: Int,
    val internalLinkingCount: Int,
    val schemaCount: Int,
    val items: List<RoadmapItem>
)

object ActionCenterRoadmapAnalyzer {

    fun analyze(
        clientId: Int,
        pages: List<CrawledPage>,
        audit: AuditResult?
    ): ActionCenterRoadmap {
        val items = mutableListOf<RoadmapItem>()

        // Fallback or Seeding Mode - if pages is empty, generate highly polished Persian sandbox recommendations
        if (pages.isEmpty()) {
            return generateMockRoadmap(clientId)
        }

        // 1. TECHNICAL SEO CATEGORY RECOMMENDATIONS
        // A) SSL/Security Check
        val unsafePages = pages.filter { !it.isSecure }
        if (unsafePages.isNotEmpty()) {
            items.add(
                RoadmapItem(
                    id = "tech_ssl",
                    category = RoadmapCategory.TECHNICAL,
                    title = "عدم فعالسازی کامل پروتکل امنیتی SSL در برخی صفحات",
                    whatIsWrong = "تعداد ${unsafePages.size} آدرس از سایت شما کماکان تحت پروتکل غیرامن HTTP فراخوانی شده و گواهینامه SSL برای آن‌ها فعال یا اجباری نشده است.",
                    whyItMatters = "امنیت کاربران بالاترین اولویت گوگل است. از سال ۲۰۱۴ پروتکل HTTPS یک سیگنال مستقیم رتبه‌بندی بوده و مرورگرهایی مثل کروم در صورت ناامن بودن صفحه، هشداری بزرگ نشان می‌دهند که نرخ پرش را تا ۸۰٪ بالا می‌برد.",
                    howToFix = "یک گواهینامه SSL معتبر (مانند Let's Encrypt یا تجاری) تهیه و نصب کنید و سپس از طریق فایل .htaccess یا تنظیمات وب‌سرور، ریدایرکت ۳۰۱ همگانی از HTTP به Version امن HTTPS روی پورت ۴۴۳ پیکربندی نمایید.",
                    estimatedImpact = OpportunityImpact.HIGH,
                    affectedPagesCount = unsafePages.size,
                    priorityScore = 98,
                    affectedUrls = unsafePages.map { it.url }.take(5)
                )
            )
        } else {
            // Safe fallback suggestion for secure site if any pages crawled
            items.add(
                RoadmapItem(
                    id = "tech_ssl_passed",
                    category = RoadmapCategory.TECHNICAL,
                    title = "پایش امنیتی دوره‌ای اتصال پیوندها و گواهی رمزنگاری SSL",
                    whatIsWrong = "اگرچه تمام صفحات اسکن شده هم‌اکنون از HTTPS استفاده می‌کنند، اما باید سیستم تمدید خودکار و هدرهای پاسخ امنیتی نظیر HSTS مورد ارزیابی دایمی قرار گیرند.",
                    whyItMatters = "انقضای ناگهانی گواهی SSL در طول سال سبب مسدود شدن لحظه‌ای ربات کراولر گوگل و کاهش سریع رتبه‌ها به دلیل فیدبک خطای امنیتی به کاربر می‌شود.",
                    howToFix = "سیستم تمدید خودکار (Auto-Renew) گواهی SSL را روی سرور فعال کنید و در فایل تنظیمات وب‌سرور هدر Strict-Transport-Security را پیاده نمایید.",
                    estimatedImpact = OpportunityImpact.LOW,
                    affectedPagesCount = 0,
                    priorityScore = 40
                )
            )
        }

        // B) Broken Links (404 issues)
        val pagesWithBrokenLinks = pages.filter { it.brokenLinksCount > 0 }
        if (pagesWithBrokenLinks.isNotEmpty()) {
            val totalBroken = pagesWithBrokenLinks.sumOf { it.brokenLinksCount }
            items.add(
                RoadmapItem(
                    id = "tech_broken_links",
                    category = RoadmapCategory.TECHNICAL,
                    title = "وجود پیوندهای آسیب‌دیده با خطای داخلی و خارجی ۴۰۴",
                    whatIsWrong = "در پایش وب‌سایت تعداد $totalBroken پیوند خراب یا از دست رفته در ${pagesWithBrokenLinks.size} لندینگ شناسایی شده است که کلیک روی آن‌ها به بن‌بست ۴۰۴ ختم می‌شود.",
                    whyItMatters = "لینک‌های شکسته بودجه خزش ربات‌های گوگل (Crawl Budget) را به شدت هدر می‌دهند و باعث جریمه‌های ساختاری سایت در درازمدت شده و کاربران کلافه وبسایت را ترک می‌کنند.",
                    howToFix = "با مراجعه به لیست آدرس‌های تحت تاثیر، پیوندهای فرسوده یا اشتباه تایپی را اصلاح کرده یا صفحات مبدا را طوری بروزرسانی کنید که به یو‌آر‌ال‌های جدید و مرتبط ریدایرکت ۳۰۱ شوند.",
                    estimatedImpact = OpportunityImpact.HIGH,
                    affectedPagesCount = pagesWithBrokenLinks.size,
                    priorityScore = 94,
                    affectedUrls = pagesWithBrokenLinks.map { "${it.url} (تعداد خطای آدرس: ${it.brokenLinksCount})" }.take(5)
                )
            )
        }

        // C) Sitemap Check
        if (audit != null && audit.sitemapsCount == 0) {
            items.add(
                RoadmapItem(
                    id = "tech_sitemap",
                    category = RoadmapCategory.TECHNICAL,
                    title = "عدم ردیابی یا نقص در راه‌اندازی فایل نقشه سایت XML",
                    whatIsWrong = "فایل نقشه‌ راهنما موسوم به Sitemap.xml در ریشه وب‌سایت یافت نشد یا در گوگل سرچ کنسول تعریف نشده است.",
                    whyItMatters = "بدون نقشه سایت، وب‌سایت شما ساختاری گنگ دارد. شناسایی مطالب نوین و کاتالوگ محصولات جدید توسط موتور خزش گوگل با تأخیر فراوان روبه‌رو می‌شود.",
                    howToFix = "یک افزونه سئو (نظیر Yoast، RankMath یا پکیج‌های اختصاصی فریمورک خود) نصب کنید تا به صورت دوره‌ای بروزرسانی نقشه سایت را انجام دهد. سپس آدرس آن را در پنل Search Console و فایل robots.txt درج نمایید.",
                    estimatedImpact = OpportunityImpact.MEDIUM,
                    affectedPagesCount = 1,
                    priorityScore = 82
                )
            )
        }

        // D) Robots.txt Check
        if (audit != null && !audit.hasRobotsTxt) {
            items.add(
                RoadmapItem(
                    id = "tech_robots",
                    category = RoadmapCategory.TECHNICAL,
                    title = "فقدان فایل ضروری پیکربندی ربات‌ها robots.txt",
                    whatIsWrong = "فایل راهنمای کنترل خزش خزنده ها (robots.txt) در ریشه سرور قرار نگرفته و یا دسترسی به آن با خطای ۴۰۴ مواجه است.",
                    whyItMatters = "ربات گوگل هر بار قبل از ارزیابی هر سکتور، robots.txt را می‌خواند. نبود آن موجب اتلاف شدید منابع سرور شما و خزش بخش‌های ناخواسته مانند ادمین یا سبد خرید می‌شود.",
                    howToFix = "یک فایل متنی ساده به نام robots.txt ایجاد کنید. آدرس نقشه سایت (Sitemap) را در انتها اضافه نمایید و دسترسی ربات‌ها به پوشه‌های تکرار شونده و حساس سیستمی را Disallow کنید.",
                    estimatedImpact = OpportunityImpact.MEDIUM,
                    affectedPagesCount = 1,
                    priorityScore = 80
                )
            )
        }


        // 2. CONTENT SEO CATEGORY RECOMMENDATIONS
        // A) Thin Content
        val thinPages = pages.filter { it.wordCount > 0 && it.wordCount < 300 && it.indexable }
        if (thinPages.isNotEmpty()) {
            items.add(
                RoadmapItem(
                    id = "content_thin",
                    category = RoadmapCategory.CONTENT,
                    title = "وجود صفحاتی با محتوای کوتاه یا بدون هم‌پوشانی معنایی (Thin Content)",
                    whatIsWrong = "تعداد ${thinPages.size} صفحه وجود دارند که حجم کلمات باارزش آن‌ها کمتر از ۳۰۰ کلمه است؛ این متون فاقد ارزش رقابتی بالایی هستند.",
                    whyItMatters = "الگوریتم‌های هسته گوگل به شدت نسبت به محتوای ضعیف یا کم‌حجم آلرژی دارند. این لندینگ‌ها نه تنها خود رتبه‌ای دریافت نمی‌کنند، بلکه بر نمره جامع کیفی کلیت دامنه اثر منفی دارند.",
                    howToFix = "با ادغام لندینگ‌های کوچک موازی، اضافه کردن تصاویر، جداول مقایسه‌ای و افزایش طول متن به حداقل ۷۰۰ واژه غنی، به محتوای خود عمق ببخشید و یا یو‌آر‌ال‌های زائد را نوفالو (Noindex) کنید.",
                    estimatedImpact = OpportunityImpact.HIGH,
                    affectedPagesCount = thinPages.size,
                    priorityScore = 91,
                    affectedUrls = thinPages.map { "${it.url} (تعداد کلمات: ${it.wordCount})" }.take(5)
                )
            )
        }

        // B) Missing Title
        val noTitlePages = pages.filter { it.title.isNullOrBlank() && it.statusCode == 200 }
        if (noTitlePages.isNotEmpty()) {
            items.add(
                RoadmapItem(
                    id = "content_no_title",
                    category = RoadmapCategory.CONTENT,
                    title = "فقدان تگ بهینه‌شده عنوان اصلی صفحات (Title Tags)",
                    whatIsWrong = "تعداد ${noTitlePages.size} صفحه با پاسخ موفق کد ۲۰۰ فاقد تگ عنوان مناسب <title> در بخش کدهای هدر خود هستند.",
                    whyItMatters = "تگ عنوان، اولین و قدرتمندترین المان برای تفکیک لندینگ‌ها در گوگل و تعیین‌کننده اصلی کلمه کلیدی رنکینگ است. نبود آن شانس ایندکس منطقی لندینگ را از بین می‌برد.",
                    howToFix = "سریعاً برای هر لندینگ شناسایی شده یک عنوان سئو فارسی جذاب، منحصربه‌فرد، حاوی کلمه کلیدی اصلی وب‌سایت با طول بین ۴۵ تا ۶۰ کاراکتر تنظیم کنید.",
                    estimatedImpact = OpportunityImpact.HIGH,
                    affectedPagesCount = noTitlePages.size,
                    priorityScore = 97,
                    affectedUrls = noTitlePages.map { it.url }.take(5)
                )
            )
        }

        // C) Missing H1
        val noH1Pages = pages.filter { it.headingsH1.isNullOrBlank() && it.indexable }
        if (noH1Pages.isNotEmpty()) {
            items.add(
                RoadmapItem(
                    id = "content_no_h1",
                    category = RoadmapCategory.CONTENT,
                    title = "عدم وجود یا پیکربندی تگ هدینگ اصلی H1 در مقاله",
                    whatIsWrong = "در پایش انجام شده، تعداد ${noH1Pages.size} لندینگ مهم فاقد مهم‌ترین ساختار درختی هدینگ متن یعنی تگ <h1> پیدا شدند.",
                    whyItMatters = "بدنه نوشتاری بدون تگ H1 درست مانند کتابی بدون نام فصل است. ربات‌های خزنده موتور جستجوگر در درک موضوع سلسله مراتبی متن دچار سردرگمی خواهند شد.",
                    howToFix = "انواع صفحات یا کدهای سیستم تولید مقاله قالب خود را به نحوی تغییر دهید که بخش تیتر اصلی متن به عنوان تنها تگ <h1> روی صفحه تنظیم شود.",
                    estimatedImpact = OpportunityImpact.HIGH,
                    affectedPagesCount = noH1Pages.size,
                    priorityScore = 90,
                    affectedUrls = noH1Pages.map { it.url }.take(5)
                )
            )
        }

        // D) Meta Description Missing
        val noDescPages = pages.filter { it.metaDescription.isNullOrBlank() && it.indexable }
        if (noDescPages.isNotEmpty()) {
            items.add(
                RoadmapItem(
                    id = "content_no_desc",
                    category = RoadmapCategory.CONTENT,
                    title = "یافت نشدن برچسب توضیحات کوتاه متا (Meta Descriptions)",
                    whatIsWrong = "تعداد ${noDescPages.size} صفحه مهم از وب‌سایت شما فاقد توضیحات خلاصه یا برچسب متا دیسکریپشن در بخش هد تگ خود هستند.",
                    whyItMatters = "کدهای متادیسکریپشن جذاب مستقیماً بر جلب چشم کاربر در نتایج اولیه گوگل و افزایش نرخ برنده شدن کلیک‌های اورگانیک (CTR) شما تاثیرگذار است.",
                    howToFix = "یک متادیسکریپشن شیوا و جذاب برای تمامی صفحات دارای نقص بنویسید که طول آن بین ۱۲۰ تا ۱۵۵ کاراکتر بوده و از عباراتی ترغیب‌کننده و کلمات کلیدی لندینگ استفاده کرده باشد.",
                    estimatedImpact = OpportunityImpact.MEDIUM,
                    affectedPagesCount = noDescPages.size,
                    priorityScore = 79,
                    affectedUrls = noDescPages.map { it.url }.take(5)
                )
            )
        }


        // 3. PERFORMANCE CATEGORY RECOMMENDATIONS
        // A) Slow Loading Pages
        val heavyPages = pages.filter { it.loadTimeMs > 1500 }
        if (heavyPages.isNotEmpty()) {
            items.add(
                RoadmapItem(
                    id = "perf_slow_pages",
                    category = RoadmapCategory.PERFORMANCE,
                    title = "بارگذاری بسیار طولانی و هدر دادن زمان لود کاربر",
                    whatIsWrong = "تعداد ${heavyPages.size} لندینگ مهم زمان لود اولیه‌شان بیش از ۱.۵ ثانیه طول می‌کشد که فراتر از استانداردهای بهینه Core Web Vitals است.",
                    whyItMatters = "سرعت لود، معیار وفاداری مشتری و یکی از پایه‌های کلیدی تجربه کاربری در الگوریتم تلفن همراه است. هر ۱ ثانیه تعلل در لود اولیه، تا ۲۰ درصد بانس ریت را تشدید می‌کند.",
                    howToFix = "فعالسازی جی‌زیپ (GZip/Brotli) روی وب‌سرور، تبدیل فرمت‌های گرافیکی JPEG/PNG به نسل جدید WebP، و حذف اسکریپت‌های مسدودکننده‌ی سنگین ثالث.",
                    estimatedImpact = OpportunityImpact.HIGH,
                    affectedPagesCount = heavyPages.size,
                    priorityScore = 88,
                    affectedUrls = heavyPages.map { "${it.url} (زمان لود: ${String.format(Locale.ROOT, "%.2f", it.loadTimeMs / 1000.0)} ثانیه)" }.take(5)
                )
            )
        }


        // 4. INTERNAL LINKING CATEGORY RECOMMENDATIONS
        // A) Orphan Pages
        val orphanPages = pages.filter { it.internalLinksCount == 0 && it.indexable && it.statusCode == 200 }
        if (orphanPages.isNotEmpty()) {
            items.add(
                RoadmapItem(
                    id = "linking_orphan",
                    category = RoadmapCategory.INTERNAL_LINKING,
                    title = "شناسایی صفحات مهم یتیم (بدون پیوند ورودی از سایر صفحات سایت)",
                    whatIsWrong = "تعداد ${orphanPages.size} صفحه در خزش کشف شده‌اند که متاسفانه هیچ لینک داخلی از دیگر بروشورها یا دسته‌بندی‌های سایت ندارند.",
                    whyItMatters = "صفحه بدون لینک ورودی (Orphan Page) عملا از نظر گوگل غیرمهم پنداشته می‌شود؛ پتانسیل رتبه‌گیری کلمات کلیدی به شدت از دست می‌رود و انتقال اعتبار و جریان بازدید مسدود می‌گردد.",
                    howToFix = "مقالات یا صفحات قدیمی‌تر و پربازدید پیوندهایی مرتبط به همراه انکور تکست‌های متناسب (Anchor Text) به این صفحات یتیم متصل کنید.",
                    estimatedImpact = OpportunityImpact.HIGH,
                    affectedPagesCount = orphanPages.size,
                    priorityScore = 86,
                    affectedUrls = orphanPages.map { it.url }.take(5)
                )
            )
        }


        // 5. SCHEMA CATEGORY RECOMMENDATIONS
        // A) Totally Missing Schema Markup
        val noSchemaPages = pages.filter { !it.hasSchema && it.indexable && it.statusCode == 200 }
        if (noSchemaPages.isNotEmpty()) {
            items.add(
                RoadmapItem(
                    id = "schema_missing",
                    category = RoadmapCategory.SCHEMA,
                    title = "عدم پیاده‌سازی همگانی یا بخشی کدهای نشانه‌گذاری معنایی اسکیما",
                    whatIsWrong = "تعداد ${noSchemaPages.size} لندینگ ایندکس‌شدنی بدون هیچ‌گونه ساختار کدهای اسکیما (Structured Data) ساخته شده‌اند.",
                    whyItMatters = "کدهای اسکیما به وب‌سایت شانس برخورداری از ریچ‌اسنیپت‌ها (شامل رتبه‌بندی پنج‌ستاره، چرخ فلک مقالات، کادرهای پرسش و پاسخ گوگل) و برجسته شدن در SERP را گارانتی خواهند کرد.",
                    howToFix = "افزودن اسکیمای پایه از جمله Organization و Breadcrumb به تمامی کدهای سیستم و نصب کدهای اسکیما لندینگ‌های تجاری شامل Product لایو و اسکیمای مقالات (Article Schema).",
                    estimatedImpact = OpportunityImpact.HIGH,
                    affectedPagesCount = noSchemaPages.size,
                    priorityScore = 84,
                    affectedUrls = noSchemaPages.map { it.url }.take(5)
                )
            )
        }

        // Sort by priorityScore descending
        val sortedItems = items.sortedByDescending { it.priorityScore }

        return ActionCenterRoadmap(
            clientId = clientId,
            totalItemsCount = sortedItems.size,
            technicalCount = sortedItems.count { it.category == RoadmapCategory.TECHNICAL },
            contentCount = sortedItems.count { it.category == RoadmapCategory.CONTENT },
            performanceCount = sortedItems.count { it.category == RoadmapCategory.PERFORMANCE },
            internalLinkingCount = sortedItems.count { it.category == RoadmapCategory.INTERNAL_LINKING },
            schemaCount = sortedItems.count { it.category == RoadmapCategory.SCHEMA },
            items = sortedItems
        )
    }

    private fun generateMockRoadmap(clientId: Int): ActionCenterRoadmap {
        val items = listOf(
            RoadmapItem(
                id = "mock_tech_ssl",
                category = RoadmapCategory.TECHNICAL,
                title = "عدم هدایت تمام آدرس‌ها به نسخه ایمن HTTPS سایت",
                whatIsWrong = "صفحات متعددی در بررسی مقدماتی بدون ریدایرکت ۳۰۱ روی پروتکل ناامن HTTP دریافت پاسخ موفق می‌کنند.",
                whyItMatters = "پروتکل HTTPS از شاخص‌های تضمین‌شده‌ی فیدبک سئو گوگل بوده و عدم توجه به آن بستر بی اعتمادی کاربران را تسریع می‌بخشد.",
                howToFix = "دستور شبیه‌سازی ریدایرکت ۳۰۱ دایمی در ریشه ادمین هاست را اعمال و گواهینامه معتبر SSL نصب نمایید.",
                estimatedImpact = OpportunityImpact.HIGH,
                affectedPagesCount = 12,
                priorityScore = 95,
                affectedUrls = listOf("http://yourdomain.com", "http://yourdomain.com/blog", "http://yourdomain.com/shop")
            ),
            RoadmapItem(
                id = "mock_content_title",
                category = RoadmapCategory.CONTENT,
                title = "تایتل تکراری یا فاقد جذابیت سئو در برخی مقالات",
                whatIsWrong = "تعداد ۸ صفحه مجزا عنوان مشابه با صفحات دیگر و یا با طول بسیار ناچیز کمتر از ۱۵ کاراکتر دارند.",
                whyItMatters = "عناوین مکرر موتورهای جستجو را در پیدا کردن صفحه کاندیدای اصلی با بحران رقابت داخلی (Cannibalization) مواجه می‌کنند.",
                howToFix = "عناوین هر صفحه را به صورت یکتا نگاشته و نام تجاری وبسایت را با پپوند جذاب به انتهای آن متصل کنید.",
                estimatedImpact = OpportunityImpact.HIGH,
                affectedPagesCount = 8,
                priorityScore = 90,
                affectedUrls = listOf("https://yourdomain.com/pricing", "https://yourdomain.com/category-one", "https://yourdomain.com/services")
            ),
            RoadmapItem(
                id = "mock_perf_lcp",
                category = RoadmapCategory.PERFORMANCE,
                title = "سرعت نامناسب ترسیم اولین محتواهای تصویری سرور (Score CWV)",
                whatIsWrong = "زمان لایف اسکوپ بارگذاری برخی از لندینگ‌های غنی تصویری بالای ۲.۲ ثانیه است.",
                whyItMatters = "زمان تاخیر لود طبق فاکتور مستقیم سرعت Core Web Vitals نرخ رتبه نهایی موبایل وب‌سایت شما را پله به پله تخریب می‌کند.",
                howToFix = "تصاویر را به کمک ابزار فشرده‌سازی به نسل مدرن WebP تبدیل کنید و کدهای سی‌اس‌اس فرعی را مینیمایز نمایید.",
                estimatedImpact = OpportunityImpact.HIGH,
                affectedPagesCount = 5,
                priorityScore = 89,
                affectedUrls = listOf("https://yourdomain.com/homepage", "https://yourdomain.com/product/12", "https://yourdomain.com/gallery")
            ),
            RoadmapItem(
                id = "mock_link_orphan",
                category = RoadmapCategory.INTERNAL_LINKING,
                title = "وجود صفحات متمایز بدون هرگونه جریان پیونددهی و لینک‌سازی داخلی",
                whatIsWrong = "تعداد ۴ صفحه یتیم یا دارای ورودی صفر شناسایی شده است که هیچ لینکی از طرف صفحات مجاور وب‌سایت ندارند.",
                whyItMatters = "خزندگان گوگل ارزش صفحات بدون پیوند داخلی را صفر در نظر گرفته و به مرور آن‌ها را از شاخه ایندکس خود حذف می‌نمایند.",
                howToFix = "در صفحات شاخص قدیمی‌تر، یک پاراگراف کوتاه حاوی کلمه کلیدی بسازید و به این صفحات یتیم متصل کنید.",
                estimatedImpact = OpportunityImpact.MEDIUM,
                affectedPagesCount = 4,
                priorityScore = 80,
                affectedUrls = listOf("https://yourdomain.com/blog/new-feature-update", "https://yourdomain.com/privacy-policy")
            ),
            RoadmapItem(
                id = "mock_schema_all",
                category = RoadmapCategory.SCHEMA,
                title = "نبود کامل ساختارهای کدهای اسکیما (Structured Schema)",
                whatIsWrong = "بخش عمده‌ای از سایت فاقد ابزار ترجمه تفاهم روبات‌ها یعنی کدهای استاندارد نقشه اسکیما هستند.",
                whyItMatters = "کدهای اسکیما به شکل مستقیم چیدمان زیبایی در نتایج جستجوی کلمات کلیدی برای شما خلق خواهند کرد و شانس کلیک اورگانیک را افزایش می‌دهند.",
                howToFix = "کدهای اسکیمای استاندارد FAQPage یا Article در سورس وبسایت اعمال کرده و در پایش معتبر گوگل سرچ کنسول تست کنید.",
                estimatedImpact = OpportunityImpact.MEDIUM,
                affectedPagesCount = 14,
                priorityScore = 75,
                affectedUrls = listOf("https://yourdomain.com/shop", "https://yourdomain.com/blog")
            )
        )

        return ActionCenterRoadmap(
            clientId = clientId,
            totalItemsCount = items.size,
            technicalCount = 1,
            contentCount = 1,
            performanceCount = 1,
            internalLinkingCount = 1,
            schemaCount = 1,
            items = items
        )
    }
}
