package com.doyouone.drawai.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.doyouone.drawai.data.model.CommunityPost
import com.doyouone.drawai.data.model.SortType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

/**
 * Repository for community features
 * Handles Firebase Storage uploads and Firestore operations
 */
class CommunityRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "CommunityRepository"
        private const val COLLECTION_POSTS = "community_posts"
        private const val COLLECTION_USER_LIKES = "user_likes"
        private const val STORAGE_PATH_COMMUNITY = "community"
        private const val PAGE_SIZE = 20
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Upload image to Firebase Storage and create Firestore post
     */
    suspend fun uploadToCommunity(
        imageFile: File,
        post: CommunityPost
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            
            // Generate unique ID for image
            val imageId = UUID.randomUUID().toString()
            val storagePath = "$STORAGE_PATH_COMMUNITY/$userId/$imageId.jpg"
            
            Log.d(TAG, "Uploading to Storage: $storagePath")
            
            // Upload to Firebase Storage
            val storageRef = storage.reference.child(storagePath)
            val putTask: com.google.firebase.storage.UploadTask = storageRef.putFile(Uri.fromFile(imageFile))
            val task: com.google.android.gms.tasks.Task<com.google.firebase.storage.UploadTask.TaskSnapshot> = putTask
            task.await()
            
            // Get download URL
            val downloadUri: Uri = storageRef.downloadUrl.await()
            val downloadUrl = downloadUri.toString()
            
            Log.d(TAG, "✅ Upload successful: $downloadUrl")
            
            // Create Firestore document
            val postWithUrl = post.copy(
                userId = userId,
                imageUrl = downloadUrl,
                thumbnailUrl = downloadUrl // Use same for now, can generate thumbnail later
            )
            
            val docRef = firestore.collection(COLLECTION_POSTS)
                .add(postWithUrl.toMap())
                .await()
            
            Log.d(TAG, "✅ Firestore doc created: ${docRef.id}")
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get community posts as Flow with real-time updates
     */
    fun getPosts(
        sortBy: SortType = SortType.POPULAR,
        limit: Int = PAGE_SIZE
    ): Flow<List<CommunityPost>> = callbackFlow {
        val userId = auth.currentUser?.uid
        
        val query = when (sortBy) {
            SortType.POPULAR -> firestore.collection(COLLECTION_POSTS)
                .orderBy("likes", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            
            SortType.RECENT -> firestore.collection(COLLECTION_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            
            SortType.TRENDING -> firestore.collection(COLLECTION_POSTS)
                // Simplified: use likes only to avoid composite index
                .orderBy("likes", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            
            SortType.MY_POSTS -> {
                if (userId != null) {
                    firestore.collection(COLLECTION_POSTS)
                        .whereEqualTo("userId", userId)
                        .limit(limit.toLong())
                } else {
                    // Return empty query if not authenticated
                    firestore.collection(COLLECTION_POSTS)
                        .whereEqualTo("userId", "")
                        .limit(0)
                }
            }
        }
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to posts", error)
                close(error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val posts = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CommunityPost::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing post ${doc.id}", e)
                        null
                    }
                }
                trySend(posts)
            }
        }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get posts by specific user
     */
    suspend fun getUserPosts(userId: String): Result<List<CommunityPost>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_POSTS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CommunityPost::class.java)?.copy(id = doc.id)
            }
            
            Result.success(posts)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user posts", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get a specific post by ID
     */
    suspend fun getPostById(postId: String): Result<CommunityPost> {
        return try {
            val doc = firestore.collection(COLLECTION_POSTS)
                .document(postId)
                .get()
                .await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Post not found"))
            }
            
            val post = doc.toObject(CommunityPost::class.java)?.copy(id = doc.id)
                ?: return Result.failure(Exception("Failed to parse post"))
            
            Result.success(post)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting post by ID", e)
            Result.failure(e)
        }
    }
    
    /**
     * Like/unlike a post
     */
    suspend fun toggleLike(postId: String): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            
            // Check if already liked
            val likeDoc = firestore.collection(COLLECTION_USER_LIKES)
                .document(userId)
                .collection("posts")
                .document(postId)
                .get()
                .await()
            
            val isLiked = likeDoc.exists()
            
            // Update Firestore in a transaction
            firestore.runTransaction { transaction ->
                val postRef = firestore.collection(COLLECTION_POSTS).document(postId)
                val postSnapshot = transaction.get(postRef)
                
                val currentLikes = postSnapshot.getLong("likes")?.toInt() ?: 0
                val newLikes = if (isLiked) currentLikes - 1 else currentLikes + 1
                
                transaction.update(postRef, "likes", newLikes.coerceAtLeast(0))
                
                // Update user likes collection
                val userLikeRef = firestore.collection(COLLECTION_USER_LIKES)
                    .document(userId)
                    .collection("posts")
                    .document(postId)
                
                if (isLiked) {
                    transaction.delete(userLikeRef)
                } else {
                    transaction.set(userLikeRef, mapOf("likedAt" to System.currentTimeMillis()))
                }
            }.await()
            
            Result.success(!isLiked) // Return new like state
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling like", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if user has liked a post
     */
    suspend fun hasLiked(postId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            
            firestore.collection(COLLECTION_USER_LIKES)
                .document(userId)
                .collection("posts")
                .document(postId)
                .get()
                .await()
                .exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking like status", e)
            false
        }
    }
    
    /**
     * Increment download count
     */
    suspend fun incrementDownload(postId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_POSTS)
                .document(postId)
                .update("downloads", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing downloads", e)
            Result.failure(e)
        }
    }
    
    /**
     * Increment view count
     */
    suspend fun incrementView(postId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_POSTS)
                .document(postId)
                .update("views", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing views", e)
            Result.failure(e)
        }
    }
    
    /**
     * Report a post
     */
    suspend fun reportPost(postId: String, reason: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            
            // Increment report count
            firestore.collection(COLLECTION_POSTS)
                .document(postId)
                .update(
                    mapOf(
                        "reportCount" to com.google.firebase.firestore.FieldValue.increment(1),
                        "isReported" to true
                    )
                )
                .await()
            
            // Log report details (can be moved to separate collection)
            Log.d(TAG, "Post $postId reported by $userId. Reason: $reason")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error reporting post", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete own post
     */
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            
            // Get post to verify ownership
            val postDoc = firestore.collection(COLLECTION_POSTS)
                .document(postId)
                .get()
                .await()
            
            val post = postDoc.toObject(CommunityPost::class.java)
            
            if (post?.userId != userId) {
                return Result.failure(Exception("Not authorized to delete this post"))
            }
            
            // Delete from Firestore
            firestore.collection(COLLECTION_POSTS)
                .document(postId)
                .delete()
                .await()
            
            // Note: Storage deletion can be done via Cloud Function or here
            // For now, we'll leave the image in Storage (cleanup later via Cloud Function)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting post", e)
            Result.failure(e)
        }
    }

    
    /**
     * Check if a post with same prompt AND workflow already exists
     */
    suspend fun isDuplicatePost(prompt: String, workflow: String): Boolean {
        return try {
            val snapshot = firestore.collection(COLLECTION_POSTS)
                .whereEqualTo("prompt", prompt)
                .whereEqualTo("workflow", workflow)
                .limit(1)
                .get()
                .await()
            
            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for duplicate post", e)
            false // Assume not duplicate on error
        }
    }
}
