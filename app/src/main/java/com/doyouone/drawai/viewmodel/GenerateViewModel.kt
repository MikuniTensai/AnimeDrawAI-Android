package com.doyouone.drawai.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doyouone.drawai.R
import com.doyouone.drawai.data.api.NetworkConfig
import com.doyouone.drawai.data.model.GenerationLimit
import com.doyouone.drawai.data.model.GenerationLimitExceededException
import com.doyouone.drawai.data.model.TaskStatusResponse
import com.doyouone.drawai.data.model.WorkflowInfo
import com.doyouone.drawai.data.repository.DrawAIRepository
import com.doyouone.drawai.data.repository.GenerationTrackingRepository
import com.doyouone.drawai.data.repository.UsageStatisticsRepository
import com.doyouone.drawai.data.preferences.UserPreferences
import com.doyouone.drawai.data.preferences.AppPreferences
import com.doyouone.drawai.utils.FirebaseDataFixer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.doyouone.drawai.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel untuk handle image generation
 */
class GenerateViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = DrawAIRepository()
    private val notificationHelper = NotificationHelper(application)
    private val userPreferences = UserPreferences(application)
    private val appPreferences = AppPreferences(application)
    private val authManager = com.doyouone.drawai.auth.AuthManager(application)
    private val auth = FirebaseAuth.getInstance()
    private val workManager = androidx.work.WorkManager.getInstance(application)
    private val generationLogRepository = com.doyouone.drawai.data.repository.GenerationLogRepository()
    private val workflowStatsRepository = com.doyouone.drawai.data.repository.WorkflowStatsRepository()
    
    companion object {
        private const val TAG = "GenerateViewModel"
        private const val BACKGROUND_GENERATION_WORK_TAG = "background_image_generation"
        
        // Cache for regex patterns to avoid excessive compilation during moderation checks
        private val REGEX_CACHE = java.util.concurrent.ConcurrentHashMap<String, Regex>()
        
        private val INAPPROPRIATE_WORDS = setOf(
            // Explicit Sexual Content
            "bugil", "telanjang", "porno", "seks", "vulgar", "cabul", "mesum", "asusila", "erotis", "sensual",
            "birahi", "nafsu", "syahwat", "lonte", "pelacur", "jablay", "bokep", "ngentot", "memek", "kontol", "ngewe",
            "jembut", "itil", "toket", "pentil", "perek", "germo", "sundal", "vagina", "pussy", "cunt",
            "coochie", "twat", "penis", "cock", "dick", "prick", "schlong", "dong", "testicles",
            "scrotum", "anus", "asshole", "butt", "booty", "bum", "arse", "rectum", "breast", "boob", "tits",
            "nipple", "areola", "clitoris", "labia", "vulva", "semen", "sperm", "cum", "jizz", "spunk", "ejaculate",
            "orgasm", "climax", "masturbation", "handjob", "blowjob", "fellatio", "cunnilingus", "rimjob", "anilingus",
            "fingering", "fisting", "intercourse", "coitus", "copulation", "fucking", "screwing", "banging", "shagging",
            "doggy style", "missionary position", "69", "anal sex", "oral sex", "group sex", "orgy", "gangbang",
            "threesome", "foursome", "incest", "bestiality", "zoophilia", "pedophilia", "necrophilia", "rape",
            "sexual assault", "sexual abuse", "molestation", "prostitute", "whore", "hooker", "slut", "harlot",
            "tramp", "pimp", "brothel", "sex worker", "pornstar", "adult entertainer", "stripper", "hentai",
            "nsfw", "explicit", "18+", "xxx", "hardcore", "softcore", "gonzo", "bukkake",
            "creampie", "deepthroat", "double penetration", "facial", "gloryhole", "undress", "seeing through clothes", "x-ray",

            // Strong Profanity & Insults
            "bajingan", "bangsat", "keparat", "biadab", "kurang ajar", "brengsek", "anjing", "babi", "monyet",
            "sampah", "tolol", "goblok", "idiot", "dungu", "bodoh", "fuck", "shit", "piss", "crap", "bullshit", "motherfucker", "bitch", "bastard", "dickhead",
            "damn", "goddamn", "what the fuck", "wtf",

            // Hate Speech & Derogatory Terms
            "rasis", "sara"
        )
        
        private val VARIATION_WORDS = mapOf(
            "s3x" to "sex",
            "s3xy" to "sexy",
            "s3ks" to "seks",
            "p0rn" to "porn",
            "p0rno" to "porno",
            "b00bs" to "boobs",
            "t1ts" to "tits",
            "d1ck" to "dick",
            "c0ck" to "cock",
            "f*ck" to "fuck",
            "fck" to "fuck",
            "sh1t" to "shit",
            "b1tch" to "bitch",
            "@ss" to "ass",
            "a$$" to "ass",
            "nud3" to "nude",
            "n4k3d" to "naked"
        )
    }
    
    // UI State
    private val _uiState = MutableStateFlow<GenerateUiState>(GenerateUiState.Idle)
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()
    
    // Workflows
    private val _workflows = MutableStateFlow<Map<String, WorkflowInfo>>(emptyMap())
    val workflows: StateFlow<Map<String, WorkflowInfo>> = _workflows.asStateFlow()
    
    // Workflow Stats (Map<WorkflowId, Map<StatType, Count>>)
    private val _workflowStats = MutableStateFlow<Map<String, Map<String, Long>>>(emptyMap())
    val workflowStats: StateFlow<Map<String, Map<String, Long>>> = _workflowStats.asStateFlow()
    
    // Current task
    private val _currentTask = MutableStateFlow<TaskStatusResponse?>(null)
    val currentTask: StateFlow<TaskStatusResponse?> = _currentTask.asStateFlow()
    
    // Network status
    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()
    
    // Generation limit
    private val _generationLimit = MutableStateFlow<GenerationLimit?>(null)
    val generationLimit: StateFlow<GenerationLimit?> = _generationLimit.asStateFlow()
    
    // Background generation enabled preference
    private val _isBackgroundGenerationEnabled = MutableStateFlow(false)
    val isBackgroundGenerationEnabled: StateFlow<Boolean> = _isBackgroundGenerationEnabled.asStateFlow()
    
    // WorkManager status
    private val _backgroundWorkStatus = MutableStateFlow<String?>(null)
    val backgroundWorkStatus: StateFlow<String?> = _backgroundWorkStatus.asStateFlow()
    
    // Gem Count (Reactive to User Changes)
    val gemCount: StateFlow<Int> = authManager.currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getGemCountFlow(user.uid)
            } else {
                flowOf(0)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    // Gem Reward Event
    private val _gemRewardChannel = kotlinx.coroutines.channels.Channel<Int>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val gemRewardEvent = _gemRewardChannel.receiveAsFlow()

    // Daily Rewards State
    private val _dailyStatus = MutableStateFlow<com.doyouone.drawai.data.model.DailyStatusResponse?>(null)
    val dailyStatus: StateFlow<com.doyouone.drawai.data.model.DailyStatusResponse?> = _dailyStatus.asStateFlow()

    private val _claimResult = kotlinx.coroutines.channels.Channel<com.doyouone.drawai.data.model.DailyClaimResponse>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val claimResult = _claimResult.receiveAsFlow()
    
    init {
        checkNetworkStatus()
        loadWorkflows()
        loadGenerationLimit()
        loadWorkflowStats()
        
        // Initial check for daily rewards
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                if (user != null) {
                    checkDailyStatus()
                } else {
                    _dailyStatus.value = null
                }
            }
        }
        
        // Observe restricted content setting and auto-reload workflows when it CHANGES
        viewModelScope.launch {
            appPreferences.isRestrictedContentEnabled
                .distinctUntilChanged() // Only emit when value actually changes
                .drop(1) // Skip initial value (already loaded above)
                .collect { enabled ->
                    Log.d(TAG, "🔄 Restricted content setting changed to: $enabled - reloading workflows")
                    loadWorkflows() // Automatically reload workflows with new filter
                }
        }
    }

    fun checkDailyStatus() {
        viewModelScope.launch {
            val result = repository.checkDailyRewardStatus()
            if (result.isSuccess) {
                _dailyStatus.value = result.getOrNull()
            } else {
                Log.e(TAG, "Failed to check daily status", result.exceptionOrNull())
            }
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val result = repository.claimDailyReward()
            if (result.isSuccess) {
                result.getOrNull()?.let { response ->
                    _claimResult.trySend(response)
                    // Refresh status after claim
                    checkDailyStatus()
                }
            } else {
                Log.e(TAG, "Failed to claim reward", result.exceptionOrNull())
            }
        }
    }
    
    // --- Engagement Features ---
    
    /**
     * Purchase Daily Booster
     */
    fun purchaseDailyBooster(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.firebaseRepo.purchaseDailyBooster(
                authManager.getCurrentUserId() ?: return@launch
            )
            if (result.isSuccess) {
                loadGenerationLimit() // Refresh UI
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }
    
    /**
     * Invite Friend (Redeem Referral)
     */
    fun redeemReferral(code: String, deviceId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.firebaseRepo.redeemReferral(
                userId = authManager.getCurrentUserId() ?: return@launch,
                referralCode = code,
                deviceId = deviceId
            )
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }
    
    /**
     * Claim Share Reward
     */
    fun claimShareReward(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.firebaseRepo.claimShareReward(
                authManager.getCurrentUserId() ?: return@launch
            )
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }
    
    /**
     * Redeem Code
     */
    fun redeemCode(code: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.firebaseRepo.redeemCode(
                userId = authManager.getCurrentUserId() ?: return@launch,
                code = code
            )
            if (result.isSuccess) {
                val reward = result.getOrNull() ?: 0
                onResult(true, "Successfully redeemed $reward Gems!")
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }
    
    fun loadWorkflowStats() {
        viewModelScope.launch {
            val result = workflowStatsRepository.getAllStats()
            if (result.isSuccess) {
                _workflowStats.value = result.getOrNull() ?: emptyMap()
            }
        }
    }
    
    fun incrementWorkflowView(workflowId: String) {
        viewModelScope.launch {
            workflowStatsRepository.incrementView(workflowId)
            loadWorkflowStats() // Force refresh stats locally
        }
    }
    
    /**
     * Check network connectivity
     */
    fun checkNetworkStatus() {
        _isNetworkAvailable.value = NetworkConfig.isNetworkAvailable(getApplication())
    }
    
    /**
     * Load available workflows from API
     */
    fun loadWorkflows() {
        viewModelScope.launch {
            try {
                // SIlent refresh: only show loading if we don't have workflows yet
                if (_workflows.value.isEmpty()) {
                    _uiState.value = GenerateUiState.Loading("Memuat workflows...")
                }
                
                // Also reload stats when reloading workflows
                loadWorkflowStats()
                
                val result = repository.getWorkflows()
                if (result.isSuccess) {
                    val allWorkflows = result.getOrNull() ?: emptyMap()
                    
                    // Filter workflows based on restricted content setting
                    val isRestrictedContentEnabled = appPreferences.isRestrictedContentEnabled.first()
                    val filteredWorkflows = if (isRestrictedContentEnabled) {
                        allWorkflows
                    } else {
                        allWorkflows.filter { (_, workflowInfo) ->
                            !workflowInfo.restricted
                        }
                    }
                    
                    _workflows.value = filteredWorkflows
                    
                    // Only reset to Idle if we were in Loading state
                    // This prevents overwriting Processing or Success states
                    if (_uiState.value is GenerateUiState.Loading) {
                        _uiState.value = GenerateUiState.Idle
                    }
                    Log.d(TAG, "Loaded ${filteredWorkflows.size} workflows (restricted: $isRestrictedContentEnabled)")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to load workflows"
                    _uiState.value = GenerateUiState.Error(error)
                    Log.e(TAG, "Error loading workflows: $error")
                }
            } catch (e: Exception) {
                _uiState.value = GenerateUiState.Error(e.message ?: "Unknown error")
                Log.e(TAG, "Exception loading workflows", e)
            }
        }
    }
    
    /**
     * Generate image with specified parameters
     */
    /**
     * Generate image with specified parameters using WorkManager for background processing
     */
    fun generateImage(
        positivePrompt: String,
        negativePrompt: String = "",
        workflow: String = "standard",
        seed: Long = 1062314217360759L  // Default seed for consistent generation
    ) {
         viewModelScope.launch {
            // 1. Network Check
            if (!_isNetworkAvailable.value) {
                _uiState.value = GenerateUiState.Error("No internet connection", R.string.error_no_internet)
                return@launch
            }
            
            // 2. Prompt Validation
            if (positivePrompt.isBlank()) {
                _uiState.value = GenerateUiState.Error("Prompt cannot be empty", R.string.error_prompt_empty)
                return@launch
            }

            // 3. Content Moderation
            val isMoreEnabled = _generationLimit.value?.moreEnabled == true
            if (!isMoreEnabled) {
                val positiveCheck = checkPromptInappropriate(positivePrompt)
                val negativeCheck = checkPromptInappropriate(negativePrompt)
        
                if (positiveCheck.first || negativeCheck.first) {
                    _uiState.value = GenerateUiState.ContentModeration(
                        message = getApplication<Application>().getString(R.string.content_moderation_description),
                        positiveInappropriateWords = if (positiveCheck.first) positiveCheck.second else emptyList(),
                        negativeInappropriateWords = if (negativeCheck.first) negativeCheck.second else emptyList(),
                        messageResId = R.string.content_moderation_description
                    )
                    return@launch
                }
            }
            
            // 4. Auth Check
            val userId = authManager.getCurrentUserId()
            if (userId == null) {
                _uiState.value = GenerateUiState.Error("User not authenticated", R.string.error_user_not_authenticated)
                return@launch
            }
            
            try {
                _uiState.value = GenerateUiState.Loading("Queuing generation...")
                Log.d(TAG, "🚀 Enqueuing Background Work for User: $userId Workflow: $workflow")

                // Prepare Input Data
                val inputData = androidx.work.workDataOf(
                    com.doyouone.drawai.worker.WorkflowGenerationWorker.KEY_USER_ID to userId,
                    com.doyouone.drawai.worker.WorkflowGenerationWorker.KEY_POSITIVE_PROMPT to positivePrompt,
                    com.doyouone.drawai.worker.WorkflowGenerationWorker.KEY_NEGATIVE_PROMPT to negativePrompt,
                    com.doyouone.drawai.worker.WorkflowGenerationWorker.KEY_WORKFLOW to workflow,
                    com.doyouone.drawai.worker.WorkflowGenerationWorker.KEY_SEED to seed
                )
                
                // Construct Work Request
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.doyouone.drawai.worker.WorkflowGenerationWorker>()
                    .setInputData(inputData)
                    .addTag(BACKGROUND_GENERATION_WORK_TAG)
                    .build()
                
                // Enqueue
                workManager.enqueue(workRequest)
                
                // Monitor
                monitorWork(workRequest.id)
                
            } catch (e: Exception) {
                _uiState.value = GenerateUiState.Error("Failed to start generation: ${e.message}")
                Log.e(TAG, "WorkManager Enqueue Failed", e)
            }
         }
    }
    
    private fun monitorWork(workId: java.util.UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workId).collect { workInfo ->
                if (workInfo == null) return@collect
                
                when (workInfo.state) {
                    androidx.work.WorkInfo.State.ENQUEUED -> {
                        _uiState.value = GenerateUiState.Loading("Queued...")
                    }
                    androidx.work.WorkInfo.State.RUNNING -> {
                         // Check progress if using setProgress? For now just say Generating
                         _uiState.value = GenerateUiState.Processing("Generating image...", null)
                    }
                    androidx.work.WorkInfo.State.SUCCEEDED -> {
                        Log.d(TAG, "Worker SUCCEEDED")
                        loadGenerationLimit() // Refresh limits
                        // Create dummy task for navigation
                        val dummyTask = TaskStatusResponse(
                             success = true,
                             taskId = workId.toString(),
                             status = "completed",
                             resultFiles = null 
                         )
                         _uiState.value = GenerateUiState.Success(dummyTask)
                    }
                    androidx.work.WorkInfo.State.FAILED -> {
                         _uiState.value = GenerateUiState.Error("Generation Failed. Check notification.")
                    }
                    androidx.work.WorkInfo.State.CANCELLED -> {
                        _uiState.value = GenerateUiState.Error("Generation Cancelled")
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * Generate image with retry mechanism for server errors
     */
    private fun generateImageWithRetry(
        positivePrompt: String,
        negativePrompt: String = "",
        workflow: String = "standard",
        seed: Long = 1062314217360759L,
        maxRetries: Int = 2,
        currentRetry: Int = 0
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            if (!_isNetworkAvailable.value) {
                _uiState.value = GenerateUiState.Error("No internet connection", R.string.error_no_internet)
                return@launch
            }
            
            if (positivePrompt.isBlank()) {
                _uiState.value = GenerateUiState.Error("Prompt cannot be empty", R.string.error_prompt_empty)
                return@launch
            }

            // Content moderation - Check 'More' enabled status
            val isMoreEnabled = _generationLimit.value?.moreEnabled == true
            
            if (!isMoreEnabled) {
                val positiveCheck = checkPromptInappropriate(positivePrompt)
                val negativeCheck = checkPromptInappropriate(negativePrompt)
        
                if (positiveCheck.first || negativeCheck.first) {
                    _uiState.value = GenerateUiState.ContentModeration(
                        message = getApplication<Application>().getString(R.string.content_moderation_description),
                        positiveInappropriateWords = if (positiveCheck.first) positiveCheck.second else emptyList(),
                        negativeInappropriateWords = if (negativeCheck.first) negativeCheck.second else emptyList(),
                        messageResId = R.string.content_moderation_description
                    )
                    return@launch
                }
            }
            
            // Get userId
            val userId = authManager.getCurrentUserId()
            if (userId == null) {
                _uiState.value = GenerateUiState.Error("User not authenticated", R.string.error_user_not_authenticated)
                return@launch
            }
            
            Log.d(TAG, "🚀 === GENERATE IMAGE CALLED ===")
            Log.d(TAG, "User ID: $userId")
            Log.d(TAG, "Workflow: '$workflow'")
            Log.d(TAG, "Prompt: '$positivePrompt'")
            
            try {
                _uiState.value = GenerateUiState.Loading("Memuat generation...")
                
                Log.d(TAG, "📡 Calling repository.generateAndWaitForCompletion...")
                Log.d(TAG, "🎲 Using seed: $seed")
                val result = repository.generateAndWaitForCompletion(
                    positivePrompt = positivePrompt,
                    negativePrompt = negativePrompt,
                    workflow = workflow,
                    userId = userId,
                    seed = seed,  // Pass hardcoded seed
                    onSuccessStart = { response ->
                        val earned = response.gemsEarned ?: 0
                        if (earned > 0) {
                            Log.d(TAG, "💎 Gem reward earned: $earned")
                            _gemRewardChannel.trySend(earned)
                        }
                    },
                    onStatusUpdate = { status, taskStatus ->
                        _uiState.value = GenerateUiState.Processing(status, taskStatus)
                        taskStatus?.let { _currentTask.value = it }
                    }
                )
                
                Log.d(TAG, "📦 Result received. isSuccess: ${result.isSuccess}")
                
                if (result.isSuccess) {
                    val taskStatus = result.getOrNull()!!
                    _currentTask.value = taskStatus
                    
                    // Refresh generation limit
                    loadGenerationLimit()
                    
                    // Increment generation count (for tracking only)
                    incrementGenerationCount(positivePrompt)
                    
                    // Log generation for Weekly/Monthly Leaderboards
                    launch {
                        generationLogRepository.logGeneration(userId, taskStatus, workflow)
                    }

                    // Increment Workflow Stats (Most Popular)
                    launch {
                        workflowStatsRepository.incrementGeneration(workflow)
                        loadWorkflowStats() // Force refresh stats locally
                    }
                    
                    // Auto-download images to gallery
                    if (!taskStatus.resultFiles.isNullOrEmpty()) {
                        _uiState.value = GenerateUiState.Processing("Downloading images...", taskStatus)
                        downloadAndSaveImages(
                            taskStatus.resultFiles,
                            positivePrompt,
                            negativePrompt,
                            workflow
                        )
                    } else {
                        _uiState.value = GenerateUiState.Success(taskStatus)
                    }
                    
                    // Show notification if app is in background
                    if (notificationHelper.hasNotificationPermission()) {
                        notificationHelper.showGenerationCompleteNotification(
                            imageUrl = taskStatus.resultFiles?.firstOrNull(),
                            success = true
                        )
                    }
                    
                    Log.d(TAG, "Generation completed successfully")
                } else {
                    val exception = result.exceptionOrNull()
                    
                    // Handle generation limit exceeded
                    if (exception is GenerationLimitExceededException) {
                        _uiState.value = GenerateUiState.LimitExceeded(
                            message = exception.message ?: "Limit generasi tercapai",
                            remaining = exception.remaining
                        )
                        Log.w(TAG, "Generation limit exceeded: ${exception.message}")
                    } 
                    // Check for 403 Forbidden (server-side limit check)
                    else if (exception?.message?.contains("403") == true || 
                             exception?.message?.contains("Forbidden", ignoreCase = true) == true ||
                             exception?.message?.contains("limit", ignoreCase = true) == true) {
                        // Server rejected due to limit - show ad dialog
                        _uiState.value = GenerateUiState.LimitExceeded(
                            message = "Daily generation limit reached. Watch an ad to get more!",
                            remaining = 0
                        )
                        Log.w(TAG, "Server-side limit exceeded (403): ${exception.message}")
                    }
                    else {
                        val error = exception?.message ?: "Generation failed"
                        
                        // Check for 530 (Maintenance)
                        if (error.contains("530")) {
                            _uiState.value = GenerateUiState.Maintenance("Server Maintenance (Error 530)")
                            Log.e(TAG, "Maintenance error detected: 530")
                            return@launch
                        }
                        
                        // Check if we should retry for server errors
                        val isRetryableError = when {
                            error.contains("500", ignoreCase = true) || 
                            error.contains("Internal Server Error", ignoreCase = true) -> true
                            error.contains("timeout", ignoreCase = true) ||
                            error.contains("took too long", ignoreCase = true) -> true
                            error.contains("Server sedang sibuk", ignoreCase = true) -> true
                            else -> false
                        }
                        
                        if (isRetryableError && currentRetry < maxRetries) {
                            Log.w(TAG, "Retryable error occurred, retrying... (attempt ${currentRetry + 1}/$maxRetries)")
                            _uiState.value = GenerateUiState.Loading("Mencoba kembali... (percobaan ${currentRetry + 1}/$maxRetries)")
                            
                            // Retry after delay
                            kotlinx.coroutines.delay(2000 * (currentRetry + 1).toLong()) // Exponential backoff
                            generateImageWithRetry(positivePrompt, negativePrompt, workflow, seed, maxRetries, currentRetry + 1)
                            return@launch
                        }
                        
                        // Better error handling for different types of errors
                        val (userFriendlyError, errorResId) = when {
                            error.contains("500", ignoreCase = true) || 
                            error.contains("Internal Server Error", ignoreCase = true) -> {
                                Pair("Server is busy. Please try again later.", R.string.error_server_busy)
                            }
                            error.contains("timeout", ignoreCase = true) ||
                            error.contains("took too long", ignoreCase = true) -> {
                                Pair("Process took too long. Please try a simpler prompt.", R.string.error_process_timeout)
                            }
                            error.contains("connection", ignoreCase = true) ||
                            error.contains("network", ignoreCase = true) -> {
                                Pair("Connection issue. Please check your internet.", R.string.error_connection_issue)
                            }
                            error.contains("drawai.site", ignoreCase = true) || 
                            error.contains("drawai-api", ignoreCase = true) -> {
                                Pair("Failed to process image. Please try again.", R.string.error_processing_failed)
                            }
                            else -> Pair(error, null)
                        }
                        
                        _uiState.value = GenerateUiState.Error(userFriendlyError, errorResId)
                        Log.e(TAG, "Generation failed: $error")
                    }
                    
                    // Show error notification
                    if (notificationHelper.hasNotificationPermission()) {
                        notificationHelper.showGenerationCompleteNotification(
                            success = false
                        )
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = GenerateUiState.Error(e.message ?: "Unknown error")
                Log.e(TAG, "Exception during generation", e)
            }
        }
    }

    private fun calculatePromptRating(positivePrompt: String, negativePrompt: String): Int {
        var score = 100 // Start from 100%

        // Check for inappropriate words
        val inappropriateCheck = checkPromptInappropriate(positivePrompt)
        if (inappropriateCheck.first) {
            score -= (inappropriateCheck.second.size * 35) // Increased penalty
        }

        val promptLower = positivePrompt.lowercase()
        val words = promptLower.split(" ", ",", ";", ".", "!", "(", ")")

        // Leetspeak detection removed as it was too aggressive

        // Repetition check
        val wordFrequency = words.groupingBy { it }.eachCount()
        val suspiciousRepetition = wordFrequency.filter { it.value > 3 && it.key.length > 2 }
        score -= suspiciousRepetition.size * 20 // Increased penalty

        // Common inappropriate patterns
        val suspiciousPatterns = setOf(
            "nsfw", "nude", "naked", "sex", "porn", "adult", "mature", "explicit",
            "hentai", "ecchi", "loli", "shota", "undress", "seeing through clothes"
        )

        val patternMatches = words.count { word ->
            suspiciousPatterns.any { pattern ->
                isWholeWordMatch(word, pattern)
            }
        }
        score -= patternMatches * 40 // Increased penalty

        // Negative prompt check
        val negativeCheck = checkPromptInappropriate(negativePrompt)
        if (negativeCheck.first) {
            score -= (negativeCheck.second.size * 25) // Increased penalty
        }

        // Filter avoidance check
        val cleanedPrompt = positivePrompt.replace(Regex("[\\s\\W_]"), "")
        val filterAvoidance = suspiciousPatterns.count { pattern ->
            cleanedPrompt.contains(pattern, ignoreCase = true)
        }
        score -= filterAvoidance * 30 // Increased penalty

        // Ensure score is not below 0
        return maxOf(score, 60) // Set a minimum score of 60
    }

    // Fungsi untuk mendeteksi variasi kata (seperti s3x, s3xy, dll)
    private fun detectWordVariation(word: String, target: String): Boolean {
        // Normalisasi kata: hapus angka dan karakter khusus, ubah ke huruf kecil
        val normalized = word.lowercase()
            .replace("3", "e")
            .replace("1", "i")
            .replace("0", "o")
            .replace("5", "s")
            .replace("7", "t")
            .replace("@", "a")
            .replace("$", "s")
            .replace("+", "t")
            .replace("!", "i")
            .replace("|", "i")
            .replace("8", "b")
            .replace("9", "g")
            .replace("2", "z")
            .replace("4", "a")
            .replace("6", "g")
            
        return normalized.contains(target, ignoreCase = true)
    }

    private fun checkPromptInappropriate(prompt: String): Pair<Boolean, List<String>> {
        val foundWords = mutableListOf<String>()
        val words = prompt.lowercase().split(Regex("""\s+|,|;|\.|!|\?|\n|\t"""))

        for (inappropriateWord in INAPPROPRIATE_WORDS) {
            if (words.any { word -> isWholeWordMatch(word, inappropriateWord) }) {
                foundWords.add(inappropriateWord)
            }
        }

        for ((variation, original) in VARIATION_WORDS) {
            if (prompt.contains(variation, ignoreCase = true) || detectWordVariation(prompt, original)) {
                foundWords.add(variation)
            }
        }

        return Pair(foundWords.isNotEmpty(), foundWords.distinct())
    }

    private fun isWholeWordMatch(sourceWord: String, targetWord: String): Boolean {
        // Simple case: equal (ignoring case)
        if (sourceWord.equals(targetWord, ignoreCase = true)) {
            return true
        }
        
        // Pre-check: if the word doesn't even contain the target as substring, it can't match.
        // This avoids expensive regex for most cases.
        if (!sourceWord.contains(targetWord, ignoreCase = true)) {
            return false
        }
        
        // Remove simple punctuation (quick cleanup)
        val cleanSource = sourceWord.filter { it.isLetterOrDigit() }
        if (cleanSource.equals(targetWord, ignoreCase = true)) {
            return true
        }

        // Only use regex if strictly necessary, using CACHE to avoid recompilation
        return try {
            val regex = REGEX_CACHE.getOrPut(targetWord) {
                Regex("(^|[^a-zA-Z0-9])${Regex.escape(targetWord)}([^a-zA-Z0-9]|$)", RegexOption.IGNORE_CASE)
            }
            regex.containsMatchIn(sourceWord)
        } catch (e: Exception) {
            // Fallback for safety
            false
        }
    }

    
    private fun isPromptInappropriate(prompt: String): Boolean {
        return checkPromptInappropriate(prompt).first
    }
    
    /**
     * Download and save images to local gallery
     */
    private suspend fun downloadAndSaveImages(
        filenames: List<String>,
        prompt: String,
        negativePrompt: String,
        workflow: String
    ) {
        try {
            Log.d(TAG, "=== DOWNLOAD AND SAVE ===")
            Log.d(TAG, "Workflow for save: '$workflow'")
            Log.d(TAG, "Filenames: $filenames")
            
            val imageStorage = com.doyouone.drawai.data.local.ImageStorage(getApplication())
            var savedCount = 0
            
            for (filename in filenames) {
                try {
                    // Extract just the filename from path
                    val actualFilename = filename.substringAfterLast("/").substringAfterLast("\\")
                    
                    val downloadResult = repository.downloadImage(
                        filename = actualFilename,
                        destinationFile = java.io.File(getApplication<Application>().cacheDir, actualFilename)
                    )
                    
                    if (downloadResult.isSuccess) {
                        val file = downloadResult.getOrNull()!!
                        val imageBytes = file.readBytes()
                        
                        // Extract seed from current task status
                        val seed = _currentTask.value?.seed
                        
                        val savedImage = imageStorage.saveImageFromBytes(
                            imageBytes = imageBytes,
                            prompt = prompt,
                            negativePrompt = negativePrompt,
                            workflow = workflow,
                            seed = seed  // Pass seed to local storage
                        )
                        
                        if (savedImage != null) {
                            savedCount++
                            Log.d(TAG, "Image saved to gallery with seed=$seed: ${savedImage.id}")
                        }
                        
                        // Clean up temp file
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading image: $filename", e)
                }
            }
            
            if (savedCount > 0) {
                _uiState.value = GenerateUiState.Success(_currentTask.value!!)
                Log.d(TAG, "Downloaded and saved $savedCount images to gallery")
            } else {
                _uiState.value = GenerateUiState.Error("Failed to download images")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in downloadAndSaveImages", e)
            _uiState.value = GenerateUiState.Error("Failed to save images: ${e.message}")
        }
    }
    
    /**
     * Download generated image
     */
    fun downloadImage(filename: String, destinationFile: File) {
        viewModelScope.launch {
            try {
                _uiState.value = GenerateUiState.Loading("Mengunduh gambar...")
                
                val result = repository.downloadImage(filename, destinationFile)
                
                if (result.isSuccess) {
                    Log.d(TAG, "Image downloaded successfully")
                    // State akan di-update oleh caller
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Download failed"
                    _uiState.value = GenerateUiState.Error(error)
                    Log.e(TAG, "Download failed: $error")
                }
                
            } catch (e: Exception) {
                _uiState.value = GenerateUiState.Error(e.message ?: "Unknown error")
                Log.e(TAG, "Exception during download", e)
            }
        }
    }
    
    /**
     * Check task status manually
     */
    fun checkTaskStatus(taskId: String) {
        viewModelScope.launch {
            try {
                val result = repository.getTaskStatus(taskId)
                if (result.isSuccess) {
                    _currentTask.value = result.getOrNull()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking task status", e)
            }
        }
    }
    
    /**
     * Reset UI state to idle
     */
    fun resetState() {
        _uiState.value = GenerateUiState.Idle
        _currentTask.value = null
    }
    
    /**
     * Health check
     */
    fun performHealthCheck() {
        viewModelScope.launch {
            try {
                val result = repository.healthCheck()
                if (result.isSuccess) {
                    Log.d(TAG, "Health check passed: ${result.getOrNull()}")
                } else {
                    Log.e(TAG, "Health check failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Health check exception", e)
            }
        }
    }
    
    /**
     * Increment generation count in DataStore and Firebase
     */
    private fun incrementGenerationCount(prompt: String) {
        Log.d(TAG, "🔢 incrementGenerationCount() CALLED")
        viewModelScope.launch {
            try {
                // Increment local DataStore
                Log.d(TAG, "📝 Calling userPreferences.incrementGenerationCount()...")
                userPreferences.incrementGenerationCount()
                Log.d(TAG, "✅ Generation count incremented in DataStore")
                
                // Get current user ID
                val authManager = com.doyouone.drawai.auth.AuthManager(getApplication())
                val userId = authManager.getCurrentUserId()
                
                if (userId != null) {
                    // Increment in Firebase UsageStatistics (total count)
                    val usageStatsRepo = UsageStatisticsRepository(userId)
                    val statsResult = usageStatsRepo.incrementGenerations()
                    if (statsResult.isSuccess) {
                        Log.d(TAG, "✅ Total generation count incremented in Firebase")
                    } else {
                        Log.e(TAG, "❌ Failed to increment stats: ${statsResult.exceptionOrNull()?.message}")
                    }
                    
                    // Increment in Firebase GenerationTracking (daily count)
                    val genTrackingRepo = GenerationTrackingRepository(userId)
                    val trackingResult = genTrackingRepo.incrementGenerationCount()
                    if (trackingResult.isSuccess) {
                        Log.d(TAG, "✅ Daily generation count incremented in Firebase for user: $userId")
                    } else {
                        Log.e(TAG, "❌ Failed to increment daily count: ${trackingResult.exceptionOrNull()?.message}")
                    }
                } else {
                    Log.w(TAG, "⚠️ Guest user - only local count incremented (Firebase sync disabled)")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error incrementing generation count: ${e.message}", e)
            }
        }
    }
    
    /**
     * Load generation limit for current user
     */
    fun loadGenerationLimit() {
        viewModelScope.launch {
            try {
                val userId = authManager.getCurrentUserId()
                if (userId != null) {
                    val result = repository.getGenerationLimit(userId)
                    if (result.isSuccess) {
                        val limit = result.getOrNull()
                        _generationLimit.value = limit
                        
                        // IMPORTANT: Sync premium status from Firebase to UserPreferences
                        // FIX: Check for expiration! If expired, treat as NOT premium so ads appear.
                        val isExpired = limit?.isSubscriptionExpired() == true
                        val effectivePremium = (limit?.isPremium == true) && !isExpired
                        
                        if (isExpired && limit?.isPremium == true) {
                             Log.d(TAG, "⚠️ User has isPremium=true but is expired. Forcing local pref to false.")
                        }
                        
                        userPreferences.setPremiumStatus(effectivePremium)
                        Log.d(TAG, "✅ Premium status synced from Firebase: isPremium=${limit?.isPremium}, Expired=$isExpired, Effective=$effectivePremium")
                        
                        Log.d(TAG, "✅ Generation limit loaded: ${_generationLimit.value?.dailyGenerations}/${_generationLimit.value?.maxDailyLimit}, bonus: ${_generationLimit.value?.bonusGenerations}")
                    } else {
                        Log.e(TAG, "Failed to load generation limit: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading generation limit", e)
            }
        }
    }
    
    /**
     * Refresh workflows when restricted content setting changes
     */
    fun refreshWorkflowsForRestrictedContent() {
        // Simply reload workflows to apply any content filtering
        loadWorkflows()
        Log.d(TAG, "🔄 Workflows refreshed for restricted content setting")
    }
    
    /**
     * Add bonus generation from rewarded ad (max +2 total)
     * Returns true if successful, false if already reached max
     */
    suspend fun addBonusGeneration(): Boolean {
        return try {
            val userId = authManager.getCurrentUserId()
            if (userId == null) {
                Log.e(TAG, "User not authenticated")
                return false
            }
            
            val firebaseRepo = com.doyouone.drawai.data.repository.FirebaseGenerationRepository()
            val result = firebaseRepo.addBonusGeneration(userId)
            
            if (result.isSuccess) {
                // Refresh limit display
                _generationLimit.value = result.getOrNull()
                val maxBonus = if (_generationLimit.value?.subscriptionType == "free") "50" else "∞"
                Log.d(TAG, "✅ Bonus generation added: ${_generationLimit.value?.bonusGenerations}/$maxBonus")
                true
            } else {
                Log.e(TAG, "Failed to add bonus: ${result.exceptionOrNull()?.message}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bonus generation", e)
            false
        }
    }
    
    /**
     * DEMO: Activate subscription (for testing without Google Play billing)
     * Directly updates Firebase with subscription details
     */
    fun activateDemoSubscription(subscriptionType: String) {
        viewModelScope.launch {
            try {
                val userId = authManager.getCurrentUserId()
                if (userId == null) {
                    Log.e(TAG, "User not authenticated")
                    _uiState.value = GenerateUiState.Error("Please login first")
                    return@launch
                }
                
                Log.d(TAG, "🎁 Activating DEMO subscription: $subscriptionType")
                
                // First, fix Firebase data inconsistency
                val firebaseDataFixer = FirebaseDataFixer(FirebaseFirestore.getInstance())
                val fixResult = firebaseDataFixer.fixCurrentUserData(userId)
                
                if (fixResult.isFailure) {
                    Log.w(TAG, "Failed to fix Firebase data: ${fixResult.exceptionOrNull()?.message}")
                }
                
                val firebaseRepo = com.doyouone.drawai.data.repository.FirebaseGenerationRepository()
                val result = firebaseRepo.activateDemoSubscription(userId, subscriptionType, resetIfExists = true)
                
                if (result.isSuccess) {
                    // Update limit display
                    _generationLimit.value = result.getOrNull()
                    
                    // IMPORTANT: Sync premium status to UserPreferences
                    userPreferences.setPremiumStatus(true)
                    
                    Log.d(TAG, "========================================")
                    Log.d(TAG, "✅ DEMO Subscription Activated in ViewModel!")
                    Log.d(TAG, "Type: ${_generationLimit.value?.subscriptionType?.uppercase()}")
                    Log.d(TAG, "Limit: ${_generationLimit.value?.subscriptionLimit}")
                    Log.d(TAG, "Remaining: ${_generationLimit.value?.getRemainingGenerations()}")
                    Log.d(TAG, "✅ UserPreferences premium status synced: true")
                    Log.d(TAG, "========================================")
                    
                    // Reset UI state
                    _uiState.value = GenerateUiState.Idle
                } else {
                    Log.e(TAG, "Failed to activate subscription: ${result.exceptionOrNull()?.message}")
                    _uiState.value = GenerateUiState.Error("Failed to activate subscription")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error activating demo subscription", e)
                _uiState.value = GenerateUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Fix Firebase data inconsistency for current user
     * Synchronizes subscription data between users and generation_limits collections
     */
    fun fixFirebaseData() {
        viewModelScope.launch {
            try {
                val userId = authManager.getCurrentUserId()
                if (userId == null) {
                    Log.e(TAG, "User not authenticated")
                    return@launch
                }
                
                Log.d(TAG, "🔧 Fixing Firebase data inconsistency...")
                
                val firebaseDataFixer = FirebaseDataFixer(FirebaseFirestore.getInstance())
                val result = firebaseDataFixer.fixCurrentUserData(userId)
                
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Firebase data fixed successfully")
                    // Reload generation limit to reflect changes
                    loadGenerationLimit()
                } else {
                    Log.e(TAG, "❌ Failed to fix Firebase data: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fixing Firebase data", e)
            }
        }
    }
    
    /**
     * Clean up duplicate users created by faulty account linking
     */
    fun cleanupDuplicateUsers() {
        viewModelScope.launch {
            try {
                val userId = authManager.getCurrentUserId()
                if (userId == null) {
                    Log.e(TAG, "Cannot cleanup duplicates: User not authenticated")
                    return@launch
                }
                
                val currentUser = authManager.getCurrentUserId()
                val email = auth.currentUser?.email
                val displayName = auth.currentUser?.displayName
                
                Log.d(TAG, "🧹 Starting duplicate user cleanup...")
                Log.d(TAG, "Current user: $userId")
                Log.d(TAG, "Email: $email")
                Log.d(TAG, "Display Name: $displayName")
                
                val duplicateCleaner = com.doyouone.drawai.utils.DuplicateUserCleaner
                val result = duplicateCleaner.cleanupDuplicateUsers(
                    firestore = FirebaseFirestore.getInstance(),
                    email = email,
                    displayName = displayName
                )
                
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Duplicate cleanup completed: ${result.getOrNull()}")
                    // Reload generation limit to reflect changes
                    loadGenerationLimit()
                } else {
                    Log.e(TAG, "❌ Failed to cleanup duplicates: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during duplicate cleanup", e)
            }
        }
    }
    
    /**
     * Find and list duplicate users (for debugging)
     */
    fun findDuplicateUsers() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔍 Finding duplicate users...")
                
                val duplicateCleaner = com.doyouone.drawai.utils.DuplicateUserCleaner
                val result = duplicateCleaner.findDuplicateUsers(FirebaseFirestore.getInstance())
                
                if (result.isSuccess) {
                    val duplicates = result.getOrNull() ?: emptyList()
                    if (duplicates.isEmpty()) {
                        Log.d(TAG, "✅ No duplicate users found")
                    } else {
                        Log.d(TAG, "🔍 Found duplicates:")
                        duplicates.forEach { Log.d(TAG, it) }
                    }
                } else {
                    Log.e(TAG, "❌ Failed to find duplicates: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finding duplicates", e)
            }
        }
    }
    
    /**
     * Generate image in background using WorkManager
     * User can leave the app while generation is running
     */
    fun generateImageInBackground(
        positivePrompt: String,
        negativePrompt: String = "",
        workflow: String = "standard"
    ) {
        // Validation
        if (!_isNetworkAvailable.value) {
            _uiState.value = GenerateUiState.Error("Tidak ada koneksi internet")
            return
        }
        
        if (positivePrompt.isBlank()) {
            _uiState.value = GenerateUiState.Error("Prompt tidak boleh kosong")
            return
        }
        
        // Content moderation
        val positiveCheck = checkPromptInappropriate(positivePrompt)
        val negativeCheck = checkPromptInappropriate(negativePrompt)
        
        if (positiveCheck.first || negativeCheck.first) {
            _uiState.value = GenerateUiState.ContentModeration(
                message = getApplication<Application>().getString(R.string.content_moderation_description),
                positiveInappropriateWords = if (positiveCheck.first) positiveCheck.second else emptyList(),
                negativeInappropriateWords = if (negativeCheck.first) negativeCheck.second else emptyList()
            )
            return
        }
        
        // Get userId
        val userId = authManager.getCurrentUserId()
        if (userId == null) {
            _uiState.value = GenerateUiState.Error("User tidak terautentikasi. Silakan login terlebih dahulu.")
            return
        }
        
        Log.d(TAG, "🔄 Starting background generation with WorkManager")
        
        // Create work request
        val workData = androidx.work.workDataOf(
            com.doyouone.drawai.worker.ImageGenerationWorker.KEY_POSITIVE_PROMPT to positivePrompt,
            com.doyouone.drawai.worker.ImageGenerationWorker.KEY_NEGATIVE_PROMPT to negativePrompt,
            com.doyouone.drawai.worker.ImageGenerationWorker.KEY_WORKFLOW_ID to workflow,
            com.doyouone.drawai.worker.ImageGenerationWorker.KEY_USER_ID to userId
        )
        
        val workRequest =androidx.work.OneTimeWorkRequestBuilder<com.doyouone.drawai.worker.ImageGenerationWorker>()
            .setInputData(workData)
            .addTag(BACKGROUND_GENERATION_WORK_TAG)
            .build()
        
        // Enqueue work
        workManager.enqueue(workRequest)
        
        _backgroundWorkStatus.value = "Background generation started"
        _uiState.value = GenerateUiState.Loading("Starting background generation...")
        
        // Observe work status
        observeBackgroundWork(workRequest.id)
        
        Log.d(TAG, "✅ Background work enqueued with ID: ${workRequest.id}")
    }
    
    /**
     * Observe background work progress and update UI accordingly
     * Note: Actual progress is shown via WorkManager notifications
     * This just logs the initial enqueue status
     */
    private fun observeBackgroundWork(workId: java.util.UUID) {
        Log.d(TAG, "Background work monitoring started for ID: $workId")
        Log.d(TAG, "Progress will be shown via notifications")
        // WorkManager handles all progress updates via notifications
        // No need to observe here - notifications provide real-time feedback
    }
    
    /**
     * Cancel background generation
     */
    fun cancelBackgroundGeneration() {
        workManager.cancelAllWorkByTag(BACKGROUND_GENERATION_WORK_TAG)
        _backgroundWorkStatus.value = null
        _uiState.value = GenerateUiState.Idle
        Log.d(TAG, "Background generation cancelled")
    }
    
    /**
     * Toggle background generation preference
     */
    fun toggleBackgroundGeneration(enabled: Boolean) {
        _isBackgroundGenerationEnabled.value = enabled
        Log.d(TAG, "Background generation ${if (enabled) "enabled" else "disabled"}")
    }
    

    

}

/**
 * UI State untuk generation screen
 */
sealed class GenerateUiState {
    object Idle : GenerateUiState()
    data class Loading(val message: String) : GenerateUiState()
    data class Processing(val status: String, val taskStatus: TaskStatusResponse?) : GenerateUiState()
    data class Success(val result: TaskStatusResponse) : GenerateUiState()
    data class Error(val message: String, val messageResId: Int? = null) : GenerateUiState()
    data class LimitExceeded(val message: String, val remaining: Int) : GenerateUiState()
    data class ContentModeration(
        val message: String,
        val positiveInappropriateWords: List<String>,
        val negativeInappropriateWords: List<String>,
        val messageResId: Int? = null
    ) : GenerateUiState()
    data class Maintenance(val message: String? = null) : GenerateUiState()
}
