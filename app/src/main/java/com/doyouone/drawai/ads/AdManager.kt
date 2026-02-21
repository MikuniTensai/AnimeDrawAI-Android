package com.doyouone.drawai.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdManager - Manages all AdMob ads (Banner, Interstitial, Rewarded, App Open)
 * Uses PRODUCTION AD UNIT IDs
 * Test devices will receive test ads automatically
 */
object AdManager {
    private const val TAG = "AdManager"
    
    // Production Ad Unit IDs
    const val INTERSTITIAL_AD_UNIT_ID_PRODUCTION = "ca-app-pub-8770525488772470/2618698168"
    const val APP_OPEN_AD_UNIT_ID_PRODUCTION = "ca-app-pub-8770525488772470/2626722433"
    const val BANNER_AD_UNIT_ID_PRODUCTION = "ca-app-pub-8770525488772470/2600540385"
    const val REWARDED_AD_UNIT_ID_PRODUCTION = "ca-app-pub-8770525488772470/5373139420"
    const val NATIVE_AD_UNIT_ID_PRODUCTION = "ca-app-pub-8770525488772470/1059990638"
    
    // Test Ad Unit IDs (Google's official test ads)
    const val INTERSTITIAL_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/1033173712"
    const val BANNER_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/6300978111"
    const val REWARDED_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/5224354917"
    const val APP_OPEN_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/9257395921"
    const val NATIVE_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/2247696110"
    
    // Auto-switch based on build type (DEBUG = test ads, RELEASE = production ads)
    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (com.doyouone.drawai.BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 DEBUG MODE: Using TEST Interstitial Ad ID")
            INTERSTITIAL_AD_UNIT_ID_TEST
        } else {
            Log.d(TAG, "🚀 RELEASE MODE: Using PRODUCTION Interstitial Ad ID")
            INTERSTITIAL_AD_UNIT_ID_PRODUCTION
        }
    
    val BANNER_AD_UNIT_ID: String
        get() = if (com.doyouone.drawai.BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 DEBUG MODE: Using TEST Banner Ad ID")
            BANNER_AD_UNIT_ID_TEST
        } else {
            Log.d(TAG, "🚀 RELEASE MODE: Using PRODUCTION Banner Ad ID")
            BANNER_AD_UNIT_ID_PRODUCTION
        }
    
    val REWARDED_AD_UNIT_ID: String
        get() = if (com.doyouone.drawai.BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 DEBUG MODE: Using TEST Rewarded Ad ID")
            REWARDED_AD_UNIT_ID_TEST
        } else {
            Log.d(TAG, "🚀 RELEASE MODE: Using PRODUCTION Rewarded Ad ID")
            REWARDED_AD_UNIT_ID_PRODUCTION
        }
    
    val APP_OPEN_AD_UNIT_ID: String
        get() = if (com.doyouone.drawai.BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 DEBUG MODE: Using TEST App Open Ad ID")
            APP_OPEN_AD_UNIT_ID_TEST
        } else {
            Log.d(TAG, "🚀 RELEASE MODE: Using PRODUCTION App Open Ad ID")
            APP_OPEN_AD_UNIT_ID_PRODUCTION
        }
    
    val NATIVE_AD_UNIT_ID: String
        get() = if (com.doyouone.drawai.BuildConfig.DEBUG) {
            Log.d(TAG, "🧪 DEBUG MODE: Using TEST Native Ad ID")
            NATIVE_AD_UNIT_ID_TEST
        } else {
            Log.d(TAG, "🚀 RELEASE MODE: Using PRODUCTION Native Ad ID")
            NATIVE_AD_UNIT_ID_PRODUCTION
        }
    
    // Test device IDs (untuk development/testing)
    // Device ID akan muncul di logcat saat pertama kali request ad
    private val TEST_DEVICE_IDS = listOf(
        AdRequest.DEVICE_ID_EMULATOR, // Emulator
        "YOUR_TEST_DEVICE_ID_HERE" // Replace dengan device ID untuk testing
        // Contoh: "33BE2250B43518CCDA7DE426D04EE232"
    )
    
    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: com.google.android.gms.ads.appopen.AppOpenAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isAdMobInitialized = false
    private var isShowingAppOpenAd = false
    private var isLoadingInterstitialAd = false
    private var isLoadingRewardedAd = false
    
