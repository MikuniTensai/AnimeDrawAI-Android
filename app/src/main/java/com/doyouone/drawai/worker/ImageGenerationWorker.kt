package com.doyouone.drawai.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.doyouone.drawai.MainActivity
import com.doyouone.drawai.R
import com.doyouone.drawai.data.repository.DrawAIRepository
import kotlinx.coroutines.delay

/**
 * Background worker untuk generate image menggunakan WorkManager
 * Allows user to leave app while generation is in progress
 * Supports Android 12+ foreground service requirements
 */
class ImageGenerationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "ImageGenerationWorker"
        
        // Input/Output keys
        const val KEY_POSITIVE_PROMPT = "positive_prompt"
        const val KEY_NEGATIVE_PROMPT = "negative_prompt"
        const val KEY_WORKFLOW_ID = "workflow_id"
        const val KEY_USER_ID = "user_id"
        const val KEY_RESULT_IMAGE_URL = "result_image_url"
        const val KEY_RESULT_TASK_ID = "result_task_id"
        const val KEY_ERROR_MESSAGE = "error_message"
        
        // Notification constants
        private const val NOTIFICATION_CHANNEL_ID = "image_generation_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Image Generation"
        private const val NOTIFICATION_ID = 1001
        
        // Polling settings
        private const val MAX_POLLING_TIME_MS = 5 * 60 * 1000L // 5 minutes max
        private const val POLL_INTERVAL_MS = 3000L // Poll every 3 seconds
    }
    
    private val repository = DrawAIRepository()
    
    override suspend fun doWork(): Result {
        val positivePrompt = inputData.getString(KEY_POSITIVE_PROMPT) ?: return Result.failure(
            workDataOf(KEY_ERROR_MESSAGE to "Missing positive prompt")
        )
        val negativePrompt = inputData.getString(KEY_NEGATIVE_PROMPT) ?: ""
        val workflowId = inputData.getString(KEY_WORKFLOW_ID) ?: return Result.failure(
            workDataOf(KEY_ERROR_MESSAGE to "Missing workflow ID")
        )
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure(
            workDataOf(KEY_ERROR_MESSAGE to "Missing user ID")
        )
        
        Log.d(TAG, "🚀 Starting background generation for user: $userId, workflow: $workflowId")
        
        // Create notification channel
        createNotificationChannel()
        
        // Set as foreground service (required for Android 12+)
        setForeground(createForegroundInfo("Starting generation..."))
        
        return try {
            // Step 1: Generate image
            setForeground(createForegroundInfo("Sending request..."))
            val generateResult = repository.generateImage(
                positivePrompt = positivePrompt,
                negativePrompt = negativePrompt,
                workflow = workflowId,
                userId = userId
            )
            
            if (generateResult.isFailure) {
                val error = generateResult.exceptionOrNull()?.message ?: "Failed to start generation"
                Log.e(TAG, "❌ Generation failed: $error")
                showNotification("Generation Failed", error, isSuccess = false)
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to error))
            }
            
            val taskId = generateResult.getOrNull()?.taskId ?: return Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "Missing task ID from response")
            )
            
            Log.d(TAG, "✅ Generation started with task ID: $taskId")
            
            // Step 2: Poll for completion
            setForeground(createForegroundInfo("Processing... (0%)"))
            
            val pollResult = repository.pollTaskUntilComplete(taskId) { statusResponse ->
                // Update notification with progress
                // Note: Can't call setForeground here (suspend function)
                // Progress is logged instead
                Log.d(TAG, "📊 Status: ${statusResponse.status}")
            }
            
            if (pollResult.isFailure) {
                val error = pollResult.exceptionOrNull()?.message ?: "Polling failed"
                Log.e(TAG, "❌ Polling failed: $error")
                showNotification("Generation Error", error, isSuccess = false)
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to error))
            }
            
            val finalStatus = pollResult.getOrNull()
            
            if (finalStatus?.status == "completed" && !finalStatus.resultFiles.isNullOrEmpty()) {
                val imageUrl = finalStatus.resultFiles.firstOrNull()
                Log.d(TAG, "🎉 Generation completed! Image: $imageUrl")
                
                showNotification(
                    "Image Generated!",
                    "Your image is ready to view",
                    isSuccess = true
                )
                
                Result.success(
                    workDataOf(
                        KEY_RESULT_IMAGE_URL to (imageUrl ?: ""),
                        KEY_RESULT_TASK_ID to taskId
                    )
                )
            } else {
                val error = finalStatus?.error ?: "Generation failed with unknown error"
                Log.e(TAG, "❌ Generation failed: $error")
                showNotification("Generation Failed", error, isSuccess = false)
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to error))
            }
            
        } catch (e: Exception) {
            val error = e.message ?: "Unexpected error during generation"
            Log.e(TAG, "❌ Exception during generation", e)
            showNotification("Generation Error", error, isSuccess = false)
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to error))
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // Low importance for ongoing work
            ).apply {
                description = "Notifications for image generation progress"
                setShowBadge(false) // Don't show badge
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Generating Image")
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true) // Cannot be dismissed while running
            .setProgress(0, 0, true) // Indeterminate progress
            .setContentIntent(pendingIntent)
            .setSilent(true) // No sound/vibration
            .build()
        
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
    
    private fun showNotification(title: String, message: String, isSuccess: Boolean = false) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (isSuccess) {
                // Navigate to gallery on success
                putExtra("navigate_to", "gallery")
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(if (isSuccess) R.drawable.ic_launcher_foreground else R.drawable.ic_launcher_foreground)
            .setAutoCancel(true) // Dismiss on tap
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for completion
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}
