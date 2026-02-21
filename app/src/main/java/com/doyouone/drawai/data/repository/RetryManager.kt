package com.doyouone.drawai.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Smart retry manager with exponential backoff
 * Queues failed generation requests and retries when network is restored
 * Note: Uses in-memory queue only (no persistence)
 */
class RetryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RetryManager"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_BACKOFF_MS = 1000L // 1 second
        private const val MAX_BACKOFF_MS = 16000L // 16 seconds
    }
    
    private val _pendingRetries = MutableStateFlow<List<GenerationRequest>>(emptyList())
    val pendingRetries: StateFlow<List<GenerationRequest>> = _pendingRetries.asStateFlow()
    
    data class GenerationRequest(
        val id: String = java.util.UUID.randomUUID().toString(),
        val positivePrompt: String,
        val negativePrompt: String,
        val workflow: String,
        val userId: String,
        val attemptCount: Int = 0,
        val lastAttemptTime: Long = System.currentTimeMillis(),
        val failureReason: String? = null
    )
    
    /**
     * Queue a failed generation request for retry
     */
    suspend fun queueForRetry(request: GenerationRequest) {
        if (request.attemptCount >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Max retry attempts reached for request ${request.id}, discarding")
            return
        }
        
        val updatedRequest = request.copy(
            attemptCount = request.attemptCount + 1,
            lastAttemptTime = System.currentTimeMillis()
        )
        
        val currentQueue = _pendingRetries.value.toMutableList()
        
        // Remove existing entry with same ID if present
        currentQueue.removeAll { it.id == updatedRequest.id }
        
        // Add updated request
        currentQueue.add(updatedRequest)
        
        _pendingRetries.value = currentQueue
        
        Log.d(TAG, "Queued request ${updatedRequest.id} for retry (attempt ${updatedRequest.attemptCount})")
    }
    
    /**
     * Retry all pending requests with exponential backoff
     */
    suspend fun retryPendingRequests(onRetry: suspend (GenerationRequest) -> Result<Unit>) {
        val queue = _pendingRetries.value.toList()
        
        if (queue.isEmpty()) {
            Log.d(TAG, "No pending retries")
            return
        }
        
        Log.d(TAG, "Retrying ${queue.size} pending requests")
        
        val successfulRetries = mutableListOf<String>()
        
        for (request in queue) {
            // Calculate backoff delay
            val backoffMs = calculateBackoff(request.attemptCount)
            
            // Wait if this isn't the first attempt
            if (request.attemptCount > 1) {
                Log.d(TAG, "Waiting ${backoffMs}ms before retry (attempt ${request.attemptCount})")
                delay(backoffMs)
            }
            
            // Attempt retry
            Log.d(TAG, "Retrying request ${request.id}")
            val result = onRetry(request)
            
            if (result.isSuccess) {
                Log.d(TAG, "✅ Retry successful for ${request.id}")
                successfulRetries.add(request.id)
            } else {
                Log.e(TAG, "❌ Retry failed for ${request.id}: ${result.exceptionOrNull()?.message}")
                
                // Re-queue with incremented attempt count if under limit
                if (request.attemptCount < MAX_RETRY_ATTEMPTS) {
                    queueForRetry(request.copy(
                        failureReason = result.exceptionOrNull()?.message
                    ))
                } else {
                    Log.w(TAG, "Max attempts reached for ${request.id}, removing from queue")
                }
            }
        }
        
        // Remove successful retries from queue
        if (successfulRetries.isNotEmpty()) {
            removeFromQueue(successfulRetries)
        }
    }
    
    /**
     * Remove specific requests from retry queue
     */
    suspend fun removeFromQueue(requestIds: List<String>) {
        val updatedQueue = _pendingRetries.value.filter { it.id !in requestIds }
        _pendingRetries.value = updatedQueue
        
        Log.d(TAG, "Removed ${requestIds.size} requests from queue")
    }
    
    /**
     * Clear all pending retries
     */
    suspend fun clearQueue() {
        _pendingRetries.value = emptyList()
        Log.d(TAG, "Cleared retry queue")
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private fun calculateBackoff(attemptCount: Int): Long {
        val backoff = INITIAL_BACKOFF_MS * (1 shl (attemptCount - 1)) // 2^(n-1)
        return minOf(backoff, MAX_BACKOFF_MS)
    }
}
