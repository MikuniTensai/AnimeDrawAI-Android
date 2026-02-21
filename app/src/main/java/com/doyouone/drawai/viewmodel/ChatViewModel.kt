package com.doyouone.drawai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doyouone.drawai.data.model.ChatMessage
import com.doyouone.drawai.data.model.MessageMetadata
import com.doyouone.drawai.data.model.OllamaModel
import com.doyouone.drawai.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    companion object {
        private const val TAG = "ChatViewModel"
    }

    private fun getSafeErrorMessage(e: Throwable): String {
        val message = e.message ?: return "An unknown error occurred"
        return when {
            message.contains("Unable to resolve host", ignoreCase = true) -> "Unable to connect to server. Please check your internet connection."
            message.contains("Failed to connect", ignoreCase = true) -> "Connection failed. Please check your internet connection."
            message.contains("timeout", ignoreCase = true) -> "Connection timed out. Please try again."
            message.contains("SSL", ignoreCase = true) -> "Secure connection failed. Please check your date/time settings."
            else -> message
        }
    }
    
    private val repository = ChatRepository()
    
    // State flows
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _availableModels = MutableStateFlow<List<OllamaModel>>(emptyList())
    val availableModels: StateFlow<List<OllamaModel>> = _availableModels.asStateFlow()
    
    private val _selectedModel = MutableStateFlow("gemma3:4b")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()
    
    init {
        createNewSession()
        loadAvailableModels()
    }
    
    fun sendMessage(messageText: String) {
        if (messageText.isBlank()) {
            Log.w(TAG, "Attempted to send empty message")
            return
        }
        
        viewModelScope.launch {
            try {
                _isSending.value = true
                _error.value = null
                
                // Add user message to UI immediately
                val userMessage = ChatMessage(
                    id = "temp_${System.currentTimeMillis()}",
                    sessionId = _currentSessionId.value ?: "",
                    role = "user",
                    content = messageText,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = _messages.value + userMessage
                
                Log.d(TAG, "Sending message: $messageText")
                
                // Send to backend
                val result = repository.sendMessage(
                    message = messageText,
                    sessionId = _currentSessionId.value
                )
                
                result.onSuccess { response ->
                    // Update session ID if it was created
                    if (response.session_id != null) {
                        _currentSessionId.value = response.session_id
                    }
                    
                    // Add assistant response
                    if (response.response != null) {
                        val assistantMessage = ChatMessage(
                            id = "assistant_${System.currentTimeMillis()}",
                            sessionId = _currentSessionId.value ?: "",
                            role = "assistant",
                            content = response.response,
                            timestamp = System.currentTimeMillis(),
                            model = response.metadata?.model ?: _selectedModel.value,
                            metadata = response.metadata?.let {
                                MessageMetadata(
                                    responseTime = it.response_time ?: 0.0,
                                    tokensUsed = it.tokens_used ?: 0
                                )
                            }
                        )
                        _messages.value = _messages.value + assistantMessage
                        Log.d(TAG, "Received response: ${response.response.take(50)}...")
                    }
                }
                
                result.onFailure { exception ->
                    Log.e(TAG, "Failed to send message", exception)
                    _error.value = getSafeErrorMessage(exception)
                    
                    // Remove the temporary user message on error
                    _messages.value = _messages.value.filterNot { it.id == userMessage.id }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                _error.value = getSafeErrorMessage(e)
            } finally {
                _isSending.value = false
            }
        }
    }
    
    fun loadHistory(sessionId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d(TAG, "Loading history for session: $sessionId")
                
                val result = repository.getHistory(sessionId)
                
                result.onSuccess { historyMessages ->
                    _messages.value = historyMessages
                    _currentSessionId.value = sessionId
                    Log.d(TAG, "Loaded ${historyMessages.size} messages")
                }
                
                result.onFailure { exception ->
                    Log.e(TAG, "Failed to load history", exception)
                    _error.value = getSafeErrorMessage(exception)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading history", e)
                _error.value = getSafeErrorMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createNewSession() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d(TAG, "Creating new session")
                
                val result = repository.createNewSession()
                
                result.onSuccess { sessionId ->
                    _currentSessionId.value = sessionId
                    _messages.value = emptyList()
                    Log.d(TAG, "New session created: $sessionId")
                }
                
                result.onFailure { exception ->
                    Log.e(TAG, "Failed to create session", exception)
                    _error.value = getSafeErrorMessage(exception)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating session", e)
                _error.value = getSafeErrorMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            try {
                _currentSessionId.value?.let { sessionId ->
                    Log.d(TAG, "Clearing session: $sessionId")
                    
                    val result = repository.deleteSession(sessionId)
                    
                    result.onSuccess {
                        // Create a new session after clearing
                        createNewSession()
                    }
                    
                    result.onFailure { exception ->
                        Log.e(TAG, "Failed to clear session", exception)
                        _error.value = getSafeErrorMessage(exception)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing history", e)
                _error.value = getSafeErrorMessage(e)
            }
        }
    }
    
    fun selectModel(model: String) {
        _selectedModel.value = model
        Log.d(TAG, "Selected model: $model")
    }
    
    fun loadAvailableModels() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading available models")
                
                val result = repository.getAvailableModels()
                
                result.onSuccess { models ->
                    _availableModels.value = models
                    Log.d(TAG, "Loaded ${models.size} models")
                }
                
                result.onFailure { exception ->
                    Log.w(TAG, "Failed to load models", exception)
                    // Not critical - use default model
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Error loading models", e)
                // Not critical - use default model
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
