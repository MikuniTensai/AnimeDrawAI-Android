package com.doyouone.drawai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doyouone.drawai.data.model.InventoryItem
import com.doyouone.drawai.data.repository.DrawAIRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class InventoryUiState {
    object Loading : InventoryUiState()
    data class Success(val items: List<InventoryItem>) : InventoryUiState()
    data class Error(val message: String) : InventoryUiState()
}

class InventoryViewModel(
    private val repository: DrawAIRepository = DrawAIRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Loading)
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        loadInventory()
    }

    fun loadInventory() {
        viewModelScope.launch {
            _uiState.value = InventoryUiState.Loading
            val result = repository.getInventory()
            
            if (result.isSuccess) {
                _uiState.value = InventoryUiState.Success(result.getOrDefault(emptyList()))
            } else {
                _uiState.value = InventoryUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load inventory")
            }
        }
    }

    fun useItem(itemId: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.useItem(itemId)
            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response?.success == true) {
                    onSuccess(response.message ?: "Item used successfully")
                    loadInventory() // Refresh inventory
                } else {
                    onError(response?.error ?: response?.message ?: "Failed to use item")
                }
            } else {
                onError(result.exceptionOrNull()?.message ?: "Network error")
            }
        }
    }
}
