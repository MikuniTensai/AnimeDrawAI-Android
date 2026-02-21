package com.doyouone.drawai.data.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Network configuration dan utility functions
 */
object NetworkConfig {
    
    /**
     * Check if device has internet connection
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo?.isConnected == true
        }
    }
    
    /**
     * Get network type (WiFi, Cellular, etc.)
     */
    fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return "None"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "None"
            
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo?.typeName ?: "None"
        }
    }
    
    /**
     * API Server URLs
     */
    object ServerUrls {
        const val PRODUCTION = "https://drawai.site/"
        const val LOCAL_EMULATOR = "http://10.0.2.2:5000/"
        const val LOCAL_DEVICE = "http://192.168.1.100:5000/" // Ganti dengan IP lokal Anda
        
        /**
         * Get appropriate server URL based on environment
         */
        fun getDefaultUrl(useLocal: Boolean = false): String {
            return if (useLocal) LOCAL_EMULATOR else PRODUCTION
        }
    }
    
    /**
     * API Endpoints
     */
    object Endpoints {
        const val WORKFLOWS = "workflows"
        const val GENERATE = "generate"
        const val STATUS = "status/{task_id}"
        const val DOWNLOAD = "download/{filename}"
        const val HEALTH = "health"
    }
    
    /**
     * Request timeout configurations (in seconds)
     */
    object Timeouts {
        const val CONNECT = 30L
        const val READ = 60L
        const val WRITE = 60L
    }
}
