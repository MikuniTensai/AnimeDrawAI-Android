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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun UpscaleImageScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGallery: (() -> Unit)? = null
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

    // Options States
    var upscaleFactor by remember { mutableFloatStateOf(1.5f) }
    var tileSize by remember { mutableIntStateOf(512) }
    var overlap by remember { mutableIntStateOf(32) }
    var feather by remember { mutableIntStateOf(0) }
    var selectedResample by remember { mutableStateOf("lanczos") }
    var clearComfyMemory by remember { mutableStateOf(false) }
    
    var showResampleDropdown by remember { mutableStateOf(false) }
    var isConfigExpanded by remember { mutableStateOf(true) }

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileSize = getFileSizeFromUri(context, it)
            if (fileSize > 2 * 1024 * 1024) { // Max 2MB
                errorMessage = "File too large. Maximum 2MB"
                selectedImageUri = null
            } else {
                selectedImageUri = it
                resultImageUrl = null
                errorMessage = null
            }
        }
    }

    val resampleOptions = listOf("lanczos", "nearest-exact", "bilinear", "area", "bicubic")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {

                // Header
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
                            "Super Resolution", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Upscale & Enhance", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1. Image Upload Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
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
                                        Icon(Icons.Default.Edit, contentDescription = "Change", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("Upload Image", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Max 2MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                // 2. Configuration
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
                                Text("Upscale Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(if(isConfigExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                        }

                        AnimatedVisibility(visible = isConfigExpanded) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                
                                // Upscale Factor Slider
                                Text("Upscale Factor: ${String.format("%.1f", upscaleFactor)}x", style = MaterialTheme.typography.labelMedium)
                                Slider(
                                    value = upscaleFactor,
                                    onValueChange = { upscaleFactor = it },
                                    valueRange = 1.5f..4.0f,
                                    steps = 4 // 1.5, 2.0, 2.5, 3.0, 3.5, 4.0 approx
                                )
                                if (upscaleFactor > 1.5f) {
                                    Text("⚠️ > 1.5x requires Basic/Premium Plan", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                                }
                                Spacer(Modifier.height(12.dp))

                                // Tile Size
                                Text("Tile Size: $tileSize", style = MaterialTheme.typography.labelMedium)
                                Slider(
                                    value = tileSize.toFloat(),
                                    onValueChange = { tileSize = it.toInt() },
                                    valueRange = 256f..1024f,
                                    steps = 5 // 256, 384, 512, ..., 1024
                                )
                                Spacer(Modifier.height(12.dp))

                                // Overlap
                                Text("Overlap: $overlap", style = MaterialTheme.typography.labelMedium)
                                Slider(
                                    value = overlap.toFloat(),
                                    onValueChange = { overlap = it.toInt() },
                                    valueRange = 0f..128f,
                                    steps = 7
                                )
                                Spacer(Modifier.height(12.dp))
                                
                                // Feather
                                Text("Feather: $feather", style = MaterialTheme.typography.labelMedium)
                                Slider(
                                    value = feather.toFloat(),
                                    onValueChange = { feather = it.toInt() },
                                    valueRange = 0f..32f,
                                    steps = 31
                                )
                                Spacer(Modifier.height(12.dp))

                                // Resample Method Dropdown
                                Box {
                                    OutlinedTextField(
                                        value = selectedResample,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Resample Method") },
                                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                                        modifier = Modifier.fillMaxWidth().clickable { showResampleDropdown = true },
                                        enabled = false,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                    Box(modifier = Modifier.matchParentSize().clickable { showResampleDropdown = true })
                                    
                                    DropdownMenu(
                                        expanded = showResampleDropdown,
                                        onDismissRequest = { showResampleDropdown = false }
                                    ) {
                                        resampleOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    selectedResample = option
                                                    showResampleDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))

                                // Clear Comfy Memory
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Optimize Memory", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Switch(
                                        checked = clearComfyMemory, 
                                        onCheckedChange = { clearComfyMemory = it },
                                        modifier = Modifier.graphicsLayer(scaleX = 0.8f, scaleY = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // 3. Action Button
                if (isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(16.dp))
                            Text(processingMessage, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                // Action Button (Hidden when processing)
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
                                        val imagePart = uriToMultipartBodyUpscale(context, uri, "image") 
                                            ?: throw Exception("Failed to read file")
                                        
                                        val options = mutableMapOf<String, RequestBody>()
                                        options["upscale_factor"] = upscaleFactor.toString().toRequestBody()
                                        options["tile_size"] = tileSize.toString().toRequestBody()
                                        options["overlap"] = overlap.toString().toRequestBody()
                                        options["feather"] = feather.toString().toRequestBody()
                                        options["resample_method"] = selectedResample.toRequestBody()
                                        options["clear_comfy_memory"] = clearComfyMemory.toString().toRequestBody()
                                        
                                        processingMessage = "Uploading..."
                                        val response = apiService.upscaleImage(imagePart, options)
                                        
                                        if (!response.isSuccessful) {
                                            val errorBody = response.errorBody()?.string()
                                            // Try to parse json error
                                            val msg = if (errorBody != null && errorBody.contains("error")) {
                                                try {
                                                     org.json.JSONObject(errorBody).getString("error")
                                                } catch(e: Exception) { "Server Error: ${response.code()}" }
                                            } else "Server Error: ${response.code()}"
                                            throw Exception(msg)
                                        }
                                        
                                        val taskId = response.body()?.taskId ?: throw Exception("No Task ID")
                                        
                                        processingMessage = "Upscaling..."
                                        var isComplete = false
                                        var attempts = 0
                                        while(!isComplete && attempts < 120) { // Longer timeout for upscale
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
                                                         processingMessage = "Saving..."
                                                         saveImageToGallery(context, fname)
                                                    }
                                                    
                                                    // Show Interstitial Ad then navigate
                                                    if (com.doyouone.drawai.ads.AdManager.isInterstitialAdReady()) {
                                                        com.doyouone.drawai.ads.AdManager.showInterstitialAd(
                                                            activity = (context as? android.app.Activity)!!,
                                                            onAdDismissed = {
                                                                onNavigateToGallery?.invoke()
                                                            }
                                                        )
                                                    } else {
                                                        // If ad not ready, just navigate
                                                        onNavigateToGallery?.invoke()
                                                    }
                                                }
                                                "failed" -> throw Exception(statusRes.body()?.error ?: "Failed")
                                                else -> processingMessage = "Upscaling... ${attempts}s"
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
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Upscale Image", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(48.dp))
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
    
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false; onNavigateToGallery?.invoke() },
            icon = { Text("✨", style = MaterialTheme.typography.displayMedium) },
            title = { Text("Upscale Complete!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (resultImageUrl != null) {
                         AsyncImage(
                            model = resultImageUrl,
                            contentDescription = "Result",
                            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Check Gallery for result.")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSuccessDialog = false; onNavigateToGallery?.invoke() }) { Text("Open Gallery") } },
            dismissButton = { TextButton(onClick = { showSuccessDialog = false }) { Text("Close") } }
        )
    }

    // Processing Modal - Blocking dialog
    ProcessingModal(
        isProcessing = isProcessing,
        message = if (processingMessage.isNotEmpty()) processingMessage else "Processing...",
        detail = "Please wait while we upscale your image."
    )
}

// Helper Extensions/Functions
private fun String.toRequestBody(): RequestBody = this.toRequestBody("text/plain".toMediaTypeOrNull())

private fun getFileSizeFromUri(context: Context, uri: Uri): Long {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
    } catch (e: Exception) { 0L }
}

fun uriToMultipartBodyUpscale(context: Context, uri: Uri, paramName: String): MultipartBody.Part? {
     try {
        val contentResolver = context.contentResolver
        val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
        val inputStream = java.io.FileInputStream(fileDescriptor.fileDescriptor)
        val file = File(context.cacheDir, "temp_upload_upscale.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        outputStream.close()
        inputStream.close()
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(paramName, file.name, requestBody)
    } catch (e: Exception) { return null }
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
                storage.saveImage(bitmap, "Super Resolution", "", "upscale")
            }
        }
    } catch (e: Exception) { }
}
