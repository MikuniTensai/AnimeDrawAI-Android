package com.doyouone.drawai.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import com.doyouone.drawai.ui.components.ProcessingModal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundRemoverScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGallery: () -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resultImageUrl by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Image picker launcher
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
                errorMessage = null
            }
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Handle manually
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

                
                // Custom Header (GenerateScreen Style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "Remove Background",
                            fontSize = 20.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "AI Tools • Image Editing",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1. Hero Image / Upload Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { if (!isProcessing) imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            // Selected Image State
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            
                            // Edit/Change Button Overlay
                            if (!isProcessing) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                ) {
                                    IconButton(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), androidx.compose.foundation.shape.CircleShape)
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
                                        .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
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
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
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
                
                // Error Message Display
                errorMessage?.let {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                // 2. Action Section
                if (isProcessing) {
                    // Processing Status Board
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
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
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
                                    processingMessage = "Uploading image..."
                                    try {
                                        // Get API service
                                        val apiService = com.doyouone.drawai.data.api.RetrofitClient.apiService
                                        
                                        // Convert URI to MultipartBody.Part
                                        val imagePart = uriToMultipartBody(context, uri, "image")
                                        
                                        if (imagePart == null) {
                                            throw Exception("Failed to read image file")
                                        }
                                        
                                        // Call remove background API
                                        processingMessage = "Uploading to server..."
                                        val response = apiService.removeBackground(imagePart, emptyMap())
                                        
                                        if (!response.isSuccessful) {
                                            throw Exception("Server error: ${response.code()} ${response.message()}")
                                        }
                                        
                                        val taskId = response.body()?.taskId
                                        if (taskId == null) {
                                            throw Exception("No task ID received from server")
                                        }
                                        
                                        // Poll status until complete
                                        processingMessage = "Removing background..."
                                        var isComplete = false
                                        var attempts = 0
                                        val maxAttempts = 60 // 60 seconds timeout
                                        
                                        while (!isComplete && attempts < maxAttempts) {
                                            delay(1000) // Wait 1 second
                                            attempts++
                                            
                                            val statusResponse = apiService.getTaskStatus(taskId)
                                            val status = statusResponse.body()?.status
                                            
                                            when (status) {
                                                "completed" -> {
                                                    isComplete = true
                                                    val resultFiles = statusResponse.body()?.resultFiles
                                                    
                                                    if (resultFiles.isNullOrEmpty()) {
                                                        throw Exception("No result files generated")
                                                    }
                                                    
                                                    // Success! Show dialog and will show image
                                                    processingMessage = "Done!"
                                                    
                                                    val fullPath = resultFiles[0]
                                                    val filename = fullPath.substringAfterLast("/").substringAfterLast("\\")
                                                    resultImageUrl = "https://drawai-api.drawai.site/download/$filename"
                                                    
                                                    // Auto-save to Local Gallery
                                                    try {
                                                        processingMessage = "Saving to Gallery..."
                                                        val repo = com.doyouone.drawai.data.repository.DrawAIRepository()
                                                        val storage = com.doyouone.drawai.data.local.ImageStorage(context)
                                                        val tempFile = File(context.cacheDir, filename)
                                                        
                                                        val result = repo.downloadImage(filename, tempFile)
                                                        if (result.isSuccess) {
                                                                val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                                                                if (bitmap != null) {
                                                                    storage.saveImage(bitmap, "Background Removal", "", "background_remover")
                                                                    android.widget.Toast.makeText(context, "Saved to Gallery!", android.widget.Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    android.util.Log.e("BackgroundRemover", "Failed to decode bitmap from ${tempFile.absolutePath}")
                                                                }
                                                        } else {
                                                                android.util.Log.e("BackgroundRemover", "Download failed: ${result.exceptionOrNull()}")
                                                        }
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("BackgroundRemover", "Auto-save failed", e)
                                                    }
                                                    
                                                    // success dialog moved to immediate navigation
                                                    
                                                    selectedImageUri = null
                                                    
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
                                                "failed" -> {
                                                    val error = statusResponse.body()?.error ?: "Unknown error"
                                                    throw Exception("Processing failed: $error")
                                                }
                                                "processing" -> {
                                                    processingMessage = "Removing background... ${attempts}s"
                                                }
                                                "queued" -> {
                                                    processingMessage = "In queue... ${attempts}s"
                                                }
                                                else -> {
                                                    processingMessage = "Processing... ${attempts}s"
                                                }
                                            }
                                        }
                                        
                                        if (!isComplete) {
                                            throw Exception("Processing timeout. Please try again.")
                                        }
                                        
                                    } catch (e: Exception) {
                                        if (e.message?.contains("403") == true || e.message?.contains("limit", ignoreCase = true) == true) {
                                            showLimitDialog = true
                                        } else if (e.message?.contains("530") == true) {
                                            showMaintenanceDialog = true
                                        } else {
                                            errorMessage = e.message ?: "Failed to process image"
                                            android.util.Log.e("BackgroundRemover", "Error: ${e.message}", e)
                                        }
                                    } finally {
                                        isProcessing = false
                                        processingMessage = ""
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = selectedImageUri != null
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Remove Background",
                            fontSize = 16.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
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
                onNavigateToGallery() 
            },
            onRetry = {
                showMaintenanceDialog = false
            }
        )
    }
    
    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onNavigateToGallery()
            },
            icon = {
                Text("✅", style = MaterialTheme.typography.displayMedium)
            },
            title = {
                Text("Success!")
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Background removed successfully!")
                    
                    if (resultImageUrl != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AsyncImage(
                            model = resultImageUrl,
                            contentDescription = "Result Image",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
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
                        onNavigateToGallery()
                    }
                ) {
                    Text("View in Gallery")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                    }
                ) {
                    Text("Close")
                }
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

/**
 * Get file size from URI in bytes
 */
private fun getFileSizeFromUri(context: Context, uri: Uri): Long {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            it.statSize
        } ?: 0L
    } catch (e: Exception) {
        0L
    }
}

/**
 * Convert URI to MultipartBody.Part for upload
 */
@Suppress("DEPRECATION")
private fun uriToMultipartBody(context: Context, uri: Uri, paramName: String): okhttp3.MultipartBody.Part? {
    return try {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "image/*"
        
        // Get filename
        val cursor = contentResolver.query(uri, null, null, null, null)
        val filename = cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) it.getString(nameIndex) else "upload.jpg"
            } else "upload.jpg"
        } ?: "upload.jpg"
        
        // Read file content
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val bytes = inputStream.readBytes()
        inputStream.close()
        
        // Create RequestBody (let OkHttp infer media type)
        val requestBody = okhttp3.RequestBody.create(null, bytes)
        
        // Create MultipartBody.Part
        okhttp3.MultipartBody.Part.createFormData(paramName, filename, requestBody)
    } catch (e: Exception) {
        android.util.Log.e("BackgroundRemover", "Failed to convert URI to MultipartBody: ${e.message}", e)
        null
    }
}
