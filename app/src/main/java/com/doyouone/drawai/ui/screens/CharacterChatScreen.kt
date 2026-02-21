package com.doyouone.drawai.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.doyouone.drawai.data.preferences.AppPreferences
import androidx.compose.ui.res.stringResource
import com.doyouone.drawai.R



import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.doyouone.drawai.data.model.*
import com.doyouone.drawai.ui.components.ZoomableImage
import com.doyouone.drawai.data.repository.CharacterRepository
import com.doyouone.drawai.data.repository.DrawAIRepository
import com.doyouone.drawai.ui.theme.*
import kotlinx.coroutines.launch
import com.doyouone.drawai.ui.components.AnimeDrawTopAppBar
import com.doyouone.drawai.ui.components.AnimeDrawErrorDialog
import com.doyouone.drawai.util.ErrorUtils
import com.doyouone.drawai.util.GenerationNotificationUtils
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.ExistingWorkPolicy
import com.doyouone.drawai.worker.PhotoGenerationWorker
import com.doyouone.drawai.ads.AdManager
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterChatScreen(
    characterId: String,
    onBack: () -> Unit,
    onNavigateToSubscription: () -> Unit = {} // Default empty for compatibility if needed, but better to force update
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val characterRepository = remember { CharacterRepository() }
    val drawAIRepository = remember { DrawAIRepository(characterRepo = characterRepository) }
    val appPreferences = remember { AppPreferences(context) }
    
    // Auth & Limits
    val userId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val generationLimit by drawAIRepository.getGenerationLimitFlow(userId).collectAsState(initial = null)
    val gemCount by drawAIRepository.getGemCountFlow(userId).collectAsState(initial = 0)
    
    var character by remember { mutableStateOf<Character?>(null) }
    var messages by remember { mutableStateOf<List<CharacterMessage>>(emptyList()) }
    var messageInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSilentLoading by remember { mutableStateOf(false) } // To hide chat loading for status updates
    var showStageChangeDialog by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) } // Existing upgrade dialog
    var showLimitSubscriptionDialog by remember { mutableStateOf(false) } // For generation limits
    var showRelationshipDialog by remember { mutableStateOf(false) }
    var newStage by remember { mutableStateOf<RelationshipStage?>(null) }
    var showOffensiveContentDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }

    var errorDialogMessage by remember { mutableStateOf("") }
    
    // Photo Generation State
    var showPhotoDialog by remember { mutableStateOf(false) }
    var isGeneratingPhoto by remember { mutableStateOf(false) }
    var generationStatus by remember { mutableStateOf("") }
    
    // Limit Dialogs
    var showRewardedAdDialog by remember { mutableStateOf(false) }
    var isLoadingAd by remember { mutableStateOf(false) }

    // Inventory State (Phase 3)
    var showInventoryDialog by remember { mutableStateOf(false) }
    var inventoryItems by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var isGiftSending by remember { mutableStateOf(false) }

    // Affection bubble animation
    var showAffectionBubble by remember { mutableStateOf(false) }
    var affectionPointsGained by remember { mutableStateOf(0) }

    fun loadInventory() {
        scope.launch {
            drawAIRepository.getInventory().onSuccess { items ->
                inventoryItems = items
            }.onFailure { e ->
                Log.e("Inventory", "Failed to load inventory", e)
            }
        }
    }

    fun sendGift(item: InventoryItem) {
        scope.launch {
            isGiftSending = true
            drawAIRepository.sendGift(characterId, item.id).onSuccess { response ->
                showInventoryDialog = false
                // Show feedback
                android.widget.Toast.makeText(context, response.message ?: "Gift sent!", android.widget.Toast.LENGTH_SHORT).show()
                
                // Update Affection Locally
                val pointsAdded = response.affectionAdded ?: item.affectionValue
                if (pointsAdded > 0) {
                     affectionPointsGained = pointsAdded
                     showAffectionBubble = true
                     
                     // Verify affection sync in background
                     // (Optional: fetch chat history or just update local object if we had write access)
                }
            }.onFailure { e ->
                android.widget.Toast.makeText(context, "Failed to send gift: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
            isGiftSending = false
        }
    }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showShopDialog by remember { mutableStateOf(false) }
    var showInsufficientGemsDialog by remember { mutableStateOf(false) }
    var showImageOptionsDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }


    if (showInsufficientGemsDialog) {
        AlertDialog(
            onDismissRequest = { showInsufficientGemsDialog = false },
            title = { Text(stringResource(R.string.gem_store_title)) },
            text = { Text("You need 250 Gems to unlock this feature.") },
            confirmButton = {
                TextButton(onClick = { showInsufficientGemsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Image Options Dialog - Full Screen Viewer with Set Profile Option
    if (showImageOptionsDialog && selectedImageUrl != null && character != null) {
        Dialog(
            onDismissRequest = { showImageOptionsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Close button (top-right)
                IconButton(
                    onClick = { showImageOptionsDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
                
                // Full-size image
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "Photo preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { /* Allow click to dismiss */ },
                    contentScale = ContentScale.Fit
                )
                
                // Bottom action bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${character!!.personality.name}'s Photo",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Set as Profile Button
                    Button(
                        onClick = {
                            scope.launch {
                                val result = drawAIRepository.updateCharacterProfileImage(
                                    characterId = characterId,
                                    imageUrl = selectedImageUrl!!
                                )
                                result.onSuccess { res ->
                                    // Update local character state with new image URL (both fields)
                                    character = character?.copy(
                                        imageUrl = selectedImageUrl!!,
                                        imageStorageUrl = selectedImageUrl!!, // Also update storage URL
                                        profileUpdatedAt = java.time.Instant.now().toString()
                                    )
                                    android.widget.Toast.makeText(
                                        context,
                                        "Profile photo updated! ✨",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    showImageOptionsDialog = false
                                }.onFailure { e ->
                                    android.widget.Toast.makeText(
                                        context,
                                        e.message ?: "Failed to update",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Set as Profile Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }


    if (showShopDialog) {
        com.doyouone.drawai.ui.components.ShopDialog(
            onDismiss = { showShopDialog = false }
        )
    }
    
    var showProfileImage by remember { mutableStateOf(false) }

    // Affection Points caching to prevent reset from stale Firestore data
    var cachedAffectionPoints by remember { mutableStateOf<Double?>(null) }
    var affectionCacheTimestamp by remember { mutableStateOf(0L) }
    
    // Limit Dialogs Logic
    if (showRewardedAdDialog) {
        RewardedAdDialog(
            onDismiss = { showRewardedAdDialog = false },
            onWatchAd = {
                isLoadingAd = true
                AdManager.showRewardedAd(
                    activity = context as android.app.Activity,
                    onUserEarnedReward = { _ ->
                        scope.launch {
                            drawAIRepository.addBonusGeneration(userId)
                            android.widget.Toast.makeText(context, "Bonus generation added!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onAdDismissed = {
                        isLoadingAd = false
                        showRewardedAdDialog = false
                    }
                )
            },
            isLoading = isLoadingAd
        )
    }

    if (showUpgradeDialog || showLimitSubscriptionDialog) {
        UpgradeDialog(
            onDismiss = { 
                showUpgradeDialog = false
                showLimitSubscriptionDialog = false 
            },
            onUpgrade = {
                showUpgradeDialog = false
                showLimitSubscriptionDialog = false
                onNavigateToSubscription()
            }
        )
    }
    
    if (showProfileImage && character != null) {
        val imageUrl = (if (!character!!.imageStorageUrl.isNullOrEmpty()) character!!.imageStorageUrl else character!!.imageUrl) ?: ""
        CharacterProfileViewer(
            imageUrl = imageUrl,
            onDismiss = { showProfileImage = false }
        )
    }

    val listState = rememberLazyListState()
    
    // Load character with real-time updates
    LaunchedEffect(characterId) {
        // Preload Interstitial Ad
        AdManager.loadInterstitialAd(context)

        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Subscribe to real-time character updates
                characterRepository.getCharacterFlow(characterId).collect { updatedCharacter ->
                    if (updatedCharacter != null) {
                        // Check if we have cached affection points (fresher than Firestore)
                        val now = System.currentTimeMillis()
                        val cacheValid = cachedAffectionPoints != null && (now - affectionCacheTimestamp) < 3000 // 3 seconds
                        
                        character = if (cacheValid) {
                            // Use cached affection points instead of Firestore value
                            updatedCharacter.copy(
                                relationship = updatedCharacter.relationship.copy(
                                    affectionPoints = cachedAffectionPoints!!
                                )
                            )
                        } else {
                            updatedCharacter
                        }
                        
                        if (isLoading) {
                            isLoading = false
                        }
                    } else if (!isLoading) {
                        // Character was deleted
                        errorMessage = "Character not found"
                    }
                }
            } catch (e: Exception) {
                errorMessage = ErrorUtils.getSafeErrorMessage(e)
                android.util.Log.e("CharacterChatScreen", "Error loading character", e)
                isLoading = false
            }
        }
        
        // Load chat history separately (one-time load)
        scope.launch {
            try {
                characterRepository.getChatHistory(characterId).onSuccess { history ->
                    // Explicitly sort by timestamp to ensure correct order (Oldest -> Newest)
                    messages = history.sortedBy { it.getTimestampString() }
                }
            } catch (e: Exception) {
                android.util.Log.e("CharacterChatScreen", "Error loading chat history", e)
            }
        }
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(isGeneratingPhoto) {
        if (isGeneratingPhoto) {
            // Wait a bit for layout to update
            kotlinx.coroutines.delay(100)
            listState.animateScrollToItem(messages.size)
        }
    }
    
    Scaffold(
        topBar = {
            if (character != null) {
                CharacterChatTopBar(
                    character = character!!,
                    gemCount = gemCount,
                    onBack = onBack,
                    onStatusClick = { showRelationshipDialog = true },
                    onShopClick = { showShopDialog = true },
                    onProfileClick = { showProfileImage = true }
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (character == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: "Character not found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Chat Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "👋",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Start chatting with ${character!!.personality.archetype}!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "They have ${character!!.personality.sinCount} deadly sin(s)",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    
                    items(messages) { message ->
                        ChatMessageBubble(
                            message = message,
                            characterImageUrl = if (!character!!.imageStorageUrl.isNullOrEmpty()) character!!.imageStorageUrl else character!!.imageUrl,
                            personality = character!!.personality,
                            onImageClick = { imageUrl ->
                                selectedImageUrl = imageUrl
                                showImageOptionsDialog = true
                            }
                        )
                    }

                    
                    if (isGeneratingPhoto) {
                        item {
                            TypingIndicator(
                                characterImageUrl = if (!character!!.imageStorageUrl.isNullOrEmpty()) character!!.imageStorageUrl else character!!.imageUrl,
                                relationshipStage = character!!.relationship.stage
                            )
                        }
                    }

                    if (isSending && !isSilentLoading) {
                        item {
                            TypingIndicator(
                                characterImageUrl = if (!character!!.imageStorageUrl.isNullOrEmpty()) character!!.imageStorageUrl else character!!.imageUrl,
                                relationshipStage = character!!.relationship.stage
                            )
                        }
                    }
                }
                
                // Affection Bubble Overlay
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Message Input

                ChatInputBar(
                    messageInput = messageInput,
                    onMessageChange = { messageInput = it },
                    onCameraClick = { showPhotoDialog = true },
                    onSend = {
                        if (messageInput.isNotBlank() && !isSending) {
                            // Check for rude words
                            if (containsRudeWords(messageInput)) {
                                showOffensiveContentDialog = true
                                return@ChatInputBar
                            }

                            val msgToSend = messageInput
                            val currentStage = character?.relationship?.stage ?: RelationshipStage.STRANGER
                            
                            // Optimistic update: Show user message immediately
                            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                            val tempUserMsg = CharacterMessage(
                                id = java.util.UUID.randomUUID().toString(),
                                characterId = characterId,
                                userId = "", // Not critical for UI
                                role = "user",
                                content = msgToSend,
                                timestamp = now,
                                relationshipStage = currentStage
                            )
                            messages = messages + tempUserMsg
                            messageInput = ""
                            
                            scope.launch {
                                isSending = true
                                try {
                                    characterRepository.sendMessage(characterId, msgToSend).onSuccess { response ->
                                        // Optimistic update: Show AI response immediately
                                        if (response.response != null) {
                                            val aiMsg = CharacterMessage(
                                                id = java.util.UUID.randomUUID().toString(),
                                                characterId = characterId,
                                                userId = "",
                                                role = "assistant",
                                                content = response.response,
                                                timestamp = now, // Approximate
                                                relationshipStage = RelationshipStage.fromString(response.relationship?.stage ?: "stranger")
                                            )
                                            messages = messages + aiMsg
                                        }
                                        
                                        // Sync history in background (silent)
                                        characterRepository.getChatHistory(characterId).onSuccess { history ->
                                            // Explicitly sort by timestamp to ensure correct order (Oldest -> Newest)
                                            messages = history.sortedBy { it.getTimestampString() }
                                        }
                                        
                                        // Cache affection points and show bubble
                                        val newAffectionPoints = response.relationship?.affectionPoints ?: 0.0
                                        val affectionChange = response.relationship?.affectionChange ?: 0.0
                                        
                                        if (affectionChange > 0) {
                                            cachedAffectionPoints = newAffectionPoints
                                            affectionCacheTimestamp = System.currentTimeMillis()
                                            affectionPointsGained = affectionChange.toInt()
                                            showAffectionBubble = true
                                        }
                                        
                                        // Update character relationship
                                        character = character?.copy(
                                            relationship = RelationshipStatus(
                                                stage = RelationshipStage.fromString(response.relationship?.stage ?: "stranger"),
                                                affectionPoints = newAffectionPoints,
                                                stageProgress = response.relationship?.stageProgress ?: 0,
                                                totalMessages = response.relationship?.totalMessages ?: 0,
                                                lastInteraction = now,
                                                lastChatDate = now,
                                                upgradeAvailable = response.relationship?.upgradeAvailable ?: false
                                            )
                                        )
                                        
                                        // Show stage change celebration
                                        if (response.relationship?.stageChanged == true) {
                                            newStage = RelationshipStage.fromString(response.relationship.newStage ?: "stranger")
                                            showStageChangeDialog = true
                                        }
                                    }.onFailure { e ->
                                        // If failed, remove the optimistic message (optional, or show error)
                                        errorDialogMessage = ErrorUtils.getSafeErrorMessage(e)
                                        showErrorDialog = true
                                        // Revert message list to remove the failed message
                                        messages = messages.filter { it.id != tempUserMsg.id }
                                        messageInput = msgToSend // Restore input
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("CharacterChatScreen", "Error sending message", e)
                                    errorDialogMessage = ErrorUtils.getSafeErrorMessage(e)
                                    showErrorDialog = true
                                    messages = messages.filter { it.id != tempUserMsg.id }
                                    messageInput = msgToSend
                                } finally {
                                    isSending = false
                                }
                            }
                        }
                    },
                    isSending = isSending,
                    onGiftClick = {
                        showInventoryDialog = true
                        loadInventory()
                    }
                )
                
                    // Affection Bubble Animation
                    if (showAffectionBubble) {
                        AffectionBubble(
                            pointsGained = affectionPointsGained,
                            onDismiss = { showAffectionBubble = false }
                        )
                    }
                }
            }
        }
    }
    
    // Stage Change Celebration Dialog
    if (showStageChangeDialog && newStage != null) {
        StageChangeDialog(
            oldStage = character?.relationship?.stage ?: RelationshipStage.STRANGER,
            newStage = newStage!!,
            onDismiss = { showStageChangeDialog = false }
        )
    }
    
    // Upgrade Relationship Dialog
    
    // Auto-show dialog if upgrade is available and not shown yet (optional)
    // For now, only show when button clicked or if we want to be proactive
    
    if (showUpgradeDialog && character != null) {
        UpgradeRelationshipDialog(
            currentStage = character!!.relationship.stage,
            onDismiss = { showUpgradeDialog = false },
            onConfirm = { message ->
                showUpgradeDialog = false
                // Send special upgrade message
                scope.launch {
                    isSilentLoading = true
                    isSending = true
                    try {
                        characterRepository.sendMessage(
                            characterId, 
                            message,
                            isUpgradeTrigger = true
                        ).onSuccess { response ->
                            // Handle success (refresh data)
                            // The normal flow will handle the response and stage change dialog
                            
                            // Update character relationship
                            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                            character = character?.copy(
                                relationship = RelationshipStatus(
                                    stage = RelationshipStage.fromString(response.relationship?.stage ?: "stranger"),
                                    stageProgress = response.relationship?.stageProgress ?: 0,
                                    affectionPoints = response.relationship?.affectionPoints ?: 0.0, // FIX: Update AP
                                    nextStageThreshold = response.relationship?.nextStageThreshold ?: 500, // FIX: Update Threshold
                                    totalMessages = response.relationship?.totalMessages ?: 0,
                                    lastInteraction = now,
                                    lastChatDate = now,
                                    upgradeAvailable = response.relationship?.upgradeAvailable ?: false
                                )
                            )
                            
                            if (response.relationship?.stageChanged == true) {
                                newStage = RelationshipStage.fromString(response.relationship.newStage ?: "stranger")
                                showStageChangeDialog = true
                            }
                            
                            // Refresh history
                            characterRepository.getChatHistory(characterId).onSuccess { history ->
                                messages = history.sortedBy { it.getTimestampString() }
                            }
                            
                        }.onFailure { e ->
                            errorDialogMessage = ErrorUtils.getSafeErrorMessage(e)
                            showErrorDialog = true
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CharacterChatScreen", "Error upgrading", e)
                    } finally {
                        isSending = false
                        isSilentLoading = false
                    }
                }
            }
        )
    }
    
    // Relationship Status Dialog (new compact popup)
    if (showRelationshipDialog && character != null) {
        RelationshipStatusDialog(
            character = character!!,
            onDismiss = { showRelationshipDialog = false },
            onUpgradeClick = {
                showRelationshipDialog = false
                showUpgradeDialog = true
            },
            onToggleNotification = { enable ->
                scope.launch {
                    val result = drawAIRepository.toggleNotification(characterId, enable)
                    result.onSuccess { res ->
                         character = character?.copy(
                             notificationEnabled = enable,
                             notificationUnlocked = (res.status == "unlocked") || character!!.notificationUnlocked
                         )
                         
                         // Update local preferences for NotificationReceiver
                         if (enable) {
                             appPreferences.addActiveReminderCharacter(character!!.personality.name)
                         } else {
                             appPreferences.removeActiveReminderCharacter(character!!.personality.name)
                         }
                         
                         android.widget.Toast.makeText(context, res.message ?: "Success", android.widget.Toast.LENGTH_SHORT).show()
                    }.onFailure { e ->
                        val msg = e.message ?: ""
                        if (msg.contains("Insufficient gems", ignoreCase = true)) {
                            showInsufficientGemsDialog = true
                        } else {
                            android.widget.Toast.makeText(context, e.message ?: "Failed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    // Offensive Content Dialog
    if (showOffensiveContentDialog) {
        AlertDialog(
            onDismissRequest = { showOffensiveContentDialog = false },
            title = { Text("Message Not Sent") },
            text = { Text("Your message contains inappropriate language. Please be respectful to the AI characters.") },
            confirmButton = {
                Button(
                    onClick = { showOffensiveContentDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("OK")
                }
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        )
    }

    // Generic Error Dialog
    if (showErrorDialog) {
        AnimeDrawErrorDialog(
            title = "Connection Error",
            message = errorDialogMessage,
            onDismiss = { showErrorDialog = false }
        )
    }

    // Inventory Dialog
    if (showInventoryDialog) {
        com.doyouone.drawai.ui.components.InventoryDialog(
            inventory = inventoryItems,
            onItemClick = { item ->
                sendGift(item)
            },
            onDismiss = { showInventoryDialog = false }
        )
    }

    // Photo Request Dialog
    if (showPhotoDialog && character != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        
        PhotoRequestDialog(
            characterName = character!!.personality.name,
            onDismiss = { showPhotoDialog = false },
            onConfirm = { mode, customPrompt, negativePrompt ->
                showPhotoDialog = false
                // isGeneratingPhoto = true // Removed to prevent blocking UI. Relies on Notification.
                // generationStatus = ...
                
                // Capture messages snapshot for context
                val currentMessages = messages.takeLast(6)
                
                val workerParams = workDataOf(
                    "characterId" to characterId,
                    "characterName" to character!!.personality.name,
                    "mode" to mode,
                    "userPrompt" to customPrompt,
                    "negativePrompt" to negativePrompt,
                    "seed" to (character!!.seed ?: 0L),
                    "appearancePrompt" to (character!!.prompt ?: ""),
                    // Logic to determine language (same as before)
                    "language" to run {
                        val prefs = context.getSharedPreferences("app_preferences", android.content.Context.MODE_PRIVATE)
                        val rawLang = prefs.getString("selected_language", "en") ?: "en"
                        if (rawLang == "in") "id" else rawLang
                    },
                    "chatContext" to currentMessages.joinToString("\n") { msg ->
                        val roleName = if (msg.role == "user") "User" else "Character"
                        "$roleName: ${msg.content}"
                    }
                )

                // Enqueue WorkManager
                val workRequest = OneTimeWorkRequestBuilder<PhotoGenerationWorker>()
                    .setInputData(workerParams)
                    .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "photo_gen_$characterId",
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
            }
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterChatTopBar(
    character: Character,
    gemCount: Int,
    onBack: () -> Unit,
    onStatusClick: () -> Unit,
    onShopClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    AnimeDrawTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Character Avatar
                AsyncImage(
                    model = if (!character.imageStorageUrl.isNullOrEmpty()) character.imageStorageUrl else character.imageUrl,
                    contentDescription = "Character",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onProfileClick)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Column {
                    Text(
                        text = character.personality.name.ifEmpty { character.personality.archetype },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${character.personality.rarity} • ${character.personality.sinCount} sins",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            // Gem Indicator
            // Gem Indicator
            com.doyouone.drawai.ui.components.GemIndicator(
                gemCount = gemCount, // Passed via closure from Screen
                modifier = Modifier.padding(end = 8.dp),
                onClick = onShopClick
            )

            // Relationship Status Badge
            Surface(
                onClick = onStatusClick,
                shape = RoundedCornerShape(12.dp),
                color = Color(character.relationship.stage.color).copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = character.relationship.stage.emoji,
                        fontSize = 14.sp
                    )
                    Text(
                        text = character.relationship.stage.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(character.relationship.stage.color)
                    )
                }
            }
        }
    )
}

@Composable
fun RelationshipStatusDialog(
    character: Character,
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit,
    onToggleNotification: (Boolean) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(character.relationship.stage.color).copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = if (!character.imageStorageUrl.isNullOrEmpty()) character.imageStorageUrl else character.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, Color(character.relationship.stage.color), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            // Sins Badge
                            if (character.personality.sinCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = 4.dp, y = 4.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                        .border(1.dp, Color.Red, CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${character.personality.sinCount} 😈",
                                        fontSize = 10.sp,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = character.personality.name.ifEmpty { character.personality.archetype },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${character.relationship.stage.emoji} ${character.relationship.stage.displayName}",
                                fontSize = 16.sp,
                                color = Color(character.relationship.stage.color),
                                fontWeight = FontWeight.Medium
                            )
                            
                            // Rarity Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        when (character.personality.rarity) {
                                            "Common" -> Color(0xFF9E9E9E)
                                            "Rare" -> Color(0xFF2196F3)
                                            "Epic" -> Color(0xFF9C27B0)
                                            "Legendary" -> Color(0xFFFF9800)
                                            else -> Color.Gray
                                        },
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = character.personality.rarity,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Tabs
                var selectedTab by remember { mutableIntStateOf(0) }
                val tabIcons = listOf(
                    Icons.Default.Favorite,      // Relationship
                    Icons.Default.Face,          // Status
                    Icons.Default.Person,        // Profile
                    Icons.Default.Assessment     // Stats
                )
                val tabLabels = listOf("Relationship", "Status", "Profile", "Stats")

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    tabIcons.forEachIndexed { index, icon ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tabLabels[index],
                                    tint = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        )
                    }
                }

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> RelationshipTab(character.relationship, onUpgradeClick)
                        1 -> StatusTab(character.emotionalState)
                        2 -> ProfileTab(character, onToggleNotification)
                        3 -> StatsTab(character.relationship, character.interactionPatterns)
                    }
                }
            }
        }
    }
}

@Composable
fun RelationshipTab(
    relationship: RelationshipStatus,
    onUpgradeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Big Stage Display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = relationship.stage.emoji,
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = relationship.stage.displayName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(relationship.stage.color)
            )
        }

        // Progress
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Affection Points", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    
                    if (relationship.stage == RelationshipStage.MARRIED) {
                        Text(
                            "MAX ❤️", 
                            fontSize = 14.sp, 
                            color = Color(0xFFE91E63),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "${relationship.affectionPoints.toInt()} / ${relationship.nextStageThreshold}", 
                            fontSize = 14.sp, 
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Calculate progress safely
                val progress = if (relationship.stage == RelationshipStage.MARRIED) {
                    1f
                } else if (relationship.nextStageThreshold > 0) {
                    (relationship.affectionPoints.toFloat() / relationship.nextStageThreshold).coerceIn(0f, 1f)
                } else 0f
                
                // Animated Progress Bar
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(1000, easing = FastOutSlowInEasing),
                    label = "ap_progress"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedProgress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(relationship.stage.color),
                                        Color(relationship.stage.color).copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (relationship.stage != RelationshipStage.MARRIED) {
                    val nextStage = when (relationship.stage) {
                        RelationshipStage.STRANGER -> RelationshipStage.FRIEND
                        RelationshipStage.FRIEND -> RelationshipStage.BEST_FRIEND
                        RelationshipStage.BEST_FRIEND -> RelationshipStage.ROMANTIC
                        RelationshipStage.ROMANTIC -> RelationshipStage.MARRIED
                        else -> RelationshipStage.MARRIED
                    }
                    val pointsNeeded = (relationship.nextStageThreshold - relationship.affectionPoints).toInt().coerceAtLeast(0)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pointsNeeded} AP to ${nextStage.displayName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Text(
                        text = "Soulmate Forever 💍",
                        fontSize = 12.sp,
                        color = Color(0xFFE91E63),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        RelationshipRoadmap(relationship.stage)

        // Upgrade Button
        if (relationship.upgradeAvailable) {
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (relationship.stage == RelationshipStage.BEST_FRIEND) "💌 Confess Feelings" else "💍 Propose Marriage",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RelationshipRoadmap(currentStage: RelationshipStage) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Relationship Roadmap",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RelationshipStage.values().forEach { stage ->
                    val isAchieved = stage.ordinal <= currentStage.ordinal
                    val isCurrent = stage == currentStage
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(if (isAchieved) 1f else 0.4f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) Color(stage.color).copy(alpha = 0.2f) 
                                    else Color.Transparent,
                                    CircleShape
                                )
                                .border(
                                    if (isCurrent) 2.dp else 0.dp,
                                    Color(stage.color),
                                    CircleShape
                                )
                        ) {
                            Text(stage.emoji, fontSize = 24.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = stage.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) Color(stage.color) else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    if (stage != RelationshipStage.values().last()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusTab(emotionalState: EmotionalState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mood Card
        StatusCard(title = "Current Mood", icon = "😊") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = emotionalState.currentMood.replaceFirstChar { it.uppercase() },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Energy Card
        StatusCard(title = "Energy Level", icon = "⚡") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${emotionalState.energyLevel}%", fontWeight = FontWeight.Bold)
                    Text(
                        text = if (emotionalState.energyLevel > 70) "High" else if (emotionalState.energyLevel > 30) "Normal" else "Tired",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Animated progress bar with better visibility
                val animatedProgress by animateFloatAsState(
                    targetValue = (emotionalState.energyLevel / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    label = "energy_progress"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = when {
                                        emotionalState.energyLevel > 70 -> listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                                        emotionalState.energyLevel > 30 -> listOf(Color(0xFFFFC107), Color(0xFFFFD54F))
                                        else -> listOf(Color(0xFFF44336), Color(0xFFEF5350))
                                    }
                                )
                            )
                    )
                }
            }
        }
    }

}

@Composable
fun ProfileTab(character: Character, onToggleNotification: (Boolean) -> Unit) {
    val personality = character.personality
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
         // Notification Settings
        StatusCard(title = "Morning Alarm", icon = "⏰") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Column(modifier = Modifier.weight(1f)) {
                     Text("Daily Greeting", fontWeight = FontWeight.Bold)
                     Text(
                         if (character.notificationUnlocked) "Receive daily messages from ${personality.name}" 
                         else "Unlock daily messages from ${personality.name} (250 ${stringResource(R.string.gem_store_title)})",
                         fontSize = 12.sp,
                         color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f)
                     )
                 }
                 
                 Switch(
                     checked = character.notificationEnabled,
                     onCheckedChange = onToggleNotification
                 )
            }
        }

        // Archetype
        StatusCard(title = "Archetype", icon = "🎭") {
            Text(personality.archetype, fontWeight = FontWeight.Bold)
        }

        // Traits
        if (personality.traits.isNotEmpty()) {
            StatusCard(title = "Traits", icon = "✨") {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    personality.traits.forEach { trait ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(trait, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // Interests
        if (personality.interests.isNotEmpty()) {
            StatusCard(title = "Interests", icon = "💖") {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    personality.interests.forEach { interest ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(interest, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        }
        
        // Sins
        StatusCard(title = "Deadly Sins", icon = "😈") {
             Text("${personality.sinCount} Sins (Hidden)", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
    }
}

@Composable
fun StatsTab(relationship: RelationshipStatus, patterns: InteractionPatterns) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatusCard(title = "Interaction Stats", icon = "📊") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRow("Total Messages", "${relationship.totalMessages}")
                StatRow("Last Chat", formatRelativeTime(relationship.getLastChatDateString()))
                StatRow("Chat Frequency", patterns.chatFrequency.replaceFirstChar { it.uppercase() })
            }
        }

        if (patterns.totalGhostsDetected > 0) {
            StatusCard(title = "Ghosting Record", icon = "👻", containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)) {
                Column {
                    Text(
                        text = "You have ghosted this character ${patterns.totalGhostsDetected} times.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    icon: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// Helper for relative time (simplified)
fun formatRelativeTime(isoString: String): String {
    if (isoString.isEmpty()) return "Never"
    try {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(isoString) ?: return "Unknown"
        val now = java.util.Date()
        val diff = now.time - date.time
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes mins ago"
            hours < 24 -> "$hours hours ago"
            days < 7 -> "$days days ago"
            else -> java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        return "Unknown"
    }
}

@Composable
fun ChatMessageBubble(
    message: CharacterMessage,
    characterImageUrl: String? = null,
    personality: CharacterPersonality,
    onImageClick: (String) -> Unit = {}
) {
    val isUser = message.role == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // AI Avatar
        if (!isUser && characterImageUrl != null) {
            AsyncImage(
                model = characterImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (!isUser) {
             Spacer(modifier = Modifier.width(36.dp))
        }



        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .background(
                    if (isUser) Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    ) else Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                    )
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Render Image Attachment if present
                if (message.imageUrl != null) {
                    var showFullImage by remember { mutableStateOf(false) }
                    
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Attachment",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp) // Fixed height for chat bubble images
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { 
                                if (!isUser) {
                                    // AI message - show set profile option
                                    onImageClick(message.imageUrl!!)
                                } else {
                                    // User message - just show full image
                                    showFullImage = true
                                }
                            },
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (showFullImage) {
                        Dialog(
                            onDismissRequest = { showFullImage = false },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.9f))
                                    .clickable { showFullImage = false },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = message.imageUrl,
                                    contentDescription = "Full Image",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                Text(
                    text = message.content,
                    fontSize = 15.sp,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(message.getTimestampString()),
                        fontSize = 10.sp,
                        color = if (isUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    if (!isUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = message.relationshipStage.emoji,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(
    characterImageUrl: String?,
    relationshipStage: RelationshipStage
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (characterImageUrl != null) {
            AsyncImage(
                model = characterImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
             Spacer(modifier = Modifier.width(36.dp))
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { index ->
                        val infiniteTransition = rememberInfiniteTransition(label = "dots")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, delayMillis = index * 200, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .alpha(alpha)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = relationshipStage.emoji,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    messageInput: String,
    onMessageChange: (String) -> Unit,
    onCameraClick: () -> Unit,
    onGiftClick: () -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    isGeneratingPhoto: Boolean = false
) {
    var showFeatureMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Feature Button (Combined)
            Box(
                modifier = Modifier.wrapContentSize(Alignment.TopStart)
            ) {
                 Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.5f))
                        .clickable(enabled = !isSending, onClick = { showFeatureMenu = true }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add, // Or GridView or Extension
                        contentDescription = "Features",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showFeatureMenu,
                    onDismissRequest = { showFeatureMenu = false },
                    modifier = Modifier.background(Color(0xFF2D2D44)) // Dark Purple BG
                ) {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                if (isGeneratingPhoto) "Generating..." else "Request Photo", 
                                color = if (isGeneratingPhoto) Color.Gray else Color.White
                            ) 
                        },
                        leadingIcon = { 
                            if (isGeneratingPhoto) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CameraAlt, contentDescription=null, tint=MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = {
                            if (!isGeneratingPhoto) {
                                showFeatureMenu = false
                                onCameraClick()
                            }
                        },
                        enabled = !isGeneratingPhoto
                    )
                    DropdownMenuItem(
                        text = { Text("Send Gift", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription=null, tint=Color(0xFFFF4081)) },
                        onClick = {
                            showFeatureMenu = false
                            onGiftClick()
                        }
                    )
                }
            }

            // Input Field
            TextField(
                value = messageInput,
                onValueChange = onMessageChange,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f), RoundedCornerShape(24.dp)),
                placeholder = { Text("Message...", fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                maxLines = 4,
                enabled = !isSending
            )
            
            // Send Button
            val canSend = messageInput.isNotBlank()
            // Always show sending color style OR just keep enabled logic but no spinner?
            // User requested: "loadingnya bukan di icon kirim kan? tetap dibuat animasi seolah ai character sedang mengirim chat"
            // So Send Button should look normal (maybe disabled) while sending.
            
            val buttonColor = if (canSend && !isSending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val iconColor = if (canSend && !isSending) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .clickable(
                        enabled = canSend && !isSending,
                        onClick = onSend
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = iconColor,
                    modifier = Modifier.size(20.dp).offset(x = 2.dp)
                )
            }
        }
    }
}

@Composable
fun StageChangeDialog(
    oldStage: RelationshipStage,
    newStage: RelationshipStage,
    onDismiss: () -> Unit
) {
    var showAnimation by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        showAnimation = false
        onDismiss()
    }
    
    if (showAnimation) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "celebration")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉",
                        fontSize = (48 * scale).sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Relationship Level Up!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${oldStage.emoji} ${oldStage.displayName}",
                            fontSize = 16.sp,
                            color = Color(oldStage.color)
                        )
                        
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            tint = Color.Gray
                        )
                        
                        Text(
                            text = "${newStage.emoji} ${newStage.displayName}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(newStage.color)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Your bond is growing stronger!",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: String): String {
    try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val date = sdf.parse(timestamp) ?: return "Just now"
        val time = date.time
        
        val now = System.currentTimeMillis()
        val diff = now - time
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> "${diff / 86400_000}d ago"
        }
    } catch (e: Exception) {
        return "Just now"
    }
}
@Composable
fun UpgradeRelationshipDialog(
    currentStage: RelationshipStage,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }
    val title = when (currentStage) {
        RelationshipStage.BEST_FRIEND -> "Confess Your Feelings? 💌"
        RelationshipStage.ROMANTIC -> "Propose Marriage? 💍"
        else -> "Upgrade Relationship"
    }
    val placeholder = when (currentStage) {
        RelationshipStage.BEST_FRIEND -> "Write a heartfelt confession..."
        RelationshipStage.ROMANTIC -> "Will you marry me? (Write your proposal)"
        else -> "Write a message..."
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("You and your character have become very close! Do you want to take this relationship to the next level?")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Your Message") },
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(message) },
                enabled = message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (currentStage == RelationshipStage.ROMANTIC) "Propose 💍" else "Confess 💌")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not yet")
            }
        }
    )
}

@Composable
fun AffectionBubble(
    pointsGained: Int,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }
    
    // Auto-dismiss after 2 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        visible = false
        kotlinx.coroutines.delay(300) // Wait for animation to finish
        onDismiss()
    }
    
    // Slide up and fade animation
    val offsetY by animateFloatAsState(
        targetValue = if (visible) -60f else -100f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "bubble_offset"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "bubble_alpha"
    )
    
    Box(
        modifier = Modifier
            .offset(y = offsetY.dp)
            .graphicsLayer(alpha = alpha)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE91E63).copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "+$pointsGained",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// List of very rude words to filter
private val rudeWords = listOf(
    "fuck", "shit", "bitch", "bastard", "asshole", "cunt", "dick", "cock", "pussy", "whore", "slut",
    "faggot", "nigger", "retard", "spic", "kike", "chink", "dyke", "tranny",
    "anjing", "babi", "bangsat", "kontol", "memek", "jembut", "ngentot", "lonte", "perek", "goblok", "tolol"
)

private fun containsRudeWords(text: String): Boolean {
    val lowerText = text.lowercase()
    return rudeWords.any { word ->
        // Simple containment check, can be improved with regex for whole words if needed
        // For now, simple check is effective for "very rude" words
        lowerText.contains(word)
    }
}

@Composable
fun PhotoRequestDialog(
    characterName: String,
    onDismiss: () -> Unit,
    onConfirm: (mode: String, prompt: String, negativePrompt: String) -> Unit
) {
    var mode by remember { mutableStateOf("auto") } // auto, custom
    var customPrompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Request a Photo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Ask $characterName to send you a selfie or photo!",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Mode Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Auto Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if(mode == "auto") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { mode = "auto" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Snap Moment",
                            color = if(mode == "auto") Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Custom Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if(mode == "custom") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { mode = "custom" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Custom Request",
                            color = if(mode == "custom") Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (mode == "custom") {
                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        label = { Text("Vision (What to see)") },
                        placeholder = { Text("e.g. wearing a blue dress, cooking...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = negativePrompt,
                        onValueChange = { negativePrompt = it },
                        label = { Text("Avoid") },
                        placeholder = { Text("e.g. bad hands, text, blurry...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                    
                    Text(
                        text = "We'll keep their appearance consistent automatically.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription=null, modifier=Modifier.size(16.dp), tint=MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Smart Context", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "The AI will decide the best selfie based on your current conversation context.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(mode, customPrompt, negativePrompt) },
                        enabled = mode == "auto" || customPrompt.isNotBlank()
                    ) {
                        Text("Request Photo")
                    }
                }
            }
        }
    }
}

@Composable
fun RewardedAdDialog(
    onDismiss: () -> Unit,
    onWatchAd: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                imageVector = Icons.Default.Diamond,
                contentDescription = null,
                tint = Color(0xFFE91E63),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Watch Ad for Bonus?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Watch a short video to get a free generation! Limit: 5 per day.",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onWatchAd,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Watch Ad")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UpgradeDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("💎", fontSize = 48.sp) },
        title = {
            Text(
                text = "Limit Reached",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "You have reached your daily generation limit. Upgrade to Pro for unlimited access!",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Upgrade Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
@Composable
fun CharacterProfileViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
             ZoomableImage(
                imageUrl = imageUrl,
                contentDescription = null,
                 onTap = { },
                 modifier = Modifier.fillMaxSize()
             )
             
             // Close button
             IconButton(
                 onClick = onDismiss,
                 modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
             ) {
                 Text("✕", color = Color.White, fontSize = 24.sp)
             }
        }
    }
}
