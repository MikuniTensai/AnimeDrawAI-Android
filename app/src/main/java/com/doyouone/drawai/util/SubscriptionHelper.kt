package com.doyouone.drawai.util

import android.content.Context
import android.util.Log
import com.doyouone.drawai.data.repository.FirebaseGenerationRepository
import com.google.firebase.Timestamp
import java.util.*

/**
 * Helper class untuk manage subscription dan premium features
 */
class SubscriptionHelper(private val context: Context) {
    
    private val firebaseRepo = FirebaseGenerationRepository()
    
    companion object {
        private const val TAG = "SubscriptionHelper"
        
        // Subscription plans (in days)
        const val PLAN_MONTHLY = 30
        const val PLAN_YEARLY = 365
        const val PLAN_LIFETIME = 36500 // 100 years
    }
    
    /**
     * Aktivasi premium subscription setelah payment berhasil
     * 
     * @param userId User ID dari Firebase Auth
     * @param durationDays Durasi subscription dalam hari
     * @return Result dengan success/failure
     */
    suspend fun activatePremium(
        userId: String,
        durationDays: Int
    ): Result<Unit> {
        return try {
            val endDate = calculateEndDate(durationDays)
            
            Log.d(TAG, "Activating premium for user: $userId")
            Log.d(TAG, "Duration: $durationDays days, End: $endDate")
            
            val result = firebaseRepo.updatePremiumStatus(
                userId = userId,
                isPremium = true,
                subscriptionEndDate = endDate
            )
            
            if (result.isSuccess) {
                Log.d(TAG, "✅ Premium activated successfully")
            } else {
                Log.e(TAG, "❌ Failed to activate premium: ${result.exceptionOrNull()?.message}")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error activating premium", e)
            Result.failure(e)
        }
    }
    
    /**
     * Deaktivasi premium (misal: refund atau manual cancel)
     */
    suspend fun deactivatePremium(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deactivating premium for user: $userId")
            
            val result = firebaseRepo.updatePremiumStatus(
                userId = userId,
                isPremium = false,
                subscriptionEndDate = null
            )
            
            if (result.isSuccess) {
                Log.d(TAG, "✅ Premium deactivated")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error deactivating premium", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check apakah user adalah premium
     */
    suspend fun isPremiumUser(userId: String): Boolean {
        return try {
            val result = firebaseRepo.checkAndUpdateSubscription(userId)
            result.getOrNull() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking premium status", e)
            false
        }
    }
    
    /**
     * Extend existing subscription
     */
    suspend fun extendSubscription(
        userId: String,
        additionalDays: Int
    ): Result<Unit> {
        return try {
            // Get current subscription
            val limitResult = firebaseRepo.getGenerationLimit(userId)
            if (limitResult.isFailure) {
                return Result.failure(
                    limitResult.exceptionOrNull() ?: Exception("Failed to get current subscription")
                )
            }
            
            val currentLimit = limitResult.getOrNull()!!
            val currentEndDate = currentLimit.subscriptionEndDate
            
            // Calculate new end date
            val baseDate = if (currentEndDate != null && currentEndDate.toDate() > Date()) {
                // Extend from current end date
                currentEndDate.toDate()
            } else {
                // Start from now
                Date()
            }
            
            val calendar = Calendar.getInstance()
            calendar.time = baseDate
            calendar.add(Calendar.DAY_OF_YEAR, additionalDays)
            val newEndDate = Timestamp(calendar.time)
            
            Log.d(TAG, "Extending subscription for user: $userId by $additionalDays days")
            
            firebaseRepo.updatePremiumStatus(
                userId = userId,
                isPremium = true,
                subscriptionEndDate = newEndDate
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extending subscription", e)
            Result.failure(e)
        }
    }
    
    /**
     * Calculate subscription end date
     */
    private fun calculateEndDate(durationDays: Int): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, durationDays)
        return Timestamp(calendar.time)
    }
    
    /**
     * Get remaining days untuk subscription
     */
    suspend fun getRemainingDays(userId: String): Int? {
        return try {
            val limitResult = firebaseRepo.getGenerationLimit(userId)
            if (limitResult.isFailure) return null
            
            val limit = limitResult.getOrNull()!!
            if (!limit.isPremium || limit.subscriptionEndDate == null) return null
            
            val endDate = limit.subscriptionEndDate.toDate()
            val now = Date()
            
            if (endDate <= now) return 0
            
            val diffMs = endDate.time - now.time
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
            
            diffDays
        } catch (e: Exception) {
            Log.e(TAG, "Error getting remaining days", e)
            null
        }
    }
    
    /**
     * Get subscription status message
     */
    suspend fun getSubscriptionStatusMessage(userId: String): String {
        return try {
            val isPremium = isPremiumUser(userId)
            
            if (!isPremium) {
                return "Free Plan - 5 generations/day"
            }
            
            val remainingDays = getRemainingDays(userId)
            
            when {
                remainingDays == null -> "Premium - Lifetime"
                remainingDays > 365 -> "Premium - Lifetime"
                remainingDays > 30 -> "Premium - ${remainingDays / 30} bulan tersisa"
                remainingDays > 0 -> "Premium - $remainingDays hari tersisa"
                else -> "Premium expired - Renew now!"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting status message", e)
            "Unknown"
        }
    }
}
