package com.doyouone.drawai.util

/**
 * Application constants
 */
object Constants {
    
    /**
     * API Configuration
     */
    object Api {
        // Production server
        const val BASE_URL_PRODUCTION = "https://drawai.site/"
        
        // Local development servers
        const val BASE_URL_LOCAL_EMULATOR = "http://10.0.2.2:5000/"
        const val BASE_URL_LOCAL_DEVICE = "http://192.168.1.100:5000/" // Ganti dengan IP lokal Anda
        
        // Default URL (ganti sesuai kebutuhan)
        const val BASE_URL_DEFAULT = BASE_URL_PRODUCTION
        
        // Timeouts
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 60L
        const val WRITE_TIMEOUT = 60L
        
        // Polling
        const val POLLING_INTERVAL_MS = 2000L
        const val MAX_POLLING_ATTEMPTS = 150
    }
    
    /**
     * Task Status
     */
    object TaskStatus {
        const val PENDING = "pending"
        const val PROCESSING = "processing"
        const val COMPLETED = "completed"
        const val ERROR = "error"
        const val FAILED = "failed"
    }
    
    /**
     * Storage
     */
    object Storage {
        const val IMAGES_FOLDER = "DrawAI"
        const val CACHE_FOLDER = "cache"
    }
    
    /**
     * Preferences
     */
    object Prefs {
        const val PREF_NAME = "drawai_prefs"
        const val KEY_API_URL = "api_url"
        const val KEY_USE_LOCAL_SERVER = "use_local_server"
        const val KEY_API_KEY = "api_key"
        const val KEY_USER_ID = "user_id"
    }
    
    /**
     * Validation
     */
    object Validation {
        const val MIN_PROMPT_LENGTH = 3
        const val MAX_PROMPT_LENGTH = 1000
    }
}
