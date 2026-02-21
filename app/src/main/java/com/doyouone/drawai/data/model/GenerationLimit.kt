package com.doyouone.drawai.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/**
 * Model untuk generation limit user di Firebase Firestore
 * Updated with Subscription System (Basic 200/30d, Pro 600/30d)
 */
data class GenerationLimit(
    @PropertyName("userId")
    val userId: String = "",
    
    @PropertyName("dailyGenerations")
    val dailyGenerations: Int = 0,
    
    @PropertyName("maxDailyLimit")
    val maxDailyLimit: Int = FREE_USER_DAILY_LIMIT,
    
    @PropertyName("lastResetDate")
    val lastResetDate: String = "", // Format: yyyy-MM-dd
    
    @get:PropertyName("isPremium")
    @set:PropertyName("isPremium")
    var isPremium: Boolean = false,
    
    @PropertyName("subscriptionType")
    val subscriptionType: String = TYPE_FREE, // "free", "basic", "pro"
    
    @PropertyName("subscriptionLimit")
    val subscriptionLimit: Int = 0, // Total limit for subscription period (200 or 600)
    
    @PropertyName("subscriptionUsed")
    val subscriptionUsed: Int = 0, // How many used from subscription limit
    
    @PropertyName("subscriptionEndDate")
    val subscriptionEndDate: Timestamp? = null,
    
    @PropertyName("totalGenerations")
    val totalGenerations: Int = 0,
    
    @PropertyName("purchasedGenerations")
    val purchasedGenerations: Int = 0, // Generations purchased via Daily Booster (reset daily)

    @PropertyName("bonusGenerations")
    val bonusGenerations: Int = 0, // Bonus dari rewarded ads (max 10 for free, unlimited for basic/pro)

    @PropertyName("maxChatLimit")
    val maxChatLimit: Int = 1, // Limit chat dengan character (default 1)

    @PropertyName("createdAt")
    val createdAt: Timestamp? = null,
    
    @PropertyName("updatedAt")
    val updatedAt: Timestamp? = null,
    
    @get:PropertyName("moreEnabled")
    @set:PropertyName("moreEnabled")
    var moreEnabled: Boolean = false,
    
    @PropertyName("moreRequestStatus")
    val moreRequestStatus: String = "", // "pending", "approved", "rejected", ""
    
    @PropertyName("generations")
    val generations: Any? = null // Legacy field - can be Int or Map, kept for backward compatibility
) {
    /**
     * Check apakah user masih bisa generate
     */
    fun canGenerate(): Boolean {
        // Check if subscription expired
        if (subscriptionType != TYPE_FREE && isSubscriptionExpired()) {
            return false
        }
        
        // Subscription user (Basic/Pro)
        // FIX: Include purchased and bonus generations!
        if (subscriptionType != TYPE_FREE) {
            val totalLimit = subscriptionLimit + bonusGenerations + purchasedGenerations
            val remaining = totalLimit - subscriptionUsed
            return remaining > 0
        }
        
        // Free user
        return dailyGenerations < (maxDailyLimit + bonusGenerations + purchasedGenerations)
    }

    /**
     * Get remaining generations
     */
    @Exclude
    fun getRemainingGenerations(): Int {
        // Check if subscription expired
        if (subscriptionType != TYPE_FREE && isSubscriptionExpired()) {
            return 0
        }
        
        // Subscription user
        // FIX: Include purchased and bonus generations!
        if (subscriptionType != TYPE_FREE) {
            val totalLimit = subscriptionLimit + bonusGenerations + purchasedGenerations
            return maxOf(0, totalLimit - subscriptionUsed)
        }
        
        // Free user
        return maxOf(0, (maxDailyLimit + bonusGenerations + purchasedGenerations) - dailyGenerations)
    }
    
    /**
     * Get total available for current period
     */
    @Exclude
    fun getTotalAvailable(): Int {
        if (subscriptionType != TYPE_FREE) {
            // FIX: Include purchased and bonus generations!
            return subscriptionLimit + bonusGenerations + purchasedGenerations
        }
        return maxDailyLimit + bonusGenerations + purchasedGenerations
    }
    
    /**
     * Check if subscription expired
     */
    @Exclude
    fun isSubscriptionExpired(): Boolean {
        if (subscriptionType == TYPE_FREE) return false
        subscriptionEndDate?.let {
            return it.toDate().before(java.util.Date())
        }
        return true
    }
    
    /**
     * Get days remaining for subscription
     */
    @Exclude
    fun getDaysRemaining(): Int {
        if (subscriptionType == TYPE_FREE) return 0
        subscriptionEndDate?.let {
            val now = java.util.Date()
            val diff = it.toDate().time - now.time
            return maxOf(0, (diff / (1000 * 60 * 60 * 24)).toInt())
        }
        return 0
    }
    
    /**
     * Get subscription display name
     */
    @Exclude
    fun getSubscriptionName(): String {
        return when (subscriptionType) {
            TYPE_BASIC -> "Basic Plan"
            TYPE_PRO -> "Pro Plan"
            else -> "Free"
        }
    }
    
    /**
     * Get subscription details
     */
    @Exclude
    fun getSubscriptionDetails(): String {
        return when (subscriptionType) {
            TYPE_BASIC -> "$BASIC_LIMIT generations / $SUBSCRIPTION_DAYS days"
            TYPE_PRO -> "$PRO_LIMIT generations / $SUBSCRIPTION_DAYS days"
            else -> "5 generations / day"
        }
    }
    
    /**
     * Check if user can watch more ads
     * Free users: max 10 bonus
     * Basic/Pro users: unlimited
     */
    fun canWatchMoreAds(): Boolean {
        // Basic/Pro users: unlimited ads
        if (subscriptionType != TYPE_FREE) return true
        
        // Free users: max 10 bonus
        return bonusGenerations < 10
    }
    
    /**
     * Check if should show subscription popup
     */
    fun shouldShowSubscription(): Boolean {
        // Free user reached limit and watched max ads (10)
        return subscriptionType == TYPE_FREE && 
               bonusGenerations >= 10 && 
               getRemainingGenerations() == 0
    }
    
    /**
     * Check apakah perlu reset (hari sudah berganti)
     */
    fun needsReset(currentDate: String): Boolean {
        return lastResetDate != currentDate
    }
    
    companion object {
        const val FREE_USER_DAILY_LIMIT = 5
        const val COLLECTION_NAME = "generation_limits"
        
        // Subscription Plans
        const val BASIC_LIMIT = 200  // 200 generations per 30 days
        const val PRO_LIMIT = 600    // 600 generations per 30 days
        const val SUBSCRIPTION_DAYS = 30
        
        const val TYPE_FREE = "free"
        const val TYPE_BASIC = "basic"
        const val TYPE_PRO = "pro"
        
        // Chat Limits
        const val FREE_CHAT_LIMIT = 1
        const val BASIC_CHAT_LIMIT = 4  // 1 + 3
        const val PRO_CHAT_LIMIT = 11   // 1 + 10
        
        // Prices (for display only, not for payment processing)
        const val BASIC_PRICE = 29000  // 49rb
        const val PRO_PRICE = 79000    // 79rb
    }
}

/**
 * Exception ketika user mencapai limit
 */
class GenerationLimitExceededException(
    val remaining: Int = 0,
    message: String = "Generation limit exceeded. Remaining: $remaining"
) : Exception(message)
