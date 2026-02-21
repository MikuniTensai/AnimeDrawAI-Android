package com.doyouone.drawai.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit



/**
 * Retrofit client singleton untuk DrawAI API
 */
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import android.provider.Settings
import android.content.Context
import okhttp3.CertificatePinner
import java.security.MessageDigest
import java.util.concurrent.ExecutionException

/**
 * Retrofit client singleton untuk DrawAI API dengan security enhancements:
 * - SSL Certificate Pinning (MITM protection)
 * - Device fingerprinting
 * - Conditional logging (debug only)
 */
object RetrofitClient {
    
    // Base URL - Production server (accessible from outside network)
    private const val BASE_URL = "https://drawai-api.drawai.site/"
    
    // Alternative local URL untuk testing
    private const val LOCAL_URL = "http://10.0.2.2:5000/"
    
    // Use production server (tunnel sudah hidup)
    private var currentBaseUrl = BASE_URL
    
    // Logging interceptor untuk debugging (DISABLED in production for security)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (com.doyouone.drawai.BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY  // Debug: full logging
        } else {
            HttpLoggingInterceptor.Level.NONE  // Release: NO LOGGING (security)
        }
    }
    
    // Context for device fingerprinting (set by Application)
    private var appContext: Context? = null
    
    /**
     * Initialize RetrofitClient with application context for device fingerprinting
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    /**
     * SSL Certificate Pinning (MITM attack protection)
     * NOTE: You need to get the actual certificate hash from your server
     * 
     * To get the hash, run this command (you'll need openssl):
     * echo | openssl s_client -servername drawai-api.drawai.site -connect drawai-api.drawai.site:443 2>/dev/null | \
     * openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl base64
     * 
     * OR use online tools like: https://www.ssllabs.com/ssltest/
     * Then replace "REPLACE_WITH_ACTUAL_CERTIFICATE_HASH" below with the actual hash
     */
    private fun createCertificatePinner(): CertificatePinner? {
        return try {
            CertificatePinner.Builder()
                .add(
                    "drawai-api.drawai.site",
                    // Pin SHA256 from SSL Labs (valid for all Cloudflare IPs)
                    // Source: https://www.ssllabs.com/ssltest/ - drawai-api.drawai.site
                    // Verified: 26 Dec 2025
                    "sha256/I+OosmBgn83faSKLH4SD4uDpzM15Wg7lWk92lFESWHs="
                )
                .build()
        } catch (e: Exception) {
            // If pinning fails, log error but don't crash (fallback mode)
            android.util.Log.e("RetrofitClient", "Certificate pinning setup failed", e)
            null
        }
    }
    
    /**
     * Generate device fingerprint for multi-account detection
     */
    private fun getDeviceFingerprint(): String {
        val context = appContext ?: return "unknown"
        
        return try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"
            
            // Hash for privacy
            val bytes = androidId.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            android.util.Log.e("RetrofitClient", "Device fingerprint generation failed", e)
            "unknown"
        }
    }
    
    /**
     * Device fingerprinting interceptor
     */
    private val deviceInterceptor = okhttp3.Interceptor { chain ->
        val deviceId = getDeviceFingerprint()
        
        val request = chain.request().newBuilder()
            .addHeader("X-Device-ID", deviceId)
            .build()
        
        chain.proceed(request)
    }
    
    // Token caching to reduce lantency (Firebase getIdToken is slow)
    private var cachedToken: String? = null
    private var tokenExpiry: Long = 0
    private const val TOKEN_CACHE_DURATION = 55 * 60 * 1000L // 55 minutes

    /**
     * Auth interceptor untuk menambahkan Firebase Token ke Authorization header
     */
    private val apiKeyInterceptor = okhttp3.Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        
        // Get current user
        val user = FirebaseAuth.getInstance().currentUser
        
        if (user != null) {
            val currentTime = System.currentTimeMillis()
            
            // Check if we have a valid cached token
            if (cachedToken != null && currentTime < tokenExpiry) {
                requestBuilder.addHeader("Authorization", "Bearer $cachedToken")
            } else {
                try {
                    // Fetch ID token synchronously (blocking is fine here as this runs on network thread)
                    // forceRefresh = false to follow user request (let it expire naturally)
                    val result = Tasks.await(user.getIdToken(false))
                    val token = result.token
                    
                    if (token != null) {
                        // Cache the token
                        cachedToken = token
                        tokenExpiry = currentTime + TOKEN_CACHE_DURATION
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                        android.util.Log.d("RetrofitClient", "✅ New ID token cached for 55 minutes")
                    }
                } catch (e: ExecutionException) {
                    // Log error but proceed (might fail on server but we tried)
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        
        // We no longer send X-API-Key as we rely on Firebase Token
        // or server handles fallback if Authorization is missing (for legacy clients only)
        
        chain.proceed(requestBuilder.build())
    }
    
    /**
     * Interceptor untuk mendeteksi error 401 (Unauthorized) dan otomatis logout
     */
    private val unauthorizedInterceptor = okhttp3.Interceptor { chain ->
        val response = chain.proceed(chain.request())
        
        if (response.code == 401) {
            // Token is unauthorized - force logout
            android.util.Log.e("RetrofitClient", "Unauthorized (401) response received! Forcing logout...")
            clearCache() // Clear token cache
            appContext?.let { context ->
                // Use a non-UI context to trigger signOut which clears local state
                try {
                    com.doyouone.drawai.auth.AuthManager(context).signOut()
                } catch (e: Exception) {
                    android.util.Log.e("RetrofitClient", "Failed to sign out on 401", e)
                }
            }
        }
        
        response
    }
    
    /**
     * Clear token cache (e.g. on logout)
     */
    fun clearCache() {
        cachedToken = null
        tokenExpiry = 0
        android.util.Log.d("RetrofitClient", "🧹 Token cache cleared")
    }
    
    /**
     * OkHttp client dengan security enhancements:
     * - Device fingerprinting
     * - Certificate pinning (with fallback)
     * - Firebase auth token
     * - Conditional logging
     */
    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .addInterceptor(deviceInterceptor)  // Add device ID header
            .addInterceptor(apiKeyInterceptor)  // Add Firebase auth token
            .addInterceptor(unauthorizedInterceptor) // Auto-logout on 401
            .addInterceptor(loggingInterceptor) // Conditional logging
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
        
        // Add certificate pinning if configured
        val certPinner = createCertificatePinner()
        if (certPinner != null && !com.doyouone.drawai.BuildConfig.DEBUG) {
            // Only enable pinning in release builds (not debug)
            builder.certificatePinner(certPinner)
            android.util.Log.i("RetrofitClient", "Certificate pinning enabled")
        } else {
            android.util.Log.w("RetrofitClient", "Certificate pinning disabled (debug mode or not configured)")
        }
        
        builder.build()
    }
    
    /**
     * Retrofit instance
     */
    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * API Service instance
     */
    val apiService: DrawAIApiService
        get() = createRetrofit().create(DrawAIApiService::class.java)
    
    /**
     * Switch to local server for testing
     */
    fun useLocalServer() {
        currentBaseUrl = LOCAL_URL
    }
    
    /**
     * Switch to production server
     */
    fun useProductionServer() {
        currentBaseUrl = BASE_URL
    }
    
    /**
     * Set custom base URL
     */
    fun setBaseUrl(url: String) {
        currentBaseUrl = url
    }
    
    /**
     * Get current base URL
     */
    fun getBaseUrl(): String = currentBaseUrl
}
