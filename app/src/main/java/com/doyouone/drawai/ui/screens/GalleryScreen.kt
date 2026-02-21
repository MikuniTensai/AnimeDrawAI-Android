package com.doyouone.drawai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.doyouone.drawai.data.local.ImageStorage
import com.doyouone.drawai.data.model.GeneratedImage
import com.doyouone.drawai.data.model.ImageReport
import com.doyouone.drawai.data.repository.ReportRepository
import com.doyouone.drawai.ui.components.ZoomableImage
import com.doyouone.drawai.ui.theme.*
import com.doyouone.drawai.ui.components.AnimeDrawMainTopBar
import com.doyouone.drawai.util.DummyData
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.doyouone.drawai.util.ErrorUtils
import com.doyouone.drawai.ui.components.AnimeDrawErrorDialog
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.CircleShape
import java.io.File
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.doyouone.drawai.data.model.CommunityPost

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward

// Fungsi untuk menghitung jumlah kolom grid berdasarkan lebar layar
@Composable
fun getResponsiveGalleryColumns(): Int {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    
    return when {
        screenWidthDp >= 1200 -> 5 // Tablet besar atau desktop
        screenWidthDp >= 900 -> 4  // Tablet medium
        screenWidthDp >= 600 -> 3  // Tablet kecil atau phone landscape
        else -> 2                  // Phone portrait
    }
}

