package com.doyouone.drawai.data.model

data class ChatMessage(
    val id: String = "",
    val sessionId: String = "",
    val role: String = "", // "user" or "assistant" or "system"
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val model: String = "gemma3:4b",
    val metadata: MessageMetadata? = null
)

data class MessageMetadata(
    val responseTime: Double = 0.0,
    val tokensUsed: Int = 0,
    val evalCount: Int = 0
)

data class ChatSession(
    val sessionId: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList(),
    val model: String = "gemma3:4b",
    val totalTokens: Int = 0
)

data class OllamaModel(
    val name: String = "",
    val size: String = "",
    val modified: String = ""
)
