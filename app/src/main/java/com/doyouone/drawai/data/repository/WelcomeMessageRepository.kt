package com.doyouone.drawai.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

/**
 * Data class for welcome message with banner image support
 */
data class WelcomeMessageData(
    val title: String = "",
    val message: String = "",
    val iconUrl: String? = null,
    val imageUrl: String? = null, // Banner image URL (can be null)
    val buttonText: String = "Get Started",
    val isActive: Boolean = true
)

/**
 * Repository untuk mengelola welcome message dari Firebase Firestore
 */
class WelcomeMessageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    companion object {
        private const val TAG = "WelcomeMessageRepo"
    }
    
    /**
     * Get default welcome message data
     */
    private fun getDefaultWelcomeMessageData(): WelcomeMessageData {
        return WelcomeMessageData(
            title = "Welcome to DrawAI",
            message = "Create stunning anime artwork with the power of AI!",
            isActive = true
        )
    }
    
    /**
     * Mengambil welcome message dari Firebase (legacy - returns just message string)
     */
    suspend fun getWelcomeMessage(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val data = getWelcomeMessageData().getOrNull()
            if (data != null && data.isActive && data.message.isNotEmpty()) {
                Result.success(data.message)
            } else {
                Result.success("")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching welcome message", e)
            Result.success("Welcome to DrawAI")
        }
    }
    
    /**
     * Mengambil welcome message data lengkap dari Firebase (dengan imageUrl support)
     */
    suspend fun getWelcomeMessageData(): Result<WelcomeMessageData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching welcome message data from Firebase...")
            
            // Add timeout to prevent hanging
            withTimeout(5000L) { // 5 seconds timeout
                val document = firestore.collection("app_settings")
                    .document("welcome_message")
                    .get()
                    .await()
                
                if (document.exists()) {
                    val title = document.getString("title") ?: "Welcome"
                    val message = document.getString("message") ?: ""
                    val iconUrl = document.getString("iconUrl")
                    val imageUrl = document.getString("imageUrl") // Banner image (can be null)
                    val buttonText = document.getString("buttonText") ?: "Get Started"
                    
                    // Handle isActive as Boolean or String
                    val isActive = try {
                        document.getBoolean("isActive") ?: (document.getString("isActive")?.toBoolean() ?: true)
                    } catch (e: Exception) {
                        true // Default to true if parsing fails
                    }
                    
                    val data = WelcomeMessageData(
                        title = title,
                        message = message,
                        iconUrl = iconUrl,
                        imageUrl = imageUrl,
                        buttonText = buttonText,
                        isActive = isActive
                    )
                    
                    Log.d(TAG, "Welcome message data loaded: title=$title, hasImage=${imageUrl != null}, isActive=$isActive")
                    Result.success(data)
                } else {
                    Log.d(TAG, "Welcome message document not found, using default")
                    Result.success(getDefaultWelcomeMessageData())
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Timeout fetching welcome message")
            Result.success(getDefaultWelcomeMessageData())
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching welcome message", e)
            Result.success(getDefaultWelcomeMessageData())
        }
    }
    
    /**
     * Membuat default welcome message di Firebase
     */
    private suspend fun createDefaultWelcomeMessage() {
        try {
            Log.d(TAG, "Creating default welcome message document...")
            
            val welcomeData = hashMapOf(
                "title" to "Welcome to DrawAI",
                "message" to "Create stunning anime artwork with the power of AI!",
                "iconUrl" to null,
                "imageUrl" to null, // Banner image (initially null)
                "buttonText" to "Get Started",
                "isActive" to true,
                "lastUpdated" to com.google.firebase.Timestamp.now(),
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("app_settings")
                .document("welcome_message")
                .set(welcomeData)
                .await()
            
            Log.d(TAG, "Default welcome message created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating default welcome message", e)
        }
    }
    
    /**
     * Update welcome message (untuk admin)
     */
    suspend fun updateWelcomeMessage(newMessage: String): Result<Unit> {
        return try {
            Log.d(TAG, "Updating welcome message...")
            
            val updateData = hashMapOf(
                "message" to newMessage,
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("app_settings")
                .document("welcome_message")
                .update(updateData as Map<String, Any>)
                .await()
            
            Log.d(TAG, "Welcome message updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating welcome message", e)
            Result.failure(e)
        }
    }
}
