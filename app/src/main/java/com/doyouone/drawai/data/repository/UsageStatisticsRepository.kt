package com.doyouone.drawai.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * UsageStatisticsRepository - Tracks user generation statistics in Firestore
 */
class UsageStatisticsRepository(private val userId: String) {
    private val firestore = FirebaseFirestore.getInstance()
    private val userStatsRef = firestore.collection("users").document(userId)
        .collection("statistics").document("usage")
    
    companion object {
        private const val TAG = "UsageStatistics"
        private const val FIELD_TOTAL_GENERATIONS = "total_generations"
        private const val FIELD_TOTAL_SAVES = "total_saves"
        private const val FIELD_TOTAL_FAVORITES = "total_favorites"
        private const val FIELD_LAST_GENERATION_DATE = "last_generation_date"
        private const val FIELD_FIRST_GENERATION_DATE = "first_generation_date"
        private const val FIELD_LAST_UPDATED = "last_updated"
        
        private fun getCurrentDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            return sdf.format(Date())
        }
    }
    
    data class UsageStats(
        val totalGenerations: Int = 0,
        val totalSaves: Int = 0,
        val totalFavorites: Int = 0,
        val lastGenerationDate: String = "",
        val firstGenerationDate: String = "",
        val lastUpdated: Long = 0
    )
    
    /**
     * Get usage statistics
     */
    suspend fun getUsageStats(): UsageStats {
        return try {
            val snapshot = userStatsRef.get().await()
            
            if (snapshot.exists()) {
                UsageStats(
                    totalGenerations = snapshot.getLong(FIELD_TOTAL_GENERATIONS)?.toInt() ?: 0,
                    totalSaves = snapshot.getLong(FIELD_TOTAL_SAVES)?.toInt() ?: 0,
                    totalFavorites = snapshot.getLong(FIELD_TOTAL_FAVORITES)?.toInt() ?: 0,
                    lastGenerationDate = snapshot.getString(FIELD_LAST_GENERATION_DATE) ?: "",
                    firstGenerationDate = snapshot.getString(FIELD_FIRST_GENERATION_DATE) ?: "",
                    lastUpdated = snapshot.getLong(FIELD_LAST_UPDATED) ?: 0
                )
            } else {
                UsageStats()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting usage stats: ${e.message}")
            UsageStats()
        }
    }
    
    /**
     * Increment generation count
     */
    suspend fun incrementGenerations(): Result<Unit> {
        return try {
            val snapshot = userStatsRef.get().await()
            val currentCount = snapshot.getLong(FIELD_TOTAL_GENERATIONS)?.toInt() ?: 0
            val firstDate = snapshot.getString(FIELD_FIRST_GENERATION_DATE)
            val currentDate = getCurrentDate()
            
            val updates = hashMapOf<String, Any>(
                FIELD_TOTAL_GENERATIONS to (currentCount + 1),
                FIELD_LAST_GENERATION_DATE to currentDate,
                FIELD_LAST_UPDATED to System.currentTimeMillis()
            )
            
            // Set first generation date if not exists
            if (firstDate.isNullOrEmpty()) {
                updates[FIELD_FIRST_GENERATION_DATE] = currentDate
            }
            
            userStatsRef.set(updates, SetOptions.merge()).await()
            Log.d(TAG, "Generation count incremented: ${currentCount + 1}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing generations: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Increment save count
     */
    suspend fun incrementSaves(): Result<Unit> {
        return try {
            val snapshot = userStatsRef.get().await()
            val currentCount = snapshot.getLong(FIELD_TOTAL_SAVES)?.toInt() ?: 0
            
            val updates = hashMapOf<String, Any>(
                FIELD_TOTAL_SAVES to (currentCount + 1),
                FIELD_LAST_UPDATED to System.currentTimeMillis()
            )
            
            userStatsRef.set(updates, SetOptions.merge()).await()
            Log.d(TAG, "Save count incremented: ${currentCount + 1}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing saves: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Increment favorite count
     */
    suspend fun incrementFavorites(): Result<Unit> {
        return try {
            val snapshot = userStatsRef.get().await()
            val currentCount = snapshot.getLong(FIELD_TOTAL_FAVORITES)?.toInt() ?: 0
            
            val updates = hashMapOf<String, Any>(
                FIELD_TOTAL_FAVORITES to (currentCount + 1),
                FIELD_LAST_UPDATED to System.currentTimeMillis()
            )
            
            userStatsRef.set(updates, SetOptions.merge()).await()
            Log.d(TAG, "Favorite count incremented: ${currentCount + 1}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing favorites: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Decrement favorite count
     */
    suspend fun decrementFavorites(): Result<Unit> {
        return try {
            val snapshot = userStatsRef.get().await()
            val currentCount = snapshot.getLong(FIELD_TOTAL_FAVORITES)?.toInt() ?: 0
            
            val updates = hashMapOf<String, Any>(
                FIELD_TOTAL_FAVORITES to maxOf(0, currentCount - 1),
                FIELD_LAST_UPDATED to System.currentTimeMillis()
            )
            
            userStatsRef.set(updates, SetOptions.merge()).await()
            Log.d(TAG, "Favorite count decremented: ${maxOf(0, currentCount - 1)}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrementing favorites: ${e.message}")
            Result.failure(e)
        }
    }
}
