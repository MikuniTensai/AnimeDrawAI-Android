package com.doyouone.drawai.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.doyouone.drawai.MainActivity
import com.doyouone.drawai.R
import com.doyouone.drawai.data.model.GenerationLimitExceededException
import com.doyouone.drawai.data.repository.*
import com.doyouone.drawai.util.GenerationNotificationUtils
import com.doyouone.drawai.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WorkflowGenerationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationHelper = NotificationHelper(context)
    
    // Repositories
    private val repository = DrawAIRepository()
    private val generationLogRepository = GenerationLogRepository()
    private val workflowStatsRepository = WorkflowStatsRepository()
    private val imageStorage = com.doyouone.drawai.data.local.ImageStorage(context)
    private val userPreferences = com.doyouone.drawai.data.preferences.UserPreferences(context)

    companion object {
        const val KEY_USER_ID = "KEY_USER_ID"
        const val KEY_POSITIVE_PROMPT = "KEY_POSITIVE_PROMPT"
        const val KEY_NEGATIVE_PROMPT = "KEY_NEGATIVE_PROMPT"
        const val KEY_WORKFLOW = "KEY_WORKFLOW"
        const val KEY_SEED = "KEY_SEED"
        
        const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "generation_channel"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val userId = inputData.getString(KEY_USER_ID) ?: return@withContext Result.failure()
        val positivePrompt = inputData.getString(KEY_POSITIVE_PROMPT) ?: ""
        val negativePrompt = inputData.getString(KEY_NEGATIVE_PROMPT) ?: ""
        val workflowId = inputData.getString(KEY_WORKFLOW) ?: "standard"
        // WorkManager passes Longs as Long, but we need to be careful with defaults
        val seed = inputData.getLong(KEY_SEED, 1062314217360759L)

        setForeground(createForegroundInfo(positivePrompt))

        try {
            Log.d("WorkflowWorker", "Starting generation for user $userId, workflow $workflowId")

            val result = repository.generateAndWaitForCompletion(
                positivePrompt = positivePrompt,
                negativePrompt = negativePrompt,
                workflow = workflowId,
                userId = userId,
                seed = seed,
                onSuccessStart = { /* No-op in worker */ },
                onStatusUpdate = { status, _ ->
                     // Optional: Update notification with specific status text
                     updateNotification(status)
                }
            )

            if (result.isSuccess) {
                val taskStatus = result.getOrNull()!!
                
                // 1. Log Generation
                generationLogRepository.logGeneration(userId, taskStatus, workflowId)
                
                // 2. Stats
                workflowStatsRepository.incrementGeneration(workflowId)
                
                // 3. User Preferences (Local Count)
                userPreferences.incrementGenerationCount()
                
                // 4. Firebase Stats (Global & Daily)
                val usageStatsRepo = UsageStatisticsRepository(userId)
                usageStatsRepo.incrementGenerations()
                val genTrackingRepo = GenerationTrackingRepository(userId)
                genTrackingRepo.incrementGenerationCount()
                
                // 5. Download & Save Images
                var firstImagePath: String? = null
                if (!taskStatus.resultFiles.isNullOrEmpty()) {
                    updateNotification("Downloading images...")
                    
                    taskStatus.resultFiles.forEach { filename ->
                        try {
                            val actualFilename = filename.substringAfterLast("/").substringAfterLast("\\")
                            val destinationFile = File(context.cacheDir, actualFilename)
                            
                            val downloadResult = repository.downloadImage(actualFilename, destinationFile)
                            if (downloadResult.isSuccess) {
                                val file = downloadResult.getOrNull()!!
                                val imageBytes = file.readBytes()
                                
                                val savedImage = imageStorage.saveImageFromBytes(
                                    imageBytes = imageBytes,
                                    prompt = positivePrompt,
                                    negativePrompt = negativePrompt,
                                    workflow = workflowId,
                                    seed = taskStatus.seed ?: seed
                                )
                                
                                if (savedImage != null && firstImagePath == null) {
                                    firstImagePath = savedImage.imageUrl // Or keep URL?
                                }
                                file.delete()
                            }
                        } catch (e: Exception) {
                            Log.e("WorkflowWorker", "Failed to save image", e)
                        }
                    }
                }
                
                // Notification Success
                if (notificationHelper.hasNotificationPermission()) {
                    notificationHelper.showGenerationCompleteNotification(
                        imageUrl = taskStatus.resultFiles?.firstOrNull(),
                        success = true
                    )
                }
                
                // Return success with any output data if needed
                val output = workDataOf("status" to "success")
                Result.success(output)
                
            } else {
                val exception = result.exceptionOrNull()
                Log.e("WorkflowWorker", "Generation failed", exception)

                // specific notification for limits
                 if (exception?.message?.contains("403") == true || 
                     exception?.message?.contains("limit", ignoreCase = true) == true) {
                     
                     GenerationNotificationUtils.showErrorNotification(context, "Limit Reached", "Daily limit reached.")
                 } else {
                     GenerationNotificationUtils.showErrorNotification(context, "Generation Failed", "Please try again.")
                 }
                 
                Result.failure()
            }
            
        } catch (e: Exception) {
            Log.e("WorkflowWorker", "Worker Exception", e)
            GenerationNotificationUtils.showErrorNotification(context, "Error", e.message ?: "Unknown error")
            Result.failure()
        }
    }

    private fun createForegroundInfo(prompt: String): ForegroundInfo {
        val notification = GenerationNotificationUtils.getProgressNotification(
            context,
            "Generating Image",
            "Generating: ${prompt.take(20)}..."
        )
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                GenerationNotificationUtils.PENDING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
             ForegroundInfo(
                GenerationNotificationUtils.PENDING_NOTIFICATION_ID,
                notification
            )
        }
    }
    
    private fun updateNotification(status: String) {
        val notification = GenerationNotificationUtils.getProgressNotification(
            context,
            "Generating Image",
            status
        )
        notificationManager.notify(GenerationNotificationUtils.PENDING_NOTIFICATION_ID, notification)
    }
}
