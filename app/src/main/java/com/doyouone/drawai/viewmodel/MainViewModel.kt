package com.doyouone.drawai.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doyouone.drawai.auth.AuthManager
import com.doyouone.drawai.data.preferences.UserPreferences
import com.doyouone.drawai.data.repository.DrawAIRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ViewModel for MainActivity
 * Handles global app state initialization and synchronization
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)
    private val repository = DrawAIRepository()
    private val userPreferences = UserPreferences(application)

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _generationLimit = kotlinx.coroutines.flow.MutableStateFlow<com.doyouone.drawai.data.model.GenerationLimit?>(null)
    val generationLimit: kotlinx.coroutines.flow.StateFlow<com.doyouone.drawai.data.model.GenerationLimit?> = _generationLimit

    private val _characterCount = kotlinx.coroutines.flow.MutableStateFlow(0)
    val characterCount: kotlinx.coroutines.flow.StateFlow<Int> = _characterCount

    // Track current user ID and jobs
    private var currentUserId: String? = null
    private var subscriptionJob: Job? = null
    private var characterCountJob: Job? = null

    init {
        // Listen to auth state changes
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                val newUserId = user?.uid
                if (newUserId != currentUserId) {
                    Log.d(TAG, "👤 User changed: $currentUserId -> $newUserId")
                    handleUserChange(newUserId)
                }
            }
        }
    }

    /**
     * Handle user change (login/logout/switch)
     */
    private fun handleUserChange(newUserId: String?) {
        // Cancel existing listeners
        subscriptionJob?.cancel()
        characterCountJob?.cancel()
        
        // Clear data
        _generationLimit.value = null
        _characterCount.value = 0
        
        // Update current user
        currentUserId = newUserId
        
        if (newUserId != null) {
            Log.d(TAG, "🔄 Starting listeners for new user: $newUserId")
            syncSubscriptionStatus()
            fetchCharacterCount()
        } else {
            Log.d(TAG, "🚪 User logged out, data cleared")
        }
    }

    /**
     * Sync subscription status from Firebase to local UserPreferences
     */
    private fun syncSubscriptionStatus() {
        subscriptionJob = viewModelScope.launch {
            try {
                val userId = currentUserId
                if (userId != null) {
                    Log.d(TAG, "🔄 Starting subscription listener for user: $userId")
                    
                    // Collect real-time updates
                    repository.getGenerationLimitFlow(userId).collect { limit ->
                        
                        // Check if subscription expired
                        if (limit.subscriptionType != com.doyouone.drawai.data.model.GenerationLimit.TYPE_FREE && limit.isSubscriptionExpired()) {
                            Log.w(TAG, "⚠️ Subscription EXPIRED for user $userId. Downgrading to FREE.")
                            repository.downgradeToFree(userId)
                            // Flow will re-trigger with updated data
                        }
                        
                        _generationLimit.value = limit
                        
                        // Update premium status in UserPreferences
                        // FIX: Check for expiration! If expired, treat as NOT premium so ads appear.
                        // We trust isSubscriptionExpired() which checks date vs now.
                        val isExpired = limit.isSubscriptionExpired()
                        val effectivePremium = limit.isPremium && !isExpired
                        
                        if (isExpired && limit.isPremium) {
                             Log.d(TAG, "⚠️ User has isPremium=true but is expired. Forcing local pref to false.")
                        }
                        
                        userPreferences.setPremiumStatus(effectivePremium)
                        Log.d(TAG, "✅ Subscription updated: isPremium=${limit.isPremium}, Expired=$isExpired, Effective=$effectivePremium")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing subscription status", e)
            }
        }
    }

    /**
     * Fetch current character count (real-time updates)
     */
    private fun fetchCharacterCount() {
        characterCountJob = viewModelScope.launch {
            try {
                val userId = currentUserId
                if (userId != null) {
                    Log.d(TAG, "🔄 Starting character count listener for user: $userId")
                    // Collect real-time updates from Firestore
                    repository.getCharacterCountFlow(userId).collect { count ->
                        _characterCount.value = count
                        Log.d(TAG, "✅ Character count updated: $count")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching character count", e)
            }
        }
    }
}
