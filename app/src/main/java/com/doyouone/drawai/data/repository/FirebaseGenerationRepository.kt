package com.doyouone.drawai.data.repository

import android.util.Log
import com.doyouone.drawai.data.model.GenerationLimit
import com.doyouone.drawai.data.model.GenerationLimitExceededException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository untuk mengelola generation limits di Firebase Firestore
 */
class FirebaseGenerationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    companion object {
        private const val TAG = "FirebaseGenerationRepo"
        private const val COLLECTION_GENERATION_LIMITS = "generation_limits"
        private const val FREE_USER_DAILY_LIMIT = 5
        
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    
    /**
     * Get generation limit data untuk user
     * Jika belum ada, create baru
     */
    suspend fun getGenerationLimit(userId: String): Result<GenerationLimit> {
        return try {
            Log.d(TAG, "Getting generation limit for user: $userId")
            
            val docRef = firestore.collection(COLLECTION_GENERATION_LIMITS)
                .document(userId)
            
            val snapshot = docRef.get().await()
            
            if (snapshot.exists()) {
                val limit = snapshot.toObject(GenerationLimit::class.java)
                if (limit != null) {
                    // Auto-correct isPremium if subscriptionType is not free (Data Consistency Fix)
                    if (!limit.isPremium && limit.subscriptionType != "free" && 
                        (limit.subscriptionType == "basic" || limit.subscriptionType == "pro")) {
                        Log.w(TAG, "⚠️ Data Inconsistency found: isPremium=false but type=${limit.subscriptionType}. Auto-fixing...")
                        
                        // Fix in Firestore
                        docRef.update("isPremium", true)
                        
                        // Return fixed object
                        val fixedLimit = limit.copy(isPremium = true)
                        Log.d(TAG, "✅ Auto-fixed limit: ${fixedLimit.dailyGenerations}/${fixedLimit.maxDailyLimit}")
                        return Result.success(fixedLimit)
                    }

                    // Auto-correct maxDailyLimit for Free users (Fix 0/0 issue)
                    if (limit.subscriptionType == "free" && limit.maxDailyLimit < FREE_USER_DAILY_LIMIT) {
                         Log.w(TAG, "⚠️ Free user has invalid maxDailyLimit (${limit.maxDailyLimit}). Fixing to $FREE_USER_DAILY_LIMIT...")
                         
                         docRef.update("maxDailyLimit", FREE_USER_DAILY_LIMIT).await()
                         
                         // Fix local object
                         val fixedLimit = limit.copy(maxDailyLimit = FREE_USER_DAILY_LIMIT)
                         return Result.success(fixedLimit)
                    }

                    // Check if need reset
                    val currentDate = getCurrentDate()
                    if (limit.needsReset(currentDate)) {
                        Log.d(TAG, "Reset needed for user $userId")
                        return resetDailyLimit(userId)
                    }
                    
                    Log.d(TAG, "✅ Found limit: ${limit.dailyGenerations}/${limit.maxDailyLimit}, isPremium=${limit.isPremium}")
                    Result.success(limit)
                } else {
                    Log.e(TAG, "Failed to parse GenerationLimit")
                    Result.failure(Exception("Failed to parse generation limit"))
                }
            } else {
                // Create new limit for new user
                Log.d(TAG, "Creating new generation limit for user: $userId")
                createGenerationLimit(userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting generation limit", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create generation limit baru untuk user baru
     */
    private suspend fun createGenerationLimit(userId: String): Result<GenerationLimit> {
        return try {
            val currentDate = getCurrentDate()
            val timestamp = Timestamp.now()
            
            val newLimit = GenerationLimit(
                userId = userId,
                dailyGenerations = 0,
                maxDailyLimit = FREE_USER_DAILY_LIMIT,
                lastResetDate = currentDate,
                isPremium = false,
                totalGenerations = 0,
                createdAt = timestamp,
                updatedAt = timestamp
            )
            
            firestore.collection(COLLECTION_GENERATION_LIMITS)
                .document(userId)
                .set(newLimit)
                .await()
            
            Log.d(TAG, "✅ Created new generation limit for user: $userId")
            Result.success(newLimit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating generation limit", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check apakah user bisa generate dan increment counter
     * @throws GenerationLimitExceededException jika limit tercapai
     */
    suspend fun checkAndIncrementGeneration(userId: String): Result<GenerationLimit> {
        return try {
            Log.d(TAG, "Checking and incrementing generation for user: $userId")
            
            // Get current limit
            val limitResult = getGenerationLimit(userId)
            if (limitResult.isFailure) {
                return limitResult
            }
            
            val currentLimit = limitResult.getOrNull()!!
            
            // Check if can generate
            if (!currentLimit.canGenerate()) {
                Log.w(TAG, "❌ User $userId exceeded limit: ${currentLimit.dailyGenerations}/${currentLimit.maxDailyLimit}")
                return Result.failure(
                    GenerationLimitExceededException(
                        remaining = currentLimit.getRemainingGenerations(),
                        message = "Daily generation limit reached (${currentLimit.maxDailyLimit}). Subscribe for unlimited generations!"
                    )
                )
            }
            
            // Increment counter
            val docRef = firestore.collection(COLLECTION_GENERATION_LIMITS)
                .document(userId)
            
            val updates = if (currentLimit.subscriptionType != "free") {
                // Subscription user: increment subscriptionUsed
                hashMapOf<String, Any>(
                    "subscriptionUsed" to FieldValue.increment(1),
                    "totalGenerations" to FieldValue.increment(1),
                    "updatedAt" to Timestamp.now()
                )
            } else {
                // Free user: increment dailyGenerations
                hashMapOf<String, Any>(
                    "dailyGenerations" to FieldValue.increment(1),
                    "totalGenerations" to FieldValue.increment(1),
                    "updatedAt" to Timestamp.now()
                )
            }
            
            docRef.update(updates).await()
            
            // Get updated limit
            val updatedSnapshot = docRef.get().await()
            val updatedLimit = updatedSnapshot.toObject(GenerationLimit::class.java)!!
            
            Log.d(TAG, "✅ Generation incremented: ${updatedLimit.dailyGenerations}/${updatedLimit.maxDailyLimit}")
            Result.success(updatedLimit)
            
        } catch (e: GenerationLimitExceededException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing generation", e)
            Result.failure(e)
        }
    }
    
    /**
     * Reset daily limit (dipanggil otomatis saat hari berganti)
     * Also resets bonus generations
     */
    private suspend fun resetDailyLimit(userId: String): Result<GenerationLimit> {
        return try {
            val currentDate = getCurrentDate()

            val updates = hashMapOf<String, Any>(
                "dailyGenerations" to 0,
                "bonusGenerations" to 0, // Reset bonus from ads
                "purchasedGenerations" to 0, // Reset purchased boosters
                "lastResetDate" to currentDate,
                "updatedAt" to Timestamp.now()
            )
            
            val docRef = firestore.collection(COLLECTION_GENERATION_LIMITS)
                .document(userId)
            
            docRef.update(updates).await()
            
            val snapshot = docRef.get().await()
            val limit = snapshot.toObject(GenerationLimit::class.java)!!
            
            Log.d(TAG, "✅ Daily limit reset for user: $userId")
            Result.success(limit)
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting daily limit", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add bonus generation from rewarded ad
     * Free users: max 10 total
     * Basic/Pro users: unlimited
     */
    suspend fun addBonusGeneration(userId: String): Result<GenerationLimit> {
        return try {
            Log.d(TAG, "Adding bonus generation for user: $userId")
            
            // Get current limit
            val limitResult = getGenerationLimit(userId)
            if (limitResult.isFailure) {
                return Result.failure(limitResult.exceptionOrNull() ?: Exception("Failed to get limit"))
            }
            
            val currentLimit = limitResult.getOrNull()!!
            
            // Check limit based on subscription type
            if (currentLimit.subscriptionType == "free" && currentLimit.bonusGenerations >= 50) {
                Log.w(TAG, "❌ Free user already has max bonus generations (50)")
                return Result.failure(Exception("Maximum bonus generations reached (50)"))
            }
            // Basic/Pro users have unlimited bonus
            
            // Increment bonus
            val docRef = firestore.collection(COLLECTION_GENERATION_LIMITS)
                .document(userId)
            
            val updates = hashMapOf<String, Any>(
                "bonusGenerations" to FieldValue.increment(1),
                "updatedAt" to Timestamp.now()
            )
            
            docRef.update(updates).await()
            
            // Get updated limit
            val updatedSnapshot = docRef.get().await()
            val updatedLimit = updatedSnapshot.toObject(GenerationLimit::class.java)!!
            
            val maxBonus = if (currentLimit.subscriptionType == "free") "50" else "∞"
            Log.d(TAG, "✅ Bonus generation added: ${updatedLimit.bonusGenerations}/$maxBonus")
            Result.success(updatedLimit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bonus generation", e)
            Result.failure(e)
        }
    }
    
    /**
     * Activate real subscription (Production ready)
     */
    suspend fun activateSubscription(
        userId: String,
        subscriptionType: String,
        durationDays: Int = 30
    ): Result<GenerationLimit> {
        return try {
            Log.d(TAG, "⚡ Activate Subscription: $subscriptionType for $durationDays days")
            
            val limitResult = getGenerationLimit(userId)
            if (limitResult.isFailure) {
                return Result.failure(limitResult.exceptionOrNull() ?: Exception("Failed to get limit"))
            }
            
            val currentLimit = limitResult.getOrNull()!!
            
            val (limit, chatLimit) = when (subscriptionType) {
                "basic" -> Pair(GenerationLimit.BASIC_LIMIT, GenerationLimit.BASIC_CHAT_LIMIT)
                "pro" -> Pair(GenerationLimit.PRO_LIMIT, GenerationLimit.PRO_CHAT_LIMIT)
                else -> return Result.failure(Exception("Invalid subscription type: $subscriptionType"))
            }
            
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, durationDays)
            val endDate = Timestamp(calendar.time)
            
            // Should we reset usage? For a new purchase (even 1 day), yes probably.
            val subscriptionUsed = 0 
            
            val updates = hashMapOf<String, Any?>(
                "isPremium" to true,
                "subscriptionType" to subscriptionType,
                "subscriptionLimit" to limit,
                "maxChatLimit" to chatLimit,
                "subscriptionUsed" to subscriptionUsed,
                "subscriptionEndDate" to endDate,
                "updatedAt" to Timestamp.now()
            )
            
            val docRef = firestore.collection(COLLECTION_GENERATION_LIMITS).document(userId)
            docRef.update(updates).await()
            
            // Get updated limit
            val updatedSnapshot = docRef.get().await()
            val updatedLimit = updatedSnapshot.toObject(GenerationLimit::class.java)!!
            
            Log.d(TAG, "✅ Subscription Activated: ${updatedLimit.subscriptionType} for $durationDays days")
            Result.success(updatedLimit)
        } catch (e: Exception) {
            Log.e(TAG, "Error activating subscription", e)
            Result.failure(e)
        }
    }

    /**
     * DEMO: Activate subscription (for testing without Google Play billing)
     * Updates Firebase directly with subscription details
     * 
     * SECURITY: BLOCKED in production builds!
     */
    suspend fun activateDemoSubscription(
        userId: String,
        subscriptionType: String, // "basic" or "pro"
        resetIfExists: Boolean = false
    ): Result<GenerationLimit> {
        // SECURITY: Block in production builds
        if (!com.doyouone.drawai.BuildConfig.DEBUG) {
            Log.e(TAG, "❌ SECURITY: Demo subscription BLOCKED in production!")
            return Result.failure(
                Exception("Demo subscription is only available in debug builds. Please purchase through Google Play.")
            )
        }
        
        // Only allow in DEBUG builds for testing
        Log.d(TAG, "🎭 DEBUG MODE: Activating demo subscription")
        
        return try {
            Log.d(TAG, "🎁 Activating DEMO subscription: $subscriptionType for user: $userId")
            
            // Get current limit
            val limitResult = getGenerationLimit(userId)
            if (limitResult.isFailure) {
                return Result.failure(limitResult.exceptionOrNull() ?: Exception("Failed to get limit"))
            }
            
            val currentLimit = limitResult.getOrNull()!!
            
            // Calculate subscription details
            // Calculate subscription details
            val (limit, chatLimit) = when (subscriptionType) {
                "basic" -> Pair(GenerationLimit.BASIC_LIMIT, GenerationLimit.BASIC_CHAT_LIMIT)
                "pro" -> Pair(GenerationLimit.PRO_LIMIT, GenerationLimit.PRO_CHAT_LIMIT)
                else -> return Result.failure(Exception("Invalid subscription type: $subscriptionType"))
            }
            
            // Set end date to 30 days from now
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_MONTH, GenerationLimit.SUBSCRIPTION_DAYS)
            val endDate = Timestamp(calendar.time)
            
            // Determine subscriptionUsed
            val subscriptionUsed = if (resetIfExists || currentLimit.subscriptionType != subscriptionType) {
                0  // Reset counter for new subscription or different plan
            } else {
                currentLimit.subscriptionUsed  // Keep existing counter if renewing same plan
            }
            
            val updates = hashMapOf<String, Any?>(
                "isPremium" to true,
                "subscriptionType" to subscriptionType,
                "subscriptionLimit" to limit,
                "maxChatLimit" to chatLimit,
                "subscriptionUsed" to subscriptionUsed,
                "subscriptionEndDate" to endDate,
                "updatedAt" to Timestamp.now()
            )
            
            val docRef = firestore.collection(COLLECTION_GENERATION_LIMITS)
                .document(userId)
            
            docRef.update(updates).await()
            
            // Get updated limit
            val updatedSnapshot = docRef.get().await()
            val updatedLimit = updatedSnapshot.toObject(GenerationLimit::class.java)!!
            
            Log.d(TAG, "========================================")
            Log.d(TAG, "✅ DEMO Subscription Activated!")
            Log.d(TAG, "Type: ${updatedLimit.subscriptionType.uppercase()}")
            Log.d(TAG, "Limit: ${updatedLimit.subscriptionLimit} generations")
            Log.d(TAG, "Used: ${updatedLimit.subscriptionUsed}")
            Log.d(TAG, "Remaining: ${updatedLimit.getRemainingGenerations()}")
            Log.d(TAG, "Valid until: ${endDate.toDate()}")
            Log.d(TAG, "========================================")
            
            Result.success(updatedLimit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error activating demo subscription", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update premium status (legacy method)
     */
    suspend fun updatePremiumStatus(
        userId: String,
        isPremium: Boolean,
        subscriptionEndDate: Timestamp? = null
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any?>(
                "isPremium" to isPremium,
                "subscriptionEndDate" to subscriptionEndDate,
                "updatedAt" to Timestamp.now()
            )
            
            if (!isPremium) {
                updates["subscriptionType"] = "free"
                updates["maxChatLimit"] = GenerationLimit.FREE_CHAT_LIMIT
            }
            
            firestore.collection(COLLECTION_GENERATION_LIMITS)
                .document(userId)
                .update(updates)
                .await()
            
            Log.d(TAG, "✅ Premium status updated for user: $userId -> $isPremium")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating premium status", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get current date in format yyyy-MM-dd
     */
    private fun getCurrentDate(): String {
        return dateFormatter.format(Date())
    }
    
    /**
     * Check subscription status dan update jika expired
     */
    suspend fun checkAndUpdateSubscription(userId: String): Result<Boolean> {
        return try {
            val limitResult = getGenerationLimit(userId)
            if (limitResult.isFailure) {
                return Result.success(false)
            }
            
            val limit = limitResult.getOrNull()!!
            
            // Check if subscription expired
            if (limit.isPremium && limit.subscriptionEndDate != null) {
                val now = Timestamp.now()
                if (now > limit.subscriptionEndDate) {
                    // Subscription expired, update to free
                    updatePremiumStatus(userId, false, null)
                    Log.d(TAG, "Subscription expired for user: $userId")
                    return Result.success(false)
                }
            }
            
            Result.success(limit.isPremium)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking subscription", e)
            Result.failure(e)
        }
    }

    /**
     * Request access for 'More' content
     */
    suspend fun requestMoreAccess(userId: String): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "moreRequestStatus" to "pending",
                "updatedAt" to Timestamp.now()
            )
            
            firestore.collection(COLLECTION_GENERATION_LIMITS)
                .document(userId)
                .update(updates)
                .await()
            
            Log.d(TAG, "✅ More access requested for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting more access", e)
            Result.failure(e)
        }
    }

    /**
     * Downgrade user to free plan (expired)
     */
    suspend fun downgradeToFree(userId: String): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any?>(
                "isPremium" to false,
                "subscriptionType" to "free",
                "maxChatLimit" to GenerationLimit.FREE_CHAT_LIMIT,
                "maxDailyLimit" to FREE_USER_DAILY_LIMIT, // Ensure limit is reset to 5
                "subscriptionEndDate" to null,
                "updatedAt" to Timestamp.now()
            )
            
            firestore.collection(COLLECTION_GENERATION_LIMITS)
                .document(userId)
                .update(updates)
                .await()
            
            Log.d(TAG, "✅ User downgraded to FREE: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error downgrading user", e)
            Result.failure(e)
        }
    }

    /**
     * Get generation limit as Flow (Real-time updates)
     */
    fun getGenerationLimitFlow(userId: String): Flow<GenerationLimit> = callbackFlow {
        val docRef = firestore.collection(COLLECTION_GENERATION_LIMITS).document(userId)
        
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Listen failed.", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val limit = snapshot.toObject(GenerationLimit::class.java)
                if (limit != null) {
                    trySend(limit)
                }
            }
        }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Update moreEnabled status
     */
    suspend fun updateMoreEnabled(userId: String, enabled: Boolean): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_GENERATION_LIMITS)
                .document(userId)
                .update("moreEnabled", enabled)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get Gem Count as Flow (Real-time updates)
     * Reads from 'users' collection
     */
    fun getGemCountFlow(userId: String): Flow<Int> = callbackFlow {
        val docRef = firestore.collection("users").document(userId)
        
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Gem listener failed", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val gems = snapshot.getLong("gems")?.toInt() ?: 0
                trySend(gems)
            } else {
                trySend(0)
            }
        }
        
        awaitClose { listener.remove() }
    }
    
    // --- Engagement Features ---
    
    /**
     * Purchase Daily Limit Booster
     * Cost: 50 Gems -> Reward: +5 Generations (This day only)
     */
    suspend fun purchaseDailyBooster(userId: String): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            val limitRef = firestore.collection(COLLECTION_GENERATION_LIMITS).document(userId)
            
            firestore.runTransaction { transaction ->
                val userSnapshot = transaction.get(userRef)
                
                // 1. Check Gems
                val currentGems = userSnapshot.getLong("gems")?.toInt() ?: 0
                if (currentGems < 50) {
                    throw Exception("Insufficient gems. You need 50 gems.")
                }
                
                // 2. Deduct Gems
                transaction.update(userRef, "gems", currentGems - 50)
                
                // 3. Increment usage limit (purchasedGenerations)
                transaction.update(limitRef, "purchasedGenerations", FieldValue.increment(5))
                transaction.update(limitRef, "updatedAt", Timestamp.now())
                
            }.await()
            
            Log.d(TAG, "✅ Daily Booster purchased for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error purchasing daily booster", e)
            Result.failure(e)
        }
    }
    
    /**
     * Redeem Referral Code
     * - Fraud Check: Device ID
     * - Reward: Referrer 500 Gems, Referee 50 Gems (example)
     */
    suspend fun redeemReferral(userId: String, referralCode: String, deviceId: String): Result<Unit> {
        return try {
            // 1. Validate Code (Assume code IS the User ID of referrer)
            // Since ProfileScreen displays UID as payment code, we treat input code as target UID.
            val referrerDocRef = firestore.collection("users").document(referralCode)
            val referrerSnapshot = referrerDocRef.get().await()
                
            if (!referrerSnapshot.exists()) {
                return Result.failure(Exception("Invalid referral code (User not found)"))
            }
            
            val referrerId = referrerSnapshot.id
            
            // 2. Self-referral check
            if (referrerId == userId) {
                return Result.failure(Exception("You cannot refer yourself"))
            }
            
            // 3. User already redeemed check
            val userRef = firestore.collection("users").document(userId)
            val userSnapshot = userRef.get().await()
            if (userSnapshot.contains("redeemedReferral")) {
                 return Result.failure(Exception("You have already redeemed a referral code"))
            }
            
            // 4. Device ID Check (Fraud)
            // Ideally we check if this deviceId has been used for ANY referral redemption
            // But checking referrer's device ID is a basic start.
            // Better: 'referral_logs' collection -> query where deviceId == deviceId
            
            val fraudCheck = firestore.collection("referral_logs")
                .whereEqualTo("deviceId", deviceId)
                .limit(1)
                .get()
                .await()
                
            if (!fraudCheck.isEmpty) {
                return Result.failure(Exception("This device has already used a referral code"))
            }
            
            // 5. Execute Transaction
            val referrerRef = firestore.collection("users").document(referrerId)
            
            firestore.runTransaction { transaction ->
                // Reward Referrer (500 Gems)
                transaction.update(referrerRef, "gems", FieldValue.increment(500))
                
                // Reward Referee (50 Gems)
                transaction.update(userRef, "gems", FieldValue.increment(50))
                
                // Mark User as Redeemed
                transaction.update(userRef, "redeemedReferral", true)
                transaction.update(userRef, "referrerId", referrerId)
                
                // Log the redemption
                val logRef = firestore.collection("referral_logs").document()
                val logData = hashMapOf(
                    "referrerId" to referrerId,
                    "refereeId" to userId,
                    "deviceId" to deviceId,
                    "timestamp" to Timestamp.now()
                )
                transaction.set(logRef, logData)
                
            }.await()
            
            Log.d(TAG, "✅ Referral redeemed: $referrerId invited $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error redeeming referral", e)
            Result.failure(e)
        }
    }
    
    /**
     * Claim Share Reward (500 Gems for first share)
     */
    suspend fun claimShareReward(userId: String): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                
                // Check if already shared
                if (snapshot.getBoolean("hasSharedReferral") == true) {
                    throw Exception("Reward already claimed")
                }
                
                // Grant Reward
                transaction.update(userRef, "gems", FieldValue.increment(500))
                transaction.update(userRef, "hasSharedReferral", true)
                
            }.await()
            
