package com.doyouone.drawai.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doyouone.drawai.ui.components.AnimeDrawMainTopBar
import com.doyouone.drawai.ui.components.SettingsItem
import com.doyouone.drawai.ui.components.SettingsSection
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import java.text.SimpleDateFormat
import java.util.Locale
import com.doyouone.drawai.ui.theme.Purple40
import kotlinx.coroutines.launch
import com.doyouone.drawai.R
import androidx.compose.ui.res.stringResource

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    authManager: com.doyouone.drawai.auth.AuthManager? = null,
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToUsageStats: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    viewModel: com.doyouone.drawai.viewmodel.GenerateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val generationRepository = remember { com.doyouone.drawai.data.repository.FirebaseGenerationRepository() }
    
    // State
    var userDisplayName by remember { mutableStateOf("User") }
    var userEmail by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(true) }
    var generationLimit by remember { mutableStateOf<com.doyouone.drawai.data.model.GenerationLimit?>(null) }
    
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }
    
    // Oberve current user changes
    val currentUser by authManager?.currentUser?.collectAsState() ?: mutableStateOf(null)

    // Load and update data when user changes
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            userDisplayName = currentUser?.displayName ?: "User"
            userEmail = currentUser?.email ?: ""
            isAnonymous = currentUser?.isAnonymous ?: true
            
            // Re-fetch generation limit
            launch {
                generationRepository.getGenerationLimitFlow(currentUser!!.uid).collect { limit ->
                    generationLimit = limit
                }
            }
        } else {
             // Handle logged out state if needed, though navigation should kick in
        }
    }
    
    // Google Sign In Launcher
    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            val data = result.data
            authManager?.linkAnonymousToGoogle(data)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        AnimeDrawMainTopBar(
            title = stringResource(R.string.profile_title),
            onOpenDrawer = onOpenDrawer
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 1. Profile Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp) // Border space
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Edit Icon
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = 4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { 
                            newNameInput = userDisplayName
                            showEditNameDialog = true 
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.profile_edit_name),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (userDisplayName.isNotBlank()) userDisplayName else stringResource(R.string.profile_guest),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            if (userEmail.isNotBlank()) {
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (isAnonymous) {
                Text(
                    text = stringResource(R.string.profile_guest_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 2. Subscription Status Card
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.profile_section_subscription),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
            
            generationLimit?.let { limit ->
                 val remaining = limit.getRemainingGenerations()
                 val subscriptionName = limit.getSubscriptionName()
                 val isPremium = limit.subscriptionType == "pro" || limit.subscriptionType == "basic"
                 
                 // Membership Card Style
                 Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp), // Fixed height for credit card look
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                 ) {
                     Box(modifier = Modifier.fillMaxSize()) {
                         // Background
                         if (isPremium) {
                             // Premium Gradient
                             Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    )
                             )
                         } else {
                             // Free Gradient (Darker/Subtle)
                             Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    )
                             )
                         }
                         
                         // Content
                         Column(
                             modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                             verticalArrangement = Arrangement.SpaceBetween
                         ) {
                             // Top Row: Plan Name & Icon
                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Column {
                                     Text(
                                         text = subscriptionName.uppercase(),
                                         fontWeight = FontWeight.Black,
                                         fontSize = 20.sp,
                                         color = if (isPremium) Color.White else MaterialTheme.colorScheme.onSurface,
                                         letterSpacing = 1.sp
                                     )
                                     Text(
                                         text = if (isPremium) "Premium Member" else "Free Plan",
                                         fontSize = 12.sp,
                                         color = if (isPremium) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) 
                                     )
                                 }
                                 Text(
                                     text = if (isPremium) "👑" else "⚡",
                                     fontSize = 32.sp
                                 )
                             }
                             
                            // Middle: Progress Stats
                            Column {
                                val maxLimit = if (isPremium) {
                                     if (limit.subscriptionLimit > 0) limit.subscriptionLimit 
                                     else if (limit.subscriptionType == "pro") 600 
                                     else 200 // Basic
                                 } else {
                                     limit.maxDailyLimit
                                 }

                                 Row(
                                     modifier = Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                     Text(
                                         text = "Daily Generations",
                                         fontSize = 12.sp,
                                         color = if (isPremium) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
                                     )
                                     
                                     Text(
                                         text = "$remaining/$maxLimit",
                                         fontSize = 12.sp,
                                         fontWeight = FontWeight.Bold,
                                         color = if (isPremium) Color.White else MaterialTheme.colorScheme.onSurface
                                     )
                                 }
                                 Spacer(modifier = Modifier.height(8.dp))
                                     LinearProgressIndicator(
                                         progress = { remaining.toFloat() / maxLimit.toFloat() },
                                         modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                     color = if (isPremium) Color.White else MaterialTheme.colorScheme.primary,
                                     trackColor = if (isPremium) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                     gapSize = 0.dp,
                                     drawStopIndicator = {}
                                 )
                                 if (!isPremium) {
                                     Spacer(modifier = Modifier.height(4.dp))
                                     Text(
                                         text = "Resets daily at 00:00 UTC",
                                         fontSize = 10.sp,
                                         color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                     )
                                 }
                             }
                             
                             // Bottom Row: Expiry or Upgrade
                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 if (isPremium) {
                                     Column {
                                         Text(
                                             text = "EXPIRES",
                                             fontSize = 10.sp,
                                             fontWeight = FontWeight.Bold,
                                             color = Color.White.copy(alpha = 0.7f)
                                         )
                                         val locale = context.resources.configuration.locales[0]
                                         val dateFormat = SimpleDateFormat("MM/yy", locale)
                                         val dateStr = limit.subscriptionEndDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
                                         Text(
                                             text = dateStr,
                                             fontSize = 14.sp,
                                             fontWeight = FontWeight.Medium,
                                             color = Color.White
                                         )
                                     }
                                 } else {
                                     // Call to action for free users
                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                         // Watch Ad Button
                                         val activity = context as? android.app.Activity
                                         val uid = currentUser?.uid ?: ""
                                         val subscriptionRepository = remember(uid) { com.doyouone.drawai.data.repository.SubscriptionRepository(uid) }
                                         
                                         FilledTonalButton(
                                             onClick = { 
                                                if (activity != null) {
                                                     com.doyouone.drawai.ads.AdManager.showRewardedAd(
                                                         activity = activity,
                                                         onUserEarnedReward = { _ ->
                                                             scope.launch {
                                                                 if (uid.isNotEmpty()) {
                                                                     subscriptionRepository.addBonusGeneration(1)
                                                                     // Toast is optional here or use a Snackbar
                                                                 }
                                                             }
                                                         }
                                                     )
                                                }
                                             },
                                             modifier = Modifier.height(36.dp),
                                             contentPadding = PaddingValues(horizontal = 8.dp),
                                             colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                                         ) {
                                             Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                             Spacer(Modifier.width(4.dp))
                                             Text("+1 Gen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                         }
                                         
                                         Spacer(Modifier.width(8.dp))
                                         
                                         TextButton(
                                             onClick = onNavigateToSubscription,
                                             colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                         ) {
                                             Text("Upgrade >", fontWeight = FontWeight.Bold)
                                         }
                                     }
                                 }
                             }
                         }
                     }
                 }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // 3. Invite & Rewards Section
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Invite & Rewards",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Title & Description
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎁", fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Invite Friends", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Get 500 Gems for every friend!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Code Display
                    val referralCode = currentUser?.uid ?: "Loading..."
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    
                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Your Referral Code") },
                        trailingIcon = {
                            IconButton(onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(referralCode))
                                android.widget.Toast.makeText(context, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha=0.5f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Actions: Share & Enter Code
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Share Button
                        Button(
                            onClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Use my code $referralCode to get 50 Gems on AnimeDraw AI! Download now: [Link]")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
                                
                                // Claim Share Reward (First time only)
                                viewModel.claimShareReward { success, msg ->
                                    if (success) {
                                         android.widget.Toast.makeText(context, "First Share Reward: +500 Gems! 💎", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Share")
                        }
                        
                        // Enter Code Button
                        var showReferralDialog by remember { mutableStateOf(false) }
                        
                        OutlinedButton(
                            onClick = { showReferralDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Enter Code")
                        }
                        
                        if (showReferralDialog) {
                            var codeInput by remember { mutableStateOf("") }
                            AlertDialog(
                                onDismissRequest = { showReferralDialog = false },
                                title = { Text("Enter Referral Code") },
                                text = {
                                    Column {
                                        Text("Enter a friend's code to get 50 Gems!")
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = codeInput,
                                            onValueChange = { codeInput = it },
                                            singleLine = true,
                                            placeholder = { Text("Paste code here") }
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        // Get Device ID (Simple Android ID for now)
                                        val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
                                        
                                        viewModel.redeemReferral(codeInput, deviceId) { success, msg ->
                                            if (success) {
                                                android.widget.Toast.makeText(context, "Success! You got 50 Gems! 💎", android.widget.Toast.LENGTH_LONG).show()
                                                showReferralDialog = false
                                            } else {
                                                android.widget.Toast.makeText(context, msg ?: "Failed", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }) {
                                        Text("Redeem")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showReferralDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 3. Settings Cards (Grouped)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            
            // Usage & Plan Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsItem(
                        title = stringResource(R.string.profile_plan),
                        subtitle = stringResource(R.string.profile_plan_desc),
                        onClick = onNavigateToSubscription
                    )
                    SettingsItem(
                        title = stringResource(R.string.profile_usage_stats),
                        subtitle = stringResource(R.string.profile_usage_stats_desc),
                        onClick = onNavigateToUsageStats
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.profile_section_account),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
            
            // Account Management Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (isAnonymous) {
                        SettingsItem(
                            title = stringResource(R.string.profile_link_account),
                            subtitle = stringResource(R.string.profile_link_account_desc),
                            onClick = {
                                authManager?.getGoogleSignInIntent()?.let { intent ->
                                    googleSignInLauncher.launch(intent)
                                }
                            }
                        )
                    }
                    
                    SettingsItem(
                        title = stringResource(R.string.profile_sign_out),
                        subtitle = stringResource(R.string.profile_sign_out_desc),
                        onClick = { showSignOutDialog = true }
                    )
                    
                    SettingsItem(
                        title = stringResource(R.string.profile_delete_account),
                        subtitle = stringResource(R.string.profile_delete_account_desc),
                        onClick = { showDeleteAccountDialog = true },
                        textColor = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
    
    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(stringResource(R.string.profile_dialog_edit_name)) },
            text = {
                Column {
                    Text(stringResource(R.string.profile_enter_name))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newNameInput.isNotBlank()) {
                            scope.launch {
                                authManager?.updateUserDisplayName(newNameInput)
                                showEditNameDialog = false
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.profile_btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(stringResource(R.string.settings_btn_cancel)) // Reusing cancel button from settings
                }
            }
        )
    }

    // Sign Out Warning Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️ ", fontSize = 20.sp)
                    Text("Warning") // Hardcoded as requested
                }
            },
            text = { 
                Text(
                    "Signing out will delete all generated images in your localized gallery from this device. To save them, please go to Settings > Export to Gallery first."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete Account Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(stringResource(R.string.profile_dialog_delete_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.profile_delete_warning_list),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(stringResource(R.string.profile_delete_item_1))
                    Text(stringResource(R.string.profile_delete_item_2))
                    Text(stringResource(R.string.profile_delete_item_3))
                    Text(stringResource(R.string.profile_delete_item_4))
                    Text(stringResource(R.string.profile_delete_item_5))
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.profile_delete_warning_final),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.profile_delete_instruction),
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/8KzQxVyJ9mP2nR4t6"))
                        context.startActivity(intent)
                        showDeleteAccountDialog = false
                    }
                ) {
                    Text(stringResource(R.string.profile_btn_submit_request), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(stringResource(R.string.settings_btn_cancel)) // Reusing cancel button from settings
                }
            }
        )
    }
}
