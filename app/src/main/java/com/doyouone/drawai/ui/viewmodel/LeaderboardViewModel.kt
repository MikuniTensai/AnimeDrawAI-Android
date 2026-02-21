package com.doyouone.drawai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doyouone.drawai.data.repository.LeaderboardEntry
import com.doyouone.drawai.data.repository.LeaderboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class LeaderboardViewModel(
    private val repository: LeaderboardRepository
) : ViewModel() {

    private val _topCreators = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val topCreators = _topCreators.asStateFlow()

    private val _topCreatorsWeekly = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val topCreatorsWeekly = _topCreatorsWeekly.asStateFlow()

    private val _topCreatorsMonthly = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val topCreatorsMonthly = _topCreatorsMonthly.asStateFlow()

    private val _topRomancers = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val topRomancers = _topRomancers.asStateFlow()

    private val _communityMVPs = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val communityMVPs = _communityMVPs.asStateFlow()

    private val _risingStars = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val risingStars = _risingStars.asStateFlow()
    
    // Category specific
    private val _categoryLikes = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val categoryLikes = _categoryLikes.asStateFlow()
    
    private val _categoryDownloads = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val categoryDownloads = _categoryDownloads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // Load base lists
            repository.getTopCreators()
                .onSuccess { _topCreators.value = it }
                .onFailure { it.printStackTrace(); _error.value = "Creators: ${it.message}" }

            repository.getTopCreatorsWeekly()
                .onSuccess { _topCreatorsWeekly.value = it }
                .onFailure { it.printStackTrace() }

            repository.getTopCreatorsMonthly()
                .onSuccess { _topCreatorsMonthly.value = it }
                .onFailure { it.printStackTrace() }
            
            repository.getTopRomancers()
                .onSuccess { _topRomancers.value = it }
                .onFailure { it.printStackTrace(); _error.value = "Romancers: ${it.message}" }
                
            repository.getCommunityMVPs()
                .onSuccess { _communityMVPs.value = it }
                .onFailure { it.printStackTrace() }
                
            repository.getRisingStars()
                .onSuccess { _risingStars.value = it }
                .onFailure { it.printStackTrace() }
            
            // Defaut category load (Anime)
            loadCategoryData("anime")
            
            _isLoading.value = false
        }
    }
    
    fun loadCategoryData(category: String) {
        viewModelScope.launch {
            repository.getCategoryLikes(category).onSuccess { _categoryLikes.value = it }
            repository.getCategoryDownloads(category).onSuccess { _categoryDownloads.value = it }
        }
    }
}
