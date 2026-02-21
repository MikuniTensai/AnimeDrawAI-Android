package com.doyouone.drawai.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doyouone.drawai.data.model.CommunityPost
import com.doyouone.drawai.data.model.SortType
import com.doyouone.drawai.data.repository.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for Community/Explore feature
 * Manages community posts state and user interactions
 */
class CommunityViewModel(context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "CommunityViewModel"
    }
    
    private val repository = CommunityRepository(context)
    
    // UI State
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()
    
    // Current sort type
    private val _sortType = MutableStateFlow(SortType.POPULAR)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()
    
    // Upload state
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()
    
    // Current post (for detail view)
    private val _currentPost = MutableStateFlow<CommunityPost?>(null)
    val currentPost: StateFlow<CommunityPost?> = _currentPost.asStateFlow()
    
    private val userPreferences = com.doyouone.drawai.data.preferences.UserPreferences(context)
    private val drawAIRepository = com.doyouone.drawai.data.repository.DrawAIRepository()
    private val authManager = com.doyouone.drawai.auth.AuthManager(context)
    
    // Track downloaded posts in this session to prevent spam
    private val downloadedPostIds = mutableSetOf<String>()
    
    init {
        loadPosts()
        syncSubscription()
    }
    
    /**
     * Sync subscription status to ensure user has correct access
     */
    fun syncSubscription() {
        viewModelScope.launch {
            try {
                val userId = authManager.getCurrentUserId()
                if (userId != null) {
                    Log.d(TAG, "🔄 Syncing subscription status for user: $userId")
                    val result = drawAIRepository.getGenerationLimit(userId)
                    
                    if (result.isSuccess) {
                        val limit = result.getOrNull()
                        if (limit != null) {
                            // Update premium status in UserPreferences
                            userPreferences.setPremiumStatus(limit.isPremium)
                            Log.d(TAG, "✅ Premium status synced: ${limit.isPremium} (${limit.subscriptionType})")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing subscription", e)
            }
        }
    }
    
    /**
     * Load community posts with current sort type
     */
    fun loadPosts(refresh: Boolean = false) {
        if (refresh) {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                error = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = _uiState.value.posts.isEmpty(),
                error = null
            )
        }
        
        viewModelScope.launch {
            repository.getPosts(sortBy = _sortType.value)
                .catch { e ->
                    Log.e(TAG, "Error loading posts", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to load posts"
                    )
                }
                .collect { posts ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        posts = posts,
                        error = null
                    )
                    Log.d(TAG, "✅ Loaded ${posts.size} posts")
                }
        }
    }
    
    /**
     * Change sort type and reload posts
     */
    fun changeSortType(newSortType: SortType) {
        if (_sortType.value != newSortType) {
            _sortType.value = newSortType
            loadPosts(refresh = true)
        }
    }
    
    /**
     * Refresh posts (pull-to-refresh)
     */
    fun refreshPosts() {
        loadPosts(refresh = true)
    }
    
    /**
     * Toggle like on a post
     */
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val result = repository.toggleLike(postId)
            
            result.onSuccess { isLiked ->
                Log.d(TAG, "✅ Post $postId ${if (isLiked) "liked" else "unliked"}")
                
                // Update local state optimistically
                _uiState.value = _uiState.value.copy(
                    posts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(likes = if (isLiked) post.likes + 1 else (post.likes - 1).coerceAtLeast(0))
                        } else {
                            post
                        }
                    }
                )
            }.onFailure { e ->
                Log.e(TAG, "❌ Failed to toggle like", e)
            }
        }
    }
    
    /**
     * Download a post's image
     */
    fun downloadPost(post: CommunityPost) {
        if (downloadedPostIds.contains(post.id)) {
            Log.d(TAG, "Post ${post.id} already downloaded in this session")
            return
        }
        
        downloadedPostIds.add(post.id)
        viewModelScope.launch {
            // Increment download counter
            repository.incrementDownload(post.id)
            
            // Actual download logic would go here
            // For now, just log
            Log.d(TAG, "Download initiated for post ${post.id}")
        }
    }

    /**
     * Check if post is already downloaded in this session
     */
    fun isPostDownloaded(postId: String): Boolean {
        return downloadedPostIds.contains(postId)
    }
    
    /**
     * Increment view count for a post
     */
    fun viewPost(postId: String) {
        viewModelScope.launch {
            repository.incrementView(postId)
        }
    }
    
    /**
     * Get a specific post by ID
     * First checks cache, then fetches from Firestore if not found
     */
    fun getPostById(postId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Getting post by ID: $postId")
                
                // First check local cache
                val cachedPost = _uiState.value.posts.find { it.id == postId }
                if (cachedPost != null) {
                    Log.d(TAG, "✅ Post found in cache")
                    _currentPost.value = cachedPost
                    return@launch
                }
                
                // If not in cache, fetch from Firestore
                Log.d(TAG, "Post not in cache, fetching from Firestore...")
                val result = repository.getPostById(postId)
                
                result.onSuccess { post ->
                    Log.d(TAG, "✅ Post fetched from Firestore")
                    _currentPost.value = post
                }.onFailure { e ->
                    Log.e(TAG, "❌ Failed to fetch post", e)
                    _currentPost.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting post by ID: $postId", e)
                _currentPost.value = null
            }
        }
    }
    
    /**
     * Report a post
     */
    fun reportPost(postId: String, reason: String) {
        viewModelScope.launch {
            val result = repository.reportPost(postId, reason)
            
            result.onSuccess {
                Log.d(TAG, "✅ Post reported: $postId")
            }.onFailure { e ->
                Log.e(TAG, "❌ Failed to report post", e)
            }
        }
    }
    
    /**
     * Delete own post
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            val result = repository.deletePost(postId)
            
            result.onSuccess {
                Log.d(TAG, "✅ Post deleted: $postId")
                
                // Remove from local state
                _uiState.value = _uiState.value.copy(
                    posts = _uiState.value.posts.filter { it.id != postId }
                )
            }.onFailure { e ->
                Log.e(TAG, "❌ Failed to delete post", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete post"
                )
            }
        }
    }
    
    /**
     * Upload image to community
     */
    fun uploadToCommunity(
        imageFile: File,
        prompt: String,
        negativePrompt: String = "",
        workflow: String,
        username: String,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading(0f)
            
            val post = CommunityPost(
                prompt = prompt,
                negativePrompt = negativePrompt,
                workflow = workflow,
                username = username,
                tags = tags
            )
            
            val result = repository.uploadToCommunity(imageFile, post)
            
            result.onSuccess { postId ->
                Log.d(TAG, "✅ Upload successful: $postId")
                _uploadState.value = UploadState.Success(postId)
                
                // Refresh posts to show new upload
                loadPosts(refresh = true)
            }.onFailure { e ->
                Log.e(TAG, "❌ Upload failed", e)
                _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
            }
        }
    }
    
    /**
     * Reset upload state
     */
    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }
    
    /**
     * Check if user has liked a post
     */
    suspend fun hasLiked(postId: String): Boolean {
        return repository.hasLiked(postId)
    }
}

/**
 * UI State for Community screen
 */
data class CommunityUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val posts: List<CommunityPost> = emptyList(),
    val error: String? = null
)

/**
 * Upload state
 */
sealed class UploadState {
    object Idle : UploadState()
    data class Uploading(val progress: Float) : UploadState()
    data class Success(val postId: String) : UploadState()
    data class Error(val message: String) : UploadState()
}
