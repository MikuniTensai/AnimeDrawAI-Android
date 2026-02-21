package com.doyouone.drawai.data.repository

import android.util.Log
import com.doyouone.drawai.data.model.TaskStatusResponse
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for logging generation events to Firestore for analytics and leaderboards.
 */
class GenerationLogRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val generatedImagesRef = firestore.collection("generated_images")

    companion object {
        private const val TAG = "GenerationLogRepo"
    }

    /**
     * Logs a successful generation to the 'generated_images' collection.
     * This is crucial for Weekly and Monthly leaderboards.
     */
    suspend fun logGeneration(
        userId: String,
        taskStatus: TaskStatusResponse,
        workflow: String
    ): Result<Unit> {
        return try {
            val generationLog = hashMapOf(
                "userId" to userId,
                "taskId" to taskStatus.taskId,
                "workflow" to workflow,
                "prompt" to (taskStatus.positivePrompt ?: ""),
                "imageUrl" to (taskStatus.resultFiles?.firstOrNull() ?: ""),
                "createdAt" to FieldValue.serverTimestamp(),
                // Add denormalized checks for easy querying if needed
                "status" to "completed" 
            )

            generatedImagesRef.add(generationLog).await()
            Log.d(TAG, "Generation logged successfully for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging generation: ${e.message}")
            Result.failure(e)
        }
    }
}
