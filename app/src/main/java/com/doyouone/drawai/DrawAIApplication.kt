package com.doyouone.drawai

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Application class untuk setup global configuration
 * Termasuk image caching dengan Coil
 */
class DrawAIApplication : Application(), ImageLoaderFactory {
    
    private lateinit var appOpenAdManager: com.doyouone.drawai.ads.AppOpenAdManager
    
    override fun onCreate() {
        super.onCreate()
        // Setup logging atau initialization lain jika perlu
        
        // Initialize RetrofitClient dengan application context (untuk device fingerprinting)
        com.doyouone.drawai.data.api.RetrofitClient.init(this)
        
        // Initialize AdMob
        com.doyouone.drawai.ads.AdManager.initialize(this)
        

        
        // Initialize App Open Ad Manager (untuk semua user)
        appOpenAdManager = com.doyouone.drawai.ads.AppOpenAdManager(this)
        appOpenAdManager.loadAd()
    }
    
    /**
     * Configure Coil ImageLoader dengan disk & memory cache
     * untuk optimize image loading performance
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Set max memory cache to 25% of app memory
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // Set max disk cache to 50MB
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
            }
            // Add GIF decoder support
            .components {
                add(coil.decode.GifDecoder.Factory())
            }
            // Cache policies
            .respectCacheHeaders(false) // Ignore server cache headers
            .build()
    }
}
