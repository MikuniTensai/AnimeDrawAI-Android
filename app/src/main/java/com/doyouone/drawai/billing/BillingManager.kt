package com.doyouone.drawai.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BillingManager - Handles Google Play Billing for In-App Products
 * 
 * In-App Product Plans:
 * - Basic: Paket BASIC - AnimeDraw AI (IDR 29.000,00)
 * - Pro: Paket PRO - AnimeDraw AI (IDR 79.000,00)
 */
 
class BillingManager(private val context: Context) : PurchasesUpdatedListener {
    
    companion object {
        private const val TAG = "BillingManager"
        
        // In-App Product IDs (must match Google Play Console)
        const val BASIC_PRODUCT_ID = "anime_draw_basic_monthly"
        const val PRO_PRODUCT_ID = "anime_draw_pro_monthly"
        const val CHAT_RANDOM_PRODUCT_ID = "anime_draw_chat_random"
        const val CHAT_1_PRODUCT_ID = "anime_draw_chat_1"
        const val ONE_DAY_PRODUCT_ID = "anime_draw_1_day"
        
        @Volatile
        private var instance: BillingManager? = null
        
        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // Billing client
    private var billingClient: BillingClient? = null
    
    // Connection state
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    // Available products
    private val _availableProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val availableProducts: StateFlow<List<ProductDetails>> = _availableProducts.asStateFlow()
    
    // Active purchases
    private val _activePurchases = MutableStateFlow<List<Purchase>>(emptyList())
    val activePurchases: StateFlow<List<Purchase>> = _activePurchases.asStateFlow()
    
    // Purchase result callback
    var onPurchaseResult: ((success: Boolean, productId: String?, errorMessage: String?) -> Unit)? = null
    
    init {
        setupBillingClient()
    }
    
    /**
     * Setup and connect to Google Play Billing
     */
    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        connectToBilling()
    }
    
    /**
     * Connect to billing service
     */
    private fun connectToBilling() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ Billing client connected successfully")
                    _isReady.value = true
                    
                    // Query available products
                    queryInAppProducts()
                    
