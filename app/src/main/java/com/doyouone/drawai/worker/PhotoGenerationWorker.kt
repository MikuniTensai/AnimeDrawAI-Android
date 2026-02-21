package com.doyouone.drawai.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.doyouone.drawai.data.repository.CharacterRepository
import com.doyouone.drawai.data.repository.DrawAIRepository
import com.doyouone.drawai.util.GenerationNotificationUtils

class PhotoGenerationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // Repositories (manual instantiation as we don't use Hilt/Dagger yet)
    private val characterRepository = CharacterRepository()
    private val drawAIRepository = DrawAIRepository(characterRepo = characterRepository)

    override suspend fun doWork(): Result {
        val characterId = inputData.getString("characterId") ?: return Result.failure()
        val characterName = inputData.getString("characterName") ?: "Character"
        val userPrompt = inputData.getString("userPrompt") ?: ""
        val negativePrompt = inputData.getString("negativePrompt") ?: ""
        val mode = inputData.getString("mode") ?: "auto"
        val seedLong = inputData.getLong("seed", 0L)
        val seed = if (seedLong != 0L) seedLong else null
        val appearancePrompt = inputData.getString("appearancePrompt") ?: ""
        val language = inputData.getString("language") ?: "en"
        val chatContext = inputData.getString("chatContext") ?: ""

        // Start Foreground Service immediately to keep process alive
        try {
            val notification = GenerationNotificationUtils.getProgressNotification(
                appContext,
                "Asking $characterName...",
                "Generating photo request..."
            )
            
            val foregroundInfo = if (android.os.Build.VERSION.SDK_INT >= 34) {
                ForegroundInfo(
                    GenerationNotificationUtils.PENDING_NOTIFICATION_ID, 
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                ForegroundInfo(GenerationNotificationUtils.PENDING_NOTIFICATION_ID, notification)
            }
            
            setForeground(foregroundInfo)
        } catch (e: Exception) {
            Log.e("PhotoWorker", "Failed to set foreground", e)
        }

        return try {
            // STEP 1: Request Photo Prompt (LLM)
            val photoResponse = characterRepository.requestPhoto(
                characterId = characterId,
                userPrompt = if (mode == "custom") userPrompt else "",
                negativePrompt = if (mode == "custom") negativePrompt else "",
                seed = seed,
                appearancePrompt = appearancePrompt,
                language = language,
                chatContext = chatContext
            ).getOrThrow()

            if (!photoResponse.success || photoResponse.generationParams == null) {
                throw Exception(photoResponse.error ?: "Failed to get photo prompt")
            }

            val params = photoResponse.generationParams
            val prompt = params["prompt"] as? String ?: ""
            val negPrompt = params["negative_prompt"] as? String ?: ""
            val workflowId = params["workflow_id"] as? String ?: "anime_main_mix"
            val genSeed = (params["seed"] as? Number)?.toLong()
            val caption = params["caption"] as? String ?: "Here is a photo!"

            // Update Notification
            try {
                val notification = GenerationNotificationUtils.getProgressNotification(
                    appContext,
                    "Taking photo...",
                    "Rendering image for $characterName"
                )
                setForeground(ForegroundInfo(GenerationNotificationUtils.PENDING_NOTIFICATION_ID, notification))
            } catch (e: Exception) {}

            // STEP 2: Generate Image (ComfyUI)
            val result = drawAIRepository.generateAndWaitForCompletion(
                positivePrompt = prompt,
                negativePrompt = negPrompt,
                workflow = workflowId,
                userId = "", // Handled internally
                seed = genSeed
            )

            if (result.isFailure) {
                val e = result.exceptionOrNull()
                // Handle limits via Notification if possible, mostly just fail
                throw e ?: Exception("Generation failed")
            }

            val statusResult = result.getOrThrow()
            
            // Construct URL
            val relativeUrl = statusResult.downloadUrls?.firstOrNull()
            val finalImageUrl = if (relativeUrl != null) {
                if (relativeUrl.startsWith("http")) relativeUrl
                else "${com.doyouone.drawai.data.api.RetrofitClient.getBaseUrl().trimEnd('/')}$relativeUrl"
            } else {
                 val file = statusResult.resultFiles?.firstOrNull()
                 if (file != null) {
                      val filename = file.substringAfterLast('\\').substringAfterLast('/')
                      "${com.doyouone.drawai.data.api.RetrofitClient.getBaseUrl().trimEnd('/')}/download/$filename"
                 } else null
            }

            if (finalImageUrl != null) {
                // STEP 3: Inject Message to Chat History
                characterRepository.injectMessage(
                    characterId = characterId,
                    role = "assistant",
                    content = caption,
                    imageUrl = finalImageUrl
                ).getOrThrow()

                // Success Notification
                GenerationNotificationUtils.showSuccessNotification(
                    appContext,
                    "New Photo from $characterName!",
                    caption
                )
                
                // Return success output
                val output = workDataOf("imageUrl" to finalImageUrl)
                Result.success(output)
            } else {
                throw Exception("No image URL received")
            }

        } catch (e: Exception) {
            Log.e("PhotoWorker", "Work failed", e)
            
            val msg = e.message ?: ""
            if (msg.contains("403") || msg.contains("limit", ignoreCase = true)) {
                 GenerationNotificationUtils.showErrorNotification(
                    appContext,
                    "Daily Limit Reached",
                    "Watch ads or upgrade to continue."
                )
            } else {
                GenerationNotificationUtils.showErrorNotification(
                    appContext,
                    "Photo Failed",
                    msg
                )
            }
            Result.failure()
        }
    }
}
