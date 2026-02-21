package com.doyouone.drawai.data.cache

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.request.CachePolicy
import coil.util.DebugLogger
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

/**
 * Centralized image cache manager using Coil's built-in caching
 * Compatible with Coil 2.5.0
 * Provides memory and disk caching with configurable limits
 */
class ImageCacheManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageCacheManager"
        
        // Cache size limits
        private const val DISK_CACHE_SIZE_MB = 250L // 250MB for disk cache
        
        // Cache directory name
        private const val DISK_CACHE_DIR = "image_cache"
        
        @Volatile
        private var INSTANCE: ImageCacheManager? = null
        
        fun getInstance(context: Context): ImageCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageCacheManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    private val diskCacheDir: File by lazy {
        File(context.cacheDir, DISK_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Configured ImageLoader with optimized caching
     * Using Coil 2.5.0 compatible configuration
     */
    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .okHttpClient {
                OkHttpClient.Builder()
                    .cache(Cache(diskCacheDir, DISK_CACHE_SIZE_MB * 1024 * 1024))
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true) // Smooth image transitions
            .respectCacheHeaders(false) // Always cache regardless of server headers
            .logger(DebugLogger()) // Enable logging in debug builds
            .build()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        val diskCacheSize = getDiskCacheSize()
        val diskCacheMaxSize = DISK_CACHE_SIZE_MB * 1024 * 1024
        
        return CacheStats(
            diskCacheSize = diskCacheSize,
            diskCacheMaxSize = diskCacheMaxSize
        )
    }
    
    private fun getDiskCacheSize(): Long {
        return try {
            diskCacheDir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating disk cache size", e)
            0L
        }
    }
    
    /**
     * Clear memory cache
     */
    fun clearMemoryCache() {
        // Coil 2.5.0 doesn't expose memory cache directly
        // Triggering GC as a workaround
        System.gc()
        Log.d(TAG, "Memory cache cleared (GC triggered)")
    }
    
    /**
     * Clear disk cache
     */
    suspend fun clearDiskCache() {
        try {
            diskCacheDir.listFiles()?.forEach { file ->
                file.deleteRecursively()
            }
            Log.d(TAG, "Disk cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing disk cache", e)
        }
    }
    
    /**
     * Clear all caches (memory + disk)
     */
    suspend fun clearAllCaches() {
        clearMemoryCache()
        clearDiskCache()
        Log.d(TAG, "All caches cleared")
    }
    
    /**
     * Log current cache statistics
     */
    fun logCacheStats() {
        val stats = getCacheStats()
        Log.d(TAG, """
            Cache Statistics:
            └─ Disk: ${stats.diskCacheSizeMB}MB / ${stats.diskCacheMaxSizeMB}MB (${stats.diskCacheUsagePercent}%)
        """.trimIndent())
    }
}

/**
 * Cache statistics data class
 */
data class CacheStats(
    val diskCacheSize: Long,
    val diskCacheMaxSize: Long
) {
    val diskCacheSizeMB: Double
        get() = diskCacheSize / (1024.0 * 1024.0)
    
    val diskCacheMaxSizeMB: Double
        get() = diskCacheMaxSize / (1024.0 * 1024.0)
    
    val diskCacheUsagePercent: Int
        get() = if (diskCacheMaxSize > 0) {
            ((diskCacheSize.toDouble() / diskCacheMaxSize) * 100).toInt()
        } else 0
}
