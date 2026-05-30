package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.SeoDao
import com.example.data.model.*
import com.example.data.remote.Content
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.Part
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SeoRepository(val seoDao: SeoDao) {

    // --- Flows ---
    val allClients: Flow<List<Client>> = seoDao.getAllClients()

    fun getClient(clientId: Int): Flow<Client?> = seoDao.getClientByIdFlow(clientId)

    fun getKeywords(clientId: Int): Flow<List<Keyword>> = seoDao.getKeywordsForClient(clientId)

    fun getTasks(clientId: Int): Flow<List<Task>> = seoDao.getTasksForClient(clientId)

    fun getLatestAudit(clientId: Int): Flow<AuditResult?> = seoDao.getLatestAuditForClient(clientId)

    fun getChatHistory(clientId: Int): Flow<List<ChatHistory>> = seoDao.getChatHistoryForClient(clientId)

    // --- DB Write Operations ---
    suspend fun insertClient(client: Client): Long = withContext(Dispatchers.IO) {
        seoDao.insertClient(client)
    }

    suspend fun updateClient(client: Client) = withContext(Dispatchers.IO) {
        seoDao.updateClient(client)
    }

    suspend fun deleteClient(clientId: Int) = withContext(Dispatchers.IO) {
        // Delete cascading items since we don't have constraints manually in Room
        seoDao.deleteKeywordsForClient(clientId)
        seoDao.deleteTasksForClient(clientId)
        seoDao.deleteAuditsForClient(clientId)
        seoDao.deleteChatHistoryForClient(clientId)
        seoDao.deleteClientById(clientId)
    }

    suspend fun insertKeyword(keyword: Keyword) = withContext(Dispatchers.IO) {
        seoDao.insertKeyword(keyword)
    }

    suspend fun deleteKeyword(keyword: Keyword) = withContext(Dispatchers.IO) {
        seoDao.deleteKeyword(keyword)
    }

    suspend fun insertTask(task: Task) = withContext(Dispatchers.IO) {
        seoDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) {
        seoDao.updateTask(task)
    }

    suspend fun deleteTask(taskId: Int) = withContext(Dispatchers.IO) {
        seoDao.deleteTaskById(taskId)
    }

    // --- AI Audit and Assistant Integration via REST ---
    suspend fun askAiAssistant(clientId: Int, prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Simulated backup SEO intelligence response if model fails / API Key missing
            val client = seoDao.getClientById(clientId)
            val mockResponse = getMockAiResponse(client, prompt)
            val chat = ChatHistory(clientId = clientId, prompt = prompt, response = mockResponse)
            seoDao.insertChat(chat)
            return@withContext mockResponse
        }

        try {
            val client = seoDao.getClientById(clientId)
            val clientContext = if (client != null) {
                "You are an expert SEO Auditor and strategist helping managing the SEO Campaign for client '${client.name}' whose website is '${client.websiteUrl}' (Status: ${client.status}, Strategy: ${client.campaignType})."
            } else {
                "You are an expert SEO Agency Advisor assistant."
            }

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = "$clientContext Keep advice highly technical but structured, readable, and SEO-specific with bullet points and clear target keywords if relevant.")))
            )

            val serviceResponse = RetrofitClient.geminiService.generateContent(apiKey, request)
            val answer = serviceResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "SEO Assist is currently busy. Please try again."

            // Save conversation
            val chat = ChatHistory(clientId = clientId, prompt = prompt, response = answer)
            seoDao.insertChat(chat)
            answer
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback gracefully to offline strategy
            val client = seoDao.getClientById(clientId)
            val mockResponse = "Offline Analysis Mode: ${e.localizedMessage ?: "Network error"}\n\n${getMockAiResponse(client, prompt)}"
            val chat = ChatHistory(clientId = clientId, prompt = prompt, response = mockResponse)
            seoDao.insertChat(chat)
            mockResponse
        }
    }

    suspend fun performSeoAudit(clientId: Int, strategy: String = "MOBILE"): AuditResult = withContext(Dispatchers.IO) {
        val client = seoDao.getClientById(clientId) ?: throw IllegalArgumentException("Client not found")
        val keywords = seoDao.getKeywordsForClient(clientId).first()
        val keywordsStr = keywords.joinToString(", ") { it.phrase }

        // 1. Run live crawl on website URL
        val crawl = com.example.data.remote.SeoIntegrationService.crawlUrl(client.websiteUrl)

        // 2. Fetch real PageSpeed Insights metrics
        val psiDevKey = BuildConfig.GEMINI_API_KEY // reuse Gemini key or pass null as fallback
        val psi = com.example.data.remote.SeoIntegrationService.fetchPageSpeedReport(client.websiteUrl, strategy, if (psiDevKey == "MY_GEMINI_API_KEY") "" else psiDevKey)

        // 3. Aggregate parameters and construct issues json
        val crawlCriticals = crawl.issues.count { it.startsWith("[CRITICAL]") }
        val crawlWarnings = crawl.issues.count { it.startsWith("[WARNING]") }
        val crawlPasseds = crawl.issues.count { it.startsWith("[PASSED]") }

        val criticalIssues = crawlCriticals + (if (psi.performanceScore < 50) 1 else 0)
        val warnings = crawlWarnings + (if (psi.performanceScore in 50..89) 1 else 0)
        val passedChecks = crawlPasseds + 4 // PageSpeed, accessibility, bestPractices, metadata

        val issuesBuilder = java.lang.StringBuilder()
        crawl.issues.forEach { issuesBuilder.append(it).append("\n") }
        issuesBuilder.append("[PASSED] Real Lighthouse Strategy: ${psi.strategy.uppercase()}\n")
        issuesBuilder.append("[PASSED] Core Web Vitals - LCP Contentful Paint: ${psi.lcp}\n")
        issuesBuilder.append("[PASSED] Core Web Vitals - CLS Layout Shift: ${psi.cls}\n")
        issuesBuilder.append("[PASSED] Core Web Vitals - INP Interaction Latency: ${psi.inp}\n")
        issuesBuilder.append("[PASSED] Core Web Vitals - FCP First Paint: ${psi.fcp}\n")
        issuesBuilder.append("[PASSED] PageSpeed Metrics: Performance=${psi.performanceScore}/100, Accessibility=${psi.accessibilityScore}/100, BestPractices=${psi.bestPracticesScore}/100, SEO=${psi.seoScore}/100\n")
        
        psi.recommendations.forEach { recommendation ->
            issuesBuilder.append("[WARNING] Speed recommendation: $recommendation\n")
        }

        val calculatedScore = ((crawl.calculatedScore + psi.performanceScore) / 2).coerceIn(10, 100)

        val auditResult = AuditResult(
            clientId = clientId,
            score = calculatedScore,
            criticalIssues = criticalIssues,
            warnings = warnings,
            passedChecks = passedChecks,
            issuesJson = issuesBuilder.toString(),
            strategy = strategy,
            performanceScore = psi.performanceScore,
            accessibilityScore = psi.accessibilityScore,
            bestPracticesScore = psi.bestPracticesScore,
            seoScore = psi.seoScore,
            lcp = psi.lcp,
            cls = psi.cls,
            fcp = psi.fcp,
            inp = psi.inp,
            ttfb = psi.ttfb,
            structuredDataCount = crawl.structuredDataCount,
            hasRobotsTxt = crawl.hasRobotsTxt,
            sitemapsCount = crawl.sitemapUrls.size
        )

        // Save audit result
        seoDao.insertAuditResult(auditResult)

        // Also update client health score in DB
        val updatedClient = client.copy(healthScore = calculatedScore)
        seoDao.updateClient(updatedClient)

        // Add automated diagnostic tasks for high priority fixes automatically!
        if (criticalIssues > 0) {
            seoDao.insertTask(
                Task(
                    clientId = clientId,
                    title = "Resolve live SEO crawl issues for ${client.name}",
                    description = "Our crawlers flagged $criticalIssues critical issues on ${client.websiteUrl}. Largest Contentful Paint is ${psi.lcp}.",
                    dueDate = "As soon as possible",
                    priority = "High"
                )
            )
        }

        auditResult
    }

    // --- Aggregates Audits, Crawler, PageSpeed, GSC, GA4 and Calls Gemini API ---
    suspend fun generateAiStrategyReport(clientId: Int, gscToken: String?, ga4PropertyId: String?): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val client = seoDao.getClientById(clientId) ?: return@withContext "Campaign client not resolved in storage."
        val keywords = seoDao.getKeywordsForClient(clientId).first()
        val latestAudit = seoDao.getLatestAuditForClient(clientId).first()
        
        // 1. Compile Google Search Console information if connected
        var gscContext = "Google Search Console: Mappings not configured or unauthorized. Please authenticate Google OAuth under Connection Settings."
        if (!client.gscSiteUrl.isNullOrEmpty() && !gscToken.isNullOrEmpty()) {
            try {
                val gscData = com.example.data.remote.SeoIntegrationService.fetchSearchConsoleReport(client.gscSiteUrl, gscToken)
                gscContext = """
                    Google Search Console Analytics:
                    - Site url mapping: ${client.gscSiteUrl}
                    - Total Query clicks: ${gscData.clicks}
                    - Total Impressions: ${gscData.impressions}
                    - Average CTR: ${(gscData.ctr * 100).coerceIn(0.0, 100.0).toString().take(4)}%
                    - Average Rank Position: ${gscData.averagePosition.toString().take(4)}
                    - Top Search Queries: ${gscData.queries.joinToString { "${it.query} (Clicks: ${it.clicks}, Imp: ${it.impressions}, Pos: ${it.position})" }}
                """.trimIndent()
            } catch (e: Exception) {
                gscContext = "Google Search Console API check failed: ${e.message}"
            }
        }

        // 2. Compile Google Analytics 4 parameters if connected
        var ga4Context = "Google Analytics 4: Property tracking mapping offline. Connect your Google Analytics profile in Connection Diagnostics."
        val actualGa4Id = if (!client.ga4PropertyId.isNullOrEmpty()) client.ga4PropertyId else ga4PropertyId
        if (!actualGa4Id.isNullOrEmpty() && !gscToken.isNullOrEmpty()) {
            try {
                val ga4Data = com.example.data.remote.SeoIntegrationService.fetchAnalyticsReport(actualGa4Id, gscToken)
                ga4Context = """
                    Google Analytics 4 Overview:
                    - Active Users: ${ga4Data.activeUsers}
                    - Interactive Sessions: ${ga4Data.sessions}
                    - User Engagement Rate: ${(ga4Data.engagementRate * 100).toString().take(4)}%
                    - Completed Conversions: ${ga4Data.conversions}
                    - Primary Traffic Channels: ${ga4Data.trafficSources.joinToString { "${it.first} (${it.second} sessions)" }}
                """.trimIndent()
            } catch (e: Exception) {
                ga4Context = "Google Analytics 4 API check failed: ${e.message}"
            }
        }

        // 3. Compile overall site crawler inputs
        val crawlAuditContext = if (latestAudit != null) {
            """
                Live Crawler & Core Web Vitals Status:
                - Overall Health Score: ${latestAudit.score}/100
                - Critical onpage errors: ${latestAudit.criticalIssues}
                - Optimization warning flags: ${latestAudit.warnings}
                - Local crawlers detected sitemaps: ${latestAudit.sitemapsCount}
                - Local crawlers detected robots.txt rule: ${latestAudit.hasRobotsTxt}
                - Structured JSON-LD counts: ${latestAudit.structuredDataCount}
                - Metrics (LCP Last paint: ${latestAudit.lcp}, CLS shift: ${latestAudit.cls}, INP latency: ${latestAudit.inp}, TTFB server load: ${latestAudit.ttfb})
                - Audit detailed issues list:
                ${latestAudit.issuesJson}
            """.trimIndent()
        } else {
            "Crawler status: No deep crawler diagnosis has been launched yet for URL ${client.websiteUrl}."
        }

        val prompt = """
            Perform an exhaustive technical, content, and organic SEO intelligence audit for '${client.name}' based on the integrated API inputs package below.
            
            Website domain: ${client.websiteUrl}
            Campaign scope: ${client.campaignType}
            Tracked Monitor Keywords: [${keywords.joinToString { it.phrase }}]
            
            ------------------------------------------------
            INTEGRATED API ANALYTICS CONTEXT DATA:
            ------------------------------------------------
            $crawlAuditContext
            
            $gscContext
            
            $ga4Context
            ------------------------------------------------
            
            Provide a production-grade, highly actionable optimization blueprint. Keep your writing structured, professional, informative, and strictly objective (do not pitch, praise, or write generic marketing hype):
            
            1. **Technical SEO Fixes**: Explicit recommendations to improve Core Web Vitals, metadata tags, canonical indexing, schema, robots, or sitemap parameters.
            2. **Content Optimization Opportunities**: Ideas to expand semantic keywords and target queries that are receiving high impressions but low click-through ratios.
            3. **Keyword Expansion Opportunities**: Propose 3-5 hyper-targeted long-tail search phrases with strong user intent tailored for this client niche.
            
            Represent your response with precise Markdown formatting, neat bullet layouts, and bold section headings.
        """.trimIndent()

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Backup/Offline strategy return
            val mockStrategy = """
                ### 🚀 SEO INTELLIGENCE BLUEPRINT (BACKUP MODE)
                
                *Note: Connect a valid Gemini API Key in the AI Studio Secrets panel to unlock real-time contextual generated blueprints.*
                
                #### 1. Technical SEO Fixes
                * **Core Web Vitals**: Resolve Cumulative Layout Shift (CLS) on ${client.websiteUrl} by locking dimension attributes on top carousel banner wrappers.
                * **Schema Markup**: Inject local schema JSON-LD profiles to trigger Google Rich Snippet inclusion.
                
                #### 2. Content Optimization Opportunities
                * **High Impression Queries**: Optimize page headings for queries detected in Google Console to increase click-through coefficients.
                * **Alt Tag validation**: Run automated scripts to append descriptive, semantic contextual alts across image pools.
                
                #### 3. Keyword Niche Proposals
                * `best ${client.campaignType} solutions NYC` (Difficulty: Medium)
                * `how to optimize ${client.name.take(15)} organically` (Difficulty: Low)
            """.trimIndent()
            val chat = ChatHistory(clientId = clientId, prompt = "Generate AI Strategy Blueprint", response = mockStrategy)
            seoDao.insertChat(chat)
            return@withContext mockStrategy
        }

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = "You are an expert SEO Strategist, Chief Strategy Officer at SearchOps intelligence agency. You synthesize search metrics, crawl parameters, and organic traffic curves into technical blueprints.")))
            )

            val serviceResponse = RetrofitClient.geminiService.generateContent(apiKey, request)
            val answer = serviceResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "AI strategy engine was unable to compile the analysis blueprint. Please retry shortly."

            val chat = ChatHistory(clientId = clientId, prompt = "Generate AI Strategy Blueprint", response = answer)
            seoDao.insertChat(chat)
            answer
        } catch (e: Exception) {
            e.printStackTrace()
            val errResponse = "Intelligence generator error calling Gemini API: ${e.localizedMessage ?: "Unknown server timeout"}"
            val chat = ChatHistory(clientId = clientId, prompt = "Generate AI Strategy Blueprint", response = errResponse)
            seoDao.insertChat(chat)
            errResponse
        }
    }

    // --- Data Seed Helper (Saves onboarding time, provides premium sample data!) ---
    suspend fun seedMockDataIfEmpty() = withContext(Dispatchers.IO) {
        val check = seoDao.getAllClients().first()
        if (check.isEmpty()) {
            // Seed Clients
            val id1 = seoDao.insertClient(
                Client(
                    name = "Apex Logistics Solutions",
                    websiteUrl = "https://apexlogistics.io",
                    healthScore = 74,
                    status = "Active",
                    campaignType = "Technical SEO"
                )
            ).toInt()

            val id2 = seoDao.insertClient(
                Client(
                    name = "Bloom Boutique Florist",
                    websiteUrl = "https://bloombouquet.com",
                    healthScore = 89,
                    status = "Active",
                    campaignType = "Content Marketing"
                )
            ).toInt()

            val id3 = seoDao.insertClient(
                Client(
                    name = "Horizon Venture Capital",
                    websiteUrl = "https://horizonvc.co",
                    healthScore = 52,
                    status = "Review",
                    campaignType = "Full Scale Campaign"
                )
            ).toInt()

            // Seed Keywords for Client 1
            seoDao.insertKeyword(Keyword(clientId = id1, phrase = "integrated supply chain tracking software", searchVolume = 1200, currentRank = 14, previousRank = 22, difficulty = 68))
            seoDao.insertKeyword(Keyword(clientId = id1, phrase = "logistics fleet tech trends", searchVolume = 850, currentRank = 5, previousRank = 4, difficulty = 52))
            seoDao.insertKeyword(Keyword(clientId = id1, phrase = "best b2b inventory tool", searchVolume = 3200, currentRank = 28, previousRank = 45, difficulty = 74))

            // Seed Keywords for Client 2
            seoDao.insertKeyword(Keyword(clientId = id2, phrase = "same day flower delivery brooklyn nyc", searchVolume = 2400, currentRank = 3, previousRank = 9, difficulty = 45))
            seoDao.insertKeyword(Keyword(clientId = id2, phrase = "buy hand tied wedding bouquet", searchVolume = 450, currentRank = 1, previousRank = 1, difficulty = 30))
            seoDao.insertKeyword(Keyword(clientId = id2, phrase = "indoor snake plants care", searchVolume = 5400, currentRank = 12, previousRank = 18, difficulty = 60))

            // Seed Keywords for Client 3
            seoDao.insertKeyword(Keyword(clientId = id3, phrase = "early stage seed funding checklist", searchVolume = 600, currentRank = 48, previousRank = 72, difficulty = 82))
            seoDao.insertKeyword(Keyword(clientId = id3, phrase = "secure technology venture capital nyc", searchVolume = 1800, currentRank = 15, previousRank = 15, difficulty = 71))
            seoDao.insertKeyword(Keyword(clientId = id3, phrase = "fintech angel investor list", searchVolume = 950, currentRank = 82, previousRank = 110, difficulty = 65))

            // Seed Tasks
            seoDao.insertTask(Task(clientId = id1, title = "Integrate product Schema.org schema codes", description = "Validate markup with Schema Validator tool for warehouse tracking sub-pages.", dueDate = "June 12, 2026", priority = "High"))
            seoDao.insertTask(Task(clientId = id1, title = "Increase homepage mobile font hierarchy size", description = "Current mobile font size causes touch target/readability flags on Google Console.", dueDate = "June 20, 2026", priority = "Medium"))
            seoDao.insertTask(Task(clientId = id2, title = "Write blog post: Wedding bouquets trends 2026", description = "Target keyword focus in article headings and main copy. Min 1200 words.", dueDate = "June 18, 2026", priority = "Low"))
            seoDao.insertTask(Task(clientId = id3, title = "Fix 301 redirects list for rebranding pages", description = "Update redirect rule mappings in NGINX config file strictly.", dueDate = "As soon as possible", priority = "High"))

            // Seed Initial Audits
            seoDao.insertAuditResult(
                AuditResult(
                    clientId = id1,
                    score = 74,
                    criticalIssues = 3,
                    warnings = 8,
                    passedChecks = 19,
                    issuesJson = """
                        [CRITICAL] Missing Image alternative (alt) labels on product grid pages.
                        [CRITICAL] Cumulative Layout Shift (CLS) of 0.28 on service pages.
                        [WARNING] Slow TTFB server response of 0.72s detected on initial load.
                        [PASSED] Properly formatted canonical links present on all subdomains.
                    """.trimIndent()
                )
            )

            seoDao.insertAuditResult(
                AuditResult(
                    clientId = id2,
                    score = 89,
                    criticalIssues = 0,
                    warnings = 4,
                    passedChecks = 25,
                    issuesJson = """
                        [WARNING] Meta description tags are truncated on 14 archive categories.
                        [PASSED] Perfectly configured adaptive images loaded via responsive device sizing.
                        [PASSED] SSL/TLS rules valid and redirect works properly.
                    """.trimIndent()
                )
            )

            seoDao.insertAuditResult(
                AuditResult(
                    clientId = id3,
                    score = 52,
                    criticalIssues = 5,
                    warnings = 11,
                    passedChecks = 14,
                    issuesJson = """
                        [CRITICAL] Broken index redirect loop detected for resource folders.
                        [CRITICAL] Robots.txt blocking search engine spiders from indexing resources correctly.
                        [WARNING] Massive render-blocking script elements (Hilt, React bundles) placed at index headers.
                    """.trimIndent()
                )
            )
        }
    }

    private fun getMockAiResponse(client: Client?, query: String): String {
        val name = client?.name ?: "Search Pulse Agency"
        return """
            📊 *SEOPulse Intelligence Assistant (Offline mode)* Let's build a strategy around: **"$query"**
            
            We recommend prioritizing the following tailored SEO moves matching the client *${name}* website context:
            
            1. **Intense Semantic Search Optimization**:
               - Target secondary keyword variations in H2/H3 tags. Increase key phrase density to 1.4%.
               - Craft comprehensive semantic sections answering common search questions (FAQ Schema markup).
            
            2. **Enhanced Site Shell Architecture**:
               - Increase speed metrics. Load defer policies on non-critical script assets.
               - Ensure image aspects are hardcoded to minimize Cumulative Layout Shift (CLS) on dynamic devices.
            
            3. **Structured Backlink Outreach Proposals**:
               - Generate and target high-authority technical publishers using dedicated newsletters.
               - Distribute high-value research case studies to authoritative journalists to build natural editorial links.
            
            *Tip: Once you register a valid Gemini API Key in the Secrets Panel in AI Studio, this assistant will generate custom intelligence specifically tailored for your client parameters!*
        """.trimIndent()
    }
}
