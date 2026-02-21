package com.doyouone.drawai.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Ad Helper Functions
 * Provides utility functions for showing ads at strategic points
 */
object AdHelper {
    private const val TAG = "AdHelper"
    
    // Track last interstitial show time to avoid showing too frequently
    private var lastInterstitialTime = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 120_000L // 2 minutes
    
    /**
     * Show interstitial ad after successful image save/download
     * with cooldown to avoid annoying users
     */
    suspend fun showAdAfterSave(activity: Activity) {
        val currentTime = System.currentTimeMillis()
        
        // Check cooldown
        if (currentTime - lastInterstitialTime < INTERSTITIAL_COOLDOWN_MS) {
            Log.d(TAG, "Interstitial ad on cooldown, skipping")
            return
        }
        
        // Small delay to let save toast show first
        delay(500)
        
        if (AdManager.isInterstitialAdReady()) {
            lastInterstitialTime = currentTime
            AdManager.showInterstitialAd(activity) {
                Log.d(TAG, "Interstitial ad shown after save")
            }
        } else {
            // Load for next time
            AdManager.loadInterstitialAd(activity)
            Log.d(TAG, "Interstitial ad not ready, loading for next time")
        }
    }
    
    /**
     * Preload interstitial ad for next save action
     */
    fun preloadInterstitialAd(context: Context) {
        if (!AdManager.isInterstitialAdReady()) {
            AdManager.loadInterstitialAd(context)
        }
    }
}
