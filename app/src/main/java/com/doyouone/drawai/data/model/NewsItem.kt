package com.doyouone.drawai.data.model

import com.google.firebase.Timestamp

data class NewsItem(
    val id: String = "",
    val type: String = "", // "event" or "update"
    val title: String = "",
    val description: String = "",
    val date: Timestamp = Timestamp.now(),
    val imageUrl: String? = null,
    val version: String? = null,
    val actionUrl: String? = null
)
