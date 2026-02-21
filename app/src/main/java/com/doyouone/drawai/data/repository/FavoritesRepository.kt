package com.doyouone.drawai.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FavoritesRepository(private val userId: String) {
    private val firestore = FirebaseFirestore.getInstance()
    private val favoritesCollection = firestore.collection("users")
        .document(userId)
        .collection("favorites")
    
    // Get all favorites as Flow
    fun getFavorites(): Flow<List<String>> = callbackFlow {
        val listener = favoritesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Log error but don't crash - emit empty list instead
                android.util.Log.e("FavoritesRepository", "Error getting favorites: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val favoriteIds = snapshot?.documents?.map { it.id } ?: emptyList()
            trySend(favoriteIds)
        }
        
        awaitClose { listener.remove() }
    }
    
    // Add to favorites
    suspend fun addFavorite(workflowId: String): Result<Unit> {
        return try {
            favoritesCollection.document(workflowId).set(
                hashMapOf(
                    "addedAt" to com.google.firebase.Timestamp.now(),
                    "workflowId" to workflowId
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Remove from favorites
    suspend fun removeFavorite(workflowId: String): Result<Unit> {
        return try {
            favoritesCollection.document(workflowId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Check if workflow is favorite
    suspend fun isFavorite(workflowId: String): Boolean {
        return try {
            val doc = favoritesCollection.document(workflowId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }
    
    // Toggle favorite
    suspend fun toggleFavorite(workflowId: String): Result<Boolean> {
        return try {
            val isFav = isFavorite(workflowId)
            if (isFav) {
                removeFavorite(workflowId)
                Result.success(false)
            } else {
                addFavorite(workflowId)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