                    // Query active purchases
                    queryActivePurchases()
                } else {
                    Log.e(TAG, "❌ Billing setup failed: ${billingResult.debugMessage}")
                    _isReady.value = false
                }
            }
            
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "⚠️ Billing service disconnected")
                _isReady.value = false
                // Retry connection
                connectToBilling()
            }
        })
    }
    
    /**
     * Query available in-app products from Google Play
     */
    private fun queryInAppProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BASIC_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(CHAT_RANDOM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(CHAT_1_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(ONE_DAY_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "✅ Found ${productDetailsList.size} in-app products")
                _availableProducts.value = productDetailsList
                
                productDetailsList.forEach { product ->
                    val price = product.oneTimePurchaseOfferDetails?.formattedPrice
                    Log.d(TAG, "  - ${product.productId}: $price")
                }
            } else {
                Log.e(TAG, "❌ Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }
    
    /**
     * Query user's active purchases
     */
    fun queryActivePurchases() {
        if (!_isReady.value) {
            Log.w(TAG, "⚠️ Billing client not ready")
            return
        }
        
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        
        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "✅ Found ${purchases.size} active purchases")
                _activePurchases.value = purchases
                
                purchases.forEach { purchase ->
                    Log.d(TAG, "  - ${purchase.products.firstOrNull()}: ${purchase.purchaseState}")
                }
            } else {
                Log.e(TAG, "❌ Failed to query purchases: ${billingResult.debugMessage}")
            }
        }
    }
    
    /**
     * Launch in-app product purchase flow
     */
    fun launchProductPurchase(activity: Activity, productId: String) {
        // Store the attempted product ID for error tracking
        lastAttemptedProductId = productId
        
        Log.d(TAG, "🚀 Attempting to launch purchase for: $productId")
        
        // PRODUCTION: No demo mode allowed!
        // Demo mode ONLY in debug builds to prevent fake subscriptions
        if (com.doyouone.drawai.BuildConfig.DEBUG) {
            Log.d(TAG, "🎭 DEBUG BUILD - Demo mode available")
            // Check if billing is ready, if not use demo
            if (!_isReady.value) {
                Log.d(TAG, "🎭 Billing not ready - using demo mode")
                simulateDemoPurchase(productId)
                return
            }
        }
        
        // Production: Billing must be ready
        if (!_isReady.value) {
            Log.e(TAG, "❌ Billing client not ready")
            onPurchaseResult?.invoke(
                false, 
                null, 
                "Payment system not ready. Please restart the app and try again."
            )
            return
        }
        
        // Find the product
        val product = _availableProducts.value.find { it.productId == productId }
        if (product == null) {
            Log.e(TAG, "❌ Product not found: $productId")
            onPurchaseResult?.invoke(
                false, 
                null, 
                "This subscription plan is currently unavailable. Please try again later."
            )
            return
        }
        
        // Build billing flow parameters
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .build()
            )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        // Launch real billing flow
        Log.d(TAG, "🚀 Launching REAL billing flow for: $productId")
        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        
        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "❌ Failed to launch billing flow: ${billingResult?.debugMessage}")
            onPurchaseResult?.invoke(
                false, 
                null, 
                "Failed to start payment. Please try again."
            )
        }
    }
    
    
    /**
     * Simulate purchase for demo/development purposes
     * ONLY USED IN DEBUG BUILDS!
     */
    private fun simulateDemoPurchase(productId: String) {
        if (!com.doyouone.drawai.BuildConfig.DEBUG) {
            Log.e(TAG, "❌ Demo purchase blocked in production!")
            onPurchaseResult?.invoke(false, null, "Demo mode not available in production")
            return
        }
        
        Log.d(TAG, "🎭 DEMO MODE: Simulating purchase for $productId")
        
        // Show which product is being purchased
        val productName = when (productId) {
            BASIC_PRODUCT_ID -> "Paket BASIC - AnimeDraw AI (IDR 29.000)"
            PRO_PRODUCT_ID -> "Paket PRO - AnimeDraw AI (IDR 79.000)"
            CHAT_RANDOM_PRODUCT_ID -> "Gacha Chat Limit (IDR 10.000)"
            CHAT_1_PRODUCT_ID -> "Extra Chat Limit (IDR 3.000)"
            ONE_DAY_PRODUCT_ID -> "Paket 1 DAY - AnimeDraw AI"
            else -> "Unknown Product"
        }
        
        Log.d(TAG, "🛒 DEMO: Purchasing $productName")
        
        // Simulate a successful purchase after a short delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "✅ DEMO: Purchase successful for $productName")
            onPurchaseResult?.invoke(true, productId, null)
        }, 1500) // 1.5 second delay to simulate processing
    }
    
    /**
     * Handle purchase updates from Google Play
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    Log.d(TAG, "✅ Purchase successful: ${purchases.size} items")
                    purchases.forEach { purchase ->
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "⚠️ User canceled purchase")
                onPurchaseResult?.invoke(false, null, "Purchase canceled")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "⚠️ Item already owned")
                onPurchaseResult?.invoke(false, null, "Already subscribed")
                queryActivePurchases()
            }
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                Log.e(TAG, "❌ Item unavailable")
                // PRODUCTION: Show error, don't fake purchase
                onPurchaseResult?.invoke(
                    false, 
                    null, 
                    "This subscription is currently unavailable. Please contact support."
                )
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                Log.e(TAG, "❌ Developer error (app signature issue)")
                // PRODUCTION: Show error, don't fake purchase
                onPurchaseResult?.invoke(
                    false, 
                    null, 
                    "Payment configuration error. Please update the app or contact support."
                )
            }
            else -> {
                Log.e(TAG, "❌ Purchase failed: ${billingResult.debugMessage}")
                // PRODUCTION: Show error, don't fake purchase
                onPurchaseResult?.invoke(
                    false, 
                    null, 
                    "Payment failed. Please try again or contact support."
                )
            }
        }
    }
    
    private var lastAttemptedProductId: String? = null
    
    private fun getLastAttemptedProductId(): String {
        return lastAttemptedProductId ?: BASIC_PRODUCT_ID
    }
    
    /**
     * Handle successful purchase
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val productId = purchase.products.firstOrNull()
            
            // Check if it's a consumable product
            if (productId == CHAT_RANDOM_PRODUCT_ID || productId == CHAT_1_PRODUCT_ID || productId == ONE_DAY_PRODUCT_ID) {
                consumePurchase(purchase)
            } else {
                // Subscription or non-consumable
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                } else {
                    Log.d(TAG, "✅ Purchase already acknowledged")
                    onPurchaseResult?.invoke(true, productId, null)
                    queryActivePurchases()
                }
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "⏳ Purchase pending")
            onPurchaseResult?.invoke(false, null, "Purchase pending")
        }
    }
    
    /**
     * Acknowledge purchase (for subscriptions)
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "✅ Purchase acknowledged successfully")
                val productId = purchase.products.firstOrNull()
                onPurchaseResult?.invoke(true, productId, null)
                queryActivePurchases()
            } else {
                Log.e(TAG, "❌ Failed to acknowledge purchase: ${billingResult.debugMessage}")
                onPurchaseResult?.invoke(false, null, "Failed to acknowledge purchase")
            }
        }
    }
    
    /**
     * Consume purchase (for consumables like chat limits)
     */
    private fun consumePurchase(purchase: Purchase) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
            
        billingClient?.consumeAsync(params) { billingResult, purchaseToken ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "✅ Purchase consumed successfully")
                val productId = purchase.products.firstOrNull()
                onPurchaseResult?.invoke(true, productId, null)
            } else {
                Log.e(TAG, "❌ Failed to consume purchase: ${billingResult.debugMessage}")
                onPurchaseResult?.invoke(false, null, "Failed to consume purchase")
            }
        }
    }
    
    /**
     * Check if user has active purchase
     */
    fun hasActivePurchase(productId: String): Boolean {
        return _activePurchases.value.any { purchase ->
            purchase.products.contains(productId) &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
    }
    
    /**
     * Get active product type
     */
    fun getActivePurchaseType(): String? {
        val activePurchase = _activePurchases.value.firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        
        return when {
            activePurchase?.products?.contains(BASIC_PRODUCT_ID) == true -> "basic"
            activePurchase?.products?.contains(PRO_PRODUCT_ID) == true -> "pro"
            else -> null
        }
    }
    
    /**
     * Cleanup
     */
    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
        instance = null
    }
}
