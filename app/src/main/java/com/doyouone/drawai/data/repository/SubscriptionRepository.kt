package com.doyouone.drawai.data.repository

import com.doyouone.drawai.data.model.SubscriptionPlan
import com.doyouone.drawai.data.model.UserSubscription
import com.doyouone.drawai.data.repository.FirebaseGenerationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class SubscriptionRepository(private val userId: String) {
    private val firestore = FirebaseFirestore.getInstance()
    private val userDoc = firestore.collection("users").document(userId)
    
    // Get user subscription as Flow
    fun getSubscription(): Flow<UserSubscription> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(UserSubscription())
            close()
            return@callbackFlow
        }

        val listener = userDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Log error but don't crash the flow
                android.util.Log.e("SubscriptionRepo", "Error fetching subscription: ${error.message}")
                trySend(UserSubscription()) // Default to free plan on error
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                val planName = snapshot.getString("subscriptionPlan") ?: "FREE"
                val plan = try {
                    SubscriptionPlan.valueOf(planName.uppercase())
                } catch (e: IllegalArgumentException) {
                    SubscriptionPlan.FREE
                }
                
                val subscription = UserSubscription(
                    plan = plan,
                    generationUsed = snapshot.getLong("generationUsed")?.toInt() ?: 0,
                    dailyGenerationCount = snapshot.getLong("dailyGenerationCount")?.toInt() ?: 0,
                    lastGenerationDate = snapshot.getString("lastGenerationDate") ?: "",
                    subscriptionStartDate = snapshot.getString("subscriptionStartDate") ?: "",
                    expiryDate = snapshot.getString("subscriptionExpiryDate"),
                    isActive = snapshot.getBoolean("subscriptionActive") ?: true,
                    moreAccess = snapshot.getBoolean("moreAccess") ?: false
                )
                
                trySend(subscription)
            } else {
                // Default to FREE plan
                trySend(UserSubscription())
            }
        }
        
        awaitClose { listener.remove() }
    }
    
    // Initialize subscription for new user
    suspend fun initializeSubscription(): Result<Unit> {
        return try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            userDoc.update(
                mapOf(
                    "subscriptionPlan" to SubscriptionPlan.FREE.name,
                    "generationUsed" to 0,
                    "dailyGenerationCount" to 0,
                    "lastGenerationDate" to today,
                    "subscriptionStartDate" to today,
                    "subscriptionActive" to true,
                    "subscriptionExpiryDate" to null
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Upgrade subscription
    suspend fun upgradePlan(plan: SubscriptionPlan, durationMonths: Int = 1): Result<Unit> {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Date()
            val calendar = Calendar.getInstance()
            calendar.time = today
            calendar.add(Calendar.MONTH, durationMonths)
            val expiryDate = dateFormat.format(calendar.time)
            
            userDoc.update(
                mapOf(
                    "subscriptionPlan" to plan.name,
                    "subscriptionExpiryDate" to expiryDate,
                    "subscriptionActive" to true,
                    "generationUsed" to 0, // Reset counter on upgrade
                    "dailyGenerationCount" to 0,
                    "subscriptionStartDate" to dateFormat.format(today)
                )
            ).await()

            // Keep generation_limits in sync so UI unlocks immediately
            val mappedType = when (plan) {
                SubscriptionPlan.BASIC -> "basic"
                SubscriptionPlan.PRO -> "pro"
                else -> "free"
            }
            if (mappedType != "free") {
                val genRepo = FirebaseGenerationRepository()
                // Use production-ready method
                val genResult = genRepo.activateSubscription(
                    userId = userId,
                    subscriptionType = mappedType,
                    durationDays = durationMonths * 30 // Approximate month
                )
                if (genResult.isFailure) {
                    return Result.failure(genResult.exceptionOrNull() ?: Exception("Failed to update generation_limits"))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Record generation
    suspend fun recordGeneration(): Result<Unit> {
        return try {
            val snapshot = userDoc.get().await()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            val lastGenerationDate = snapshot.getString("lastGenerationDate") ?: ""
            val subscriptionStartDate = snapshot.getString("subscriptionStartDate") ?: ""
            val generationUsed = snapshot.getLong("generationUsed")?.toInt() ?: 0
            val dailyGenerationCount = snapshot.getLong("dailyGenerationCount")?.toInt() ?: 0
            
            // Reset daily counter if new day
            val newDailyCount = if (lastGenerationDate == today) {
                dailyGenerationCount + 1
            } else {
                1
            }
            
            // Increment generation used
            userDoc.update(
                mapOf(
                    "generationUsed" to generationUsed + 1,
                    "dailyGenerationCount" to newDailyCount,
                    "lastGenerationDate" to today
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Check if user can generate
    suspend fun canGenerate(): Result<Boolean> {
        return try {
            val snapshot = userDoc.get().await()
            
            val planName = snapshot.getString("subscriptionPlan") ?: "FREE"
            val plan = SubscriptionPlan.valueOf(planName)
            
            val subscription = UserSubscription(
                plan = plan,
                generationUsed = snapshot.getLong("generationUsed")?.toInt() ?: 0,
                dailyGenerationCount = snapshot.getLong("dailyGenerationCount")?.toInt() ?: 0,
                lastGenerationDate = snapshot.getString("lastGenerationDate") ?: "",
                subscriptionStartDate = snapshot.getString("subscriptionStartDate") ?: "",
                expiryDate = snapshot.getString("subscriptionExpiryDate"),
                isActive = snapshot.getBoolean("subscriptionActive") ?: true,
                moreAccess = snapshot.getBoolean("moreAccess") ?: false
            )
            
            Result.success(subscription.canGenerate())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add chat limit (for consumables)
    suspend fun addChatLimit(amount: Int): Result<Unit> {
        return try {
            // Update generation_limits collection directly
            val limitRef = firestore.collection("generation_limits").document(userId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(limitRef)
                if (snapshot.exists()) {
                    val currentLimit = snapshot.getLong("maxChatLimit")?.toInt() ?: 1
                    transaction.update(limitRef, "maxChatLimit", currentLimit + amount)
                } else {
                    // Create if not exists (should exist, but safe fallback)
                    val newLimit = hashMapOf(
                        "userId" to userId,
                        "maxChatLimit" to 1 + amount,
                        "maxDailyLimit" to 5 // Default
                    )
                    transaction.set(limitRef, newLimit)
                }
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Activate 1 Day Access (Day Pass - treated as BASIC)
    suspend fun activateDayPass(plan: SubscriptionPlan = SubscriptionPlan.BASIC): Result<Unit> {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Date()
            val calendar = Calendar.getInstance()
            calendar.time = today
            calendar.add(Calendar.DAY_OF_YEAR, 1) // Valid for 1 day
            val expiryDate = dateFormat.format(calendar.time)
            
            userDoc.update(
                mapOf(
                    "subscriptionPlan" to plan.name,
                    "subscriptionExpiryDate" to expiryDate,
                    "subscriptionActive" to true,
                    "generationUsed" to 0, 
                    "dailyGenerationCount" to 0,
                    "subscriptionStartDate" to dateFormat.format(today),
                    "moreAccess" to true
                )
            ).await()

            // Keep generation_limits in sync
            val mappedType = when (plan) {
                SubscriptionPlan.BASIC -> "basic"
                SubscriptionPlan.PRO -> "pro"
                else -> "basic"
            }
            
            val genRepo = FirebaseGenerationRepository()
            val genResult = genRepo.activateSubscription(
                userId = userId,
                subscriptionType = mappedType,
                durationDays = 1
            )
            
            if (genResult.isFailure) {
                return Result.failure(genResult.exceptionOrNull() ?: Exception("Failed to update generation_limits"))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add bonus generation (from ads)
    suspend fun addBonusGeneration(amount: Int): Result<Unit> {
        return try {
            val limitRef = firestore.collection("generation_limits").document(userId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(limitRef)
                if (snapshot.exists()) {
                    transaction.update(limitRef, "bonusGenerations", com.google.firebase.firestore.FieldValue.increment(amount.toLong()))
                } else {
                    // Initialize if missing
                    val newLimit = hashMapOf(
                        "userId" to userId,
                        "maxChatLimit" to 1,
                        "maxDailyLimit" to 5,
                        "bonusGenerations" to amount
                    )
                    transaction.set(limitRef, newLimit)
                }
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
