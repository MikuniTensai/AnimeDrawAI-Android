package com.doyouone.drawai.data.model

data class ImageReport(
    val id: String = "",
    val imageId: String = "",
    val prompt: String = "",
    val negativePrompt: String = "",
    val workflow: String = "",
    val imageUrl: String = "",
    val reportReason: String = "",
    val reportedAt: Long = System.currentTimeMillis(),
    val reportedBy: String = "", // User ID or email
    val status: String = "pending" // pending, reviewed, resolved
)