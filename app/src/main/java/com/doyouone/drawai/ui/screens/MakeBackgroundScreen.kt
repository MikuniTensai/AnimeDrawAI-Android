package com.doyouone.drawai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import android.graphics.BitmapFactory
import java.io.File
import com.doyouone.drawai.ui.components.ProcessingModal

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.doyouone.drawai.R
import com.doyouone.drawai.ui.components.AnimeDrawMainTopBar
import com.doyouone.drawai.data.repository.SubscriptionRepository
import com.doyouone.drawai.data.model.SubscriptionPlan
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlin.random.Random
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeBackgroundScreen(
    onOpenDrawer: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    // State
    var visionPrompt by remember { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf("1280x720") }
    var seed by remember { mutableLongStateOf(799753484426013L) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedImageUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    var blockedWord by remember { mutableStateOf("") }
    
    val sizeOptions = listOf(
        "1024x1024" to "Square (1:1)",
        "1280x720" to "Landscape (16:9)"
    )
    
    // Load Interstitial Ad
    LaunchedEffect(Unit) {
        com.doyouone.drawai.ads.AdManager.loadInterstitialAd(context)
    }
    
    var expandedSizeMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Custom Header (Consistent Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp), // Reduced from 24.dp
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "←",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Make Background",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "AI Tools • Scene Generator",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced from 24.dp
        ) {
            // Hero Image / Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                AsyncImage(
                    model = "https://drawai-api.drawai.site/workflow-image/make_background_v1",
                    contentDescription = "Make Background Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Description
            Text(
                text = "Generate beautiful anime-style backgrounds without characters. Perfect for wallpapers, game assets, or creative projects.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 20.sp
            )
            

            
            // Configuration
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().animateContentSize()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(Modifier.height(24.dp))

                    // Vision Input
                    Text("Vision Request", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = visionPrompt,
                        onValueChange = { visionPrompt = it },
                        label = { Text("What scene to generate?") },
                        placeholder = { Text("e.g. A cyberpunk city street at night, neon lights") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Canvas Size
                    Text("Canvas Size", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedSizeMenu,
                        onExpandedChange = { expandedSizeMenu = !expandedSizeMenu }
                    ) {
                        OutlinedTextField(
                            value = sizeOptions.find { it.first == selectedSize }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Aspect Ratio") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSizeMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedSizeMenu,
                            onDismissRequest = { expandedSizeMenu = false }
                        ) {
                            sizeOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedSize = value
                                        expandedSizeMenu = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Seed
                    Text("Seed (Optional)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = seed.toString(),
                            onValueChange = { 
                                seed = it.toLongOrNull() ?: seed
                            },
                            label = { Text("Random Seed") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        IconButton(
                            onClick = { seed = Random.nextLong(100000000000000L, 999999999999999L) },
                            modifier = Modifier
                                .size(50.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Random seed",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Generate Button (Hidden when generating)
            if (!isGenerating) {
                Button(
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            errorMessage = null
                            
                            // 1. NSFW / Safety Filter (Client-Side)
                            val nsfwKeywords = listOf(
                                "nsfw", "nude", "naked", "sex", "porn", "adult", "mature", "explicit",
                                "lewd", "waifu", "hentai", "ecchi", "loli", "shota", "undress",
                                "n4k3d", "s3x", "p0rn", "fuck", "dick", "vagina", "pussy", "breast"
                            )
                            
                            val promptToCheck = visionPrompt.lowercase()
                            val foundKeyword = nsfwKeywords.firstOrNull { promptToCheck.contains(it) }
                            
                            if (foundKeyword != null) {
                                 val userId = FirebaseAuth.getInstance().currentUser?.uid
                                 if (userId != null) {
                                     try {
                                         val repo = SubscriptionRepository(userId)
                                         val subscription = repo.getSubscription().first()
                                         
                                         // REQUIRE 'moreAccess' flag for NSFW (Strict Play Store Policy)
                                         if (!subscription.moreAccess) {
                                             blockedWord = foundKeyword
                                             showBlockedDialog = true
                                             isGenerating = false
                                             return@launch
                                         }
                                     } catch(e: Exception) {
                                         errorMessage = "Verification failed: ${e.message}"
                                         isGenerating = false
                                         return@launch
                                     }
                                 } else {
                                     errorMessage = "Please login to generate content."
                                     isGenerating = false
                                     return@launch
                                 }
                            }
                            
                            try {
                                val apiService = com.doyouone.drawai.data.api.RetrofitClient.apiService
                                
                                // Prepend base prompt
                                val basePrompt = "((masterpiece, best quality, anime background))"
                                val finalPrompt = if (visionPrompt.isNotBlank()) {
                                    "$basePrompt, $visionPrompt"
                                } else {
                                    basePrompt
                                }
                                
                                // Parse size
                                val dimensions = selectedSize.split("x")
                                val width = dimensions.getOrNull(0)?.toIntOrNull() ?: 1280
                                val height = dimensions.getOrNull(1)?.toIntOrNull() ?: 720
                                
                                android.util.Log.d("MakeBackground", "Sending Request - Size: ${width}x${height}, Seed: ${seed}")
                                
                                val request = com.doyouone.drawai.data.model.GenerateRequest(
                                    positivePrompt = finalPrompt,
                                    negativePrompt = "",
                                    workflow = "make_background_v1",
                                    width = width,
                                    height = height,
                                    seed = seed
                                )
                                
                                val response = apiService.generateImage(request)
                                
                                if (!response.isSuccessful) {
                                    throw Exception("Server error: ${response.code()}")
                                }
                                
                                val taskId = response.body()?.taskId
                                    ?: throw Exception("No task ID received")
                                
                                var isComplete = false
                                var attempts = 0
                                
                                while (!isComplete && attempts < 120) {
                                    delay(1000)
                                    attempts++
                                    
                                    val statusResponse = apiService.getTaskStatus(taskId)
                                    val status = statusResponse.body()?.status
                                    
                                    when (status) {
                                        "completed" -> {
                                            isComplete = true
                                            val resultFiles = statusResponse.body()?.resultFiles
                                            if (!resultFiles.isNullOrEmpty()) {
                                                val fullPath = resultFiles[0]
                                                val filename = fullPath.substringAfterLast("/").substringAfterLast("\\")
                                                val baseUrl = com.doyouone.drawai.data.api.RetrofitClient.getBaseUrl()
                                                generatedImageUrl = "${baseUrl}download/$filename"
                                                
                                                // Auto-save to Local Gallery
                                                try {
                                                    val repo = com.doyouone.drawai.data.repository.DrawAIRepository()
                                                    val storage = com.doyouone.drawai.data.local.ImageStorage(context)
                                                    val tempFile = File(context.cacheDir, filename)
                                                    
                                                    val result = repo.downloadImage(filename, tempFile)
                                                    if (result.isSuccess) {
                                                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                                                        if (bitmap != null) {
                                                            storage.saveImage(bitmap, "Created Background", visionPrompt, "make_background")
                                                            android.widget.Toast.makeText(context, "Saved to Gallery!", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("MakeBackground", "Auto-save failed", e)
                                                }
                                                
                                                // Show Interstitial Ad then navigate
                                                if (com.doyouone.drawai.ads.AdManager.isInterstitialAdReady()) {
                                                    com.doyouone.drawai.ads.AdManager.showInterstitialAd(
                                                        activity = (context as? android.app.Activity)!!,
                                                        onAdDismissed = {
                                                            onNavigateToGallery()
                                                        }
                                                    )
                                                } else {
                                                    onNavigateToGallery()
                                                }
                                            }
                                        }
                                        "failed" -> {
                                            throw Exception(statusResponse.body()?.error ?: "Generation failed")
                                        }
                                    }
                                }
                                
                                if (!isComplete) {
                                    throw Exception("Timeout - please try again")
                                }
                                
                            } catch (e: Exception) {
                                if (e.message?.contains("403") == true || e.message?.contains("limit", ignoreCase = true) == true) {
                                    showLimitDialog = true
                                } else if (e.message?.contains("530") == true) {
                                    showMaintenanceDialog = true
                                } else {
                                    errorMessage = e.message ?: "Unknown error"
                                }
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Generate Background", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            // Error Message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(100.dp))
        }

        if (showBlockedDialog) {
            AlertDialog(
                onDismissRequest = { showBlockedDialog = false },
                title = { Text("Restricted Content") },
                text = { Text("Your prompt contains a restricted word: \"$blockedWord\"\n\nPlease remove it to comply with safety guidelines.") },
                confirmButton = {
                    TextButton(onClick = { showBlockedDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }

    if (showLimitDialog) {
        val context = LocalContext.current
        com.doyouone.drawai.ui.components.RewardedAdDialog(
            remainingGenerations = 0,
            onDismiss = { showLimitDialog = false },
            onWatchAd = {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    com.doyouone.drawai.ads.AdManager.showRewardedAd(
                        activity,
                        onUserEarnedReward = {
                            scope.launch {
                                val userId = FirebaseAuth.getInstance().currentUser?.uid
                                if (userId != null) {
                                   val repo = com.doyouone.drawai.data.repository.FirebaseGenerationRepository()
                                   val result = repo.addBonusGeneration(userId)
                                   // Optional Toast
                                }
                            }
                            showLimitDialog = false
                        }
                    )
                }
            },
            onUpgradeToPremium = { showLimitDialog = false }
        )
    }

    // Maintenance Popup
    if (showMaintenanceDialog) {
        com.doyouone.drawai.ui.components.MaintenancePopup(
            onGoToGalleryClick = { 
                showMaintenanceDialog = false
                onNavigateToGallery() 
            },
            onRetry = {
                showMaintenanceDialog = false
            }
        )
    }

    // Processing Modal - Blocking dialog
    ProcessingModal(
        isProcessing = isGenerating,
        message = "Generating...",
        detail = "Please wait while we create your background."
    )
}
