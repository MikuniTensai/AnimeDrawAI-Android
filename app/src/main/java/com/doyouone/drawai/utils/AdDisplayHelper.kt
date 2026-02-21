package com.doyouone.drawai.utils

import com.doyouone.drawai.data.model.GenerationLimit
import android.util.Log

object AdDisplayHelper {
    private const val TAG = "AdDisplayHelper"
    
    fun shouldShowAds(isPremium: Boolean, generationLimit: GenerationLimit?): Boolean {
        // 1. Check local premium flag (from UserPreferences)
        if (isPremium) {
            Log.d(TAG, "Ads hidden: User is premium (local pref)")
            return false
        }
        
        // 2. Check GenerationLimit object
        generationLimit?.let { limit ->
            // Check implicit premium flag in object
            if (limit.isPremium) {
                // Double check if subscription is expired. If expired, we should show ads!
                if (limit.isSubscriptionExpired()) {
                    Log.d(TAG, "Ads shown: Subscription expired (even though isPremium=true in obj)")
                    // Continue to next checks or return true? 
                    // Better to just return true here because expired means free.
                    return true
                }
                
                Log.d(TAG, "Ads hidden: User is premium (GenerationLimit.isPremium)")
                return false
            }
            
            // Check if subscription expired - TREAT AS FREE
            if (limit.isSubscriptionExpired()) {
                Log.d(TAG, "Ads shown: Subscription expired")
                return true
            }
            
            // Check subscription type (case-insensitive)
            val type = limit.subscriptionType.lowercase()
            if (type == "basic" || type == "pro" || type == "premium" || type == "unlimited") {
                Log.d(TAG, "Ads hidden: Subscription type is $type")
                return false
            }
            
            // SAFETY CHECK: If limit is high (>100), treat as premium
            // This handles cases where subscriptionType might be "free" but user has high limit (legacy/migrated)
            // Note: We only check maxDailyLimit. subscriptionLimit might be leftover from expired sub.
            if (limit.maxDailyLimit > 100) {
                 Log.d(TAG, "Ads hidden: High limit detected (Daily: ${limit.maxDailyLimit})")
                 return false
            }
        }
        
        Log.d(TAG, "Ads shown: User is Free/Guest")
        return true
    }
}