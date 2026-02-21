package com.doyouone.drawai.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateContentSize
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import com.doyouone.drawai.ui.components.ProcessingModal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundRemoverAdvancedScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGallery: (() -> Unit)? = null // Optional if not available in nav graph yet
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Image States
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var resultImageUrl by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    
    // Processing States
    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Advanced Options States
    var selectedModel by remember { mutableStateOf("u2net") }
    var postProcessing by remember { mutableStateOf(false) }
    var onlyMask by remember { mutableStateOf(false) }
    var alphaMatting by remember { mutableStateOf(false) }
    var amForeground by remember { mutableFloatStateOf(240f) }
    var amBackground by remember { mutableFloatStateOf(10f) }
    var amErode by remember { mutableFloatStateOf(10f) }
    var backgroundColor by remember { mutableStateOf("none") }
    
    var showModelDropdown by remember { mutableStateOf(false) }
    var isConfigExpanded by remember { mutableStateOf(true) }

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Validate file size
            val fileSize = getFileSizeFromUri(context, it)
            if (fileSize > 2 * 1024 * 1024) {
                errorMessage = "File too large. Maximum 2MB (${fileSize / 1024 / 1024} MB)"
                selectedImageUri = null
            } else {
                selectedImageUri = it
                resultImageUrl = null
                errorMessage = null
            }
        }
    }

    val modelOptions = listOf(
        "u2net" to "Standard (Best)",
        "u2netp" to "Fast (Lower Qual)",
        "u2netp_human_seg" to "Human Focus",
        "silueta" to "Silhouette / Object",
        "isnet-general-use" to "General (ISNet)",
        "isnet-anime" to "Anime Style"
    )
    
    val bgColors = listOf(
        "none" to Color.Transparent,
        "first" to Color.Transparent,
        "white" to Color.White,
        "black" to Color.Black,
        "magenta" to Color.Magenta,
        "chroma green" to Color(0xFF00FF00),
        "chroma blue" to Color(0xFF0000FF)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {


                // Custom Header (Matching BackgroundRemoverScreen)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "Advanced Remover", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Professional Tools", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1. Image Upload Section (Matching Style)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp) // Slightly shorter to fit controls
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { if (!isProcessing) imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            
                            // Edit Button Overlay
                            if (!isProcessing) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                ) {
                                    IconButton(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit, 
                                            contentDescription = "Change Image",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Empty State
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Tap to Upload Photo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Supports JPG, PNG • Max 2MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))

                // Error Message
                errorMessage?.let {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // 2. Advanced Configuration
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isConfigExpanded = !isConfigExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(
                                if(isConfigExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = isConfigExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                // Model Dropdown
                                Box {
                                    OutlinedTextField(
                                        value = modelOptions.find { it.first == selectedModel }?.second ?: selectedModel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("AI Model") },
                                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                                        modifier = Modifier.fillMaxWidth().clickable { showModelDropdown = true },
                                        enabled = false,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Box(modifier = Modifier.matchParentSize().clickable { showModelDropdown = true })
                                    
                                    DropdownMenu(
                                        expanded = showModelDropdown,
                                        onDismissRequest = { showModelDropdown = false }
                                    ) {
                                        modelOptions.forEach { (id, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    selectedModel = id
                                                    showModelDropdown = false
                                                },
                                                leadingIcon = if(selectedModel == id) { { Icon(Icons.Default.Check, null) } } else null
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // Switches
                                AdvancedOptionSwitch("Post Processing (Smooth)", postProcessing) { postProcessing = it }
                                AdvancedOptionSwitch("Return Mask Only", onlyMask) { onlyMask = it }
                                AdvancedOptionSwitch("Alpha Matting (Details)", alphaMatting) { alphaMatting = it }

                                // Alpha Matting Details
                                if (alphaMatting) {
                                    Spacer(Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Foreground Threshold: ${amForeground.toInt()}", style = MaterialTheme.typography.labelSmall)
                                            Slider(value = amForeground, onValueChange = { amForeground = it }, valueRange = 0f..255f)
                                            
                                            Text("Background Threshold: ${amBackground.toInt()}", style = MaterialTheme.typography.labelSmall)
                                            Slider(value = amBackground, onValueChange = { amBackground = it }, valueRange = 0f..255f)
                                            
                                            Text("Erode Size: ${amErode.toInt()}", style = MaterialTheme.typography.labelSmall)
                                            Slider(value = amErode, onValueChange = { amErode = it }, valueRange = 0f..50f)
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                
                                // Background Color Picker
                                Text("Output Background", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    bgColors.forEach { (id, color) ->
                                        // Skip "first" if it's a duplicate or placeholder
                                        if (id != "first") {
                                            val isSelected = backgroundColor == id
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(if(id == "none") MaterialTheme.colorScheme.surfaceVariant else color)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { backgroundColor = id },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (id == "none") Text("None", style = MaterialTheme.typography.labelSmall)
                                                if (isSelected && id != "none") {
                                                    Icon(
                                                        Icons.Default.Check, 
                                                        null, 
                                                        tint = if(id == "white" || id == "chroma green") Color.Black else Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 3. Action Section
                if (isProcessing) {
                    // Processing Board (Matching Style)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = processingMessage.ifEmpty { "Processing..." },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                // Primary Action Button (Hidden when processing)
                if (!isProcessing) {
                    Button(
                        onClick = {
                            selectedImageUri?.let { uri ->
                                scope.launch {
                                    isProcessing = true
                                    errorMessage = null
                                    processingMessage = "Preparing..."
                                    try {
                                        val apiService = com.doyouone.drawai.data.api.RetrofitClient.apiService
                                        val imagePart = uriToMultipartBodyAdvanced(context, uri, "image") 
                                            ?: throw Exception("Failed to read file")
                                        
                                        // Construct Options
                                        val options = mutableMapOf<String, RequestBody>()
                                        options["model"] = selectedModel.toRequestBody()
                                        options["post_processing"] = postProcessing.toString().toRequestBody()
                                        options["only_mask"] = onlyMask.toString().toRequestBody()
                                        options["alpha_matting"] = alphaMatting.toString().toRequestBody()
                                        options["alpha_matting_foreground_threshold"] = amForeground.toInt().toString().toRequestBody()
                                        options["alpha_matting_background_threshold"] = amBackground.toInt().toString().toRequestBody()
                                        options["alpha_matting_erode_size"] = amErode.toInt().toString().toRequestBody()
                                        options["background_color"] = backgroundColor.toRequestBody()
                                        
                                        processingMessage = "Uploading..."
                                        val response = apiService.removeBackground(imagePart, options)
                                        
                                        if (!response.isSuccessful) throw Exception("Error: ${response.code()}")
                                        
                                        val taskId = response.body()?.taskId ?: throw Exception("No Task ID")
                                        
                                        // Polling Logic
                                        processingMessage = "Processing..."
                                        var isComplete = false
                                        var attempts = 0
                                        
                                        while(!isComplete && attempts < 60) {
                                            delay(1000)
                                            attempts++
                                            val statusRes = apiService.getTaskStatus(taskId)
                                            val status = statusRes.body()?.status
                                            
                                            when(status) {
                                                "completed" -> {
                                                    isComplete = true
                                                    val files = statusRes.body()?.resultFiles ?: emptyList()
                                                    if (files.isNotEmpty()) {
                                                        val fname = files[0].substringAfterLast("/").substringAfterLast("\\")
                                                        resultImageUrl = "https://drawai-api.drawai.site/download/$fname"
                                                        
                                                        // Save logic
                                                        processingMessage = "Saving to Gallery..."
                                                        saveImageToGallery(context, fname)
                                                    }
                                                    // Show Interstitial Ad then navigate
                                                    if (com.doyouone.drawai.ads.AdManager.isInterstitialAdReady()) {
                                                        com.doyouone.drawai.ads.AdManager.showInterstitialAd(
                                                            activity = (context as? android.app.Activity)!!,
                                                            onAdDismissed = {
                                                                if (onNavigateToGallery != null) onNavigateToGallery() else onNavigateBack()
                                                            }
                                                        )
                                                    } else {
                                                        if (onNavigateToGallery != null) onNavigateToGallery() else onNavigateBack()
                                                    }
                                                }
                                                "failed" -> throw Exception(statusRes.body()?.error ?: "Failed")
                                                else -> processingMessage = "Processing... ${attempts}s"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        if (e.message?.contains("403") == true || e.message?.contains("limit", ignoreCase = true) == true) {
                                            showLimitDialog = true
                                        } else if (e.message?.contains("530") == true) {
                                            showMaintenanceDialog = true
                                        } else {
                                            errorMessage = e.message
                                        }
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = selectedImageUri != null
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Remove Background",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Footer Info
                Spacer(Modifier.height(32.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                ) {
                     Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                     Spacer(Modifier.width(8.dp))
                     Text(
                         text = "Processed image is automatically saved to Gallery",
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant,
                         textAlign = TextAlign.Center
                     )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    
    // Limit Dialog
    if (showLimitDialog) {
        com.doyouone.drawai.ui.components.RewardedAdDialog(
            remainingGenerations = 0,
            onDismiss = { showLimitDialog = false },
            onWatchAd = {
                if (context is android.app.Activity) {
                    com.doyouone.drawai.ads.AdManager.showRewardedAd(
                        context,
                        onUserEarnedReward = {
                            scope.launch {
                                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                if (userId != null) {
                                   val repo = com.doyouone.drawai.data.repository.FirebaseGenerationRepository()
                                   val result = repo.addBonusGeneration(userId)
                                   if (result.isSuccess) {
                                       android.widget.Toast.makeText(context, "Bonus generation added!", android.widget.Toast.LENGTH_SHORT).show()
                                   }
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
                onNavigateToGallery?.invoke() 
            },
            onRetry = {
                showMaintenanceDialog = false
            }
        )
    }

    // Result Dialog (Matching BackgroundRemoverScreen)
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onNavigateToGallery?.invoke()
            },
            icon = { Text("✅", style = MaterialTheme.typography.displayMedium) },
            title = { Text("Success!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Background removed successfully!")
                    if (resultImageUrl != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AsyncImage(
                            model = resultImageUrl,
                            contentDescription = "Result Image",
                            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Check your Gallery to see the result.")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToGallery?.invoke()
                    }
                ) {
                    Text("View in Gallery")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuccessDialog = false }) { Text("Close") }
            }
        )
    }

    // Processing Modal - Blocking dialog
    ProcessingModal(
        isProcessing = isProcessing,
        message = if (processingMessage.isNotEmpty()) processingMessage else "Processing...",
        detail = "Please wait while we remove the background."
    )
}

@Composable
private fun AdvancedOptionSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f)
        )
    }
}

// Reuse modifier extension for scale if not available
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

// Helper Functions
private fun String.toRequestBody(): RequestBody = this.toRequestBody("text/plain".toMediaTypeOrNull())

private fun getFileSizeFromUri(context: Context, uri: Uri): Long {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            it.statSize
        } ?: 0L
    } catch (e: Exception) {
        0L
    }
}

private suspend fun saveImageToGallery(context: Context, filename: String) {
    try {
        val repo = com.doyouone.drawai.data.repository.DrawAIRepository()
        val storage = com.doyouone.drawai.data.local.ImageStorage(context)
        val tempFile = File(context.cacheDir, filename)
        val result = repo.downloadImage(filename, tempFile)
        if (result.isSuccess) {
            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
            if (bitmap != null) {
                storage.saveImage(bitmap, "Background Removal Advanced", "", "background_remover")
                android.widget.Toast.makeText(context, "Saved to Gallery!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("AdvRembg", "Save Error", e)
    }
}

fun uriToMultipartBodyAdvanced(context: Context, uri: Uri, paramName: String): MultipartBody.Part? {
    try {
        val contentResolver = context.contentResolver
        val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
        val inputStream = java.io.FileInputStream(fileDescriptor.fileDescriptor)
        val file = File(context.cacheDir, "temp_upload_adv.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        outputStream.close()
        inputStream.close()
        
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(paramName, file.name, requestBody)
    } catch (e: Exception) {
        return null
    }
}
