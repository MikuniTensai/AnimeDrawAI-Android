package com.doyouone.drawai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * UserPreferences - Manages user subscription and ad preferences
 */
class UserPreferences(private val context: Context) {
    
    companion object {
        private val IS_PREMIUM_KEY = booleanPreferencesKey("is_premium")
        private val GENERATION_COUNT_KEY = intPreferencesKey("generation_count")
        private val AD_FREE_KEY = booleanPreferencesKey("ad_free")
        private val DAILY_GENERATION_LIMIT_KEY = intPreferencesKey("daily_generation_limit")
        private val EXTRA_GENERATIONS_KEY = intPreferencesKey("extra_generations")
        private val GUIDELINES_ACCEPTED_KEY = booleanPreferencesKey("guidelines_accepted")
        
        // Free user limit: 5 generations per day
        const val FREE_USER_DAILY_LIMIT = 5
    }
    
    /**
     * Check if user has premium subscription
     */
    val isPremium: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[IS_PREMIUM_KEY] ?: false
    }
    
    /**
     * Check if user should see ads (not premium)
     */
    val shouldShowAds: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        val isPremium = preferences[IS_PREMIUM_KEY] ?: false
        val isAdFree = preferences[AD_FREE_KEY] ?: false
        !isPremium && !isAdFree
    }
    
    /**
     * Get generation count for today
     */
    val generationCount: Flow<Int> = context.userDataStore.data.map { preferences ->
        preferences[GENERATION_COUNT_KEY] ?: 0
    }
    
    /**
     * Get extra generations (from rewarded ads)
     */
    val extraGenerations: Flow<Int> = context.userDataStore.data.map { preferences ->
        preferences[EXTRA_GENERATIONS_KEY] ?: 0
    }
    
    /**
     * Check if user can generate (has remaining generations)
     */
    fun canGenerate(isPremium: Boolean, currentCount: Int, extraGens: Int): Boolean {
        if (isPremium) return true // Premium users = unlimited
        return (currentCount < FREE_USER_DAILY_LIMIT) || (extraGens > 0)
    }
    
    /**
     * Get remaining generations
     */
    fun getRemainingGenerations(isPremium: Boolean, currentCount: Int, extraGens: Int): Int {
        if (isPremium) return Int.MAX_VALUE // Unlimited
        val dailyRemaining = maxOf(0, FREE_USER_DAILY_LIMIT - currentCount)
        return dailyRemaining + extraGens
    }
    
    /**
     * Set premium status
     */
    suspend fun setPremiumStatus(isPremium: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[IS_PREMIUM_KEY] = isPremium
        }
    }
    
    /**
     * Set ad-free status (for rewarded ads)
     */
    suspend fun setAdFreeStatus(isAdFree: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[AD_FREE_KEY] = isAdFree
        }
    }
    
    /**
     * Increment generation count
     */
    suspend fun incrementGenerationCount() {
        context.userDataStore.edit { preferences ->
            val currentCount = preferences[GENERATION_COUNT_KEY] ?: 0
            val newCount = currentCount + 1
            preferences[GENERATION_COUNT_KEY] = newCount
            android.util.Log.d("UserPreferences", "✅ DataStore incremented: $currentCount → $newCount")
        }
    }
    
    /**
     * Reset generation count (call daily)
     */
    suspend fun resetGenerationCount() {
        context.userDataStore.edit { preferences ->
            preferences[GENERATION_COUNT_KEY] = 0
        }
    }
    
    /**
     * Add extra generations from rewarded ad
     */
    suspend fun addExtraGenerations(amount: Int) {
        context.userDataStore.edit { preferences ->
            val current = preferences[EXTRA_GENERATIONS_KEY] ?: 0
            preferences[EXTRA_GENERATIONS_KEY] = current + amount
        }
    }
    
    /**
     * Use one generation (decrement extra if daily limit reached)
     */
    suspend fun useGeneration() {
        context.userDataStore.edit { preferences ->
            val dailyCount = preferences[GENERATION_COUNT_KEY] ?: 0
            val extraGens = preferences[EXTRA_GENERATIONS_KEY] ?: 0
            
            // Increment daily count
            preferences[GENERATION_COUNT_KEY] = dailyCount + 1
            
            // If over daily limit, use extra generation
            if (dailyCount >= FREE_USER_DAILY_LIMIT && extraGens > 0) {
                preferences[EXTRA_GENERATIONS_KEY] = extraGens - 1
            }
        }
    }
    
    /**
     * Check if user has accepted community guidelines
     */
    val hasAcceptedGuidelines: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[GUIDELINES_ACCEPTED_KEY] ?: false
    }
    
    /**
     * Set community guidelines acceptance
     */
    suspend fun setGuidelinesAccepted(accepted: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[GUIDELINES_ACCEPTED_KEY] = accepted
        }
    }
}
