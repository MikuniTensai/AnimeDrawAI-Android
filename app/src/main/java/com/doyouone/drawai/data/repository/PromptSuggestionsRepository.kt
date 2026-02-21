package com.doyouone.drawai.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

data class PromptSuggestion(
    val id: String = "",
    val type: String = "positive",
    val text: String = "",
    val vision: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)

class PromptSuggestionsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("prompt_suggestions")

    suspend fun getAll(): Result<Pair<List<PromptSuggestion>, List<PromptSuggestion>>> {
        return try {
            val snapshot = collection
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val suggestions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(PromptSuggestion::class.java)?.copy(id = doc.id)
            }
            val positives = suggestions.filter { it.type.equals("positive", ignoreCase = true) }
            val negatives = suggestions.filter { it.type.equals("negative", ignoreCase = true) }
            Result.success(positives to negatives)
        } catch (e: Exception) {
            Log.e("PromptSuggestionsRepo", "Failed to get suggestions", e)
            Result.failure(e)
        }
    }

    suspend fun addSuggestion(
        text: String,
        type: String,
        vision: Boolean,
        userId: String
    ): Result<String> {
        return try {
            val data = hashMapOf(
                "type" to type,
                "text" to text,
                "vision" to vision,
                "createdAt" to System.currentTimeMillis(),
                "createdBy" to userId
            )
            val docRef = collection.document()
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("PromptSuggestionsRepo", "Failed to add suggestion", e)
            Result.failure(e)
        }
    }

    fun getAllAsync(onComplete: (Result<Pair<List<PromptSuggestion>, List<PromptSuggestion>>>) -> Unit) {
        collection.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val suggestions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PromptSuggestion::class.java)?.copy(id = doc.id)
                }
                val positives = suggestions.filter { it.type.equals("positive", ignoreCase = true) }
                val negatives = suggestions.filter { it.type.equals("negative", ignoreCase = true) }
                onComplete(Result.success(positives to negatives))
            }
            .addOnFailureListener { e ->
                Log.e("PromptSuggestionsRepo", "Failed to get suggestions", e)
                onComplete(Result.failure(e))
            }
    }

    fun addSuggestionAsync(
        text: String,
        type: String,
        vision: Boolean,
        userId: String,
        onComplete: (Result<String>) -> Unit
    ) {
        val data = hashMapOf(
            "type" to type,
            "text" to text,
            "vision" to vision,
            "createdAt" to System.currentTimeMillis(),
            "createdBy" to userId
        )
        val docRef = collection.document()
        docRef.set(data)
            .addOnSuccessListener { onComplete(Result.success(docRef.id)) }
            .addOnFailureListener { e ->
                Log.e("PromptSuggestionsRepo", "Failed to add suggestion", e)
                onComplete(Result.failure(e))
            }
    }
}