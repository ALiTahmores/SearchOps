package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.SeoRepository
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

    fun saveGoogleAccessToken(token: String?) {
        prefs.edit().putString("google_access_token", token).apply()
        _googleAccessToken.value = token
    }

    fun saveGa4PropertyId(propertyId: String) {
        prefs.edit().putString("ga4_property_id", propertyId).apply()
        _ga4PropertyConfigurationId.value = propertyId
    }

    // --- Technical Crawl Site Audit ---
    fun runTechnicalAudit(clientId: Int, strategy: String = "MOBILE") {
        viewModelScope.launch {
            _isAuditing.value = true
            try {
                repository.performSeoAudit(clientId, strategy)
            } catch (e: Exception) {
                _errorMessage.value = "Audit Engine Failure: ${e.message}"
            } finally {
                _isAuditing.value = false
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
