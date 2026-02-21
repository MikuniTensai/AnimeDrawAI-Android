package com.doyouone.drawai.data.model

data class User(
    val id: String,
    val name: String,
    val email: String? = null,
    val apiKey: String,
    val isGuest: Boolean = false,
    val isPremium: Boolean = false,
    val subscriptionEndDate: Long? = null, // Unix timestamp in milliseconds
    val gems: Int = 0
)

data class LoginRequest(
    val apiKey: String
)

data class ApiKeyResponse(
    val success: Boolean,
    val api_key: String,
    val name: String,
    val rate_limit: String,
    val note: String
)
