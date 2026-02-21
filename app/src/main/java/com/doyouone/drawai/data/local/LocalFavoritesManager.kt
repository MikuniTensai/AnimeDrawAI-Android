package com.doyouone.drawai.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton manager for local favorites (fallback when Firestore fails)
 * This ensures favorites work immediately even with Firestore permission issues
 * NOW PERSISTENT using SharedPreferences
 */
object LocalFavoritesManager {
    private const val PREF_NAME = "anime_draw_favorites"
    private const val KEY_FAVORITES = "favorite_ids"
    
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()
    
    private var preferences: SharedPreferences? = null
    
    fun init(context: Context) {
        if (preferences == null) {
            preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            loadFavorites()
        }
    }
    
    private fun loadFavorites() {
        val savedIds = preferences?.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        _favoriteIds.value = savedIds
        android.util.Log.d("LocalFavorites", "Loaded ${savedIds.size} favorites from disk")
    }
    
    private fun saveFavorites() {
        preferences?.edit()?.putStringSet(KEY_FAVORITES, _favoriteIds.value)?.apply()
    }
    
    fun addFavorite(workflowId: String) {
        val current = _favoriteIds.value.toMutableSet()
        current.add(workflowId)
        _favoriteIds.value = current
        saveFavorites()
        android.util.Log.d("LocalFavorites", "Added: $workflowId, Total: ${_favoriteIds.value.size}")
    }
    
    fun removeFavorite(workflowId: String) {
        val current = _favoriteIds.value.toMutableSet()
        current.remove(workflowId)
        _favoriteIds.value = current
        saveFavorites()
        android.util.Log.d("LocalFavorites", "Removed: $workflowId, Total: ${_favoriteIds.value.size}")
    }
    
    fun toggleFavorite(workflowId: String) {
        if (workflowId in _favoriteIds.value) {
            removeFavorite(workflowId)
        } else {
            addFavorite(workflowId)
        }
    }
    
    fun isFavorite(workflowId: String): Boolean {
        return workflowId in _favoriteIds.value
    }
    
    fun clear() {
        _favoriteIds.value = emptySet()
        saveFavorites()
    }
}
