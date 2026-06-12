package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.SeoRepository
import com.example.data.remote.ContentAuditEngine
import com.example.data.remote.WebsiteContentAuditReport
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class SeoViewModel(
    application: Application,
    private val repository: SeoRepository,
    private val authRepository: AuthRepository
) : AndroidViewModel(application) {

    // --- SharedPreferences Persistence for Onboarding & Theme ---
    private val prefs = application.getSharedPreferences("searchops_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "System") ?: "System")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // --- Firebase Auth State Flows ---
    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser)

    val isLoggedIn: StateFlow<Boolean> = currentUser.map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.isLoggedIn())

    val userName: StateFlow<String> = currentUser.map { it?.displayName ?: "SearchOps Executive" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser?.displayName ?: "SearchOps Executive")

    val userEmail: StateFlow<String> = currentUser.map { it?.email ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser?.email ?: "")

    val userPhone: StateFlow<String> = currentUser.map { it?.phoneNumber ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser?.phoneNumber ?: "")

    // Phone OTP Verification States
    private val _verificationId = MutableStateFlow<String?>(null)
    val verificationId: StateFlow<String?> = _verificationId.asStateFlow()

    private val _phoneAuthError = MutableStateFlow<String?>(null)
    val phoneAuthError: StateFlow<String?> = _phoneAuthError.asStateFlow()

    private val _verificationInProgress = MutableStateFlow(false)
    val verificationInProgress: StateFlow<Boolean> = _verificationInProgress.asStateFlow()

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setVerificationId(id: String?) {
        _verificationId.value = id
    }

    fun setPhoneAuthError(error: String?) {
        _phoneAuthError.value = error
    }

    fun setVerificationInProgress(inProgress: Boolean) {
        _verificationInProgress.value = inProgress
    }

    // --- Firebase Authentication Methods ---
    fun signInWithGoogleToken(idToken: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.signInWithGoogle(idToken)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage)
            }
        }
    }

    fun startPhoneVerification(
        phoneNumber: String,
        activity: android.app.Activity,
        callbacks: com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        _verificationInProgress.value = true
        _phoneAuthError.value = null
        authRepository.startPhoneVerification(phoneNumber, activity, callbacks)
    }

    fun signInWithOtp(code: String, onComplete: (Boolean, String?) -> Unit) {
        val verificationIdVal = _verificationId.value
        if (verificationIdVal == null) {
            onComplete(false, "Verification ID expired or invalid. Please request a new OTP.")
            return
        }
        viewModelScope.launch {
            try {
                val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationIdVal, code)
                authRepository.signInWithPhoneCredential(credential)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Invalid verification code entered.")
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }

    // --- Loading indicators ---
    private val _isSeeding = MutableStateFlow(false)
    val isSeeding: StateFlow<Boolean> = _isSeeding.asStateFlow()

    private val _isAuditing = MutableStateFlow(false)
    val isAuditing: StateFlow<Boolean> = _isAuditing.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- Active states ---
    private val _selectedClientId = MutableStateFlow<Int?>(null)
    val selectedClientId: StateFlow<Int?> = _selectedClientId.asStateFlow()

    // --- Flows from DB ---
    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedClient: StateFlow<Client?> = _selectedClientId
        .flatMapLatest { id ->
            if (id != null) repository.getClient(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedClientKeywords: StateFlow<List<Keyword>> = _selectedClientId
        .flatMapLatest { id ->
            if (id != null) repository.getKeywords(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedClientTasks: StateFlow<List<Task>> = _selectedClientId
        .flatMapLatest { id ->
            if (id != null) repository.getTasks(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedClientAudit: StateFlow<AuditResult?> = _selectedClientId
        .flatMapLatest { id ->
            if (id != null) repository.getLatestAudit(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedClientChat: StateFlow<List<ChatHistory>> = _selectedClientId
        .flatMapLatest { id ->
            if (id != null) repository.getChatHistory(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedClientCrawledPages: StateFlow<List<CrawledPage>> = _selectedClientId
        .flatMapLatest { id ->
            if (id != null) repository.getCrawledPages(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedClientContentAuditReport: StateFlow<WebsiteContentAuditReport?> = combine(
        selectedClientCrawledPages,
        selectedClientKeywords,
        _selectedClientId
    ) { pages, keywords, id ->
        if (id != null && pages.isNotEmpty()) {
            ContentAuditEngine.analyze(id, pages, keywords)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedClientLinkingAnalysis: StateFlow<InternalLinkingAnalysis?> = combine(
        selectedClientCrawledPages,
        selectedClient,
        _selectedClientId
    ) { pages, client, id ->
        if (id != null && client != null && pages.isNotEmpty()) {
            InternalLinkingAnalyzer.analyze(id, pages, client.websiteUrl)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedClientIndexabilityReport: StateFlow<IndexabilityReport?> = combine(
        selectedClientCrawledPages,
        selectedClient,
        _selectedClientId
    ) { pages, client, id ->
        if (id != null && client != null && pages.isNotEmpty()) {
            IndexabilityAnalyzer.analyze(id, pages, client.websiteUrl)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedClientOpportunityReport: StateFlow<OpportunityReport?> = combine(
        selectedClientCrawledPages,
        _selectedClientId
    ) { pages, id ->
        if (id != null && pages.isNotEmpty()) {
            OpportunityAnalyzer.analyze(id, pages)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedClientRoadmap: StateFlow<ActionCenterRoadmap?> = combine(
        selectedClientCrawledPages,
        selectedClientAudit,
        _selectedClientId
    ) { pages, audit, id ->
        if (id != null) {
            ActionCenterRoadmapAnalyzer.analyze(id, pages, audit)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedClientCompetitors: StateFlow<List<Competitor>> = _selectedClientId
        .flatMapLatest { id ->
            if (id != null) repository.getCompetitors(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Global Stats ---
    val globalStats: StateFlow<GlobalStats> = clients.combine(repository.seoDao.getAllTasks()) { clientList, allTasks ->
        val totalClients = clientList.size
        val avgHealth = if (totalClients > 0) clientList.map { it.healthScore }.average().toInt() else 0
        val pendingTasks = allTasks.count { !it.isCompleted }
        
        // Count statuses
        val activeCount = clientList.count { it.status == "Active" }
        val reviewCount = clientList.count { it.status == "Review" }

        GlobalStats(
            totalClients = totalClients,
            averageHealthScore = avgHealth,
            pendingTasksCount = pendingTasks,
            activeCampaignsCount = activeCount,
            reviewCampaignsCount = reviewCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GlobalStats())

    init {
        // Trigger DB seeding immediately on load to offer premium sandbox data!
        seedInitialSandboxData()
    }

    fun seedInitialSandboxData() {
        viewModelScope.launch {
            _isSeeding.value = true
            try {
                repository.seedMockDataIfEmpty()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to seed sandbox data: ${e.message}"
            } finally {
                _isSeeding.value = false
            }
        }
    }

    fun selectClient(clientId: Int) {
        _selectedClientId.value = clientId
        fetchGoogleReportsForClient(clientId)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // --- Client Operations ---
    fun addClient(name: String, websiteUrl: String, campaignType: String, status: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val newClient = Client(
                    name = name,
                    websiteUrl = websiteUrl.let { url ->
                        if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                    },
                    campaignType = campaignType,
                    status = status,
                    healthScore = 70 // default health score
                )
                val newId = repository.insertClient(newClient)
                
                // Add initial dummy task & keyword to start off beautifully
                repository.insertKeyword(Keyword(clientId = newId.toInt(), phrase = "your target keyword", searchVolume = 1000, currentRank = 99, previousRank = 99, difficulty = 50))
                repository.insertTask(Task(clientId = newId.toInt(), title = "Do initial keyword research", description = "Identify competitive terms and search intents.", dueDate = "Within 7 days", priority = "Medium"))
                
                selectClient(newId.toInt())
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add client: ${e.message}"
            }
        }
    }

    fun removeClient(clientId: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteClient(clientId)
                if (_selectedClientId.value == clientId) {
                    _selectedClientId.value = null
                }
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete client: ${e.message}"
            }
        }
    }

    // --- Keyword Operations ---
    private val _isGroundingKeyword = MutableStateFlow(false)
    val isGroundingKeyword: StateFlow<Boolean> = _isGroundingKeyword.asStateFlow()

    fun addKeyword(phrase: String, searchVolume: Int, currentRank: Int, difficulty: Int) {
        val id = _selectedClientId.value ?: return
        viewModelScope.launch {
            try {
                val keyword = Keyword(
                    clientId = id,
                    phrase = phrase,
                    searchVolume = searchVolume,
                    currentRank = currentRank,
                    previousRank = currentRank,
                    difficulty = difficulty
                )
                repository.insertKeyword(keyword)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add keyword: ${e.message}"
            }
        }
    }

    fun addKeywordWithGrounding(phrase: String) {
        val id = _selectedClientId.value ?: return
        viewModelScope.launch {
            _isGroundingKeyword.value = true
            try {
                repository.performKeywordSearchGrounding(id, phrase)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to run search grounding for keyword: ${e.message}"
            } finally {
                _isGroundingKeyword.value = false
            }
        }
    }

    fun refreshKeywordWithGrounding(keyword: Keyword) {
        val id = _selectedClientId.value ?: return
        viewModelScope.launch {
            _isGroundingKeyword.value = true
            try {
                // Delete the old keyword entry and run grounding to search and fetch updated rank and volume
                repository.deleteKeyword(keyword)
                repository.performKeywordSearchGrounding(id, keyword.phrase)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh keyword ranking: ${e.message}"
            } finally {
                _isGroundingKeyword.value = false
            }
        }
    }

    fun removeKeyword(keyword: Keyword) {
        viewModelScope.launch {
            try {
                repository.deleteKeyword(keyword)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete keyword: ${e.message}"
            }
        }
    }

    // --- Task Operations ---
    fun addTask(title: String, description: String, dueDate: String, priority: String) {
        val id = _selectedClientId.value ?: return
        viewModelScope.launch {
            try {
                val task = Task(
                    clientId = id,
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    priority = priority,
                    isCompleted = false
                )
                repository.insertTask(task)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add task: ${e.message}"
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            try {
                repository.updateTask(task.copy(isCompleted = !task.isCompleted))
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update task: ${e.message}"
            }
        }
    }

    fun removeTask(taskId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete task: ${e.message}"
            }
        }
    }

    // --- Google OAuth Access Tokens & GA4 Property Custom Mappings persistence ---
    private val _googleAccessToken = MutableStateFlow(prefs.getString("google_access_token", null))
    val googleAccessToken: StateFlow<String?> = _googleAccessToken.asStateFlow()

    private val _ga4PropertyConfigurationId = MutableStateFlow<String>(prefs.getString("ga4_property_id", "properties/395641042") ?: "properties/395641042")
    val ga4PropertyConfigurationId: StateFlow<String> = _ga4PropertyConfigurationId.asStateFlow()

    private val _pagespeedApiKey = MutableStateFlow(prefs.getString("pagespeed_api_key", "") ?: "")
    val pagespeedApiKey: StateFlow<String> = _pagespeedApiKey.asStateFlow()

    private val _currentGscReport = MutableStateFlow<com.example.data.remote.GscReport?>(null)
    val currentGscReport: StateFlow<com.example.data.remote.GscReport?> = _currentGscReport.asStateFlow()

    private val _currentGa4Report = MutableStateFlow<com.example.data.remote.Ga4Report?>(null)
    val currentGa4Report: StateFlow<com.example.data.remote.Ga4Report?> = _currentGa4Report.asStateFlow()

    private val _isFetchingGoogleReports = MutableStateFlow(false)
    val isFetchingGoogleReports: StateFlow<Boolean> = _isFetchingGoogleReports.asStateFlow()

    private val _verifiedSites = MutableStateFlow<List<com.example.data.remote.GscSiteItem>>(emptyList())
    val verifiedSites: StateFlow<List<com.example.data.remote.GscSiteItem>> = _verifiedSites.asStateFlow()

    private val _sitemapsList = MutableStateFlow<List<com.example.data.remote.GscSitemapItem>>(emptyList())
    val sitemapsList: StateFlow<List<com.example.data.remote.GscSitemapItem>> = _sitemapsList.asStateFlow()

    private val _urlInspectionResult = MutableStateFlow<com.example.data.remote.UrlInspectionReport?>(null)
    val urlInspectionResult: StateFlow<com.example.data.remote.UrlInspectionReport?> = _urlInspectionResult.asStateFlow()

    private val _isInspectingUrl = MutableStateFlow(false)
    val isInspectingUrl: StateFlow<Boolean> = _isInspectingUrl.asStateFlow()

    fun saveGoogleAccessToken(token: String?) {
        prefs.edit().putString("google_access_token", token).apply()
        _googleAccessToken.value = token
        if (token != null) {
            fetchGscVerifiedSites()
        }
    }

    fun saveGa4PropertyId(propertyId: String) {
        prefs.edit().putString("ga4_property_id", propertyId).apply()
        _ga4PropertyConfigurationId.value = propertyId
    }

    fun savePagespeedApiKey(key: String) {
        prefs.edit().putString("pagespeed_api_key", key).apply()
        _pagespeedApiKey.value = key
    }

    fun fetchGoogleReportsForClient(clientId: Int) {
        viewModelScope.launch {
            _isFetchingGoogleReports.value = true
            try {
                val client = repository.seoDao.getClientById(clientId)
                val token = _googleAccessToken.value
                if (client != null && !token.isNullOrEmpty()) {
                    val siteUrl = if (!client.gscSiteUrl.isNullOrEmpty()) client.gscSiteUrl else client.websiteUrl
                    val ga4Id = if (!client.ga4PropertyId.isNullOrEmpty()) client.ga4PropertyId else _ga4PropertyConfigurationId.value
                    
                    try {
                        val gsc = com.example.data.remote.SeoIntegrationService.fetchSearchConsoleReport(siteUrl, token)
                        _currentGscReport.value = gsc
                    } catch (e: Exception) {
                        android.util.Log.e("SeoViewModel", "GSC report fetch fail: ${e.message}")
                    }

                    try {
                        val ga4 = com.example.data.remote.SeoIntegrationService.fetchAnalyticsReport(ga4Id, token)
                        _currentGa4Report.value = ga4
                    } catch (e: Exception) {
                        android.util.Log.e("SeoViewModel", "GA4 report fetch fail: ${e.message}")
                    }

                    fetchGscSitemaps(clientId, siteUrl)
                } else {
                    _currentGscReport.value = null
                    _currentGa4Report.value = null
                    _sitemapsList.value = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("SeoViewModel", "Main report fetch fail: ${e.message}")
            } finally {
                _isFetchingGoogleReports.value = false
            }
        }
    }

    fun fetchGscVerifiedSites() {
        val token = _googleAccessToken.value ?: return
        viewModelScope.launch {
            try {
                val sites = com.example.data.remote.SeoIntegrationService.fetchVerifiedSites(token)
                _verifiedSites.value = sites
            } catch (e: Exception) {
                android.util.Log.e("SeoViewModel", "Fetch sites failed", e)
            }
        }
    }

    fun fetchGscSitemaps(clientId: Int, siteUrl: String) {
        val token = _googleAccessToken.value ?: return
        viewModelScope.launch {
            try {
                val list = com.example.data.remote.SeoIntegrationService.fetchSitemaps(siteUrl, token)
                _sitemapsList.value = list
            } catch (e: Exception) {
                android.util.Log.e("SeoViewModel", "Fetch sitemaps failed", e)
            }
        }
    }

    fun submitGscSitemap(siteUrl: String, sitemapPath: String, onDone: (Boolean) -> Unit) {
        val token = _googleAccessToken.value ?: return
        viewModelScope.launch {
            try {
                val isOk = com.example.data.remote.SeoIntegrationService.submitSitemap(siteUrl, sitemapPath, token)
                if (isOk && _selectedClientId.value != null) {
                    fetchGscSitemaps(_selectedClientId.value!!, siteUrl)
                }
                onDone(isOk)
            } catch (e: Exception) {
                android.util.Log.e("SeoViewModel", "Sitemap submission error", e)
                onDone(false)
            }
        }
    }

    fun inspectUrlWithGsc(inspectionUrl: String, siteUrl: String) {
        val token = _googleAccessToken.value ?: return
        viewModelScope.launch {
            _isInspectingUrl.value = true
            _urlInspectionResult.value = null
            try {
                val res = com.example.data.remote.SeoIntegrationService.fetchUrlInspection(inspectionUrl, siteUrl, token)
                _urlInspectionResult.value = res
            } catch (e: Exception) {
                android.util.Log.e("SeoViewModel", "Inspection fail: ${e.message}")
            } finally {
                _isInspectingUrl.value = false
            }
        }
    }

    // --- Technical Crawl Site Audit ---
    fun runTechnicalAudit(clientId: Int, strategy: String = "MOBILE") {
        viewModelScope.launch {
            _isAuditing.value = true
            try {
                repository.performSeoAudit(clientId, strategy, _pagespeedApiKey.value)
            } catch (e: Exception) {
                _errorMessage.value = "Audit Engine Failure: ${e.message}"
            } finally {
                _isAuditing.value = false
            }
        }
    }

    // --- Competitor Analysis Scrapers ---
    fun runCompetitorAnalysis(clientId: Int, domain: String) {
        viewModelScope.launch {
            try {
                val crawl = com.example.data.remote.SeoIntegrationService.crawlUrl(domain)
                val comp = Competitor(
                    clientId = clientId,
                    domain = domain,
                    title = crawl.title,
                    metaDesc = crawl.metaDescription,
                    h1 = crawl.h1Tags.firstOrNull() ?: "مشخص نشده",
                    h2Count = crawl.h2Tags.size,
                    schemaCount = crawl.structuredDataCount,
                    internalLinksCount = crawl.discoveredUrls.size,
                    isSecure = domain.startsWith("https") || domain.contains("https://"),
                    score = crawl.calculatedScore
                )
                repository.insertCompetitor(comp)
            } catch (e: Exception) {
                _errorMessage.value = "خطا در خزش رقیب: ${e.localizedMessage}"
            }
        }
    }

    // --- AI-Powered Strategy Blueprint Flow ---
    private val _aiStrategyResult = MutableStateFlow<String?>(null)
    val aiStrategyResult: StateFlow<String?> = _aiStrategyResult.asStateFlow()

    private val _isGeneratingStrategy = MutableStateFlow(false)
    val isGeneratingStrategy: StateFlow<Boolean> = _isGeneratingStrategy.asStateFlow()

    fun generateStrategyReport(clientId: Int) {
        viewModelScope.launch {
            _isGeneratingStrategy.value = true
            _aiStrategyResult.value = null
            try {
                val token = _googleAccessToken.value
                val propId = _ga4PropertyConfigurationId.value
                val result = repository.generateAiStrategyReport(clientId, token, propId)
                _aiStrategyResult.value = result
            } catch (e: Exception) {
                _errorMessage.value = "Strategy Compilation Failure: ${e.message}"
            } finally {
                _isGeneratingStrategy.value = false
            }
        }
    }

    // --- Chat Intelligence Assistant ---
    fun sendAssistantMessage(prompt: String) {
        val id = _selectedClientId.value ?: return
        if (prompt.isBlank()) return
        
        viewModelScope.launch {
            _isChatLoading.value = true
            try {
                repository.askAiAssistant(id, prompt)
            } catch (e: Exception) {
                _errorMessage.value = "AI Engine Timeout: ${e.message}"
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun clearAllAnalysisData(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                _selectedClientId.value = null
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to clear analysis data: ${e.message}"
            }
        }
    }
}

// Global Stats Data Model
data class GlobalStats(
    val totalClients: Int = 0,
    val averageHealthScore: Int = 0,
    val pendingTasksCount: Int = 0,
    val activeCampaignsCount: Int = 0,
    val reviewCampaignsCount: Int = 0
)

// Simple ViewModel Custom Factory
class SeoViewModelFactory(
    private val application: Application,
    private val repository: SeoRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SeoViewModel::class.java)) {
            return SeoViewModel(application, repository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
