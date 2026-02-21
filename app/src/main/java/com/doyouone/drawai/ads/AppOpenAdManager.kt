package com.doyouone.drawai.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.*

/**
 * AppOpenAdManager - Manages App Open Ads that show when app comes from background
 * Shows for ALL users (including premium)
 */
class AppOpenAdManager(private val application: Application) : 
    DefaultLifecycleObserver,
    Application.ActivityLifecycleCallbacks {
    
    private var appOpenAd: AppOpenAd? = null
    private var currentActivity: Activity? = null
    private var isShowingAd = false
    private var loadTime: Long = 0
    
    companion object {
        private const val TAG = "AppOpenAdManager"
        private const val AD_UNIT_ID = "ca-app-pub-8770525488772470/2626722433"
        private const val AD_TIMEOUT_MS = 4 * 60 * 60 * 1000L // 4 hours
    }
    
    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    /** LifecycleObserver method that shows the app open ad when the app moves to foreground. */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "App moved to foreground")
        // Show ad when app comes to foreground
        showAdIfAvailable()
    }
    
    /** Request an ad. */
    fun loadAd() {
        // Do not load ad if there is an unused ad or one is already loading.
        if (isLoadingAd() || isAdAvailable()) {
            return
        }
        
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            application,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App open ad loaded")
                    appOpenAd = ad
                    loadTime = Date().time
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "App open ad failed to load: ${error.message}")
                }
            }
        )
    }
    
    /** Check if ad was loaded more than n hours ago. */
    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour = 3600000L
        return dateDifference < numMilliSecondsPerHour * 4
    }
    
    /** Check if ad exists and can be shown. */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo()
    }
    
    private fun isLoadingAd(): Boolean {
        // Simple implementation - can be improved with actual loading state
        return false
    }
    
    /** Show the ad if one isn't already showing and one is available. */
    private fun showAdIfAvailable() {
        // Only show ad if:
        // 1. Not already showing an ad
        // 2. We have an activity
        // 3. Ad is available
        if (!isShowingAd && currentActivity != null) {
            if (isAdAvailable()) {
                Log.d(TAG, "Will show app open ad")
                
                appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "App open ad dismissed")
                        appOpenAd = null
                        isShowingAd = false
                        // Preload next ad
                        loadAd()
                    }
                    
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.e(TAG, "App open ad failed to show: ${error.message}")
                        appOpenAd = null
                        isShowingAd = false
                    }
                    
                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "App open ad showed")
                        isShowingAd = true
                    }
                }
                
                currentActivity?.let { activity ->
                    appOpenAd?.show(activity)
                }
            } else {
                Log.d(TAG, "App open ad not available, loading...")
                loadAd()
            }
        }
    }
    
    /** ActivityLifecycleCallback methods to track current activity */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    
    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) {
            currentActivity = activity
        }
    }
    
    override fun onActivityResumed(activity: Activity) {
        if (!isShowingAd) {
            currentActivity = activity
        }
    }
    
    override fun onActivityPaused(activity: Activity) {}
    
    override fun onActivityStopped(activity: Activity) {}
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
