package com.doyouone.drawai.utils

import android.util.Log
import com.doyouone.drawai.data.model.GenerationLimit
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class to fix Firebase data inconsistency
 * Synchronizes subscription data between users and generation_limits collections
 */
class FirebaseDataFixer(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirebaseDataFixer"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_GENERATION_LIMITS = "generation_limits"
    }

    /**
     * Fix data for current user
     * Synchronizes subscription data from users collection to generation_limits
     */
    suspend fun fixCurrentUserData(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔧 Fixing Firebase data for user: $userId")
            
            // Get user data from users collection
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            
            if (!userDoc.exists()) {
                Log.w(TAG, "User document not found for: $userId")
                return Result.failure(Exception("User document not found"))
            }
            
            val userData = userDoc.data!!
            Log.d(TAG, "📊 User data: ${userData.keys}")
            
            // Determine subscription type from users collection
            val subscriptionType = determineSubscriptionType(userData)
            val subscriptionLimit = getSubscriptionLimit(subscriptionType)
            val isPremium = subscriptionType != "free"
            
            Log.d(TAG, "✅ Determined subscription: $subscriptionType (limit: $subscriptionLimit, premium: $isPremium)")
            
            // Get current generation_limits data
            val generationLimitRef = firestore.collection(COLLECTION_GENERATION_LIMITS)
                .document(userId)
            val generationLimitDoc = generationLimitRef.get().await()
            
            val currentDate = getCurrentDate()
            val timestamp = Timestamp.now()
            
            // Prepare updated generation_limits data
            val generationLimitData = if (generationLimitDoc.exists()) {
                val existingData = generationLimitDoc.data!!
                
                // Update existing document with correct subscription data
                mapOf(
                    "userId" to userId,
                    "subscriptionType" to subscriptionType,
                    "subscriptionLimit" to subscriptionLimit,
                    "isPremium" to isPremium,
                    "subscriptionUsed" to (existingData["subscriptionUsed"] ?: 0),
                    "dailyGenerations" to if (subscriptionType == "free") (existingData["dailyGenerations"] ?: 0) else 0,
                    "maxDailyLimit" to if (subscriptionType == "free") 5 else 0, // Premium users don't have daily limits
                    "lastResetDate" to currentDate,
                    "bonusGenerations" to if (subscriptionType == "free") (existingData["bonusGenerations"] ?: 0) else 0,
                    "totalGenerations" to (existingData["totalGenerations"] ?: 0),
                    "subscriptionEndDate" to if (isPremium) getSubscriptionEndDate() else null,
                    "updatedAt" to timestamp,
                    "createdAt" to (existingData["createdAt"] ?: timestamp)
                )
            } else {
                // Create new document
                mapOf(
                    "userId" to userId,
                    "subscriptionType" to subscriptionType,
                    "subscriptionLimit" to subscriptionLimit,
                    "isPremium" to isPremium,
                    "subscriptionUsed" to 0,
                    "dailyGenerations" to if (subscriptionType == "free") 0 else 0,
                    "maxDailyLimit" to if (subscriptionType == "free") 5 else 0,
                    "lastResetDate" to currentDate,
                    "bonusGenerations" to 0,
                    "totalGenerations" to 0,
                    "subscriptionEndDate" to if (isPremium) getSubscriptionEndDate() else null,
                    "createdAt" to timestamp,
                    "updatedAt" to timestamp
                )
            }
            
            // Update generation_limits collection
            generationLimitRef.set(generationLimitData).await()
            Log.d(TAG, "✅ Updated generation_limits collection")
            
            // Clean up users collection data
            val cleanUserData = mapOf(
                "displayName" to (userData["displayName"] ?: "User${userId.substring(0, 8)}"),
                "isAnonymous" to (userData["isAnonymous"] ?: false),
                "subscriptionPlan" to when (subscriptionType) {
                    "basic" -> "BASIC"
                    "pro" -> "PRO"
                    else -> "FREE"
                },
                "subscriptionActive" to isPremium,
                "premium" to isPremium,
                "generationCount" to (userData["generationCount"] ?: 0),
                "dailyGenerationCount" to if (subscriptionType == "free") (userData["dailyGenerationCount"] ?: 0) else 0,
                "lastGenerationDate" to (userData["lastGenerationDate"] ?: currentDate),
                "lastResetDate" to currentDate,
                "subscriptionExpiryDate" to if (isPremium) getSubscriptionEndDate() else null,
                "updatedAt" to timestamp,
                "createdAt" to (userData["createdAt"] ?: timestamp)
            )
            
            // Update users collection with clean data
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .set(cleanUserData)
                .await()
            
            Log.d(TAG, "✅ Updated users collection with clean data")
            Log.d(TAG, "🎉 Firebase data fix completed successfully for user: $userId")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fixing Firebase data", e)
            Result.failure(e)
        }
    }
    
    private fun determineSubscriptionType(userData: Map<String, Any>): String {
        // Check subscriptionPlan field
        val subscriptionPlan = userData["subscriptionPlan"] as? String
        if (subscriptionPlan != null && subscriptionPlan != "FREE") {
            return when {
                subscriptionPlan.contains("BASIC", ignoreCase = true) -> "basic"
                subscriptionPlan.contains("PRO", ignoreCase = true) -> "pro"
                else -> "free"
            }
        }
        
        // Check premium field
        val premium = userData["premium"] as? Boolean
        if (premium == true) {
            // Default to basic if premium but no specific plan
            return "basic"
        }
        
        // Check subscriptionType field (if exists)
        val subscriptionType = userData["subscriptionType"] as? String
        if (subscriptionType != null && subscriptionType != "free") {
            return subscriptionType
        }
        
        return "free"
    }
    
    private fun getSubscriptionLimit(subscriptionType: String): Int {
        return when (subscriptionType) {
            "basic" -> GenerationLimit.BASIC_LIMIT // 200
            "pro" -> GenerationLimit.PRO_LIMIT     // 600
            else -> 0
        }
    }
    
    private fun getSubscriptionEndDate(): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, GenerationLimit.SUBSCRIPTION_DAYS) // 30 days
        return Timestamp(calendar.time)
    }
    
    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}