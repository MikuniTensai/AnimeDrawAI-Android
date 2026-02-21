package com.doyouone.drawai.data.model

import java.text.SimpleDateFormat
import java.util.*

enum class SubscriptionPlan(
    val planName: String,
    val price: Int, // in Rupiah
    val originalPrice: Int, // Original price for crossed-out display
    val periodLimit: Int, // Total generations for subscription period (30 days)
    val periodDays: Int, // Duration in days
    val hasAds: Boolean,
    val discountPercentage: Int = 0 // Discount percentage for marketing
) {
    FREE(
        planName = "Free (Ads)",
        price = 0,
        originalPrice = 0,
        periodLimit = 5, // 5 per day (handled by daily system)
        periodDays = 1, // Daily reset
        hasAds = true
    ),
    BASIC(
        planName = "Basic Plan",
        price = 29000,
        originalPrice = 60000, // Crossed-out price
        periodLimit = 200, // 200 generations per 30 days
        periodDays = 30,
        hasAds = false,
        discountPercentage = 52 // 52% discount
    ),
    PRO(
        planName = "Pro Plan",
        price = 79000,
        originalPrice = 160000, // Crossed-out price
        periodLimit = 600, // 600 generations per 30 days
        periodDays = 30,
        hasAds = false,
        discountPercentage = 51 // 51% discount
    );
    
    fun getFormattedPrice(): String {
        return if (price == 0) "Gratis" else "Rp${String.format("%,d", price)}"
    }
    
    fun getFormattedOriginalPrice(): String {
        return if (originalPrice == 0) "" else "Rp${String.format("%,d", originalPrice)}"
    }
    
    fun hasDiscount(): Boolean {
        return discountPercentage > 0 && originalPrice > price
    }
    
    fun getDiscountText(): String {
        return if (hasDiscount()) "DISCOUNT ${discountPercentage}%" else ""
    }
    
    fun getLimitText(): String {
        return if (this == FREE) {
            "5 generations / day"
        } else {
            "$periodLimit generations / $periodDays days"
        }
    }
    
    fun getDescription(): String {
        return when (this) {
            FREE -> "Daily limit with ads"
            BASIC -> "Auto-renews monthly"
            PRO -> "Auto-renews monthly"
        }
    }
}

data class UserSubscription(
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val generationUsed: Int = 0, // Total used in current period
    val dailyGenerationCount: Int = 0, // For FREE plan only
    val lastGenerationDate: String = "", // Format: YYYY-MM-DD
    val subscriptionStartDate: String = "", // Format: YYYY-MM-DD
    val expiryDate: String? = null, // Format: YYYY-MM-DD (null for FREE)
    val isActive: Boolean = true,
    val moreAccess: Boolean = false
) {
    fun isExpired(): Boolean {
        if (plan == SubscriptionPlan.FREE) return false
        if (expiryDate == null) return false
        
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return today > expiryDate
    }
    
    fun canGenerate(): Boolean {
        if (!isActive || isExpired()) return false
        
        if (plan == SubscriptionPlan.FREE) {
            // Free plan: daily limit
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            return !(lastGenerationDate == today && dailyGenerationCount >= 5)
        } else {
            // Subscription: period limit
            return generationUsed < plan.periodLimit
        }
    }
    
    fun getRemainingGenerations(): Int {
        return if (plan == SubscriptionPlan.FREE) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (lastGenerationDate == today) {
                (5 - dailyGenerationCount).coerceAtLeast(0)
            } else {
                5
            }
        } else {
            (plan.periodLimit - generationUsed).coerceAtLeast(0)
        }
    }
    
    fun getDaysRemaining(): Int {
        if (plan == SubscriptionPlan.FREE || expiryDate == null) return 0
        
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Date()
            val expiry = dateFormat.parse(expiryDate) ?: return 0
            val diffInMillis = expiry.time - today.time
            val days = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
            return days.coerceAtLeast(0)
        } catch (e: Exception) {
            return 0
        }
    }
}
