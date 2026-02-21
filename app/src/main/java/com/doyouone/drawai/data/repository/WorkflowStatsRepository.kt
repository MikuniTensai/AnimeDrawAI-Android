package com.doyouone.drawai.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import android.util.Log

import com.google.firebase.firestore.Source

/**
 * Repository for managing workflow statistics (views, generations)
 */
class WorkflowStatsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val statsCollection = db.collection("workflow_stats")
    
    companion object {
        private const val TAG = "WorkflowStatsRepo"
        private const val FIELD_VIEW_COUNT = "viewCount"
        private const val FIELD_GENERATION_COUNT = "generationCount"
    }

    /**
     * Get all workflow stats as a Map<WorkflowId, DataMap>
     */
    suspend fun getAllStats(): Result<Map<String, Map<String, Long>>> {
        return try {
            val snapshot = statsCollection.get(Source.SERVER).await()
            val statsMap = mutableMapOf<String, Map<String, Long>>()
            
            for (document in snapshot.documents) {
                val views = document.getLong(FIELD_VIEW_COUNT) ?: 0L
                val generations = document.getLong(FIELD_GENERATION_COUNT) ?: 0L
                
                statsMap[document.id] = mapOf(
                    FIELD_VIEW_COUNT to views,
                    FIELD_GENERATION_COUNT to generations
                )
            }
            Result.success(statsMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching workflow stats", e)
            Result.failure(e)
        }
    }

    /**
     * Increment view count for a workflow
     */
    suspend fun incrementView(workflowId: String) {
        try {
            val docRef = statsCollection.document(workflowId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                
                if (snapshot.exists()) {
                    val newCount = (snapshot.getLong(FIELD_VIEW_COUNT) ?: 0) + 1
                    transaction.update(docRef, FIELD_VIEW_COUNT, newCount)
                } else {
                    transaction.set(docRef, mapOf(
                        FIELD_VIEW_COUNT to 1L,
                        FIELD_GENERATION_COUNT to 0L
                    ))
                }
            }.await()
            Log.d(TAG, "Incremented view count for $workflowId")
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing view count for $workflowId", e)
        }
    }

    /**
     * Increment generation count for a workflow
     */
    suspend fun incrementGeneration(workflowId: String) {
        try {
            val docRef = statsCollection.document(workflowId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                
                if (snapshot.exists()) {
                    val newCount = (snapshot.getLong(FIELD_GENERATION_COUNT) ?: 0) + 1
                    transaction.update(docRef, FIELD_GENERATION_COUNT, newCount)
                } else {
                    transaction.set(docRef, mapOf(
                        FIELD_VIEW_COUNT to 0L,
                        FIELD_GENERATION_COUNT to 1L
                    ))
                }
            }.await()
            Log.d(TAG, "Incremented generation count for $workflowId")
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing generation count for $workflowId", e)
        }
    }
}