@Composable
fun FullscreenImage(
    initialIndex: Int,
    images: List<GeneratedImage>,
    onDismiss: () -> Unit,
    onDelete: (GeneratedImage) -> Unit,
    onReport: (GeneratedImage) -> Unit,
    onToggleFavorite: (GeneratedImage) -> Unit,
    onShare: (GeneratedImage) -> Unit = {},
    onUseAgain: (String) -> Unit = {}, // NEW: Navigate to generate with workflow
    isPremium: Boolean = false
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
    val scope = rememberCoroutineScope()
    var areControlsVisible by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> images.getOrNull(index)?.id ?: index }
        ) { page ->
            val image = images.getOrNull(page)
            if (image != null) {
                ZoomableImage(
                    imageUrl = image.imageUrl,
                    contentDescription = image.prompt,
                    onTap = { areControlsVisible = !areControlsVisible },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Controls Overlay (Top Bar)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val currentImage = images.getOrNull(pagerState.currentPage)
            if (currentImage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close Button
                    Card(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onDismiss() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("✕", fontSize = 24.sp, color = AccentWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Delete
                        Card(
                            modifier = Modifier.size(48.dp).clickable { onDelete(currentImage) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.8f))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("🗑", fontSize = 20.sp, color = AccentWhite)
                            }
                        }
                        
                        // Report
                        Card(
                            modifier = Modifier.size(48.dp).clickable { onReport(currentImage) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("🚩", fontSize = 20.sp, color = AccentWhite)
                            }
                        }
                        
                        // Favorite
                        Card(
                            modifier = Modifier.size(48.dp).clickable { onToggleFavorite(currentImage) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentImage.isFavorite) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("♥", fontSize = 24.sp, color = AccentWhite)
                            }
                        }
                        
                        // Share
                        if (isPremium) {
                            Card(
                                modifier = Modifier.size(48.dp).clickable { onShare(currentImage) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("📤", fontSize = 20.sp, color = AccentWhite)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Navigation Arrows
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
        ) {
             if (pagerState.currentPage > 0) {
                 IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    modifier = Modifier.background(Color.Black.copy(alpha=0.5f), CircleShape)
                 ) {
                     Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Previous", 
                        tint = Color.White, 
                        modifier = Modifier.size(32.dp)
                     )
                 }
             }
        }
        
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
        ) {
             if (pagerState.currentPage < images.size - 1) {
                 IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = Modifier.background(Color.Black.copy(alpha=0.5f), CircleShape)
                 ) {
                     Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward, 
                        contentDescription = "Next", 
                        tint = Color.White, 
                        modifier = Modifier.size(32.dp)
                     )
                 }
             }
        }

        // Bottom Bar
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val currentImage = images.getOrNull(pagerState.currentPage)
            if (currentImage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                        .padding(24.dp)
                ) {
                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(currentImage.prompt))
                                Toast.makeText(context, "Prompt Copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Copy Vision", fontSize = 12.sp) }
                        
                        Button(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(currentImage.negativePrompt ?: ""))
                                Toast.makeText(context, "Copy Avoid", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Copy Avoid", fontSize = 12.sp) }
                    }
                    
                    // Check if this is a workflow (not content like background_remover, upscale, etc)
                    val isWorkflowImage = !currentImage.workflow.contains("background_remover", ignoreCase = true) &&
                                         !currentImage.workflow.contains("upscale", ignoreCase = true) &&
                                         !currentImage.workflow.contains("face_restore", ignoreCase = true) &&
                                         !currentImage.workflow.contains("make_background", ignoreCase = true) &&
                                         !currentImage.workflow.contains("sketch_to_image", ignoreCase = true) &&
                                         !currentImage.workflow.contains("photo_editor", ignoreCase = true)
                    
                    // Use Again button (only for workflows)
                    if (isWorkflowImage) {
                        Button(
                            onClick = {
                                // Save prompt and avoid to shared preferences for generate screen
                                val sharedPrefs = context.getSharedPreferences("generate_prefs", android.content.Context.MODE_PRIVATE)
                                sharedPrefs.edit()
                                    .putString("prefilled_prompt", currentImage.prompt)
                                    .putString("prefilled_avoid", currentImage.negativePrompt ?: "")
                                    .putString("prefilled_workflow", currentImage.workflow)
                                    .putString("prefilled_seed", currentImage.seed?.toString() ?: "")
                                    .putBoolean("has_prefilled_data", true)
                                    .apply()
                                
                                Toast.makeText(context, "✨ Opening Generate Menu...", Toast.LENGTH_SHORT).show()
                                onDismiss() // Close fullscreen viewer
                                
                                // Navigate to generate screen with workflow
                                onUseAgain(currentImage.workflow)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use Again", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentImage.workflow.replace("_", " ").replaceFirstChar { it.uppercase() },
                            fontSize = 14.sp,
                            color = AccentWhite.copy(alpha = 0.9f),
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = formatDate(currentImage.createdAt),
                            fontSize = 14.sp,
                            color = AccentWhite.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Seed display
                    currentImage.seed?.let { seed ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Seed: $seed",
                            fontSize = 12.sp,
                            color = AccentWhite.copy(alpha = 0.6f),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryScreen(
    onImageClick: (GeneratedImage) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onNavigateToGenerate: () -> Unit,
    onNavigateToGenerateWithWorkflow: (String) -> Unit = {}, // NEW: Navigate with workflow param
    authManager: com.doyouone.drawai.auth.AuthManager,
    onBack: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onNavigateToCharacterChat: (String) -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToSummoningAnimation: (characterId: String, imageUrl: String, sinCount: Int, rarity: String) -> Unit = { _, _, _, _ -> },
    characterCount: Int = 0,
    maxChatLimit: Int = 1,
    isPremium: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageStorage = remember { ImageStorage(context) }
    val reportRepository = remember { ReportRepository() }
    val characterRepository = remember { com.doyouone.drawai.data.repository.CharacterRepository() }
    val userPreferences = remember { com.doyouone.drawai.data.preferences.UserPreferences(context) }
    val appPreferences = remember { com.doyouone.drawai.data.preferences.AppPreferences(context) }
    val communityRepository = remember { com.doyouone.drawai.data.repository.CommunityRepository(context) }
    
    var images by remember { mutableStateOf<List<GeneratedImage>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf<String?>(null) } // Changed to nullable or use resource directly if possible, but simplest is to initialize with empty or check resource
    // To safe keeping logic simple, we can init with "All" string resource if we are inside Composable context.
    val filterAll = stringResource(com.doyouone.drawai.R.string.gallery_filter_all)
    val filterFavorites = stringResource(com.doyouone.drawai.R.string.gallery_filter_favorites)
    
    // We need to initialize selectedFilter with the translated "All" string
    if (selectedFilter == null) {
        selectedFilter = filterAll
    }
    var selectedImage by remember { mutableStateOf<GeneratedImage?>(null) }
    var imageToDelete by remember { mutableStateOf<GeneratedImage?>(null) }
    var imageToReport by remember { mutableStateOf<GeneratedImage?>(null) }
    var pendingShareImage by remember { mutableStateOf<GeneratedImage?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    
    // Lock State
    var isLocked by remember { mutableStateOf(false) }
    var isLoadingLock by remember { mutableStateOf(true) }
    
    // Check lock status
    LaunchedEffect(Unit) {
        val lockEnabled = appPreferences.isGalleryLockEnabled.first()
        isLocked = lockEnabled
        isLoadingLock = false
    }
    
    // Character-related state
    var hasSelectedCharacter by remember { mutableStateOf(false) }
    var selectedCharacterId by remember { mutableStateOf<String?>(null) }
    var selectedCharacterImageId by remember { mutableStateOf<String?>(null) }
    var showCharacterCreationDialog by remember { mutableStateOf(false) }
    var pendingCharacterImage by remember { mutableStateOf<GeneratedImage?>(null) }
    var isCreatingCharacter by remember { mutableStateOf(false) }
    var characterCreationMessage by remember { mutableStateOf("") }
    var characterName by remember { mutableStateOf("") }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showGuidelinesDialog by remember { mutableStateOf(false) }

    
    var reportReason by remember { mutableStateOf("") }
    var isReporting by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    var selectedLanguage by remember { mutableStateOf("en") }
    var selectedGender by remember { mutableStateOf("female") }

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf("") }
    
    var showDownloadInfoDialog by remember { mutableStateOf(false) }

    // Load images
    LaunchedEffect(refreshTrigger) {
        images = imageStorage.getAllImages()
    }
    
    // Load generation limit for More features check
    val viewModel: com.doyouone.drawai.viewmodel.GenerateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val generationLimit by viewModel.generationLimit.collectAsState()
    val isMoreEnabled = generationLimit?.moreEnabled == true
    
    // Determine if ads should be shown using helper (ignoring passed param which might be stale)
    val isPremiumPref by userPreferences.isPremium.collectAsState(initial = false)
    val shouldShowAds = remember(isPremiumPref, generationLimit) {
        com.doyouone.drawai.utils.AdDisplayHelper.shouldShowAds(isPremiumPref, generationLimit)
    }
    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    
    LaunchedEffect(user) {
        viewModel.loadGenerationLimit()
    }
    
    // Load character status
    // Load character status
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val repository = com.doyouone.drawai.data.repository.CharacterRepository()
                val result = repository.getUserCharacter()
                
                result.onSuccess { response ->
                    hasSelectedCharacter = response.hasCharacter
                    selectedCharacterId = response.characterId
                    
                    // If we have a character ID but no image ID, we might need to fetch the profile
                    // But for now, just knowing we have a character is enough to hide the create buttons
                    if (response.hasCharacter && response.characterId != null) {
                        // Optionally fetch profile to get image ID if needed for "Chat" button on specific image
                        // For now, we'll rely on the backend check
                        val profileResult = repository.getCharacterProfile(response.characterId)
                        profileResult.onSuccess { character ->
                            selectedCharacterImageId = character.imageId
                        }
                    }
                }.onFailure { e ->
                    android.util.Log.e("GalleryScreen", "Failed to get user character", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryScreen", "Error loading character status", e)
            }
        }
    }

    val filters = listOf(stringResource(com.doyouone.drawai.R.string.gallery_filter_all), stringResource(com.doyouone.drawai.R.string.gallery_filter_favorites)) 
    
    val filteredImages = remember(images, selectedFilter, filterAll, filterFavorites) {
        if (selectedFilter == filterAll) images
        else if (selectedFilter == filterFavorites) images.filter { it.isFavorite }
        else images.filter { it.workflow == selectedFilter }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        if (isLoadingLock) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (isLocked) {
            GalleryPinScreen(
                onPinVerified = { isLocked = false },
                onBack = onBack
            )
        } else {
            // Header
            AnimeDrawMainTopBar(
                title = stringResource(com.doyouone.drawai.R.string.gallery_title),
                subtitle = stringResource(com.doyouone.drawai.R.string.gallery_subtitle_count, filteredImages.size),
                onOpenDrawer = onOpenDrawer,
                actions = {
                    // Download Button
                    IconButton(onClick = { showDownloadInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download Gallery",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Filter Menu
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp)
                        ) {
                            filters.forEach { filter ->
                                FilterChip(
                                    filter = filter,
                                    isSelected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter }
                                )
                            }
                        }
                    }
                }
            )
            
            // Images Grid
            if (filteredImages.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                    RoundedCornerShape(30.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        RoundedCornerShape(15.dp)
                                    )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = stringResource(com.doyouone.drawai.R.string.gallery_empty_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Text(
                            text = stringResource(com.doyouone.drawai.R.string.gallery_empty_subtitle),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = onNavigateToGenerate,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(stringResource(com.doyouone.drawai.R.string.gallery_generate_now))
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(getResponsiveGalleryColumns()),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredImages) { image ->
                        // Check if image is eligible for character creation (not a utility workflow)
                        val isEligibleForCharacter = !image.workflow.contains("background_remover", ignoreCase = true) &&
                                             !image.workflow.contains("upscale", ignoreCase = true) &&
                                             !image.workflow.contains("face_restore", ignoreCase = true) &&
                                             !image.workflow.contains("make_background", ignoreCase = true) &&
                                             !image.workflow.contains("sketch_to_image", ignoreCase = true) &&
                                             !image.workflow.contains("photo_editor", ignoreCase = true)

                        ImageCard(
                            image = image,
                            isSelectable = isEligibleForCharacter,
                            onClick = { 
                                if (isSelectionMode) {
                                    if (!isEligibleForCharacter) {
                                        Toast.makeText(context, context.getString(com.doyouone.drawai.R.string.gallery_msg_invalid_character_source), Toast.LENGTH_SHORT).show()
                                        return@ImageCard
                                    }
                                    
                                    if (characterCount < maxChatLimit) {
                                        pendingCharacterImage = image
                                        showCharacterCreationDialog = true
                                        isSelectionMode = false 
                                    } else {
                                        Toast.makeText(context, context.getString(com.doyouone.drawai.R.string.gallery_msg_limit_reached), Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    selectedImage = image 
                                }
                            },
                            onToggleFavorite = {
                                imageStorage.toggleFavorite(image.id)
                                onToggleFavorite(image.id)
                                // Refresh images
                                scope.launch {
                                    images = imageStorage.getAllImages()
                                }
                            },
                            onReport = {
                                imageToReport = image
                                showReportDialog = true
                            },
                            hasSelectedCharacter = hasSelectedCharacter,
                            isThisImageCharacter = image.id == selectedCharacterImageId,
                            onChatWithCharacter = {
                                if (selectedCharacterId != null) {
                                    onNavigateToCharacterChat(selectedCharacterId!!)
                                } else {
                                    errorDialogMessage = "Error: Character ID not found"
                                    showErrorDialog = true
                                }
                            },
                            onCreateCharacter = {
                                // Deprecated: Triggered via FAB now
                            },
                            canCreateCharacter = characterCount < maxChatLimit,
                            isSelectionMode = isSelectionMode
                        )
                    }
                }
            }
            
            // Banner Ad Logic
            if (shouldShowAds) {
                Spacer(modifier = Modifier.height(8.dp))
                com.doyouone.drawai.ads.BannerAdView(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    // Chat FAB (Star with Animated Particles)
    val infiniteTransition = rememberInfiniteTransition(label = "particleAnimation")
    val animScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val animAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    FloatingActionButton(
        onClick = {
            if (characterCount < maxChatLimit) {
                isSelectionMode = !isSelectionMode
                if(isSelectionMode) {
                    android.widget.Toast.makeText(context, context.getString(com.doyouone.drawai.R.string.gallery_msg_select_character), android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                 android.widget.Toast.makeText(context, context.getString(com.doyouone.drawai.R.string.gallery_msg_limit_reached), android.widget.Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 24.dp, end = 24.dp)
            .size(64.dp),
        containerColor = if (isSelectionMode) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
             if (isSelectionMode) {
                 Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = stringResource(com.doyouone.drawai.R.string.gallery_content_desc_select_char),
                    modifier = Modifier.size(32.dp)
                )
             } else {
                // Particles Effects
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier
                        .size(12.dp)
                        .offset(x = (-14).dp, y = (-12).dp)
                        .scale(animScale)
                        .alpha(animAlpha * 0.7f),
                    tint = Color.White
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier
                        .size(10.dp)
                        .offset(x = 14.dp, y = (-8).dp)
                        .scale(animScale)
                        .alpha(animAlpha * 0.5f),
                    tint = Color.White
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier
                        .size(8.dp)
                        .offset(x = 0.dp, y = 16.dp)
                        .scale(animScale)
                        .alpha(animAlpha * 0.4f),
                    tint = Color.White
                )

                // Main Icon
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = stringResource(com.doyouone.drawai.R.string.gallery_content_desc_create_char),
                    modifier = Modifier.size(32.dp)
                )
             }
        }
    }
    }
        
        // Fullscreen Image Viewer
        if (selectedImage != null) {
            val initialIndex = filteredImages.indexOf(selectedImage!!).takeIf { it >= 0 } ?: 0

            FullscreenImage(
                initialIndex = initialIndex,
                images = filteredImages,
                onDismiss = { selectedImage = null },
                onDelete = { img ->
                    imageToDelete = img
                    showDeleteDialog = true
                },
                onReport = { img ->
                    imageToReport = img
                    showReportDialog = true
                },
                onToggleFavorite = { img ->
                    imageStorage.toggleFavorite(img.id)
                    
                    // Update list
                    scope.launch {
                        images = imageStorage.getAllImages()
                    }
                },
                isPremium = isPremium && !isMoreEnabled,
                onUseAgain = onNavigateToGenerateWithWorkflow,
                onShare = { img ->
                    // Check if user has premium access
                    scope.launch {
                        val userPreferences = com.doyouone.drawai.data.preferences.UserPreferences(context)
                        val isPremiumCheck = userPreferences.isPremium.first()
                        
                        if (!isPremiumCheck) {
                            // Show toast for free users
                            errorDialogMessage = context.getString(com.doyouone.drawai.R.string.gallery_share_premium_only)
                            showErrorDialog = true
                        } else {
                            // Check if guidelines accepted
                            val accepted = userPreferences.hasAcceptedGuidelines.first()
                            if (!accepted) {
                                // Show guidelines dialog
                                pendingShareImage = img
                                showGuidelinesDialog = true
                            } else {
                                // Proceed directly
                                isUploading = true
                                try {
                                    val userDisplayName = authManager.getUserDisplayName()
                                    val post = com.doyouone.drawai.data.model.CommunityPost(
                                        prompt = img.prompt,
                                        negativePrompt = img.negativePrompt ?: "",
                                        workflow = img.workflow,
                                        username = userDisplayName,
                                        createdAt = java.util.Date()
                                    )
                                    
                                    val imageFile = java.io.File(img.imageUrl)
                                    if (imageFile.exists()) {
                                        // Check for duplicate prompt
                                        val isDuplicate = communityRepository.isDuplicatePost(post.prompt, post.workflow)
                                        if (isDuplicate) {
                                            android.widget.Toast.makeText(context, context.getString(com.doyouone.drawai.R.string.gallery_share_duplicate), android.widget.Toast.LENGTH_LONG).show()
                                        } else {
                                            val result = communityRepository.uploadToCommunity(imageFile, post)
                                            result.onSuccess {
                                                android.widget.Toast.makeText(context, context.getString(com.doyouone.drawai.R.string.gallery_share_success), android.widget.Toast.LENGTH_SHORT).show()
                                            }.onFailure { e ->
                                                android.widget.Toast.makeText(context, context.getString(com.doyouone.drawai.R.string.gallery_share_failed, e.message), android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        android.widget.Toast.makeText(context, context.getString(com.doyouone.drawai.R.string.gallery_share_file_not_found), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, context.getString(com.doyouone.drawai.R.string.gallery_share_error, e.message), android.widget.Toast.LENGTH_SHORT).show()
                                } finally {
                                    isUploading = false
                                }
                            }
                        }
                    }
                }
            )
        }
        
        // Download Info Dialog
        if (showDownloadInfoDialog) {
            AlertDialog(
                onDismissRequest = { showDownloadInfoDialog = false },
                title = { Text("Download Gallery") },
                text = { Text("To download your gallery, please go to Settings > Export Gallery.") },
                confirmButton = {
                    TextButton(onClick = { showDownloadInfoDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // Report Dialog
        if (showReportDialog && imageToReport != null) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = {
                    Text(
                        text = stringResource(com.doyouone.drawai.R.string.gallery_report_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(stringResource(com.doyouone.drawai.R.string.gallery_report_desc))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reportReason,
                            onValueChange = { reportReason = it },
                            label = { Text(stringResource(com.doyouone.drawai.R.string.gallery_report_reason_label)) },
                            placeholder = { Text(stringResource(com.doyouone.drawai.R.string.gallery_report_reason_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                isReporting = true
                                try {
                                    imageToReport?.let { image ->
                                        val userId = authManager.getCurrentUserId()
                                        if (userId != null) {
                                            reportRepository.reportImage(
                                                imageId = image.id,
                                                prompt = image.prompt,
                                                negativePrompt = image.negativePrompt ?: "",
                                                workflow = image.workflow,
                                                imageUrl = image.imageUrl,
                                                reportReason = reportReason,
                                                reportedBy = userId
                                            )
                                        }
                                    }
                                    showReportDialog = false
                                    reportReason = ""
                                    imageToReport = null
                                } catch (e: Exception) {
                                    // Handle error
                                    android.util.Log.e("GalleryScreen", "Failed to report image", e)
                                } finally {
                                    isReporting = false
                                }
                            }
                        },
                        enabled = reportReason.isNotBlank() && !isReporting && authManager.getCurrentUserId() != null
                    ) {
                        if (isReporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(com.doyouone.drawai.R.string.gallery_report_button))
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showReportDialog = false 
                            reportReason = ""
                            imageToReport = null
                        }
                    ) {
                        Text(stringResource(com.doyouone.drawai.R.string.gallery_cancel))
                    }
                }
            )
        }
        

        
        // Character Creation Dialog
        if (showCharacterCreationDialog && pendingCharacterImage != null) {

            
            AlertDialog(
                onDismissRequest = { 
                    if (!isCreatingCharacter) {
                        showCharacterCreationDialog = false 
                        pendingCharacterImage = null
                    }
                },
                title = {
                    Text(
                        text = stringResource(com.doyouone.drawai.R.string.gallery_create_char_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(stringResource(com.doyouone.drawai.R.string.gallery_create_char_message))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(com.doyouone.drawai.R.string.gallery_create_char_warning),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = characterName,
                            onValueChange = { characterName = it },
                            label = { Text(stringResource(com.doyouone.drawai.R.string.gallery_char_name_label)) },
                            placeholder = { Text(stringResource(com.doyouone.drawai.R.string.gallery_char_name_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(com.doyouone.drawai.R.string.gallery_select_language_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val languages = listOf(
                            "en" to "English 🇬🇧",
                            "id" to "Indonesia 🇮🇩",
                            "es" to "Español 🇪🇸",
                            "pt" to "Português 🇧🇷",
                            "fr" to "Français 🇫🇷",
                            "de" to "Deutsch 🇩🇪",
                            "zh" to "中文 🇨🇳",
                            "ja" to "日本語 🇯🇵",
                            "ko" to "한국어 🇰🇷",
                            "hi" to "हिन्दी 🇮🇳"
                        )

                        Column {
                            languages.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { (code, name) ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedLanguage = code }
                                                .padding(vertical = 2.dp)
                                        ) {
                                            RadioButton(
                                                selected = selectedLanguage == code,
                                                onClick = { selectedLanguage = code },
                                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                            )
                                            Text(
                                                text = name,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (rowItems.size == 1) {
                                         Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(com.doyouone.drawai.R.string.gallery_select_gender_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedGender == "male",
                                onClick = { selectedGender = "male" },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(stringResource(com.doyouone.drawai.R.string.gallery_gender_male), fontSize = 14.sp)
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            RadioButton(
                                selected = selectedGender == "female",
                                onClick = { selectedGender = "female" },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(stringResource(com.doyouone.drawai.R.string.gallery_gender_female), fontSize = 14.sp)
                        }
                        
                        if (isCreatingCharacter) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    if (characterCreationMessage.isNotEmpty()) characterCreationMessage 
                                    else stringResource(com.doyouone.drawai.R.string.gallery_analyzing_personality),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                isCreatingCharacter = true
                                characterCreationMessage = context.getString(com.doyouone.drawai.R.string.gallery_summoning_soul)
                                
                                try {
                                    val repository = com.doyouone.drawai.data.repository.CharacterRepository()
                                    val result = repository.createCharacter(
                                        imageId = pendingCharacterImage!!.id,
                                        imageUrl = pendingCharacterImage!!.imageUrl,
                                        prompt = pendingCharacterImage!!.prompt,
                                        language = selectedLanguage,
                                        gender = selectedGender,
                                        name = characterName.ifEmpty { null },
                                        seed = pendingCharacterImage!!.seed, // Pass seed for character consistency
                                        workflow = pendingCharacterImage!!.workflow // Pass workflow for style consistency
                                    )
                                    
                                    result.onSuccess { response ->
                                        characterCreationMessage = context.getString(com.doyouone.drawai.R.string.gallery_character_created)
                                        hasSelectedCharacter = true
                                        selectedCharacterId = response.character?.id
                                        selectedCharacterImageId = response.character?.imageId
                                        
                                        // Increment chat limit in Firebase (+1)

                                        
                                        // Navigate to summoning animation with character data
                                        if (response.character?.id != null) {
                                            val sinCount = response.character.personality?.sinCount ?: 1
                                            val rarity = response.character.personality?.rarity ?: "Common"
                                            val imageUrl = response.character.imageUrl ?: pendingCharacterImage!!.imageUrl
                                            
                                            // Close dialog and navigate to summoning animation
                                            showCharacterCreationDialog = false
                                            pendingCharacterImage = null
                                            
                                            onNavigateToSummoningAnimation(
                                                response.character.id,
                                                imageUrl,
                                                sinCount,
                                                rarity
                                            )
                                        }
                                    }.onFailure { e ->
                                        errorDialogMessage = ErrorUtils.getSafeErrorMessage(e)
                                        showErrorDialog = true
                                    }
                                } catch (e: Exception) {
                                    errorDialogMessage = ErrorUtils.getSafeErrorMessage(e)
                                    showErrorDialog = true
                                } finally {
                                    isCreatingCharacter = false
                                    characterCreationMessage = ""
                                }
                            }
                        },
                        enabled = !isCreatingCharacter,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(com.doyouone.drawai.R.string.gallery_create_and_chat))
                    }
                },
                dismissButton = {
                    if (!isCreatingCharacter) {
                        TextButton(
                            onClick = { 
                                showCharacterCreationDialog = false 
                                pendingCharacterImage = null
                            }
                        ) {
                            Text(stringResource(com.doyouone.drawai.R.string.gallery_cancel))
                        }
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog && imageToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        text = stringResource(com.doyouone.drawai.R.string.gallery_delete_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(stringResource(com.doyouone.drawai.R.string.gallery_delete_message))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            imageToDelete?.let { image ->
                                imageStorage.deleteImage(image.id)
                                images = images.filter { it.id != image.id }
                                refreshTrigger++
                            }
                            showDeleteDialog = false
                            selectedImage = null
                            imageToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(com.doyouone.drawai.R.string.gallery_delete_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(com.doyouone.drawai.R.string.gallery_cancel))
                    }
                }
            )
        }
        
        // Community Guidelines Dialog
        if (showGuidelinesDialog) {
            AlertDialog(
                onDismissRequest = { showGuidelinesDialog = false },
                icon = {
                    Text("⚠️", fontSize = 48.sp)
                },
                title = {
                    Text(
                        stringResource(com.doyouone.drawai.R.string.gallery_guidelines_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            stringResource(com.doyouone.drawai.R.string.gallery_guidelines_intro),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        GuidelineItem(stringResource(com.doyouone.drawai.R.string.gallery_guideline_nsfw))
                        GuidelineItem(stringResource(com.doyouone.drawai.R.string.gallery_guideline_violence))
                        GuidelineItem(stringResource(com.doyouone.drawai.R.string.gallery_guideline_illegal))
                        GuidelineItem(stringResource(com.doyouone.drawai.R.string.gallery_guideline_hate))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Red.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    stringResource(com.doyouone.drawai.R.string.gallery_guidelines_warning_title),
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    stringResource(com.doyouone.drawai.R.string.gallery_guidelines_warning_text),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                userPreferences.setGuidelinesAccepted(true)
                                pendingShareImage?.let { image ->
                                    isUploading = true
                                    try {
                                        // Check for duplicate prompt first
                                        val isDuplicate = communityRepository.isDuplicatePost(image.prompt, image.workflow)
                                        if (isDuplicate) {
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(com.doyouone.drawai.R.string.gallery_share_already_shared),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                            isUploading = false
                                            showGuidelinesDialog = false
                                            pendingShareImage = null
                                            return@launch
                                        }

                                        val imageFile = File(image.imageUrl)
                                        if (imageFile.exists()) {
                                            val post = CommunityPost(
                                                prompt = image.prompt,
                                                negativePrompt = image.negativePrompt,
                                                workflow = image.workflow
                                            )
                                            
                                            val result = communityRepository.uploadToCommunity(imageFile, post)
                                            
                                            if (result.isSuccess) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(com.doyouone.drawai.R.string.gallery_share_success_full),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "❌ Failed to share: ${result.exceptionOrNull()?.message}",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                "❌ Image file not found",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "❌ Error: ${e.message}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        isUploading = false
                                        showGuidelinesDialog = false
                                        pendingShareImage = null
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(com.doyouone.drawai.R.string.gallery_guidelines_agree))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showGuidelinesDialog = false
                            pendingShareImage = null
                        }
                    ) {
                        Text(stringResource(com.doyouone.drawai.R.string.gallery_cancel))
                    }
                }
            )
        }


}

@Composable
fun GuidelineItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

// Helper function for date formatting
fun formatDate(dateString: String): String {
    // Simple date formatting - in real app use proper date formatting
    return dateString.substring(0, 10)
}

@Composable
fun FilterChip(
    filter: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = filter,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ImageCard(
    image: GeneratedImage,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onReport: () -> Unit,
    hasSelectedCharacter: Boolean = false,
    isThisImageCharacter: Boolean = false,
    onChatWithCharacter: () -> Unit = {},
    onCreateCharacter: () -> Unit = {},
    canCreateCharacter: Boolean = true,
    isSelectionMode: Boolean = false,
    isSelectable: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable { onClick() }
            .then(
                if (isSelectionMode && !isThisImageCharacter && isSelectable) {
                    Modifier.border(3.dp, Color(0xFF4CAF50), RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image
            AsyncImage(
                model = image.imageUrl,
                contentDescription = image.prompt,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
            
            // Info overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 5.dp, end = 5.dp, bottom = 1.dp)
                    .fillMaxWidth()
            ) {
                // Prompt text removed for cleaner thumbnail view
                
                Column(
                    modifier = Modifier.fillMaxWidth().offset(y = (-6).dp)
                ) {
                    Text(
                        text = image.workflow.replace("_", " ").replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp,
                        color = AccentWhite.copy(alpha = 0.9f),
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 3.dp, vertical = 0.dp)
                    )
                    
                    Text(
                        text = formatDate(image.createdAt),
                        fontSize = 10.sp,
                        color = AccentWhite.copy(alpha = 0.7f)
                    )
                    
                    // Seed display
                    image.seed?.let { seed ->
                        Text(
                            text = "Seed: $seed",
                            fontSize = 9.sp,
                            color = AccentWhite.copy(alpha = 0.6f),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
            
            // Favorite indicator / Toggle
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .background(
                        if (image.isFavorite) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onToggleFavorite() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♥",
                    fontSize = 16.sp,
                    color = if (image.isFavorite) AccentWhite else AccentWhite.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Character interaction buttons
            if (!isThisImageCharacter && canCreateCharacter) {
                // Create Character Icon Removed as it is now handled by FAB Mode
            } else if (isThisImageCharacter) {
                // Show "Chat" button for the selected character
                Button(
                    onClick = onChatWithCharacter,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = stringResource(com.doyouone.drawai.R.string.gallery_chat_button),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Character indicator badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            Color(0xFFFF9800).copy(alpha = 0.95f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(com.doyouone.drawai.R.string.gallery_your_character_badge),
                        fontSize = 9.sp,
                        color = AccentWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}