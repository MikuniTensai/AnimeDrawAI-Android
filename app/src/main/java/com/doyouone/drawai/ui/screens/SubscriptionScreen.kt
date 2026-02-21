package com.doyouone.drawai.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doyouone.drawai.billing.BillingManager
import com.doyouone.drawai.data.model.SubscriptionPlan
import com.doyouone.drawai.data.model.UserSubscription
import com.doyouone.drawai.data.repository.SubscriptionRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.doyouone.drawai.data.repository.FirebaseGenerationRepository
import com.doyouone.drawai.data.repository.CharacterRepository
import com.doyouone.drawai.data.model.GenerationLimit
import androidx.compose.animation.core.*
import androidx.compose.ui.res.stringResource
import com.doyouone.drawai.R

@Composable
fun SubscriptionScreen(
    userId: String,
    onBackPressed: () -> Unit,
    onSubscribed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Fix: Add userId as key to remember so repo is recreated when user changes
    val subscriptionRepo = remember(userId) { SubscriptionRepository(userId) }
    val generationRepo = remember(userId) { FirebaseGenerationRepository() }
    val characterRepo = remember(userId) { CharacterRepository() }
    val billingManager = remember { BillingManager.getInstance(context) }
    
    val currentSubscription by subscriptionRepo.getSubscription().collectAsState(initial = UserSubscription())
    val generationLimit by generationRepo.getGenerationLimitFlow(userId).collectAsState(initial = null)
    val characters by characterRepo.getUserCharactersFlow().collectAsState(initial = emptyList())
    val characterCount = characters.size

    val availableProducts by billingManager.availableProducts.collectAsState()
    val oneDayProduct = availableProducts.find { it.productId == BillingManager.ONE_DAY_PRODUCT_ID }
    val oneDayPrice = oneDayProduct?.oneTimePurchaseOfferDetails?.formattedPrice ?: "Loading..."

    // Check expiration on load (Syncs Firestore if needed)
    LaunchedEffect(userId) {
        generationRepo.checkAndUpdateSubscription(userId)
    }


    // Calculate effective plan based on both sources (prioritize generation_limits for consistency)
    val effectivePlan = remember(currentSubscription, generationLimit) {
        // If expired, treat as FREE regardless of type string
        if (generationLimit?.isSubscriptionExpired() == true) {
            SubscriptionPlan.FREE
        } else {
            val genType = generationLimit?.subscriptionType?.lowercase()
            when {
                genType == "pro" -> SubscriptionPlan.PRO
                genType == "basic" -> SubscriptionPlan.BASIC
                currentSubscription.plan == SubscriptionPlan.PRO -> SubscriptionPlan.PRO
                currentSubscription.plan == SubscriptionPlan.BASIC -> SubscriptionPlan.BASIC
                else -> SubscriptionPlan.FREE
            }
        }
    }

    // Animation for Pro users
    val infiniteTransition = rememberInfiniteTransition(label = "pro_gradient")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )
    var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    var showGachaResult by remember { mutableStateOf<Int?>(null) }
    
    // Setup purchase result callback
    LaunchedEffect(Unit) {
        billingManager.onPurchaseResult = { success, productId, errorMessage ->
            if (success && productId != null) {
                // Purchase successful
                scope.launch {
                    when (productId) {
                        BillingManager.BASIC_PRODUCT_ID -> {
                            subscriptionRepo.upgradePlan(SubscriptionPlan.BASIC)
                            com.doyouone.drawai.data.preferences.UserPreferences(context).setPremiumStatus(true)
                            onSubscribed()
                        }
                        BillingManager.PRO_PRODUCT_ID -> {
                            subscriptionRepo.upgradePlan(SubscriptionPlan.PRO)
                            com.doyouone.drawai.data.preferences.UserPreferences(context).setPremiumStatus(true)
                            onSubscribed()
                        }
                        BillingManager.CHAT_1_PRODUCT_ID -> {
                            subscriptionRepo.addChatLimit(1)
                            android.widget.Toast.makeText(context, context.getString(R.string.subscription_toast_added_chat), android.widget.Toast.LENGTH_SHORT).show()
                        }
                        BillingManager.CHAT_RANDOM_PRODUCT_ID -> {
                            // Gacha Logic: 10% chance for 9, else 1-8
                            val isJackpot = (1..100).random() <= 10
                            val amount = if (isJackpot) 9 else (1..8).random()
                            
                            subscriptionRepo.addChatLimit(amount)
                            showGachaResult = amount
                        }
                        BillingManager.ONE_DAY_PRODUCT_ID -> {
                           subscriptionRepo.activateDayPass()
                           com.doyouone.drawai.data.preferences.UserPreferences(context).setPremiumStatus(true)
                           onSubscribed()
                           android.widget.Toast.makeText(context, "1 Day Premium Access Activated!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                // Handle purchase error - could show error dialog here
                errorMessage?.let {
                    Log.e("SubscriptionScreen", "❌ Purchase failed: $it")
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onBackPressed() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "←",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = stringResource(R.string.subscription_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Current Plan Info (Updated Design from AppDrawer)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (effectivePlan == SubscriptionPlan.PRO) {
                                // Pro: Animated Gradient
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.primary
                                    ),
                                    start = androidx.compose.ui.geometry.Offset(animatedOffset, 0f),
                                    end = androidx.compose.ui.geometry.Offset(animatedOffset + 500f, 500f),
                                    tileMode = androidx.compose.ui.graphics.TileMode.Mirror
                                )
                            } else if (effectivePlan == SubscriptionPlan.BASIC) {
                                // Basic: Static Gradient
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            } else {
                                // Free: Darker/Different Gradient
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha=0.8f),
                                        MaterialTheme.colorScheme.primary.copy(alpha=0.6f)
                                    )
                                )
                            }
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            // User Info Header
                            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            val userEmail = user?.email ?: "Guest"
                            Text(
                                text = stringResource(R.string.subscription_status_user),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (userEmail == "Guest") stringResource(R.string.profile_guest) else userEmail,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Limit Stats
                            generationLimit?.let { limit ->
                                val remaining = limit.getRemainingGenerations()
                                val max = if (limit.subscriptionType == "free") limit.maxDailyLimit else limit.subscriptionLimit
                                
                                Text(
                                    text = stringResource(R.string.subscription_limit_format, "$remaining", "$max"),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                
                                // Expired Date - Use app's locale, not system locale
                                if (limit.subscriptionType != "free" && limit.subscriptionEndDate != null) {
                                    val locale = context.resources.configuration.locales[0]
                                    val dateFormat = SimpleDateFormat("dd MMM yyyy", locale)
                                    val dateStr = dateFormat.format(limit.subscriptionEndDate.toDate())
                                    Text(
                                        text = stringResource(R.string.subscription_exp_format, dateStr),
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                
                                // Chat Limit
                                val chatLimitText = "$characterCount/${limit.maxChatLimit}"
                                Text(
                                    text = stringResource(R.string.subscription_chat_format, "$characterCount/${limit.maxChatLimit}"),
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            } ?: run {
                                // Fallback if limit not loaded yet
                                Text(
                                    text = stringResource(R.string.subscription_loading_limits),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Status Badge
                        val statusText = when (effectivePlan) {
                            SubscriptionPlan.PRO -> stringResource(R.string.subscription_status_pro)
                            SubscriptionPlan.BASIC -> stringResource(R.string.subscription_status_basic)
                            else -> stringResource(R.string.subscription_status_free)
                        }
                        
                        val statusColor = when (effectivePlan) {
                            SubscriptionPlan.PRO -> Color(0xFFFFD700) // Gold
                            SubscriptionPlan.BASIC -> Color(0xFFE0E0E0) // Silver
                            else -> Color.White.copy(alpha = 0.7f)
                        }
                        
                        Text(
                            text = statusText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier
                                .background(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .align(Alignment.Top)
                        )
                    }
                }
            }
            
            // Spacer removed to reduce gap

            
            var showBenefitsPopup by remember { mutableStateOf(false) }

            // Title Row with Benefits Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.subscription_choose_plan),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(
                    onClick = { showBenefitsPopup = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "See Benefits", // Should be string resource ideally
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showBenefitsPopup) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showBenefitsPopup = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                     // Reuse PremiumOnlyFeature logic but adapted for this context
                     // Since we don't have access to PremiumOnlyFeature in this file without import, we'll recreate a clean version here
                     // But wait, we can just import it if the package structure allows. 
                     // Assuming we can't easily add import right now, let's inline a nice UI.
                     
                     Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp)
                     ) {
                         Column(
                             modifier = Modifier.fillMaxSize(), // Scrollable content
                             horizontalAlignment = Alignment.CenterHorizontally
                         ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { showBenefitsPopup = false }) {
                                    Text("✕", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                             
                            Text(
                                text = "Premium Benefits",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Benefits List
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.3f))
                            ) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    BenefitRow("⚡ Increased Gen Limits", "Generate 200 or 600 images daily instead of 5")
                                    BenefitRow("🚫 Ad-Free Experience", "No more interruptions while creating")
                                    BenefitRow("💬 Extra Chat Limits", "Chat more with AI characters")
                                    BenefitRow("✨ Community Access", "Share and browse the Explore feed")
                                    BenefitRow("❤️ Likes & Engagement", "Interact with other creators")
                                    BenefitRow("📥 Bulk Export", "Export all your creations at once")
                                    BenefitRow("🔐 Gallery Lock", "Protect your gallery with PIN")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Button(
                                onClick = { showBenefitsPopup = false },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Got it!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                         }
                     }
                }
            }
            
            // 1 Day Pass Card - Full Width (Style matched to CompactPlanCard)
            // Use surface color for dark mode support
            val oneDayBackground = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surface
                )
            )
            
            val oneDayBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) // Subtle border like selection
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, oneDayBorderColor, RoundedCornerShape(12.dp))
                    .clickable {
                         val activity = context as? androidx.activity.ComponentActivity
                         activity?.let { billingManager.launchProductPurchase(it, BillingManager.ONE_DAY_PRODUCT_ID) }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                 Box(modifier = Modifier.fillMaxWidth().background(oneDayBackground)) {
                     Column(
                         modifier = Modifier.padding(12.dp).fillMaxWidth()
                     ) {
                         // Header: Title and Price
                         Row(
                             modifier = Modifier.fillMaxWidth(),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Text(
                                 text = "Try in 1 DAY", 
                                 fontSize = 16.sp,
                                 fontWeight = FontWeight.Bold,
                                 color = MaterialTheme.colorScheme.onSurface
                             )
                             
                             Text(
                                 text = oneDayPrice,
                                 fontSize = 14.sp,
                                 fontWeight = FontWeight.Bold,
                                 color = MaterialTheme.colorScheme.primary
                             )
                         }
                         
                         Spacer(modifier = Modifier.height(4.dp))
                         
                         Text(
                             text = "Full Premium Access for 24h", 
                             fontSize = 12.sp,
                             color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                         )
                         
                         Spacer(modifier = Modifier.height(12.dp))
                         
                         Button(
                             onClick = {
                                 val activity = context as? androidx.activity.ComponentActivity
                                 activity?.let { billingManager.launchProductPurchase(it, BillingManager.ONE_DAY_PRODUCT_ID) }
                             },
                             modifier = Modifier.fillMaxWidth().height(32.dp),
                             contentPadding = PaddingValues(0.dp),
                             colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                         ) {
                             Text(stringResource(R.string.subscription_btn_buy), fontSize = 12.sp)
                         }
                     }
                 }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compact Plans Row
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Basic Plan
                CompactPlanCard(
                    plan = SubscriptionPlan.BASIC,
                    isCurrentPlan = effectivePlan == SubscriptionPlan.BASIC,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onSelectPlan = {
                        selectedPlan = SubscriptionPlan.BASIC
                        showConfirmDialog = true
                    }
                )
                
                // Pro Plan
                CompactPlanCard(
                    plan = SubscriptionPlan.PRO,
                    isCurrentPlan = effectivePlan == SubscriptionPlan.PRO,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onSelectPlan = {
                        selectedPlan = SubscriptionPlan.PRO
                        showConfirmDialog = true
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            

            
            Text(
                text = stringResource(R.string.subscription_chat_addons),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            // Add-ons Row
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Gacha Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { 
                            val activity = context as? androidx.activity.ComponentActivity
                            activity?.let { billingManager.launchProductPurchase(it, BillingManager.CHAT_RANDOM_PRODUCT_ID) }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.subscription_gacha_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stringResource(R.string.subscription_gacha_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("IDR 10k", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800), fontSize = 14.sp)
                    }
                }
                
                // Single Limit Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { 
                            val activity = context as? androidx.activity.ComponentActivity
                            activity?.let { billingManager.launchProductPurchase(it, BillingManager.CHAT_1_PRODUCT_ID) }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.subscription_single_limit_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stringResource(R.string.subscription_single_limit_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("IDR 3k", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Support Notice
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ⓘ",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "Payment Issues?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "If your plan doesn't update after payment, please contact us via Email in Settings.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
    
    // Gacha Result Dialog
    if (showGachaResult != null) {
        AlertDialog(
            onDismissRequest = { showGachaResult = null },
            title = { Text(if (showGachaResult == 9) stringResource(R.string.subscription_gacha_result_jackpot) else stringResource(R.string.subscription_gacha_result_normal)) },
            text = { 
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "+$showGachaResult",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (showGachaResult == 9) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary
                    )
                    Text(stringResource(R.string.subscription_gacha_result_msg), fontSize = 16.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showGachaResult = null }) {
                    Text(stringResource(R.string.subscription_btn_awesome))
                }
            }
        )
    }
    
    // Confirm Dialog
    if (showConfirmDialog && selectedPlan != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.subscription_upgrade_title, selectedPlan!!.planName),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(stringResource(R.string.subscription_price_format, selectedPlan!!.getFormattedPrice()), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• ${selectedPlan!!.getLimitText()}")
                    if (!selectedPlan!!.hasAds) Text(stringResource(R.string.subscription_feature_ad_free))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        if (selectedPlan != SubscriptionPlan.FREE) {
                            val productId = when (selectedPlan) {
                                SubscriptionPlan.BASIC -> BillingManager.BASIC_PRODUCT_ID
                                SubscriptionPlan.PRO -> BillingManager.PRO_PRODUCT_ID
                                else -> null
                            }
                            productId?.let {
                                val activity = context as? androidx.activity.ComponentActivity
                                activity?.let { act -> billingManager.launchProductPurchase(act, it) }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.subscription_btn_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.settings_btn_cancel)) // Reusing cancel button from settings
                }
            }
        )
    }
}

