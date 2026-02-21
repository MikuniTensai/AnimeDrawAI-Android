package com.doyouone.drawai.ads

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Native Ad View for Compose
 * Shows native ads in workflow lists
 */
@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadNativeAd(context) { ad, error ->
            isLoading = false
            if (ad != null) {
                nativeAd = ad
                loadFailed = false
            } else {
                loadFailed = true
                Log.e("NativeAdCard", "Failed to load native ad: ${error?.message}")
            }
        }
    }

    // Only show ad if loaded successfully
    if (!loadFailed && nativeAd != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                factory = { ctx ->
                    createNativeAdView(ctx, nativeAd!!)
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
        }
    }
}

/**
 * Load Native Ad
 */
private fun loadNativeAd(
    context: Context,
    onAdLoaded: (NativeAd?, LoadAdError?) -> Unit
) {
    val adLoader = AdLoader.Builder(context, AdManager.NATIVE_AD_UNIT_ID)
        .forNativeAd { ad ->
            Log.d("NativeAdCard", "Native ad loaded successfully")
            onAdLoaded(ad, null)
        }
        .withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e("NativeAdCard", "Native ad failed to load: ${error.message}")
                onAdLoaded(null, error)
            }

            override fun onAdClicked() {
                Log.d("NativeAdCard", "Native ad clicked")
            }
        })
        .withNativeAdOptions(
            NativeAdOptions.Builder()
                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                .build()
        )
        .build()

    adLoader.loadAd(AdRequest.Builder().build())
}

/**
 * Create Native Ad View with layout
 */
private fun createNativeAdView(context: Context, nativeAd: NativeAd): NativeAdView {
    // Inflate the ad view layout (which is already a NativeAdView)
    val inflater = android.view.LayoutInflater.from(context)
    val adView = inflater.inflate(
        com.doyouone.drawai.R.layout.native_ad_layout,
        null
    ) as NativeAdView
    
    // Populate the ad view with the native ad assets
    adView.apply {
        // Set the media view
        mediaView = findViewById(com.doyouone.drawai.R.id.ad_media)
        
        // Set other ad assets
        headlineView = findViewById(com.doyouone.drawai.R.id.ad_headline)
        bodyView = findViewById(com.doyouone.drawai.R.id.ad_body)
        callToActionView = findViewById(com.doyouone.drawai.R.id.ad_call_to_action)
        iconView = findViewById(com.doyouone.drawai.R.id.ad_icon)
        
        // Populate the views with ad data
        (headlineView as? android.widget.TextView)?.text = nativeAd.headline
        (bodyView as? android.widget.TextView)?.text = nativeAd.body
        (callToActionView as? android.widget.Button)?.text = nativeAd.callToAction
        
        nativeAd.icon?.let { icon ->
            (iconView as? android.widget.ImageView)?.setImageDrawable(icon.drawable)
            iconView?.visibility = android.view.View.VISIBLE
        } ?: run {
            iconView?.visibility = android.view.View.GONE
        }
        
        // Handle MediaView visibility
        if (nativeAd.mediaContent != null) {
             mediaView?.visibility = android.view.View.VISIBLE
             mediaView?.mediaContent = nativeAd.mediaContent
        } else {
             mediaView?.visibility = android.view.View.GONE
        }
        
        // Register the native ad
        setNativeAd(nativeAd)
    }
    
    return adView
}
