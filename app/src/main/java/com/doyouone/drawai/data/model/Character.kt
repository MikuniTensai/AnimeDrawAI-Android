package com.doyouone.drawai.data.model

import com.google.gson.annotations.SerializedName

import com.google.firebase.firestore.PropertyName

data class Character(
    val id: String = "",
    val userId: String = "",
    val imageId: String = "",
    val imageUrl: String = "",
    val imageStorageUrl: String? = null,  // Firebase Storage URL for cross-device sync
    val prompt: String = "",
    val seed: Long? = null,  // Generation seed for facial consistency
    val workflow: String? = null, // Workflow used to create this character
    val personality: CharacterPersonality = CharacterPersonality(),
    val relationship: RelationshipStatus = RelationshipStatus(),
    val emotionalState: EmotionalState = EmotionalState(), // NEW
    val interactionPatterns: InteractionPatterns = InteractionPatterns(), // NEW
    val createdAt: String = "",
    val language: String = "en",
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false, // Soft delete flag
    
    val notificationEnabled: Boolean = false,
    val notificationUnlocked: Boolean = false,
    val profileUpdatedAt: String? = null // Timestamp when profile image was last updated
)

// ... existing code ...

// NEW: Toggle Notification Models
data class ToggleNotificationRequest(
    val characterId: String,
    val enable: Boolean
)

data class ToggleNotificationResponse(
    val success: Boolean,
    val status: String? = null,
    val gemsDeducted: Int = 0,
    val newGems: Int? = null,
    val message: String? = null,
    val error: String? = null
)


data class CharacterPersonality(
    val archetype: String = "",
    val traits: List<String> = emptyList(),
    val background: String = "",
    val communicationStyle: String = "",
    val interests: List<String> = emptyList(),
    val appearance: String = "",
    val rarity: String = "Common",
    val sinCount: Int = 0,
    val name: String = "",
    val gender: String = "female"
)

data class RelationshipStatus(
    val stage: RelationshipStage = RelationshipStage.STRANGER,
    val stageProgress: Int = 0, // Deprecated, kept for compatibility
    val affectionPoints: Double = 0.0, // NEW: Main progress metric
    val nextStageThreshold: Int = 500, // NEW: Target for next level
    val totalMessages: Int = 0,
    val lastInteraction: Any? = null, // Changed from String to Any? to handle Timestamp/String
    val lastChatDate: Any? = null,    // Changed from String to Any? to handle Timestamp/String
    val upgradeAvailable: Boolean = false
) {
    // Helper to safely get String date regardless of type
    fun getLastInteractionString(): String {
        return when (lastInteraction) {
            is String -> lastInteraction
            is com.google.firebase.Timestamp -> {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US)
                sdf.format(lastInteraction.toDate())
            }
            else -> ""
        }
    }

    fun getLastChatDateString(): String {
        return when (lastChatDate) {
            is String -> lastChatDate
            is com.google.firebase.Timestamp -> {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US)
                sdf.format(lastChatDate.toDate())
            }
            else -> ""
        }
    }
}

enum class RelationshipStage(val displayName: String, val emoji: String, val color: Long) {
    STRANGER("Stranger", "👤", 0xFF9E9E9E),
    FRIEND("Friend", "👋", 0xFF4CAF50),
    BEST_FRIEND("Best Friend", "⭐", 0xFF2196F3),
    ROMANTIC("Romantic", "💖", 0xFFE91E63),
    MARRIED("Married", "💍", 0xFF9C27B0);

    companion object {
        fun fromString(value: String): RelationshipStage {
            return when (value.lowercase()) {
                "stranger" -> STRANGER
                "friend" -> FRIEND
                "bestfriend", "best_friend" -> BEST_FRIEND
                "romantic" -> ROMANTIC
                "married" -> MARRIED
                else -> STRANGER
            }
        }
    }
}

data class CharacterMessage(
    val id: String = "",
    val characterId: String = "",
    val userId: String = "",
    val role: String = "", // "user" or "assistant"
    val content: String = "",
    val timestamp: Any? = null, // Changed to Any?
    val relationshipStage: RelationshipStage = RelationshipStage.STRANGER,
    val imageUrl: String? = null // NEW: For photo requests
) {
    fun getTimestampString(): String {
        return when (timestamp) {
            is String -> timestamp
            is com.google.firebase.Timestamp -> {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US)
                sdf.format(timestamp.toDate())
            }
            else -> ""
        }
    }
}

