package com.doyouone.drawai.auth

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class RateLimitManager(private val authManager: AuthManager) {
    private val firestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val MAX_REQUESTS_PER_HOUR = 2
        private const val HOUR_IN_MILLIS = 60 * 60 * 1000L // 1 hour in milliseconds
    }
    
    data class RateLimitStatus(
        val canGenerate: Boolean,
        val remainingRequests: Int,
        val resetTimeMillis: Long,
        val message: String
    )
    
    // Check if user can generate
    suspend fun canUserGenerate(): RateLimitStatus {
        val userId = authManager.getCurrentUserId()
            ?: return RateLimitStatus(
                canGenerate = false,
                remainingRequests = 0,
                resetTimeMillis = 0,
                message = "Please sign in to generate images"
            )
        
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (!document.exists()) {
                // New user, allow generation
                return RateLimitStatus(
                    canGenerate = true,
                    remainingRequests = MAX_REQUESTS_PER_HOUR,
                    resetTimeMillis = System.currentTimeMillis() + HOUR_IN_MILLIS,
                    message = "You have $MAX_REQUESTS_PER_HOUR requests available"
                )
            }
            
            val lastGenerationTime = document.getLong("lastGenerationTime") ?: 0L
            val generationCount = document.getLong("generationCount")?.toInt() ?: 0
            val currentTime = System.currentTimeMillis()
            val timeSinceLastGeneration = currentTime - lastGenerationTime
            
            // Reset counter if more than 1 hour has passed
            if (timeSinceLastGeneration >= HOUR_IN_MILLIS) {
                return RateLimitStatus(
                    canGenerate = true,
                    remainingRequests = MAX_REQUESTS_PER_HOUR,
                    resetTimeMillis = currentTime + HOUR_IN_MILLIS,
                    message = "You have $MAX_REQUESTS_PER_HOUR requests available"
                )
            }
            
            // Check if user has exceeded limit
            if (generationCount >= MAX_REQUESTS_PER_HOUR) {
                val resetTime = lastGenerationTime + HOUR_IN_MILLIS
                val minutesUntilReset = TimeUnit.MILLISECONDS.toMinutes(resetTime - currentTime)
                return RateLimitStatus(
                    canGenerate = false,
                    remainingRequests = 0,
                    resetTimeMillis = resetTime,
                    message = "Rate limit exceeded. Try again in $minutesUntilReset minutes"
                )
            }
            
            // User can still generate
            val remaining = MAX_REQUESTS_PER_HOUR - generationCount
            RateLimitStatus(
                canGenerate = true,
                remainingRequests = remaining,
                resetTimeMillis = lastGenerationTime + HOUR_IN_MILLIS,
                message = "You have $remaining request${if (remaining > 1) "s" else ""} remaining"
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            RateLimitStatus(
                canGenerate = false,
                remainingRequests = 0,
                resetTimeMillis = 0,
                message = "Error checking rate limit: ${e.message}"
            )
        }
    }
    
    // Record a generation request
    suspend fun recordGeneration(): Boolean {
        val userId = authManager.getCurrentUserId() ?: return false
        
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            val currentTime = System.currentTimeMillis()
            val lastGenerationTime = document.getLong("lastGenerationTime") ?: 0L
            val generationCount = document.getLong("generationCount")?.toInt() ?: 0
            val timeSinceLastGeneration = currentTime - lastGenerationTime
            
            // Reset counter if more than 1 hour has passed
            val newCount = if (timeSinceLastGeneration >= HOUR_IN_MILLIS) {
                1 // Reset to 1 (this generation)
            } else {
                generationCount + 1
            }
            
            // Update Firestore
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "generationCount" to newCount,
                        "lastGenerationTime" to currentTime
                    )
                )
                .await()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Get remaining requests
    suspend fun getRemainingRequests(): Int {
        val status = canUserGenerate()
        return status.remainingRequests
    }
    
    // Get time until reset in minutes
    suspend fun getMinutesUntilReset(): Long {
        val status = canUserGenerate()
        if (status.canGenerate) return 0
        
        val currentTime = System.currentTimeMillis()
        return TimeUnit.MILLISECONDS.toMinutes(status.resetTimeMillis - currentTime)
    }
    
    // Format time until reset
    suspend fun getFormattedResetTime(): String {
        val minutes = getMinutesUntilReset()
        if (minutes <= 0) return "Now"
        
        val hours = minutes / 60
        val mins = minutes % 60
        
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }
}