    /**
     * Initialize AdMob SDK
     */
    fun initialize(context: Context) {
        if (isAdMobInitialized) {
            Log.d(TAG, "AdMob already initialized")
            return
        }
        
        // Configure test devices for demo
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(TEST_DEVICE_IDS)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "========================================")
            Log.d(TAG, "🎬 AdMob Initialized Successfully!")
            Log.d(TAG, "========================================")
            Log.d(TAG, "📱 Build Type: ${if (com.doyouone.drawai.BuildConfig.DEBUG) "DEBUG" else "RELEASE"}")
            Log.d(TAG, "🎯 Ad Mode: ${if (com.doyouone.drawai.BuildConfig.DEBUG) "TEST ADS" else "PRODUCTION ADS"}")
            Log.d(TAG, "🎁 Rewarded Ad Unit: $REWARDED_AD_UNIT_ID")
            Log.d(TAG, "📺 Interstitial Ad Unit: $INTERSTITIAL_AD_UNIT_ID")
            Log.d(TAG, "📰 Banner Ad Unit: $BANNER_AD_UNIT_ID")
            Log.d(TAG, "🚀 App Open Ad Unit: $APP_OPEN_AD_UNIT_ID")
            Log.d(TAG, "📱 Adapter Status: ${initializationStatus.adapterStatusMap}")
            Log.d(TAG, "========================================")
            isAdMobInitialized = true
        }
    }
    
    /**
     * Create Banner Ad
     */
    fun createBannerAd(context: Context): AdView {
        return AdView(context).apply {
            adUnitId = BANNER_AD_UNIT_ID
            setAdSize(AdSize.BANNER)
            
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner ad loaded")
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Banner ad failed to load: ${error.message}")
                }
                
                override fun onAdClicked() {
                    Log.d(TAG, "Banner ad clicked")
                }
            }
            
            loadAd(AdRequest.Builder().build())
        }
    }
    
    /**
     * Load Interstitial Ad
     */
    fun loadInterstitialAd(context: Context, onAdLoaded: () -> Unit = {}) {
        if (interstitialAd != null || isLoadingInterstitialAd) {
            if (interstitialAd != null) Log.d(TAG, "Interstitial ad already loaded")
            if (isLoadingInterstitialAd) Log.d(TAG, "Interstitial ad currently loading")
            return
        }

        isLoadingInterstitialAd = true
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded")
                    interstitialAd = ad
                    isLoadingInterstitialAd = false
                    onAdLoaded()
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                    interstitialAd = null
                    isLoadingInterstitialAd = false
                }
            }
        )
    }
    
    /**
     * Show Interstitial Ad
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    interstitialAd = null
                    onAdDismissed()
                    // Preload next ad
                    loadInterstitialAd(activity)
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "Interstitial ad failed to show: ${error.message}")
                    interstitialAd = null
                }
                
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad showed")
                }
            }
            
            interstitialAd?.show(activity)
        } else {
            Log.w(TAG, "Interstitial ad not ready")
            onAdDismissed()
            // Try to load for next time
            loadInterstitialAd(activity)
        }
    }
    
    /**
     * Load App Open Ad (for all users including premium)
     */
    fun loadAppOpenAd(context: Context, onAdLoaded: () -> Unit = {}) {
        if (isShowingAppOpenAd) {
            Log.d(TAG, "App open ad already showing")
            return
        }
        
        val adRequest = AdRequest.Builder().build()
        
        com.google.android.gms.ads.appopen.AppOpenAd.load(
            context,
            APP_OPEN_AD_UNIT_ID,
            adRequest,
            object : com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: com.google.android.gms.ads.appopen.AppOpenAd) {
                    Log.d(TAG, "App open ad loaded")
                    appOpenAd = ad
                    onAdLoaded()
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "App open ad failed to load: ${error.message}")
                    appOpenAd = null
                }
            }
        )
    }
    
    /**
     * Show App Open Ad (for all users)
     */
    fun showAppOpenAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (appOpenAd != null && !isShowingAppOpenAd) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "App open ad dismissed")
                    appOpenAd = null
                    isShowingAppOpenAd = false
                    onAdDismissed()
                    // Preload next ad
                    loadAppOpenAd(activity)
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "App open ad failed to show: ${error.message}")
                    appOpenAd = null
                    isShowingAppOpenAd = false
                }
                
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App open ad showed")
                    isShowingAppOpenAd = true
                }
            }
            
            appOpenAd?.show(activity)
        } else {
            Log.w(TAG, "App open ad not ready or already showing")
            onAdDismissed()
            // Try to load for next time
            if (!isShowingAppOpenAd) {
                loadAppOpenAd(activity)
            }
        }
    }
    
    /**
     * Load Rewarded Ad
     */
    fun loadRewardedAd(context: Context, onAdLoaded: () -> Unit = {}) {
        if (rewardedAd != null || isLoadingRewardedAd) {
            if (rewardedAd != null) Log.d(TAG, "Rewarded ad already loaded")
            if (isLoadingRewardedAd) Log.d(TAG, "Rewarded ad currently loading")
            return
        }

        isLoadingRewardedAd = true
        Log.d(TAG, "🔄 Loading rewarded ad...")
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "========================================")
                    Log.d(TAG, "✅ Rewarded Ad Loaded Successfully!")
                    Log.d(TAG, "🎬 Ad is ready to show")
                    Log.d(TAG, "🎁 Reward: +1 Generation")
                    Log.d(TAG, "========================================")
                    rewardedAd = ad
                    isLoadingRewardedAd = false
                    onAdLoaded()
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "========================================")
                    Log.e(TAG, "❌ Rewarded Ad Failed to Load")
                    Log.e(TAG, "Error Code: ${error.code}")
                    Log.e(TAG, "Error Message: ${error.message}")
                    Log.e(TAG, "Domain: ${error.domain}")
                    Log.e(TAG, "========================================")
                    rewardedAd = null
                    isLoadingRewardedAd = false
                }
            }
        )
    }
    
    /**
     * Show Rewarded Ad
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (Int) -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        if (rewardedAd != null) {
            Log.d(TAG, "🎬 Showing rewarded ad...")
            
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "========================================")
                    Log.d(TAG, "✅ Rewarded Ad Dismissed")
                    Log.d(TAG, "🔄 Preloading next ad...")
                    Log.d(TAG, "========================================")
                    rewardedAd = null
                    onAdDismissed()
                    // Preload next ad
                    loadRewardedAd(activity)
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "========================================")
                    Log.e(TAG, "❌ Rewarded Ad Failed to Show")
                    Log.e(TAG, "Error: ${error.message}")
                    Log.e(TAG, "========================================")
                    rewardedAd = null
                }
                
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "========================================")
                    Log.d(TAG, "🎬 Rewarded Ad Showing Now!")
                    Log.d(TAG, "========================================")
                }
            }
            
            rewardedAd?.show(activity) { rewardItem ->
                val rewardAmount = rewardItem.amount
                Log.d(TAG, "========================================")
                Log.d(TAG, "🎁 REWARD EARNED!")
                Log.d(TAG, "Amount: $rewardAmount")
                Log.d(TAG, "Type: ${rewardItem.type}")
                Log.d(TAG, "✨ User gets +1 Generation!")
                Log.d(TAG, "========================================")
                onUserEarnedReward(rewardAmount)
            }
        } else {
            Log.w(TAG, "========================================")
            Log.w(TAG, "⚠️ Rewarded Ad Not Ready")
            Log.w(TAG, "Loading ad now...")
            Log.w(TAG, "========================================")
            onAdDismissed()
            // Try to load for next time
            loadRewardedAd(activity)
        }
    }
    
    /**
     * Check if Interstitial Ad is ready
     */
    fun isInterstitialAdReady(): Boolean = interstitialAd != null
    
    /**
     * Check if App Open Ad is ready
     */
    fun isAppOpenAdReady(): Boolean = appOpenAd != null && !isShowingAppOpenAd
    
    /**
     * Check if Rewarded Ad is ready
     */
    fun isRewardedAdReady(): Boolean = rewardedAd != null
}