@Composable
private fun CompactPlanCard(
    plan: SubscriptionPlan,
    isCurrentPlan: Boolean,
    modifier: Modifier = Modifier,
    onSelectPlan: () -> Unit
) {
    val borderColor = if (isCurrentPlan) MaterialTheme.colorScheme.primary else Color.Transparent
    val backgroundColor = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface
            )
        )
    
    Card(
        modifier = modifier
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !isCurrentPlan) { onSelectPlan() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
            Column(
                modifier = Modifier.padding(12.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = plan.planName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = plan.getFormattedPrice(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (plan.hasDiscount()) {
                    Text(
                        text = plan.getFormattedOriginalPrice(),
                        fontSize = 10.sp,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Key Feature
                Text(
                    text = if (plan == SubscriptionPlan.PRO) "600 Gens" else "${plan.periodLimit} Gens",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = if (plan == SubscriptionPlan.PRO) "💬 +10 Chat Limit" else "💬 +3 Chat Limit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (!isCurrentPlan) {
                    Button(
                        onClick = onSelectPlan,
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.subscription_btn_buy), fontSize = 12.sp)
                    }
                } else {
                    Text(
                        text = stringResource(R.string.subscription_current_plan),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BenefitRow(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "✓",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, // Green or Primary
            modifier = Modifier.padding(end = 12.dp)
        )
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

