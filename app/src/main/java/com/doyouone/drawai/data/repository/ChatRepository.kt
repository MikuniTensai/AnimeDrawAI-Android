package com.doyouone.drawai.data.repository

import android.util.Log
import com.doyouone.drawai.data.model.ChatMessage
import com.doyouone.drawai.data.model.OllamaModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ChatApiService {
    @POST("api/chat/send")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Body request: ChatSendRequest
    ): ChatSendResponse
    
    @GET("api/chat/history/{sessionId}")
    suspend fun getHistory(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String
    ): ChatHistoryResponse
    
    @POST("api/chat/session/new")
    suspend fun createSession(
        @Header("Authorization") token: String
    ): NewSessionResponse
    
    @DELETE("api/chat/session/{sessionId}")
    suspend fun deleteSession(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String
    ): DeleteSessionResponse
    
    @GET("api/chat/models")
    suspend fun getModels(): ModelsResponse
}

data class ChatSendRequest(
    val message: String,
    val session_id: String? = null,
    val temperature: Double? = null,
    val max_tokens: Int? = null
)

data class ChatSendResponse(
    val success: Boolean,
    val session_id: String?,
    val response: String?,
    val metadata: ResponseMetadata?,
    val error: String?
)

data class ResponseMetadata(
    val model: String?,
    val response_time: Double?,
    val tokens_used: Int?
)

data class ChatHistoryResponse(
    val success: Boolean,
    val session_id: String?,
    val messages: List<HistoryMessage>?,
    val count: Int?,
    val error: String?
)

data class HistoryMessage(
    val role: String,
    val content: String,
    val timestamp: String,
    val metadata: Map<String, Any>?
)

data class NewSessionResponse(
    val success: Boolean,
    val session_id: String?,
    val error: String?
)

data class DeleteSessionResponse(
    val success: Boolean,
    val message: String?,
    val error: String?
)

data class ModelsResponse(
    val success: Boolean,
    val models: List<ModelInfo>?,
    val error: String?
)

data class ModelInfo(
    val name: String,
    val size: String?,
    val modified: String?
)

class ChatRepository {
    companion object {
        private const val TAG = "ChatRepository"
        // Cloudflare tunnel domain (using same domain as main API with path routing)
        private const val BASE_URL = "https://drawai-api.drawai.site/"
        // Alternative: For local testing use "http://10.0.2.2:8002/" (emulator) or "http://192.168.x.x:8002/" (real device)
    }
    
    private val api: ChatApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApiService::class.java)
    }
    
    private val auth = FirebaseAuth.getInstance()
    
    private suspend fun getAuthToken(): String {
        val user = auth.currentUser
        if (user == null) {
            Log.w(TAG, "No authenticated user")
            return ""
        }
        
        return try {
            val token = user.getIdToken(false).await().token
            "Bearer $token"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get auth token", e)
            ""
        }
    }
    
    suspend fun sendMessage(
        message: String,
        sessionId: String? = null,
        temperature: Double? = null,
        maxTokens: Int? = null
    ): Result<ChatSendResponse> {
        return try {
            val token = getAuthToken()
            val request = ChatSendRequest(
                message = message,
                session_id = sessionId,
                temperature = temperature,
                max_tokens = maxTokens
            )
            
            Log.d(TAG, "Sending message to session: $sessionId")
            val response = api.sendMessage(token, request)
            
            if (response.success) {
                Log.d(TAG, "Message sent successfully")
                Result.success(response)
            } else {
                Log.e(TAG, "Message send failed: ${response.error}")
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }
    
    suspend fun getHistory(sessionId: String): Result<List<ChatMessage>> {
        return try {
            val token = getAuthToken()
            Log.d(TAG, "Getting history for session: $sessionId")
            val response = api.getHistory(token, sessionId)
            
            if (response.success && response.messages != null) {
                val messages = response.messages.map { historyMsg ->
                    ChatMessage(
                        id = "${sessionId}_${historyMsg.timestamp}",
                        sessionId = sessionId,
                        role = historyMsg.role,
                        content = historyMsg.content,
                        timestamp = parseTimestamp(historyMsg.timestamp)
                    )
                }
                Log.d(TAG, "Retrieved ${messages.size} messages")
                Result.success(messages)
            } else {
                Log.e(TAG, "Get history failed: ${response.error}")
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting history", e)
            Result.failure(e)
        }
    }
    
    suspend fun createNewSession(): Result<String> {
        return try {
            val token = getAuthToken()
            Log.d(TAG, "Creating new session")
            val response = api.createSession(token)
            
            if (response.success && response.session_id != null) {
                Log.d(TAG, "New session created: ${response.session_id}")
                Result.success(response.session_id)
            } else {
                Log.e(TAG, "Create session failed: ${response.error}")
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating session", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteSession(sessionId: String): Result<Boolean> {
        return try {
            val token = getAuthToken()
            Log.d(TAG, "Deleting session: $sessionId")
            val response = api.deleteSession(token, sessionId)
            
            if (response.success) {
                Log.d(TAG, "Session deleted successfully")
                Result.success(true)
            } else {
                Log.e(TAG, "Delete session failed: ${response.error}")
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting session", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAvailableModels(): Result<List<OllamaModel>> {
        return try {
            Log.d(TAG, "Getting available models")
            val response = api.getModels()
            
            if (response.success && response.models != null) {
                val models = response.models.map { modelInfo ->
                    OllamaModel(
                        name = modelInfo.name,
                        size = modelInfo.size ?: "",
                        modified = modelInfo.modified ?: ""
                    )
                }
                Log.d(TAG, "Retrieved ${models.size} models")
                Result.success(models)
            } else {
                Log.e(TAG, "Get models failed: ${response.error}")
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting models", e)
            Result.failure(e)
        }
    }
    
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            // Try to parse ISO format timestamp
            val instant = java.time.Instant.parse(timestamp)
            instant.toEpochMilli()
        } catch (e: Exception) {
            // Fallback to current time
            System.currentTimeMillis()
        }
    }
}
