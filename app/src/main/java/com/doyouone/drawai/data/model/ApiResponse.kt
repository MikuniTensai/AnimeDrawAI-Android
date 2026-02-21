package com.doyouone.drawai.data.model

import com.google.gson.annotations.SerializedName

/**
 * Base API Response wrapper
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: T? = null,
    
    @SerializedName("error")
    val error: String? = null,
    
    @SerializedName("message")
    val message: String? = null
)

/**
 * Workflow information
 */
data class WorkflowInfo(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("estimated_time")
    val estimatedTime: String,
    
    @SerializedName("file_exists")
    val fileExists: Boolean,
    
    @SerializedName("isPremium")
    val isPremium: Boolean = false,
    
    @SerializedName("restricted")
    val restricted: Boolean = false
)

/**
 * Workflows list response
 */
data class WorkflowsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("workflows")
    val workflows: Map<String, WorkflowInfo>
)

/**
 * Generate image request
 */
data class GenerateRequest(
    @SerializedName("positive_prompt")
    val positivePrompt: String,
    
    @SerializedName("negative_prompt")
    val negativePrompt: String = "",
    
    @SerializedName("workflow")
    val workflow: String = "standard",
    
    @SerializedName("width")
    val width: Int? = null,
    
    @SerializedName("height")
    val height: Int? = null,
    
    @SerializedName("seed")
    val seed: Long? = null,
    
    @SerializedName("ckpt_name")
    val ckptName: String? = null,
    
    @SerializedName("steps")
    val steps: Int? = null,
    
    @SerializedName("cfg")
    val cfg: Float? = null,
    
    @SerializedName("sampler_name")
    val samplerName: String? = null,
    
    @SerializedName("scheduler")
    val scheduler: String? = null,
    
    @SerializedName("denoise")
    val denoise: Float? = null,
    
    @SerializedName("upscale_method")
    val upscaleMethod: String? = null
)

data class UserCharacterResponse(
    val success: Boolean,
    val hasCharacter: Boolean,
    val characterId: String? = null,
    val error: String? = null
)

data class CharacterListResponse(
    val success: Boolean,
    val characters: List<Character>? = null,
    val error: String? = null
)

/**
 * Generate image response
 */
data class GenerateResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("task_id")
    val taskId: String? = null,
    
    @SerializedName("workflow")
    val workflow: String? = null,
    
    @SerializedName("workflow_name")
    val workflowName: String? = null,
    
    @SerializedName("estimated_time")
    val estimatedTime: String? = null,
    
    @SerializedName("status")
    val status: String? = null,
    
    @SerializedName("gems_earned")
    val gemsEarned: Int? = null,
    
    @SerializedName("error")
    val error: String? = null
)

/**
 * Task status response
 */
data class TaskStatusResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("task_id")
    val taskId: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("positive_prompt")
    val positivePrompt: String? = null,
    
    @SerializedName("negative_prompt")
    val negativePrompt: String? = null,
    
    @SerializedName("workflow")
    val workflow: String? = null,
    
    @SerializedName("workflow_name")
    val workflowName: String? = null,
    
    @SerializedName("created_at")
    val createdAt: String? = null,
    
    @SerializedName("started_at")
    val startedAt: String? = null,
    
    @SerializedName("completed_at")
    val completedAt: String? = null,
    
    @SerializedName("estimated_time")
    val estimatedTime: String? = null,
    
    @SerializedName("result_files")
    val resultFiles: List<String>? = null,

    @SerializedName("download_urls")
    val downloadUrls: List<String>? = null,
    
    @SerializedName("queue_position")
    val queuePosition: Int? = null,
    
    @SerializedName("queue_total")
    val queueTotal: Int? = null,
    
    @SerializedName("queue_info")
    val queueInfo: String? = null,
    
    @SerializedName("progress")
    val progress: Int? = null,
    
    @SerializedName("seed")
    val seed: Long? = null,  // Generation seed for reproducibility
    
    @SerializedName("error")
    val error: String? = null
)

/**
 * Health check response
 */
data class HealthResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("message")
    val message: String? = null
)

/**
 * Workflow model for UI
 */
data class Workflow(
    val id: String,
    val name: String,
    val description: String,
    val estimatedTime: String,
    val category: String = "General",
    val isFavorite: Boolean = false,
    val previewImage: String? = null,
    val isPremium: Boolean = false,
    val viewCount: Long = 0,
    val generationCount: Long = 0
)

/**
 * Generated image model
 */
data class GeneratedImage(
    val id: String,
    val prompt: String,
    val negativePrompt: String,
    val workflow: String,
    val imageUrl: String,
    val createdAt: String,
    val isFavorite: Boolean = false,
    val seed: Long? = null  // Generation seed for reproducibility
)

// --- DAILY REWARDS (Phase 2) ---

data class DailyRewardConfig(
    @SerializedName("day") val day: Int,
    @SerializedName("type") val type: String,
    @SerializedName("id") val id: String,
    @SerializedName("amount") val amount: Int,
    @SerializedName("name") val name: String
)

data class DailyStatusResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("is_claimable") val isClaimable: Boolean,
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("last_claim_date") val lastClaimDate: String?,
    @SerializedName("next_day_index") val nextDayIndex: Int,
    @SerializedName("reward") val reward: DailyRewardConfig?,
    @SerializedName("reward_cycle") val rewardCycle: String? = "A", // "A" or "B"
    @SerializedName("streak_saved") val streakSaved: Boolean = false,
    @SerializedName("error") val error: String? = null
)

data class DailyClaimResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("reward") val reward: DailyRewardConfig?,
    @SerializedName("new_streak") val newStreak: Int,
    @SerializedName("streak_freeze_used") val streakFreezeUsed: Boolean = false,
    @SerializedName("error") val error: String? = null
)

// --- INVENTORY & GIFTING (Phase 3) ---

data class InventoryItem(
    val id: String,
    val name: String,
    val amount: Int,
    val affectionValue: Int,
    val description: String
)

data class InventoryResponse(
    val success: Boolean,
    val inventory: List<InventoryItem>? = null,
    val error: String? = null
)

data class GiftRequest(
    val characterId: String,
    val itemId: String
)

data class GiftResponse(
    val success: Boolean,
    val affectionAdded: Int?,
    val message: String?,
    val error: String? = null
)

// --- SHOP SYSTEM (Phase 4) ---

data class ShopItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("type") val type: String, // "item", "currency" or "subscription"
    @SerializedName("amount") val amount: Int?, // if currency or item
    @SerializedName("cost_usd") val costUsd: Double?,
    @SerializedName("cost_gems") val costGems: Int?, // for items
    @SerializedName("item_id") val itemId: String?, // for items
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("duration_days") val durationDays: Int? // if subscription
)

data class ShopItemsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("items") val items: List<ShopItem>? = null,
    @SerializedName("error") val error: String? = null
)

data class PurchaseRequest(
    @SerializedName("itemId") val itemId: String
    // In real app, purchaseToken from Google Play
)

data class PurchaseResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String? = null
)

data class UseItemRequest(
    @SerializedName("itemId") val itemId: String
)

data class UseItemResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("new_expiry") val newExpiry: String? = null,
    @SerializedName("error") val error: String? = null
)
