package com.doyouone.drawai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.doyouone.drawai.data.api.RetrofitClient
import com.doyouone.drawai.data.model.PromptTemplates
import com.doyouone.drawai.util.DummyData
import com.doyouone.drawai.ui.theme.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.doyouone.drawai.ui.components.showcaseTarget
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType


@Composable
fun LoadingModal(
    uiState: com.doyouone.drawai.viewmodel.GenerateUiState,
    generationProgress: Float
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Get queue info from taskStatus if available
                val taskStatus = when (uiState) {
                    is com.doyouone.drawai.viewmodel.GenerateUiState.Processing -> uiState.taskStatus
                    else -> null
                }
                
                // Check if task is queued
                val isQueued = taskStatus?.status == "queued"
                val queuePosition = taskStatus?.queuePosition
                val queueTotal = taskStatus?.queueTotal
                val queueInfo = taskStatus?.queueInfo
                
                // DEBUG: Log queue information
                android.util.Log.d("LoadingModal", "=== QUEUE DEBUG ===")
                android.util.Log.d("LoadingModal", "taskStatus: $taskStatus")
                android.util.Log.d("LoadingModal", "status: ${taskStatus?.status}")
                android.util.Log.d("LoadingModal", "isQueued: $isQueued")
                android.util.Log.d("LoadingModal", "queuePosition: $queuePosition")
                android.util.Log.d("LoadingModal", "queueTotal: $queueTotal")
                android.util.Log.d("LoadingModal", "queueInfo: $queueInfo")
                android.util.Log.d("LoadingModal", "==================")
                
                val title = when {
                    isQueued -> "Queued"
                    uiState is com.doyouone.drawai.viewmodel.GenerateUiState.Loading -> "Preparing..."
                    uiState is com.doyouone.drawai.viewmodel.GenerateUiState.Processing -> "Processing"
                    else -> "Processing..."
                }
                
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val detail = when {
                    isQueued -> "Your request is in queue, please wait."
                    uiState is com.doyouone.drawai.viewmodel.GenerateUiState.Loading -> "Connecting to server..."
                    uiState is com.doyouone.drawai.viewmodel.GenerateUiState.Processing -> "Please wait, image is being generated."
                    else -> ""
                }
                
                if (detail.isNotEmpty()) {
                    Text(
                        detail,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
                
                // ALWAYS show status info below detail message
                Spacer(modifier = Modifier.height(8.dp))
                
                val statusInfo = when {
                    // If position 1 of 1, treat as processing (first in line, being processed)
                    isQueued && queuePosition == 1 && queueTotal == 1 -> {
                        "Processing your request now..."
                    }
                    // If queued with position > 1, show queue position  
                    isQueued && queuePosition != null && queuePosition > 1 -> {
                        queueInfo ?: "Position $queuePosition of $queueTotal"
                    }
                    // If processing, show processing status
                    taskStatus?.status == "processing" -> {
                        "Processing your request now..."
                    }
                    // Default
                    else -> "Initializing..."
                }
                
                android.util.Log.d("LoadingModal", "STATUS INFO: $statusInfo")
                
                Text(
                    statusInfo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )


            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    workflowId: String,
    onGenerateComplete: () -> Unit,
    onBackPressed: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onLinkGoogleAccount: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToVision: (String) -> Unit = {},
    vision: String? = null,
    avoid: String? = null,
    currentRoute: String = "generate",
    viewModel: com.doyouone.drawai.viewmodel.GenerateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val userPreferences = remember { com.doyouone.drawai.data.preferences.UserPreferences(context) }
    
    val isPremium by userPreferences.isPremium.collectAsState(initial = false)
    
    // NEW: Use Firebase generation_limits from ViewModel
    val generationLimit by viewModel.generationLimit.collectAsState()
    
    // Load generation limit on screen start
    LaunchedEffect(Unit) {
        viewModel.loadGenerationLimit()
    }
    // Refresh limit every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            viewModel.loadGenerationLimit()
        }
    }
    
    // Check if user has subscription (basic or pro) from Firebase
    val hasSubscription = remember(generationLimit) {
        val subscriptionType = generationLimit?.subscriptionType ?: "free"
        subscriptionType == "basic" || subscriptionType == "pro" || isPremium
    }
    
    var showRewardedAdDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var showGuestBindDialog by remember { mutableStateOf(false) }
    var showContentModerationDialog by remember { mutableStateOf(false) }
    var isLoadingAd by remember { mutableStateOf(false) }
    
    // Check if user is guest/anonymous
    val authManager = remember { com.doyouone.drawai.auth.AuthManager(context) }
    val currentUser by authManager.currentUser.collectAsState()
    val isGuestUser = currentUser?.isAnonymous == true
    
    var positivePrompt by remember { mutableStateOf(vision ?: "") }
    var negativePrompt by remember { mutableStateOf(avoid ?: "") }
    var seedInput by remember { mutableStateOf("1062314217360759") } // Default seed set to 1062314217360759
    
    // Update prompts when vision/avoid parameters change
    LaunchedEffect(vision, avoid) {
        positivePrompt = vision ?: ""
        negativePrompt = avoid ?: ""
    }
    
    // NEW: Load pre-filled data from Gallery "Use Again" button
    LaunchedEffect(workflowId) {
        try {
            val sharedPrefs = context.getSharedPreferences("generate_prefs", android.content.Context.MODE_PRIVATE)
            if (sharedPrefs.getBoolean("has_prefilled_data", false)) {
                val prefilledPrompt = sharedPrefs.getString("prefilled_prompt", "") ?: ""
                val prefilledAvoid = sharedPrefs.getString("prefilled_avoid", "") ?: ""
                val prefilledWorkflow = sharedPrefs.getString("prefilled_workflow", "") ?: ""
                val prefilledSeed = sharedPrefs.getString("prefilled_seed", "") ?: ""
                
                // Only apply if workflow matches or if vision/avoid params are empty
                if ((prefilledWorkflow == workflowId || prefilledWorkflow.isEmpty()) && 
                    vision.isNullOrEmpty() && avoid.isNullOrEmpty()) {
                    positivePrompt = prefilledPrompt
                    negativePrompt = prefilledAvoid
                    seedInput = prefilledSeed
                    
                    android.util.Log.d("GenerateScreen", "✨ Pre-filled from Gallery: prompt='$prefilledPrompt', seed='$prefilledSeed'")
                }
                
                // Clear the flag after reading
                sharedPrefs.edit().putBoolean("has_prefilled_data", false).apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("GenerateScreen", "Error reading prefilled data: ${e.message}")
        }
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle Maintenance (Error 530)
    if (uiState is com.doyouone.drawai.viewmodel.GenerateUiState.Maintenance) {
         com.doyouone.drawai.ui.components.MaintenancePopup(
             onGoToGalleryClick = onNavigateToGallery,
             onRetry = {
                 viewModel.resetState()
             }
         )
    }
    
    val workflowsMap by viewModel.workflows.collectAsState()
    
    // Get workflow info from API
    val workflow = remember(workflowsMap, workflowId) {
        workflowsMap[workflowId]?.let { info ->
            com.doyouone.drawai.data.model.Workflow(
                id = workflowId,
                name = info.name,
                description = info.description,
                estimatedTime = info.estimatedTime,
                category = if (workflowId.contains("anime")) "Anime" else "General"
            )
        }
    }
    // Premium workflow detection and lock state
    val isPremiumWorkflow = remember(workflowsMap, workflowId) {
        workflowsMap[workflowId]?.isPremium ?: (workflowId == "anime_premium_ultra")
    }
    val isLocked = isPremiumWorkflow && !hasSubscription
    var showUpgradeDialog by remember { mutableStateOf(false) }
    
    // SECURITY FIX: Always re-check premium lock when returning to screen
    // This prevents bypass when user goes to subscription and comes back
    LaunchedEffect(isPremiumWorkflow, hasSubscription, generationLimit) {
        // CRITICAL: Only show upgrade dialog for FREE users on premium workflows
        // Do NOT show for basic/pro users who already have subscription
        if (isPremiumWorkflow && !hasSubscription) {
            showUpgradeDialog = true
        } else if (hasSubscription) {
            showUpgradeDialog = false
        }
    }
    // Screen-level upgrade dialog to block premium workflows for FREE users
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { }, // SECURITY: Prevent dismissal by clicking background
            icon = {
                Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(text = "🔒", fontSize = 24.sp) }
            },
            title = { Text(text = "⭐ Premium Workflow", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary) },
            text = {
                Column {
                    Text("This workflow is available for subscribers only.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Upgrade to unlock:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("✨ Premium workflows", fontSize = 13.sp)
                    Text("📈 More generations", fontSize = 13.sp)
                    Text("🚫 No advertisements", fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUpgradeDialog = false
                        onNavigateToSubscription()
                    }
                ) {
                    Text("Upgrade Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showUpgradeDialog = false
                    onNavigateToHome()
                }) { Text("Back") }
            }
        )
    }
    
    // Handle generation state
    val isGenerating = uiState is com.doyouone.drawai.viewmodel.GenerateUiState.Loading || 
                       uiState is com.doyouone.drawai.viewmodel.GenerateUiState.Processing
    
    val generationProgress = when (val state = uiState) {
        is com.doyouone.drawai.viewmodel.GenerateUiState.Processing -> 0.5f
        is com.doyouone.drawai.viewmodel.GenerateUiState.Success -> 1.0f
        else -> 0f
    }
    
    // Handle success
    LaunchedEffect(uiState) {
        if (uiState is com.doyouone.drawai.viewmodel.GenerateUiState.Success) {
            kotlinx.coroutines.delay(1000)
            onGenerateComplete()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image dari workflow yang dipilih - Secure loading
        SubcomposeAsyncImage(
            model = "${RetrofitClient.getBaseUrl()}workflow-image/${workflowId}",
            contentDescription = "Workflow Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f,
            loading = {
                // Silent loading - just show background color
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            },
            error = {
                // Silent error - just show background color
                // NO error messages exposed to user
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        )
        
        // Overlay untuk readability (60% opacity) - support dark mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                        )
                    )
                )
        )
    
    // Tutorial Auto-Scroll Logic
    LaunchedEffect(com.doyouone.drawai.ui.components.ShowcaseManager.step) {
        if (com.doyouone.drawai.ui.components.ShowcaseManager.step == 3) {
            // Scroll to bottom to show Generate button
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.ime) // Handle keyboard
    ) {
        // Content Area (Takes remaining space)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Compact Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Compact Back Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onNavigateToHome() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "←",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = workflow?.name ?: "Generate",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            // Pro Badge
                             if (isLocked) {
                                 Spacer(modifier = Modifier.width(6.dp))
                                 Text(
                                     text = "PRO",
                                     fontSize = 10.sp,
                                     fontWeight = FontWeight.Bold,
                                     color = MaterialTheme.colorScheme.tertiary,
                                     modifier = Modifier
                                         .background(MaterialTheme.colorScheme.tertiary.copy(alpha=0.15f), RoundedCornerShape(4.dp))
                                         .padding(horizontal=4.dp, vertical=1.dp)
                                 )
                            }
                        }
                        Text(
                            text = "${workflow?.category ?: "General"} • ${workflow?.estimatedTime ?: "15s"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Compact Limit Area
                Row(verticalAlignment = Alignment.CenterVertically) {
                    generationLimit?.let { limit ->
                        // Watch Ad Button (Always visible for Free users who can earn bonus)
                        if (limit.subscriptionType == "free" && limit.canWatchMoreAds()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.tertiary,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clickable { showRewardedAdDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🎬", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+1",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onTertiary
                                    )
                                }
                            }
                        }
                        
                        // Limit Badge
                        val remaining = limit.getRemainingGenerations()
                        val isLow = remaining <= 0
                        val statusColor = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            onClick = { 
                                if (limit.canWatchMoreAds() && isLow) showRewardedAdDialog = true 
                                else onNavigateToSubscription() 
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (limit.subscriptionType == "free") "⚡" else "👑", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$remaining", 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 14.sp,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }
            }
            
            // Positive Prompt
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(com.doyouone.drawai.R.string.generate_describe_vision),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                     Text(
                        text = stringResource(com.doyouone.drawai.R.string.generate_suggest_more),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onNavigateToVision(workflowId) }
                            .padding(start = 8.dp)
                    )
                }

                // Tutorial: Auto-advance if prompt is pre-filled
                LaunchedEffect(positivePrompt) {
                     if (positivePrompt.contains("vision", ignoreCase = true) && 
                         com.doyouone.drawai.ui.components.ShowcaseManager.step == 2) {
                          com.doyouone.drawai.ui.components.ShowcaseManager.nextStep(context)
                          keyboardController?.hide()
                     }
                }

                OutlinedTextField(
                    value = positivePrompt,
                    onValueChange = { 
                        positivePrompt = it
                        // Tutorial Logic
                        if (it.contains("vision", ignoreCase = true) && 
                            com.doyouone.drawai.ui.components.ShowcaseManager.step == 2) {
                             com.doyouone.drawai.ui.components.ShowcaseManager.nextStep(context)
                             keyboardController?.hide()
                        }
                    },
                    placeholder = { 
                        Text(
                            text = stringResource(com.doyouone.drawai.R.string.generate_prompt_placeholder),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 180.dp) // Fixed height range
                        .then(Modifier.showcaseTarget("prompt_input")),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    maxLines = 5
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PromptTemplates.getSuggestionsForWorkflow(workflowId)) { suggestion ->
                        SuggestionChip(
                            onClick = {
                                positivePrompt = if (positivePrompt.isBlank()) suggestion else "$positivePrompt, $suggestion"
                            },
                            label = { Text(suggestion, fontSize = 10.sp) }, // Smaller text
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Negative Prompt
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(com.doyouone.drawai.R.string.generate_things_to_avoid) + " (Optional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = negativePrompt,
                    onValueChange = { negativePrompt = it },
                    placeholder = { 
                        Text(
                            text = stringResource(com.doyouone.drawai.R.string.generate_avoid_placeholder),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 140.dp), // Fixed height range
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.error,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    maxLines = 4
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PromptTemplates.commonNegatives.take(5)) { suggestion ->
                        SuggestionChip(
                            onClick = {
                                negativePrompt = if (negativePrompt.isBlank()) suggestion else "$negativePrompt, $suggestion"
                            },
                            label = { Text(suggestion, fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                                labelColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Seed Input (Optional)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Seed (Optional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = seedInput,
                    onValueChange = { 
                        // Only allow numeric input
                        if (it.all { char -> char.isDigit() }) {
                            seedInput = it 
                        }
                    },
                    placeholder = { 
                        Text(
                            text = "Random (-1) or specific seed",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        } // End of Weighted Column

        
        // Generate Button
        Button(
            onClick = {
                // Tutorial Logic: End
                if (com.doyouone.drawai.ui.components.ShowcaseManager.step > 0) {
                    com.doyouone.drawai.ui.components.ShowcaseManager.endTutorial(context)
                    scope.launch {
                        com.doyouone.drawai.data.preferences.ThemePreferences(context).setTutorialCompleted()
                    }
                }
                
                // Block FREE users on premium workflow
                if (isLocked) {
                    showUpgradeDialog = true
                    return@Button
                }
                
                // Check if user is guest
                if (isGuestUser) {
                    showGuestBindDialog = true
                    return@Button
                }
                
                if (positivePrompt.isNotBlank() && !isGenerating) {
                    // NEW: ViewModel handles limit checking automatically via Firebase
                    // It will throw GenerationLimitExceededException if limit reached
                    
                    // Parse seed input
                    // If empty, use random (override hardcoded default by passing random)
                    // If not empty, use provided seed
                    val seedToUse = if (seedInput.isBlank()) {
                         kotlin.random.Random.nextLong(1, 999999999999999) // Random seed if field is empty
                    } else {
                        seedInput.toLongOrNull() ?: 1062314217360759L // Use input or fallback
                    }
                    
                    viewModel.generateImage(
                        positivePrompt = positivePrompt,
                        negativePrompt = negativePrompt,
                        workflow = workflowId,
                        seed = seedToUse
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp) // Regular bottom padding
                .windowInsetsPadding(WindowInsets.navigationBars) // Handle navbar
                .height(56.dp)
                .then(Modifier.showcaseTarget("generate_button")),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            enabled = !isGenerating && positivePrompt.isNotBlank(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isGenerating || positivePrompt.isBlank()) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Generating...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Text(
                        text = "Generate Image",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        
        
        
        
        // Limit Exceeded Display - Check if show ad or subscription
        if (uiState is com.doyouone.drawai.viewmodel.GenerateUiState.LimitExceeded) {
            // Auto-trigger dialog based on bonus status
            LaunchedEffect(uiState) {
                generationLimit?.let { limit ->
                    if (limit.canWatchMoreAds()) {
                        // Show rewarded ad dialog
                        showRewardedAdDialog = true
                    } else {
                        // Already watched 2 ads, show subscription
                        showSubscriptionDialog = true
                    }
                }
            }
        }
        
        // Content Moderation Display - Auto-trigger dialog
        if (uiState is com.doyouone.drawai.viewmodel.GenerateUiState.ContentModeration) {
            LaunchedEffect(uiState) {
                showContentModerationDialog = true
            }
        }
        

        
        // Bottom spacing
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Modal loading dialog overlay (blocks background clicks)
    if (isGenerating) {
        LoadingModal(
            uiState = uiState,
            generationProgress = generationProgress
        )
    }
    
    // Rewarded Ad Dialog (NEW)
    if (showRewardedAdDialog) {
        AlertDialog(
            onDismissRequest = { 
                showRewardedAdDialog = false
                isLoadingAd = false
                viewModel.resetState()
            },
            icon = { 
                if (isLoadingAd) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                } else {
                    Text("🎬", fontSize = 48.sp)
                }
            },
            title = { 
                Text(if (isLoadingAd) "Loading Ad..." else "Watch Ad for +1 Generation") 
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isLoadingAd) {
                        Text("Please wait, preparing ad...")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This may take a few seconds",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        Text("Watch a short ad to get +1 extra generation!")
                        Spacer(modifier = Modifier.height(8.dp))
                        generationLimit?.let {
                            val maxBonus = if (it.subscriptionType == "free") "50" else "∞"
                            Text(
                                "Bonus: ${it.bonusGenerations}/$maxBonus from ads",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (activity != null) {
                            // Check if ad is ready
                            if (com.doyouone.drawai.ads.AdManager.isRewardedAdReady()) {
                                showRewardedAdDialog = false
                                
                                // Show ad immediately
                                com.doyouone.drawai.ads.AdManager.showRewardedAd(
                                    activity = activity,
                                    onUserEarnedReward = { rewardAmount ->
                                        scope.launch {
                                            // Add bonus generation in Firebase
                                            val success = viewModel.addBonusGeneration()
                                            if (success) {
                                                // Refresh UI
                                                viewModel.loadGenerationLimit()
                                                viewModel.resetState()
                                            }
                                        }
                                    },
                                    onAdDismissed = {
                                        isLoadingAd = false
                                        viewModel.resetState()
                                    }
                                )
                            } else {
                                // Ad not ready, load it first
                                isLoadingAd = true
                                
                                com.doyouone.drawai.ads.AdManager.loadRewardedAd(context) {
                                    isLoadingAd = false
                                    showRewardedAdDialog = false
                                    
                                    // Show ad after loading
                                    com.doyouone.drawai.ads.AdManager.showRewardedAd(
                                        activity = activity,
                                        onUserEarnedReward = { rewardAmount ->
                                            scope.launch {
                                                val success = viewModel.addBonusGeneration()
                                                if (success) {
                                                    viewModel.loadGenerationLimit()
                                                    viewModel.resetState()
                                                }
                                            }
                                        },
                                        onAdDismissed = {
                                            viewModel.resetState()
                                        }
                                    )
                                }
                            }
                        }
                    },
                    enabled = !isLoadingAd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    if (isLoadingAd) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onTertiary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Watch Ad")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRewardedAdDialog = false
                        isLoadingAd = false
                        viewModel.resetState()
                    },
                    enabled = !isLoadingAd
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Guest User Bind Dialog (when guest tries to generate)
    if (showGuestBindDialog) {
        AlertDialog(
            onDismissRequest = {
                showGuestBindDialog = false
            },
            icon = { Text("🔒", fontSize = 48.sp) },
            title = { 
                Text(
                    "Sign In Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ) 
            },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Guest users cannot generate images.",
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Please sign in with your Google account to start creating amazing AI artwork!",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Benefits
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "With Google Account:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("✨ Generate AI images", fontSize = 13.sp)
                            Text("💾 Save your creations", fontSize = 13.sp)
                            Text("📊 Track your usage", fontSize = 13.sp)
                            Text("🎁 Access premium features", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGuestBindDialog = false
                        // Call the callback to trigger Google Sign-In in MainActivity
                        onLinkGoogleAccount()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🔗", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Link Google Account",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGuestBindDialog = false
                    }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        )
    }
    
    // Subscription Dialog (when reached max ads or user chooses to upgrade)
    if (showSubscriptionDialog) {
        AlertDialog(
            onDismissRequest = {
                showSubscriptionDialog = false
                viewModel.resetState()
            },
            icon = { Text("👑", fontSize = 48.sp) },
            title = { Text("Choose Your Plan") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Unlock unlimited creativity!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Choose a plan that fits your needs:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Basic Plan
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💎", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Basic Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Text("Rp 29.000", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("✨ 200 generations", fontSize = 12.sp)
                            Text("📅 Valid for 30 days", fontSize = 12.sp)
                            Text("🚀 No ads", fontSize = 12.sp)
                            Button(
                                onClick = {
                                    showSubscriptionDialog = false
                                    // TODO: Implement proper subscription purchase
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("Get Basic")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Pro Plan
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⭐", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Pro Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Best Value!", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Rp 79.000", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("✨ 600 generations", fontSize = 12.sp)
                            Text("📅 Valid for 30 days", fontSize = 12.sp)
                            Text("🚀 No ads", fontSize = 12.sp)
                            Text("💎 3x more than Basic", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {
                                    showSubscriptionDialog = false
                                    // TODO: Implement proper subscription purchase
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Get Pro")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showSubscriptionDialog = false
                    viewModel.resetState()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Content Moderation Dialog - Shows inappropriate words in a user-friendly popup
    if (showContentModerationDialog && uiState is com.doyouone.drawai.viewmodel.GenerateUiState.ContentModeration) {
        val moderationState = uiState as com.doyouone.drawai.viewmodel.GenerateUiState.ContentModeration
        
        AlertDialog(
            onDismissRequest = {
                showContentModerationDialog = false
                viewModel.resetState()
            },
            icon = { 
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) { 
                    Text("⚠️", fontSize = 24.sp) 
                }
            },
            title = { 
                Text(
                    text = stringResource(com.doyouone.drawai.R.string.content_moderation_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(
                        text = moderationState.messageResId?.let { stringResource(it) } ?: moderationState.message,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Show inappropriate words from positive prompt
                    if (moderationState.positiveInappropriateWords.isNotEmpty()) {
                        Text(
                            text = stringResource(com.doyouone.drawai.R.string.content_moderation_positive_words),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = moderationState.positiveInappropriateWords.joinToString(", "),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Show inappropriate words from negative prompt
                    if (moderationState.negativeInappropriateWords.isNotEmpty()) {
                        Text(
                            text = stringResource(com.doyouone.drawai.R.string.content_moderation_negative_words),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = moderationState.negativeInappropriateWords.joinToString(", "),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showContentModerationDialog = false
                        viewModel.resetState()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(com.doyouone.drawai.R.string.content_moderation_okay))
                }
            }
        )
    }
    }
    
    // Error Dialog (NEW: Replaces inline error card)
    if (uiState is com.doyouone.drawai.viewmodel.GenerateUiState.Error) {
        val errorState = uiState as com.doyouone.drawai.viewmodel.GenerateUiState.Error
        val errorMessage = errorState.messageResId?.let { stringResource(it) } 
            ?: if(errorState.message.isNotBlank()) errorState.message else stringResource(com.doyouone.drawai.R.string.error_generic_message)
            
        val isMaintenance = errorMessage.contains("530") || errorMessage.contains("Maintenance", ignoreCase = true)
        
        if (isMaintenance) {
            com.doyouone.drawai.ui.components.MaintenancePopup(
                onGoToGalleryClick = {
                    viewModel.resetState()
                    onNavigateToGallery()
                },
                onRetry = {
                    viewModel.resetState()
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { viewModel.resetState() },
                icon = { 
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) { 
                        Text("⚠️", fontSize = 24.sp) 
                    }
                },
                title = { 
                    Text(
                        text = stringResource(com.doyouone.drawai.R.string.error_title), 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                text = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetState() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(com.doyouone.drawai.R.string.error_retry))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            viewModel.resetState()
                            onNavigateToHome()
                        }
                    ) {
                        Text(stringResource(com.doyouone.drawai.R.string.error_back))
                    }
                }
            )
        }
    }
    
} // End of main Box and GenerateScreen function
