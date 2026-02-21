package com.doyouone.drawai.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Composable Banner Ad View
 * Shows AdMob banner ad in Compose UI
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier
) {
    // Determine the ad size to use - start with standard BANNER
    // In production apps, we might calculate adaptive size here
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp) // Fixed height to prevent layout jumps
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                // Create AdView
                com.google.android.gms.ads.AdView(context).apply {
                    setAdSize(com.google.android.gms.ads.AdSize.BANNER)
                    adUnitId = com.doyouone.drawai.ads.AdManager.BANNER_AD_UNIT_ID
                    
                    // Set listener
                    adListener = object : com.google.android.gms.ads.AdListener() {
                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            super.onAdFailedToLoad(error)
                            // Log error but don't crash
                            // Log.e("BannerAd", "Ad failed to load: ${error.message}")
                        }
                    }
                    
                    // Load ad
                    loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
                }
            },
            update = { 
                // View is created, nothing dynamic to update for now
            },
            onRelease = { adView ->
                // Called when the view is detached from the window
                adView.destroy()
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }
}
