package com.doyouone.drawai.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * GenerationTrackingRepository - Tracks daily generation usage in Firestore
 * Provides server-side validation and daily reset
 */
class GenerationTrackingRepository(private val userId: String) {
    private val firestore = FirebaseFirestore.getInstance()
    private val userGenerationsRef = firestore.collection("users").document(userId)
        .collection("generation_tracking").document("daily")
    
    companion object {
        private const val TAG = "GenerationTracking"
        private const val FIELD_DATE = "date"
        private const val FIELD_COUNT = "count"
        private const val FIELD_EXTRA = "extra_generations"
        private const val FIELD_LAST_UPDATED = "last_updated"
        
        private fun getTodayDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }
    }
    
    /**
     * Get today's generation count from Firestore
     */
    suspend fun getTodayGenerationCount(): Int {
        return try {
            val snapshot = userGenerationsRef.get().await()
            
            if (snapshot.exists()) {
                val storedDate = snapshot.getString(FIELD_DATE)
                val todayDate = getTodayDate()
                
                // Check if it's a new day
                if (storedDate == todayDate) {
                    // Same day, return stored count
                    snapshot.getLong(FIELD_COUNT)?.toInt() ?: 0
                } else {
                    // New day, reset to 0
                    resetDailyCount()
                    0
                }
            } else {
                // No data yet, initialize
                initializeTracking()
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting generation count: ${e.message}")
            0
        }
    }
    
    /**
     * Get extra generations (from rewarded ads)
     */
    suspend fun getExtraGenerations(): Int {
        return try {
            val snapshot = userGenerationsRef.get().await()
            snapshot.getLong(FIELD_EXTRA)?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting extra generations: ${e.message}")
            0
        }
    }
    
    /**
     * Increment generation count (use one generation)
     */
    suspend fun incrementGenerationCount(): Result<Unit> {
        return try {
            val snapshot = userGenerationsRef.get().await()
            val storedDate = snapshot.getString(FIELD_DATE)
            val todayDate = getTodayDate()
            
            // Check if it's a new day
            if (storedDate != todayDate) {
                // New day, reset before incrementing
                resetDailyCount()
            }
            
            val currentCount = snapshot.getLong(FIELD_COUNT)?.toInt() ?: 0
            val extraGens = snapshot.getLong(FIELD_EXTRA)?.toInt() ?: 0
            
            // Update count
            val updates = hashMapOf<String, Any>(
                FIELD_COUNT to (currentCount + 1),
                FIELD_DATE to todayDate,
                FIELD_LAST_UPDATED to System.currentTimeMillis()
            )
            
            // If over daily limit, use extra generation
            if (currentCount >= com.doyouone.drawai.data.preferences.UserPreferences.FREE_USER_DAILY_LIMIT 
                && extraGens > 0) {
                updates[FIELD_EXTRA] = extraGens - 1
            }
            
            userGenerationsRef.set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing count: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Add extra generations from rewarded ad
     */
    suspend fun addExtraGenerations(amount: Int): Result<Unit> {
        return try {
            val snapshot = userGenerationsRef.get().await()
            val currentExtra = snapshot.getLong(FIELD_EXTRA)?.toInt() ?: 0
            
            val updates = hashMapOf<String, Any>(
                FIELD_EXTRA to (currentExtra + amount),
                FIELD_LAST_UPDATED to System.currentTimeMillis()
            )
            
            userGenerationsRef.set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding extra generations: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Check if user can generate (has remaining generations)
     */
    suspend fun canGenerate(isPremium: Boolean): Boolean {
        if (isPremium) return true
        
        return try {
            val count = getTodayGenerationCount()
            val extra = getExtraGenerations()
            val limit = com.doyouone.drawai.data.preferences.UserPreferences.FREE_USER_DAILY_LIMIT
            
            (count < limit) || (extra > 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking can generate: ${e.message}")
            false
        }
    }
    
    /**
     * Get remaining generations
     */
    suspend fun getRemainingGenerations(isPremium: Boolean): Int {
        if (isPremium) return Int.MAX_VALUE
        
        return try {
            val count = getTodayGenerationCount()
            val extra = getExtraGenerations()
            val limit = com.doyouone.drawai.data.preferences.UserPreferences.FREE_USER_DAILY_LIMIT
            
            val dailyRemaining = maxOf(0, limit - count)
            dailyRemaining + extra
        } catch (e: Exception) {
            Log.e(TAG, "Error getting remaining generations: ${e.message}")
            0
        }
    }
    
    /**
     * Reset daily count (called on new day)
     */
    private suspend fun resetDailyCount() {
        try {
            val todayDate = getTodayDate()
            val updates = hashMapOf<String, Any>(
                FIELD_DATE to todayDate,
                FIELD_COUNT to 0,
                FIELD_LAST_UPDATED to System.currentTimeMillis()
            )
            
            userGenerationsRef.set(updates, SetOptions.merge()).await()
            Log.d(TAG, "Daily count reset for new day: $todayDate")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting daily count: ${e.message}")
        }
    }
    
    /**
     * Initialize tracking document
     */
    private suspend fun initializeTracking() {
        try {
            val todayDate = getTodayDate()
            val data = hashMapOf(
                FIELD_DATE to todayDate,
                FIELD_COUNT to 0,
                FIELD_EXTRA to 0,
                FIELD_LAST_UPDATED to System.currentTimeMillis()
            )
            
            userGenerationsRef.set(data).await()
            Log.d(TAG, "Tracking initialized for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing tracking: ${e.message}")
        }
    }
}
