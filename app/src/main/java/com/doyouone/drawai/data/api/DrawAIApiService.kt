package com.doyouone.drawai.data.api

import com.doyouone.drawai.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * DrawAI API Service Interface
 * Base URL: https://drawai.site or your local server
 */
interface DrawAIApiService {
    
    /**
     * Get list of available workflows
     */
    @GET("workflows")
    suspend fun getWorkflows(): Response<WorkflowsResponse>
    
    /**
     * Generate image with specified workflow
     */
    @POST("generate")
    suspend fun generateImage(
        @Body request: GenerateRequest
    ): Response<GenerateResponse>
    
    /**
     * Check generation task status
     */
    @GET("status/{task_id}")
    suspend fun getTaskStatus(
        @Path("task_id") taskId: String
    ): Response<TaskStatusResponse>
    
    /**
     * Download generated image
     */
    @GET("download/{filename}")
    @Streaming
    suspend fun downloadImage(
        @Path("filename") filename: String
    ): Response<ResponseBody>
    
    /**
     * Health check endpoint
     */
    @GET("health")
    suspend fun healthCheck(): Response<HealthResponse>
    
    /**
     * Remove background from uploaded image
     */
    @Multipart
    @POST("remove-background")
    suspend fun removeBackground(
        @Part image: okhttp3.MultipartBody.Part,
        @PartMap options: Map<String, @JvmSuppressWildcards okhttp3.RequestBody>
    ): Response<GenerateResponse>

    /**
     * Upscale image (Super Resolution)
     */
    @Multipart
    @POST("upscale-image")
    suspend fun upscaleImage(
        @Part image: okhttp3.MultipartBody.Part,
        @PartMap options: Map<String, @JvmSuppressWildcards okhttp3.RequestBody>
    ): Response<GenerateResponse>

    /**
     * Sketch to Image (ControlNet)
     */
    @Multipart
    @POST("sketch-to-image")
    suspend fun sketchToImage(
        @Part image: okhttp3.MultipartBody.Part,
        @PartMap options: Map<String, @JvmSuppressWildcards okhttp3.RequestBody>
    ): Response<GenerateResponse>

    /**
     * Draw to Image (Canvas)
     */
    @Multipart
    @POST("draw-to-image")
    suspend fun drawToImage(
        @Part image: okhttp3.MultipartBody.Part,
        @PartMap options: Map<String, @JvmSuppressWildcards okhttp3.RequestBody>
    ): Response<GenerateResponse>

    /**
     * Face Restore tool
     */
    @Multipart
    @POST("face-restore")
    suspend fun executeTool( 
        @Part("tool_name") toolName: okhttp3.RequestBody,
        @Part image: okhttp3.MultipartBody.Part,
        @PartMap options: Map<String, @JvmSuppressWildcards okhttp3.RequestBody>
    ): Response<GenerateResponse>
    
    /**
     * Get API information
     */
    @GET("/")
    suspend fun getApiInfo(): Response<Map<String, Any>>

    // --- Daily Rewards ---
    @GET("daily-rewards/status")
    suspend fun checkDailyStatus(): Response<DailyStatusResponse>

    @POST("daily-rewards/claim")
    suspend fun claimDailyReward(): Response<DailyClaimResponse>

    // --- Inventory & Gift ---
    @GET("user/inventory")
    suspend fun getInventory(): Response<InventoryResponse>

    @POST("character/gift")
    suspend fun sendGift(@Body request: GiftRequest): Response<GiftResponse>

    // --- Shop ---
    @GET("shop/items")
    suspend fun getShopItems(): Response<ShopItemsResponse>

    @POST("shop/purchase")
    suspend fun purchaseShopItem(@Body request: PurchaseRequest): Response<PurchaseResponse>

    @POST("user/use-item")
    suspend fun useItem(@Body request: UseItemRequest): Response<UseItemResponse>

    @POST("character/toggle-notification")
    suspend fun toggleNotification(@Body request: ToggleNotificationRequest): Response<ToggleNotificationResponse>

    @POST("character/update-profile-image")
    suspend fun updateCharacterProfileImage(@Body request: UpdateProfileImageRequest): Response<UpdateProfileImageResponse>
}