            Log.d(TAG, "✅ Share reward claimed for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error claiming share reward", e)
            Result.failure(e)
        }
    }
    
    /**
     * Redeem Promo Code (1000 Gems)
     */
    suspend fun redeemCode(userId: String, code: String): Result<Int> {
        return try {
            val normalizedCode = code.trim().uppercase()
            val codeRef = firestore.collection("redeem_codes").document(normalizedCode)
            val userRef = firestore.collection("users").document(userId)
            
            val rewardAmount = firestore.runTransaction { transaction ->
                val codeSnapshot = transaction.get(codeRef)
                
                if (!codeSnapshot.exists()) {
                    throw Exception("Invalid code")
                }
                
                // Check limit
                val maxRedemptions = codeSnapshot.getLong("maxRedemptions")?.toInt() ?: Int.MAX_VALUE
                val currentRedemptions = codeSnapshot.getLong("currentRedemptions")?.toInt() ?: 0
                
                if (currentRedemptions >= maxRedemptions) {
                    throw Exception("Code limit reached")
                }
                
                // Check if user already redeemed
                val redeemedUsers = codeSnapshot.get("redeemedUsers") as? List<String> ?: emptyList()
                if (redeemedUsers.contains(userId)) {
                    throw Exception("You have already used this code")
                }
                
                val rewardGems = codeSnapshot.getLong("rewardGems")?.toInt() ?: 1000
                
                // Execute
                transaction.update(userRef, "gems", FieldValue.increment(rewardGems.toLong()))
                transaction.update(codeRef, "currentRedemptions", FieldValue.increment(1))
                transaction.update(codeRef, "redeemedUsers", FieldValue.arrayUnion(userId))
                
                rewardGems // Return the reward amount
            }.await()
            
            Log.d(TAG, "✅ Code $normalizedCode redeemed by $userId")
            Result.success(rewardAmount)
        } catch (e: Exception) {
            Log.e(TAG, "Error redeeming code", e)
            Result.failure(e)
        }
    }
}