/**
 * API Request/Response models
 */

data class CreateCharacterRequest(
    val imageId: String,
    val imageUrl: String,
    val prompt: String,
    val language: String = "en",
    val gender: String = "female",
    val replace: Boolean = false,
    val name: String? = null,
    val seed: Long? = null,
    val appearancePrompt: String = "", // Original prompt defining the character's look
    val workflow: String? = null
)

data class CreateCharacterResponse(
    val success: Boolean,
    val character: Character?,
    val message: String?,
    val error: String?,
    val existingCharacterId: String?
)

data class CharacterProfileResponse(
    val success: Boolean,
    val character: Character?,
    val error: String?
)

data class CharacterChatRequest(
    val characterId: String,
    val message: String,
    val isUpgradeTrigger: Boolean = false
)

data class RequestPhotoRequest(
    val characterId: String,
    @SerializedName("prompt_override") val userPrompt: String = "",
    val negativePrompt: String = "",
    val seed: Long? = null,
    val appearancePrompt: String = "",
    val language: String? = null,
    val context: String? = null
)

data class RequestPhotoResponse(
    val success: Boolean,
    val action: String?, // "generate_image"
    val generationParams: Map<String, Any>?,
    val message: String?,
    val error: String?
)

data class InjectMessageRequest(
    val characterId: String,
    val role: String, // "assistant" or "user"
    val content: String,
    val imageUrl: String? = null
)

data class CharacterChatResponse(
    val success: Boolean,
    val response: String?,  // Backward compatibility - combined response
    val messages: List<MessageBurst>? = null,  // NEW: Multi-message format
    val characterState: CharacterState? = null,  // NEW: Emotional state
    val relationship: RelationshipProgressInfo?,
    val metadata: ChatMetadata?,
    val error: String?
)

// NEW: For multi-message bursts
data class MessageBurst(
    val id: String,
    val content: String,
    val typingDelay: Int,  // milliseconds
    val hasTypo: Boolean = false,
    val isCorrection: Boolean = false
)

// NEW: Character emotional state
data class CharacterState(
    val mood: String,
    val moodChanged: Boolean,
    val energy: Int
)

data class RelationshipProgressInfo(
    val stage: String,
    val stageProgress: Int,
    val affectionPoints: Double = 0.0, // NEW
    val affectionChange: Double = 0.0, // Points gained this message
    val nextStageThreshold: Int = 500, // NEW
    val totalMessages: Int,
    val stageChanged: Boolean,
    val newStage: String?,
    val upgradeAvailable: Boolean = false
)

data class ChatMetadata(
    val model: String?,
    val timestamp: String?,
    val messageCount: Int? = null,  // NEW
    val ghostingDetected: Boolean? = null,  // NEW
    val hoursGhosted: Int? = null  // NEW
)

data class RelationshipResponse(
    val success: Boolean,
    val relationship: RelationshipStatus?,
    val error: String?
)


data class CharacterChatHistoryResponse(
    val success: Boolean,
    val characterId: String?,
    val messages: List<CharacterMessage>?,
    val count: Int?,
    val error: String?
)

// NEW: Emotional State Data Model
data class EmotionalState(
    val currentMood: String = "neutral",
    val energyLevel: Int = 80,
    val lastMoodChange: Any? = null // Changed to Any?
) {
    fun getLastMoodChangeString(): String {
        return when (lastMoodChange) {
            is String -> lastMoodChange
            is com.google.firebase.Timestamp -> {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US)
                sdf.format(lastMoodChange.toDate())
            }
            else -> ""
        }
    }
}

// NEW: Interaction Patterns Data Model
data class InteractionPatterns(
    val totalGhostsDetected: Int = 0,
    val averageResponseTime: Double = 0.0,
    val chatFrequency: String = "daily",
    val lastGhostWarning: String? = null // Timestamp of last ghost warning
)

// Update Profile Image Models
data class UpdateProfileImageRequest(
    val characterId: String,
    val imageUrl: String
)

data class UpdateProfileImageResponse(
    val success: Boolean,
    val message: String?,
    val imageUrl: String?
)
