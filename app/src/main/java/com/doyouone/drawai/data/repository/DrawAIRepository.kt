package com.doyouone.drawai.data.repository

import android.util.Log
import com.doyouone.drawai.data.api.DrawAIApiService
import com.doyouone.drawai.data.api.RetrofitClient
import com.doyouone.drawai.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

/**
 * Repository untuk handle semua API calls ke DrawAI server
 */
class DrawAIRepository(
    private val apiService: DrawAIApiService = RetrofitClient.apiService,
    val firebaseRepo: FirebaseGenerationRepository = FirebaseGenerationRepository(),
    private val characterRepo: CharacterRepository = CharacterRepository()
) {
    
    companion object {
        private const val TAG = "DrawAIRepository"
        private const val POLLING_INTERVAL_MS = 2000L // Poll setiap 2 detik
        private const val MAX_POLLING_ATTEMPTS = 180 // Max 6 menit (180 * 2 detik) - increased from 150
    }
    
    /**
     * Get list of available workflows
     * Falls back to local dummy data if API fails
     */
    suspend fun getWorkflows(): Result<Map<String, WorkflowInfo>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching workflows from API...")
            val response = apiService.getWorkflows()
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    if (body.workflows.isNullOrEmpty()) {
                        Log.w(TAG, "⚠️ API returned empty workflow list, using fallback")
                        return@withContext Result.success(getDummyWorkflowsMap())
                    }
                    Log.d(TAG, "✅ Successfully loaded ${body.workflows.size} workflows from API")
                    return@withContext Result.success(body.workflows)
                } else {
                    Log.e(TAG, "❌ API returned success=false")
                    return@withContext Result.failure(Exception("Failed to get workflows"))
                }
            } else {
                Log.e(TAG, "❌ API Error: ${response.code()} - ${response.message()}")
                return@withContext Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getting workflows from API: ${e.message}", e)
            
            // Fallback to local dummy data
            Log.w(TAG, "⚠️  Falling back to local dummy workflows")
            return@withContext try {
                val dummyWorkflows = getDummyWorkflowsMap()
                Log.d(TAG, "✅ Loaded ${dummyWorkflows.size} workflows from local data")
                Result.success(dummyWorkflows)
            } catch (fallbackError: Exception) {
                Log.e(TAG, "❌ Even fallback failed: ${fallbackError.message}")
                Result.failure(e) // Return original error
            }
        }
    }
    
    /**
     * Get dummy workflows as fallback
     */
    private fun getDummyWorkflowsMap(): Map<String, WorkflowInfo> {
        // Return fallback workflows to prevent empty list UI
        return mapOf(
            "anime_red_lily" to WorkflowInfo(
                name = "Red Lily",
                description = "Best Quality Anime Style",
                estimatedTime = "40s",
                fileExists = true,
                isPremium = false
            ),
            "general_asia_blend_illustrious" to WorkflowInfo(
                name = "Asia Blend",
                description = "Detailed Asian Illustration",
                estimatedTime = "45s",
                fileExists = true,
                isPremium = false
            ),
        )
    }
    
    /**
     * Generate image dengan workflow tertentu
     * Server handles ALL limit checking atomically!
     */
    suspend fun generateImage(
        positivePrompt: String,
        negativePrompt: String = "",
        workflow: String = "standard",
        userId: String,
        seed: Long? = null
    ): Result<GenerateResponse> = withContext(Dispatchers.IO) {
        try {
            // REMOVED: Client-side limit check
            // Server now does atomic check+increment to prevent race conditions!
            Log.d(TAG, "Sending generation request for user: $userId")
            
            // Proceed directly with generation - server will enforce limits
            val request = GenerateRequest(
                positivePrompt = positivePrompt,
                negativePrompt = negativePrompt,
                workflow = workflow,
                seed = seed
            )
            
            val response = apiService.generateImage(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.taskId != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: "Generation failed"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating image", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check status dari generation task
     */
    suspend fun getTaskStatus(taskId: String): Result<TaskStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTaskStatus(taskId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: "Failed to get task status"))
                }
            } else {
                val errorMessage = when (response.code()) {
                    500 -> "Server sedang sibuk (Error 500)"
                    502, 503, 504 -> "Server tidak tersedia (Error ${response.code()})"
                    404 -> "Task tidak ditemukan (Error 404)"
                    401 -> "Autentikasi gagal (Error 401)"
                    else -> "API Error: ${response.code()} - ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting task status", e)
            Result.failure(Exception("Connection error: ${e.message}"))
        }
    }
    
    /**
     * Poll task status sampai selesai atau error
     * @return Result dengan TaskStatusResponse final
     */
    suspend fun pollTaskUntilComplete(
        taskId: String,
        onStatusUpdate: ((TaskStatusResponse) -> Unit)? = null
    ): Result<TaskStatusResponse> = withContext(Dispatchers.IO) {
        var attempts = 0
        var consecutiveErrors = 0
        val maxConsecutiveErrors = 5
        
        while (attempts < MAX_POLLING_ATTEMPTS) {
            try {
                val result = getTaskStatus(taskId)
                
                if (result.isSuccess) {
                    val status = result.getOrNull()!!
                    consecutiveErrors = 0 // Reset error counter on success
                    onStatusUpdate?.invoke(status)
                    
                    when (status.status) {
                        "completed" -> {
                            Log.d(TAG, "Task $taskId completed successfully")
                            return@withContext Result.success(status)
                        }
                        "error", "failed" -> {
                            Log.e(TAG, "Task $taskId failed: ${status.error}")
                            return@withContext Result.failure(
                                Exception(status.error ?: "Generation failed")
                            )
                        }
                        "queued", "pending", "processing" -> {
                            // Continue polling
                            Log.d(TAG, "Task $taskId status: ${status.status}, attempt $attempts")
                        }
                        else -> {
                            Log.w(TAG, "Unknown status: ${status.status}")
                        }
                    }
                } else {
                    consecutiveErrors++
                    Log.e(TAG, "Failed to get status for task $taskId (attempt $consecutiveErrors)")
                    
                    // If too many consecutive errors, give up
                    if (consecutiveErrors >= maxConsecutiveErrors) {
                        return@withContext Result.failure(
                            Exception("Server error: Too many consecutive failures. Please try again later.")
                        )
                    }
                }
                
                delay(POLLING_INTERVAL_MS)
                attempts++
                
            } catch (e: Exception) {
                consecutiveErrors++
                Log.e(TAG, "Error polling task $taskId (attempt $consecutiveErrors)", e)
                
                // If too many consecutive errors, give up
                if (consecutiveErrors >= maxConsecutiveErrors) {
                    return@withContext Result.failure(
                        Exception("Connection error: Too many consecutive failures. Please check your connection and try again.")
                    )
                }
                
                delay(POLLING_INTERVAL_MS)
                attempts++
            }
        }
        
        Result.failure(Exception("Timeout: Task took too long to complete (5 minutes). Please try again with a simpler prompt."))
    }
    
    /**
     * Download generated image
     */
    suspend fun downloadImage(
        filename: String,
        destinationFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.downloadImage(filename)
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                // Save to file
                FileOutputStream(destinationFile).use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
                
                Log.d(TAG, "Image downloaded successfully to ${destinationFile.absolutePath}")
                Result.success(destinationFile)
            } else {
                Result.failure(Exception("Download failed: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate image dan tunggu sampai selesai (all-in-one function)
     * Now includes generation limit checking
     */
    suspend fun generateAndWaitForCompletion(
        positivePrompt: String,
        negativePrompt: String = "",
        workflow: String = "standard",
        userId: String,
        seed: Long? = null,
        onSuccessStart: ((GenerateResponse) -> Unit)? = null,
        onStatusUpdate: ((String, TaskStatusResponse?) -> Unit)? = null
    ): Result<TaskStatusResponse> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Start generation (includes limit check)
            onStatusUpdate?.invoke("Memulai generation...", null)
            val generateResult = generateImage(positivePrompt, negativePrompt, workflow, userId, seed)
            
            if (generateResult.isFailure) {
                return@withContext Result.failure(
                    generateResult.exceptionOrNull() ?: Exception("Failed to start generation")
                )
            }
            
            // Invoke callback with response (containing gems_earned)
            generateResult.getOrNull()?.let { onSuccessStart?.invoke(it) }
            
            val taskId = generateResult.getOrNull()?.taskId
                ?: return@withContext Result.failure(Exception("No task ID received"))
            
            // Step 2: Poll until complete
            onStatusUpdate?.invoke("Processing...", null)
            val pollResult = pollTaskUntilComplete(taskId) { status ->
                onStatusUpdate?.invoke("Status: ${status.status}", status)
            }
            
            pollResult
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateAndWaitForCompletion", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get generation limit info for user
     */
    suspend fun getGenerationLimit(userId: String): Result<GenerationLimit> {
        return firebaseRepo.getGenerationLimit(userId)
    }
    
    /**
     * Get generation limit as Flow
     */
    fun getGenerationLimitFlow(userId: String): Flow<GenerationLimit> {
        return firebaseRepo.getGenerationLimitFlow(userId)
    }
    suspend fun addBonusGeneration(userId: String): Result<GenerationLimit> {
        return firebaseRepo.addBonusGeneration(userId)
    }

    /**
     * Health check
     */
    suspend fun healthCheck(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.healthCheck()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Health check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in health check", e)
            Result.failure(e)
        }
    }
    
    /**
     * Downgrade user to free plan
     */
    suspend fun downgradeToFree(userId: String): Result<Unit> {
        return firebaseRepo.downgradeToFree(userId)
    }

    /**
     * Get character count as Flow for real-time updates
     */
    fun getCharacterCountFlow(userId: String): Flow<Int> {
        return characterRepo.getCharactersFlow(userId, includeDeleted = true).map { it.size }
    }

    /**
     * Get gem count as Flow for real-time updates
     */
    fun getGemCountFlow(userId: String): Flow<Int> {
        return firebaseRepo.getGemCountFlow(userId)
    }

    // --- Daily Rewards ---

    suspend fun checkDailyRewardStatus(): Result<DailyStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.checkDailyStatus()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to check daily status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking daily status", e)
            Result.failure(e)
        }
    }

    suspend fun claimDailyReward(): Result<DailyClaimResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.claimDailyReward()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Failed to claim reward: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error claiming reward", e)
            Result.failure(e)
        }
    }

    // --- Inventory & Gift ---

    suspend fun getInventory(): Result<List<InventoryItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getInventory()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body.inventory ?: emptyList())
                } else {
                    Result.failure(Exception(body.error ?: "Unknown error"))
                }
            } else {
                Result.failure(Exception("Failed to get inventory: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting inventory", e)
            Result.failure(e)
        }
    }

    suspend fun sendGift(characterId: String, itemId: String): Result<GiftResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.sendGift(GiftRequest(characterId, itemId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to send gift: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending gift", e)
            Result.failure(e)
        }
    }

    // --- Shop ---

    suspend fun getShopItems(): Result<List<ShopItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getShopItems()
            if (response.isSuccessful && response.body() != null) {
                 val body = response.body()!!
                 if (body.success) {
                     Result.success(body.items ?: emptyList())
                 } else {
                     Result.failure(Exception(body.error ?: "Unknown error"))
                 }
            } else {
                Result.failure(Exception("Failed to get shop items: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting shop items", e)
            Result.failure(e)
        }
    }

    suspend fun purchaseShopItem(itemId: String): Result<PurchaseResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.purchaseShopItem(PurchaseRequest(itemId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Purchase failed: ${response.code()}"))
            }
        } catch (e: Exception) {
             Log.e(TAG, "Error purchasing item", e)
             Result.failure(e)
        }
    }

    suspend fun useItem(itemId: String): Result<UseItemResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.useItem(UseItemRequest(itemId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Use item failed: ${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error using item", e)
            Result.failure(e)
        }
    }

    suspend fun toggleNotification(characterId: String, enable: Boolean): Result<ToggleNotificationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.toggleNotification(ToggleNotificationRequest(characterId, enable))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Toggle failed: ${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling notification", e)
            Result.failure(e)
        }
    }

    suspend fun updateCharacterProfileImage(characterId: String, imageUrl: String): Result<UpdateProfileImageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateCharacterProfileImage(UpdateProfileImageRequest(characterId, imageUrl))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Update failed: ${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile image", e)
            Result.failure(e)
        }
    }
}
