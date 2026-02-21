package com.doyouone.drawai.data.repository

import android.util.Log
import com.doyouone.drawai.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface CharacterApiService {
    @POST("api/character/create")
    suspend fun createCharacter(
        @Header("Authorization") token: String,
        @Body request: CreateCharacterRequest
    ): CreateCharacterResponse
    
    @GET("api/character/profile/{characterId}")
    suspend fun getCharacterProfile(
        @Header("Authorization") token: String,
        @Path("characterId") characterId: String
    ): CharacterProfileResponse
    
    @POST("api/character/chat")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Body request: CharacterChatRequest
    ): CharacterChatResponse
    
    @GET("api/character/relationship/{characterId}")
    suspend fun getRelationship(
        @Header("Authorization") token: String,
        @Path("characterId") characterId: String
    ): RelationshipResponse
    
    @GET("api/character/me")
    suspend fun getUserCharacter(
        @Header("Authorization") token: String
    ): UserCharacterResponse
    
    @GET("api/character/history/{characterId}")
    suspend fun getHistory(
        @Header("Authorization") token: String,
        @Path("characterId") characterId: String,
        @Query("limit") limit: Int = 50
    ): CharacterChatHistoryResponse
    
    @GET("api/character/list")
    suspend fun getCharacters(
        @Header("Authorization") token: String
    ): CharacterListResponse

    @POST("api/character/request-photo")
    suspend fun requestPhoto(
        @Header("Authorization") token: String,
        @Body request: RequestPhotoRequest
    ): RequestPhotoResponse

    @POST("api/character/message/inject")
    suspend fun injectMessage(
        @Header("Authorization") token: String,
        @Body request: InjectMessageRequest
    ): CharacterChatResponse // Reuse ChatResponse as it's similar structure (success/error) or create generic
}

class CharacterRepository {
    companion object {
        private const val TAG = "CharacterRepository"
        private const val BASE_URL = "https://drawai-api.drawai.site/" // Production via Cloudflare
        // Local: "http://192.168.100.6:8003/"
    }
    
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(RelationshipStage::class.java, RelationshipStageDeserializer())
        .create()
    
    // Configure OkHttp with longer timeout for AI responses
    private val okHttpClient: okhttp3.OkHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS) // Increased from 90s to 180s for photo generation
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
    
    private val api: CharacterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Use custom OkHttp client
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CharacterApiService::class.java)
    }
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
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
    
    
    /**
     * Upload local image file to Firebase Storage
     */
    private suspend fun uploadImageToStorage(localFilePath: String, characterId: String): String? {
        return try {
            val file = java.io.File(localFilePath)
            if (!file.exists()) {
                Log.e(TAG, "Image file does not exist: $localFilePath")
                return null
            }
            
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("characters/$characterId.jpg")
            
            val uploadTask = imageRef.putFile(android.net.Uri.fromFile(file))
            val taskSnapshot = uploadTask.await()
            
            // Get download URL
            val downloadUrl = imageRef.downloadUrl.await()
            Log.d(TAG, "Image uploaded to Storage: $downloadUrl")
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload image to Storage", e)
            null
        }
    }
    
    /**
     * Create a character from a generated image
     */
    suspend fun createCharacter(
        imageId: String,
        imageUrl: String,
        prompt: String,
        language: String = "en",
        gender: String = "female",
        replace: Boolean = false,
        name: String? = null,
        seed: Long? = null,
        workflow: String? = null
    ): Result<CreateCharacterResponse> {
        return try {
            val token = getAuthToken()
            
            // Generate character ID first
            val characterId = java.util.UUID.randomUUID().toString()
            
            // Upload image to Firebase Storage first
            var storageUrl = imageUrl
            if (imageUrl.startsWith("/")) {
                // Local file path - upload to Storage
                val uploaded = uploadImageToStorage(imageUrl, characterId)
                if (uploaded != null) {
                    storageUrl = uploaded
                    Log.d(TAG, "Using uploaded Storage URL: $storageUrl")
                } else {
                    Log.w(TAG, "Failed to upload image, using local path")
                }
            }
            
            val request = CreateCharacterRequest(
                imageId = imageId, 
                imageUrl = storageUrl, 
                prompt = prompt, 
                language = language, 
                gender = gender, 
                replace = replace, 
                name = name, 
                seed = seed,
                appearancePrompt = prompt, // Use original prompt as appearance
                workflow = workflow
            )
            
            Log.d(TAG, "Creating character from image: $imageId")
            val response = api.createCharacter(token, request)
            
            if (response.success) {
                Log.d(TAG, "Character created successfully: ${response.character?.id}")
                Result.success(response)
            } else {
                Log.e(TAG, "Character creation failed: ${response.error}")
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating character", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get character profile
     */
    suspend fun getCharacterProfile(characterId: String): Result<Character> {
        return try {
            val token = getAuthToken()
            Log.d(TAG, "Getting character profile: $characterId")
            val response = api.getCharacterProfile(token, characterId)
            
            if (response.success && response.character != null) {
                Log.d(TAG, "Character profile retrieved")
                Result.success(response.character)
            } else {
                Log.e(TAG, "Get profile failed: ${response.error}")
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting character profile", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send message to character
     */
    suspend fun sendMessage(
        characterId: String,
        message: String,
        isUpgradeTrigger: Boolean = false
    ): Result<CharacterChatResponse> {
        return try {
            val token = getAuthToken()
            val request = CharacterChatRequest(characterId, message, isUpgradeTrigger)
            
            Log.d(TAG, "Sending message to character: $characterId")
            val response = api.sendMessage(token, request)
            
            if (response.success) {
                Log.d(TAG, "Message sent successfully")
                if (response.relationship?.stageChanged == true) {
                    Log.i(TAG, "🎉 Relationship upgraded to: ${response.relationship.newStage}")
                }
                Result.success(response)
            } else {
                Log.e(TAG, "Send message failed: ${response.error}")
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get relationship status
     */
    suspend fun getRelationship(characterId: String): Result<RelationshipStatus> {
        return try {
            val token = getAuthToken()
            Log.d(TAG, "Getting relationship status: $characterId")
            val response = api.getRelationship(token, characterId)
            
            if (response.success && response.relationship != null) {
                Log.d(TAG, "Relationship status retrieved")
                Result.success(response.relationship)
            } else {
                Log.e(TAG, "Get relationship failed: ${response.error}")
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting relationship", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get chat history
     */
    suspend fun getChatHistory(
        characterId: String,
        limit: Int = 50
    ): Result<List<CharacterMessage>> {
        return try {
            val token = getAuthToken()
            Log.d(TAG, "Getting chat history: $characterId")
            val response = api.getHistory(token, characterId, limit)
            
            if (response.success && response.messages != null) {
                Log.d(TAG, "Chat history retrieved: ${response.count} messages")
                // Reverse the list because API returns newest first (DESC), 
                // but UI renders top-to-bottom (so we need oldest first)
                Result.success(response.messages.reversed())
            } else {
                Log.e(TAG, "Get history failed: ${response.error}")
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat history", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's character info
     */
    suspend fun getUserCharacter(): Result<UserCharacterResponse> {
        return try {
            val token = getAuthToken()
            val response = api.getUserCharacter(token)
            
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user character", e)
            Result.failure(e)
        }
    }
    
    // Get all characters for current user
    suspend fun getCharacters(): Result<List<Character>> {
        return try {
            val token = getAuthToken()
            if (token.isEmpty()) {
                return Result.failure(Exception("Not authenticated"))
            }
            
            val response = api.getCharacters(token)
            if (response.success && response.characters != null) {
                Result.success(response.characters)
            } else {
                Result.failure(Exception(response.error ?: "Failed to get characters"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get characters", e)
            Result.failure(e)
        }
    }
    
    // Get characters as Flow for real-time updates
    fun getCharactersFlow(userId: String, includeDeleted: Boolean = false): Flow<List<Character>> = callbackFlow {
        val listener = db.collection("characters")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to characters", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                Log.d(TAG, "Flow snapshot received: ${snapshot?.documents?.size ?: 0} documents")
                
                val characters = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val character = doc.toObject(Character::class.java)
                        if (character != null) {
                            Log.d(TAG, "Character ${doc.id}: isDeleted=${character.isDeleted}, name=${character.personality.name}")
                            // Filter out deleted characters at the repository level
                            if (character.isDeleted && !includeDeleted) {
                                Log.d(TAG, "  -> Filtering out deleted character ${doc.id}")
                                null
                            } else {
                                Log.d(TAG, "  -> Including active character ${doc.id}")
                                character
                            }
                        } else {
                            Log.w(TAG, "Failed to deserialize character ${doc.id}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing character ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                Log.d(TAG, "Flow emitting ${characters.size} characters after filtering")
                trySend(characters)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Get single character as Flow for real-time updates
    fun getCharacterFlow(characterId: String): Flow<Character?> = callbackFlow {
        val listener = db.collection("characters")
            .document(characterId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to character", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val character = snapshot?.toObject(Character::class.java)
                trySend(character)
            }
        
        awaitClose { listener.remove() }
    }
    // Soft delete character and associated chats
    suspend fun deleteCharacter(characterId: String): Result<Unit> {
        return try {
            val token = getAuthToken()
            if (token.isEmpty()) {
                Log.e(TAG, "Delete failed: Not authenticated (no token)")
                return Result.failure(Exception("Not authenticated"))
            }
            
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e(TAG, "Delete failed: Not authenticated (no userId)")
                return Result.failure(Exception("Not authenticated"))
            }
            
            Log.d(TAG, "Attempting to delete character: $characterId for user: $userId")
            
            // First, verify the character exists and belongs to this user
            val characterDoc = db.collection("characters").document(characterId).get().await()
            if (!characterDoc.exists()) {
                Log.e(TAG, "Delete failed: Character document does not exist")
                return Result.failure(Exception("Character not found"))
            }
            
            val docUserId = characterDoc.getString("userId")
            Log.d(TAG, "Character userId in Firestore: $docUserId, Current userId: $userId")
            
            if (docUserId != userId) {
                Log.e(TAG, "Delete failed: userId mismatch. Doc userId: $docUserId, Current userId: $userId")
                return Result.failure(Exception("Permission denied: Character belongs to different user"))
            }
            
            // Update Firestore directly for soft delete
            // Use set with merge to handle cases where isDeleted field doesn't exist yet
            db.collection("characters").document(characterId)
                .set(mapOf("isDeleted" to true), com.google.firebase.firestore.SetOptions.merge())
                .await()
                
            Log.d(TAG, "Character soft deleted successfully: $characterId")
            
            // Also delete associated chat messages
            try {
                val chatSnapshot = db.collection("characterChats")
                    .whereEqualTo("characterId", characterId)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                
                // Delete all chat documents for this character
                val batch = db.batch()
                chatSnapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                
                Log.d(TAG, "Deleted ${chatSnapshot.size()} chat messages for character: $characterId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete chat messages (non-critical): ${e.message}")
                // Don't fail the whole operation if chat deletion fails
            }
                
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete character: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            if (e is com.google.firebase.firestore.FirebaseFirestoreException) {
                Log.e(TAG, "Firestore error code: ${e.code}")
            }
            Result.failure(e)
        }
    }

    // Helper to get flow for current user
    fun getUserCharactersFlow(includeDeleted: Boolean = false): Flow<List<Character>> {
        val user = auth.currentUser
        return if (user != null) {
            getCharactersFlow(user.uid, includeDeleted)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Request a photo from the character
     */
    suspend fun requestPhoto(
        characterId: String,
        userPrompt: String,
        negativePrompt: String = "",
        seed: Long? = null,
        appearancePrompt: String = "",
        language: String = "en",
        chatContext: String = ""
    ): Result<RequestPhotoResponse> {
        return try {
            val token = getAuthToken()
            val request = RequestPhotoRequest(
                characterId = characterId, 
                userPrompt = userPrompt, 
                negativePrompt = negativePrompt, 
                seed = seed, 
                appearancePrompt = appearancePrompt,
                language = language,
                context = chatContext
            )
            
            Log.d(TAG, "Requesting photo for character: $characterId with lang=$language contextLen=${chatContext.length}")
            val response = api.requestPhoto(token, request)
            
            if (response.success) {
                Log.d(TAG, "Photo request accepted: ${response.message}")
                Result.success(response)
            } else {
                Log.e(TAG, "Photo request failed: ${response.error}")
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting photo", e)
            Result.failure(e)
        }
    }

    /**
     * Inject a message into the chat (internal use or photo injection)
     */
    suspend fun injectMessage(
        characterId: String,
        role: String,
        content: String,
        imageUrl: String? = null
    ): Result<Unit> {
        return try {
            val token = getAuthToken()
            val request = InjectMessageRequest(characterId, role, content, imageUrl)
            
            Log.d(TAG, "Injecting message for character: $characterId")
            // CharacterChatResponse usually has 'success' field
            // Note: The server actually returns { success: true, messageId: ... } which might not match CharacterChatResponse perfectly
            // But usually we just check success. Ideally we should have a specific response model, 
            // but CharacterChatResponse has 'success' and 'error'. We might miss 'messageId' but that's fine.
            // Let's check CharacterChatResponse definition: success, response, messages, error...
            // It's compatible enough for boolean check.
            val response = api.injectMessage(token, request)
            
            if (response.success) {
                Log.d(TAG, "Message injected successfully")
                Result.success(Unit)
            } else {
                Log.e(TAG, "Message injection failed: ${response.error}")
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting message", e)
            Result.failure(e)
        }
    }
}

/**
 * Custom deserializer for RelationshipStage enum
 */
class RelationshipStageDeserializer : com.google.gson.JsonDeserializer<RelationshipStage> {
    override fun deserialize(
        json: com.google.gson.JsonElement?,
        typeOfT: java.lang.reflect.Type?,
        context: com.google.gson.JsonDeserializationContext?
    ): RelationshipStage {
        return if (json != null && json.isJsonPrimitive) {
            RelationshipStage.fromString(json.asString)
        } else {
            RelationshipStage.STRANGER
        }
    }
}
