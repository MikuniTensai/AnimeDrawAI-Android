package com.doyouone.drawai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import com.doyouone.drawai.ui.components.showcaseTarget
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import com.doyouone.drawai.data.model.Workflow
import com.doyouone.drawai.data.api.RetrofitClient
import com.doyouone.drawai.util.DummyData
import com.doyouone.drawai.ui.theme.*
import com.doyouone.drawai.data.repository.FavoritesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Face
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.doyouone.drawai.ui.components.MaintenancePopup
import com.doyouone.drawai.ui.components.WelcomePopup
import com.doyouone.drawai.ui.components.AnimeDrawMainTopBar
import com.doyouone.drawai.viewmodel.GenerateUiState
import com.doyouone.drawai.data.repository.WelcomeMessageRepository
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import com.doyouone.drawai.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Fungsi untuk menghitung jumlah kolom grid berdasarkan lebar layar
@Composable
fun getResponsiveGridColumns(): Int {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    
    return when {
        screenWidthDp >= 1200 -> 5 // Tablet besar atau desktop
        screenWidthDp >= 900 -> 4  // Tablet medium
        screenWidthDp >= 600 -> 3  // Tablet kecil atau phone landscape
        else -> 2                  // Phone portrait
    }
}

// Session tracker for welcome popup (resets when app is closed/killed)
private object WelcomeSessionTracker {
    var welcomeShownThisSession = false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onWorkflowSelected: (String) -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToBackgroundRemover: () -> Unit = {},
    onNavigateToBackgroundRemoverAdvanced: () -> Unit = {},
    onNavigateToUpscaleImage: () -> Unit = {},
    onNavigateToMakeBackground: () -> Unit = {},
    onNavigateToMakeBackgroundAdvanced: () -> Unit = {},
    onNavigateToFaceRestore: () -> Unit = {},
    onNavigateToSketchToImage: () -> Unit = {},
    onNavigateToDrawToImage: () -> Unit = {},
    userId: String = "",
    onOpenDrawer: () -> Unit = {},
    viewModel: com.doyouone.drawai.viewmodel.GenerateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userPreferences = remember { com.doyouone.drawai.data.preferences.UserPreferences(context) }
    val shouldShowAds by userPreferences.shouldShowAds.collectAsState(initial = true)
    val isPremium by userPreferences.isPremium.collectAsState(initial = false)
    
    // NEW: Get generation limit from ViewModel to check subscription type
    val generationLimit by viewModel.generationLimit.collectAsState()
    val gemCount by viewModel.gemCount.collectAsState()
    
    // Load generation limit on screen start
    LaunchedEffect(Unit) {
        viewModel.loadGenerationLimit()
    }
    
    // NEW: Determine if user has premium access (considers both UserPreferences and subscription type)
    val userHasPremiumAccess = remember(isPremium, generationLimit) {
        val subscriptionType = generationLimit?.subscriptionType ?: "free"
        isPremium || subscriptionType == "basic" || subscriptionType == "pro"
    }
    
    // Enhanced debug logging
    LaunchedEffect(isPremium, generationLimit) {
        android.util.Log.d("HomeScreen_Debug", "=== PREMIUM ACCESS DEBUG ===")
        android.util.Log.d("HomeScreen_Debug", "UserPreferences.isPremium: $isPremium")
        android.util.Log.d("HomeScreen_Debug", "GenerationLimit object: $generationLimit")
        android.util.Log.d("HomeScreen_Debug", "GenerationLimit.isPremium: ${generationLimit?.isPremium}")
        android.util.Log.d("HomeScreen_Debug", "GenerationLimit.subscriptionType: ${generationLimit?.subscriptionType}")
        android.util.Log.d("HomeScreen_Debug", "GenerationLimit.subscriptionLimit: ${generationLimit?.subscriptionLimit}")
        android.util.Log.d("HomeScreen_Debug", "GenerationLimit.subscriptionUsed: ${generationLimit?.subscriptionUsed}")
        android.util.Log.d("HomeScreen_Debug", "GenerationLimit.subscriptionEndDate: ${generationLimit?.subscriptionEndDate}")
        android.util.Log.d("HomeScreen_Debug", "Final userHasPremiumAccess: $userHasPremiumAccess")
        android.util.Log.d("HomeScreen_Debug", "=== END PREMIUM ACCESS DEBUG ===")
    }
    
    // Gem Reward Listener
    LaunchedEffect(Unit) {
         viewModel.gemRewardEvent.collect { earned: Int ->
             android.widget.Toast.makeText(context, "+$earned Gems! 💎", android.widget.Toast.LENGTH_SHORT).show()
         }
    }

    // Daily Rewards
    val dailyStatus by viewModel.dailyStatus.collectAsState()
    var showDailyDialog by remember { mutableStateOf(false) }
    var showShopDialog by remember { mutableStateOf(false) }

    // Check daily status on screen load
    LaunchedEffect(Unit) {
        viewModel.checkDailyStatus()
    }

    LaunchedEffect(dailyStatus) {
        if (dailyStatus?.isClaimable == true && !showDailyDialog) {
            showDailyDialog = true
        }
    }
    
    // Claim Result Listener
    LaunchedEffect(Unit) {
        viewModel.claimResult.collect { result: com.doyouone.drawai.data.model.DailyClaimResponse ->
            if (result.success) {
                // Determine what to show
                val rewardText = result.reward?.let { "${it.amount} ${it.name}" } ?: "Reward"
                
                if (result.streakFreezeUsed) {
                     android.widget.Toast.makeText(context, "Streak Saved with Freeze! ❄️", android.widget.Toast.LENGTH_LONG).show()
                }
                
                android.widget.Toast.makeText(context, "Claimed: $rewardText! 🎉", android.widget.Toast.LENGTH_SHORT).show()
                showDailyDialog = false
            } else {
                 android.widget.Toast.makeText(context, "Failed: ${result.error}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showDailyDialog && dailyStatus != null) {
        val status = dailyStatus!!
        com.doyouone.drawai.ui.components.DailyLoginDialog(
            currentStreak = status.currentStreak,
            isClaimable = status.isClaimable,
            nextDayIndex = status.nextDayIndex,
            rewardCycle = status.rewardCycle ?: "A",
            streakSaved = status.streakSaved,
            onClaim = {
                viewModel.claimDailyReward()
            },
            onDismiss = {
                showDailyDialog = false
            }
        )
    }

    if (showShopDialog) {
        com.doyouone.drawai.ui.components.ShopDialog(
            onDismiss = { showShopDialog = false },
            onPurchaseBooster = { callback ->
                 viewModel.purchaseDailyBooster { success, msg ->
                     callback(success, msg)
                 }
            }
        )
    }
    
    var searchQuery by remember { mutableStateOf("") }
    val defaultFilterLabel = stringResource(R.string.filter_all)
    var selectedFilter by remember { mutableStateOf(defaultFilterLabel) }
    val filters = listOf(
        stringResource(R.string.filter_all),
        stringResource(R.string.filter_favorites),
        stringResource(R.string.filter_anime),
        stringResource(R.string.filter_general),
        stringResource(R.string.filter_animal),
        stringResource(R.string.filter_flower),
        stringResource(R.string.filter_food),
        stringResource(R.string.filter_background)
    )
    val scope = rememberCoroutineScope()

    // Filter Sheet State
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedSortOption by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // Sort Labels (cached for comparison)
    val mostViewedLabel = stringResource(R.string.workflows_most_viewed)
    val newestLabel = stringResource(R.string.workflows_newest)
    val mostPopularLabel = stringResource(R.string.workflows_most_popular)
    
    // Workflow caching mechanism
    val sharedPrefs = remember { 
        context.getSharedPreferences("workflow_cache", android.content.Context.MODE_PRIVATE) 
    }
    var cachedWorkflows by remember { mutableStateOf<Map<String, com.doyouone.drawai.data.model.WorkflowInfo>>(emptyMap()) }
    var isLoadingFromCache by remember { mutableStateOf(true) }
    
    // Load cached workflows from SharedPreferences
    LaunchedEffect(Unit) {
        try {
            val cachedJson = sharedPrefs.getString("workflows_data", null)
            val cacheTimestamp = sharedPrefs.getLong("workflows_timestamp", 0)
            val currentTime = System.currentTimeMillis()
            val cacheAge = currentTime - cacheTimestamp
            val maxCacheAge = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
            
            if (cachedJson != null && cacheAge < maxCacheAge) {
                // Parse cached workflows
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<Map<String, com.doyouone.drawai.data.model.WorkflowInfo>>() {}.type
                cachedWorkflows = gson.fromJson(cachedJson, type) ?: emptyMap()
                android.util.Log.d("HomeScreen", "Loaded ${cachedWorkflows.size} workflows from cache (age: ${cacheAge / 1000 / 60} minutes)")
            } else {
                android.util.Log.d("HomeScreen", "Cache is old or empty, will fetch from API")
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Failed to load cached workflows: ${e.message}")
        } finally {
            isLoadingFromCache = false
        }
    }
    
    // Load workflows from API
    val workflowsMap by viewModel.workflows.collectAsState()
    val workflowStats by viewModel.workflowStats.collectAsState()
    val uiState: GenerateUiState by viewModel.uiState.collectAsState()
    
    // Save workflows to cache when they're loaded from API
    LaunchedEffect(workflowsMap) {
        if (workflowsMap.isNotEmpty()) {
            try {
                val gson = com.google.gson.Gson()
                val json = gson.toJson(workflowsMap)
                sharedPrefs.edit()
                    .putString("workflows_data", json)
                    .putLong("workflows_timestamp", System.currentTimeMillis())
                    .apply()
                android.util.Log.d("HomeScreen", "Saved ${workflowsMap.size} workflows to cache")
            } catch (e: Exception) {
                android.util.Log.e("HomeScreen", "Failed to save workflows to cache: ${e.message}")
            }
        }
    }
    
    // Favorites
    val favoritesRepo = remember(userId) { 
        if (userId.isNotEmpty()) {
            android.util.Log.d("HomeScreen", "Creating FavoritesRepo for userId: $userId")
            com.doyouone.drawai.data.repository.FavoritesRepository(userId)
        } else null 
    }
    val favoriteIds by favoritesRepo?.getFavorites()?.collectAsState(initial = emptyList()) 
        ?: remember { mutableStateOf(emptyList()) }
    
    // Local favorites (shared across app)
    val localFavoriteIds by com.doyouone.drawai.data.local.LocalFavoritesManager.favoriteIds.collectAsState()
    
    // Merge Firestore and local favorites
    val allFavoriteIds = remember(favoriteIds, localFavoriteIds) {
        (favoriteIds + localFavoriteIds).toSet().toList()
    }
    
    // Log favorite IDs for debugging
    LaunchedEffect(allFavoriteIds) {
        android.util.Log.d("HomeScreen", "Current favorite IDs: $allFavoriteIds")
    }
    
    // Convert API workflows to UI model with stats
    // Use cached workflows if API workflows are empty
    val activeWorkflowsMap = remember(workflowsMap, cachedWorkflows, isLoadingFromCache) {
        if (workflowsMap.isNotEmpty()) workflowsMap
        else if (!isLoadingFromCache && cachedWorkflows.isNotEmpty()) cachedWorkflows
        else emptyMap()
    }
    
    val workflows: List<Workflow> = remember(activeWorkflowsMap, workflowStats) {
        activeWorkflowsMap.map { (id, info) ->
            val stats = workflowStats[id]
            val viewCount = stats?.get("viewCount") ?: 0L
            val generationCount = stats?.get("generationCount") ?: 0L
            
            Workflow(
                id = id,
                name = info.name,
                description = info.description,
                estimatedTime = info.estimatedTime,
                category = when {
                    id.contains("anime", ignoreCase = true) -> "Anime"
                    id.contains("animal", ignoreCase = true) -> "Animal"
                    id.contains("flower", ignoreCase = true) -> "Flower"
                    id.contains("background", ignoreCase = true) -> "Background"
                    id.contains("food", ignoreCase = true) -> "Food"
                    else -> "General"
                },
                isFavorite = false,
                viewCount = viewCount,
                generationCount = generationCount
            )
        }.toList()
    }

    val welcomeRepo = remember { WelcomeMessageRepository() }
    var welcomeData by remember { mutableStateOf<com.doyouone.drawai.data.repository.WelcomeMessageData?>(null) }
    var showWelcome by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Only fetch and show welcome if not shown yet in this session
        if (!WelcomeSessionTracker.welcomeShownThisSession) {
            val result = welcomeRepo.getWelcomeMessageData()
            val data = result.getOrNull()
            if (data != null && data.isActive && data.message.isNotEmpty()) {
                welcomeData = data
                showWelcome = true
                WelcomeSessionTracker.welcomeShownThisSession = true // Mark as shown for this session
            }
        }
    }

    // Only show maintenance popup if both API and cache are empty and not loading
    if (workflows.isEmpty() && uiState !is GenerateUiState.Loading && !isLoadingFromCache && cachedWorkflows.isEmpty()) {
        MaintenancePopup(
            onGoToGalleryClick = onNavigateToGallery,
            onRetry = {
                viewModel.loadWorkflows()
                viewModel.loadWorkflowStats()
            }
        )
    }
    
    // Data sorting - REAL STATS
    val mostViewed = remember(workflows) { workflows.sortedByDescending { it.viewCount } }
    val newest = remember(workflows) { workflows.sortedBy { it.generationCount } } // Low usage = New
    val mostPopular = remember(workflows) { workflows.sortedByDescending { it.generationCount } }
    
    // Load workflows on first composition (and reload stats)
    // Only load from API if cache is empty or old
    LaunchedEffect(Unit) {
        // Always try to load from API in background to refresh cache
        if (cachedWorkflows.isEmpty() || !isLoadingFromCache) {
            viewModel.loadWorkflows()
            viewModel.loadWorkflowStats()
        } else {
            // Load in background even if cache exists (for fresh data)
            kotlinx.coroutines.delay(1000L) // Small delay to show cached content first
            viewModel.loadWorkflows()
            viewModel.loadWorkflowStats()
        }
    }

    // Get filter labels for comparison
    val filterAll = stringResource(R.string.filter_all)
    val filterFavorites = stringResource(R.string.filter_favorites)
    val filterAnime = stringResource(R.string.filter_anime)
    val filterGeneral = stringResource(R.string.filter_general)
    val filterAnimal = stringResource(R.string.filter_animal)
    val filterFlower = stringResource(R.string.filter_flower)
    val filterFood = stringResource(R.string.filter_food)
    val filterBackground = stringResource(R.string.filter_background)
    
    val randomSeed = remember { System.currentTimeMillis() }
    
    val filteredWorkflows = remember(searchQuery, selectedFilter, selectedSortOption, workflows, allFavoriteIds) {
        com.doyouone.drawai.util.WorkflowFilterUtils.filterAndSortWorkflows(
            workflows = workflows,
            searchQuery = searchQuery,
            selectedFilter = selectedFilter,
            selectedSortOption = selectedSortOption,
            allFavoriteIds = allFavoriteIds,
            filterAllLabel = filterAll,
            filterFavoritesLabel = filterFavorites,
            filterAnimeLabel = filterAnime,
            filterGeneralLabel = filterGeneral,
            filterAnimalLabel = filterAnimal,
            filterFlowerLabel = filterFlower,
            filterFoodLabel = filterFood,
            filterBackgroundLabel = filterBackground,
            sortMostViewedLabel = mostViewedLabel,
            sortNewestLabel = newestLabel,
            sortMostPopularLabel = mostPopularLabel,
            randomSeed = randomSeed
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (showWelcome && welcomeData != null) {
                com.doyouone.drawai.ui.components.WelcomePopupEnhanced(
                    data = welcomeData!!,
                    onDismiss = { showWelcome = false }
                )
            }
            // Header with Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                AnimeDrawMainTopBar(
                    title = stringResource(R.string.discover_workflows_title),
                    onOpenDrawer = onOpenDrawer,
                    actions = {
                         com.doyouone.drawai.ui.components.GemIndicator(
                             gemCount = gemCount,
                             modifier = Modifier.padding(end = 8.dp),
                             onClick = { showShopDialog = true }
                         )
                         
                         IconButton(onClick = { showDailyDialog = true }) {
                             Icon(
                                 imageVector = Icons.Filled.DateRange, // Calendar icon
                                 contentDescription = "Daily Reward",
                                 tint = MaterialTheme.colorScheme.primary
                             )
                         }
                    }
                )
                
            // Search Bar & Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.search_workflows)) },
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )

                FilledTonalIconButton(
                    onClick = { showFilterSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(56.dp), // Match height of OutlinedTextField approximately
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Filter"
                    )
                }
            }
            

            }
            
            
            // Unified Grid Content
            // Grid State
            var gridColumns by remember { mutableIntStateOf(2) }

            // Unified Content Container (Scrollable Column)
            // Unified Content Container (Scrollable Column)
            val scrollState = rememberScrollState()
            Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val isContentFilterSelected = selectedFilter.contains("Content", ignoreCase = true) || selectedFilter.contains("Background", ignoreCase = true)

                // Banner: Always at Top (Not affected by filters)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable {
                            onNavigateToDrawToImage()
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    coil.compose.AsyncImage(
                        model = R.drawable.create_imagination_banner,
                        contentDescription = "Create My Imagination",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                val WorkflowSection = @Composable {
                    // 1. Workflow Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Workflows (${filteredWorkflows.size})",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Refresh Button
                            IconButton(onClick = { 
                                viewModel.loadWorkflows()
                                viewModel.loadWorkflowStats()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Eye Icon (Grid Toggle)
                            IconButton(onClick = { 
                                 gridColumns = when(gridColumns) {
                                    2 -> 3
                                    3 -> 5
                                    else -> 2
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_view), // Eye/View icon
                                    contentDescription = "Toggle View",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            TextButton(onClick = { onNavigateToCategory("all") }) {
                                Text("See All")
                            }
                        }
                    }
    
                    // 2. Workflow Grid (Fixed Height "Inner Slider")
                    if (filteredWorkflows.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                             Text(
                                text = "No workflows found", 
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = {
                                viewModel.loadWorkflows()
                                viewModel.loadWorkflowStats()
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp) // Fixed height for inner scroll
                        ) {
                            filteredWorkflows.forEachIndexed { index, workflow ->
                                item(key = workflow.id) {
                                    val wfInfo = workflowsMap[workflow.id]
                                    val isPremium = wfInfo?.isPremium ?: (workflow.id == "anime_premium_ultra")
        
                                    // Add showcaseTarget to first workflow card
                                    val cardModifier = if (index == 0) {
                                        Modifier.showcaseTarget("workflow_item")
                                    } else {
                                        Modifier
                                    }
                                    
                                    com.doyouone.drawai.ui.components.WorkflowCard(
                                        workflow = workflow,
                                        modifier = cardModifier,
                                        isPremium = isPremium,
                                        userIsPremium = userHasPremiumAccess,
                                        onClick = { onWorkflowSelected(workflow.id) },
                                        onUpgradeClick = onNavigateToSubscription,
                                        isFavorite = workflow.id in allFavoriteIds,
                                        onFavoriteClick = { id ->
                                            com.doyouone.drawai.data.local.LocalFavoritesManager.toggleFavorite(id)
                                            scope.launch { favoritesRepo?.toggleFavorite(id) }
                                        },
                                        gridColumns = gridColumns
                                    )
                                }
                                
                                // Add native ad every 6 items (full width) - ONLY IF NOT PREMIUM
                                if (!userHasPremiumAccess && (index + 1) % 6 == 0 && index < filteredWorkflows.size - 1) {
                                    item(
                                        key = "native_ad_$index",
                                        span = { androidx.compose.foundation.lazy.grid.GridItemSpan(gridColumns) }
                                    ) {
                                        com.doyouone.drawai.ads.NativeAdCard(
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val ContentSectionBlock = @Composable {
                    // 3. Content Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 100.dp)
                    ) {
                        // Header (matching Workflows style)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Content",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
    
                        // Content Grid (3x*)
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                            modifier = Modifier.height(430.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            userScrollEnabled = false
                        ) {
                             item {
                                 // Background Remover Item
                                 Card(
                                     modifier = Modifier
                                         .clickable { onNavigateToBackgroundRemover() },
                                     shape = RoundedCornerShape(12.dp),
                                     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                 ) {
                                     Box(modifier = Modifier
                                         .fillMaxSize()
                                         .aspectRatio(1f)
                                         .background(MaterialTheme.colorScheme.surfaceVariant)) {
                                         // Image
                                         AsyncImage(
                                             model = androidx.compose.ui.platform.LocalContext.current.let { 
                                                 coil.request.ImageRequest.Builder(it)
                                                     .data("https://drawai-api.drawai.site/workflow-image/background_remover_v1")
                                                     .crossfade(true)
                                                     .build()
                                             },
                                             contentDescription = "Background Remover",
                                             contentScale = ContentScale.Crop,
                                             modifier = Modifier.fillMaxSize()
                                         )
                                         // Label Overlay (Gradient)
                                         Box(
                                             modifier = Modifier
                                                 .align(Alignment.BottomCenter)
                                                 .fillMaxWidth()
                                                 .background(
                                                     androidx.compose.ui.graphics.Brush.verticalGradient(
                                                         colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                     )
                                                 )
                                                 .padding(4.dp)
                                         ) {
                                             Text(
                                                 text = "Remove BG",
                                                 fontSize = 10.sp,
                                                 fontWeight = FontWeight.SemiBold,
                                                 color = Color.White,
                                                 textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                 modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                                 maxLines = 1,
                                                 overflow = TextOverflow.Ellipsis
                                             )
                                         }
                                         
                                         // Tag Badge "NEW"
                                         Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                             Text(
                                                 text = "NEW",
                                                 fontSize = 8.sp,
                                                 fontWeight = FontWeight.Bold,
                                                 color = Color.White,
                                                 modifier = Modifier
                                                     .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                                     .padding(horizontal = 4.dp, vertical = 2.dp)
                                             )
                                         }
                                     }
                                 }
                             }
                             
                             item {
                                 // Advanced Background Remover Item
                                 Card(
                                     modifier = Modifier
                                         .clickable { onNavigateToBackgroundRemoverAdvanced() },
                                     shape = RoundedCornerShape(12.dp),
                                     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                 ) {
                                     Box(modifier = Modifier
                                         .fillMaxSize()
                                         .aspectRatio(1f)
                                         .background(MaterialTheme.colorScheme.surfaceVariant)) {
                                         // Image
                                         AsyncImage(
                                             model = androidx.compose.ui.platform.LocalContext.current.let { 
                                                 coil.request.ImageRequest.Builder(it)
                                                     .data("https://drawai-api.drawai.site/workflow-image/background_remover_v2")
                                                     .crossfade(true)
                                                     .build()
                                             },
                                             contentDescription = "Background Remover Advanced",
                                             contentScale = ContentScale.Crop,
                                             modifier = Modifier.fillMaxSize()
                                         )
                                         // Label Overlay (Gradient)
                                         Box(
                                             modifier = Modifier
                                                 .align(Alignment.BottomCenter)
                                                 .fillMaxWidth()
                                                 .background(
                                                     androidx.compose.ui.graphics.Brush.verticalGradient(
                                                         colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                     )
                                                 )
                                                 .padding(4.dp)
                                         ) {
                                             Text(
                                                 text = "Adv Remove BG",
                                                 fontSize = 10.sp,
                                                 fontWeight = FontWeight.SemiBold,
                                                 color = Color.White,
                                                 textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                 modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                                 maxLines = 1,
                                                 overflow = TextOverflow.Ellipsis
                                             )
                                         }
                                         
                                         // Icon Badge
                                         Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                            Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                         }
                                     }
                                 }
                             }
     
                             item {
                                 // Face Restore Item
                                 Card(
                                     modifier = Modifier
                                         .clickable { onNavigateToFaceRestore() },
                                     shape = RoundedCornerShape(12.dp),
                                     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                 ) {
                                     Box(modifier = Modifier
                                         .fillMaxSize()
                                         .aspectRatio(1f)
                                         .background(MaterialTheme.colorScheme.surfaceVariant)) {
                                         // Image
                                         AsyncImage(
                                             model = androidx.compose.ui.platform.LocalContext.current.let { 
                                                 coil.request.ImageRequest.Builder(it)
                                                     .data("https://drawai-api.drawai.site/workflow-image/face_restore_v1")
                                                     .crossfade(true)
                                                     .build()
                                             },
                                             contentDescription = "Face Restore",
                                             contentScale = ContentScale.Crop,
                                             modifier = Modifier.fillMaxSize()
                                         )
                                         // Label Overlay (Gradient)
                                         Box(
                                             modifier = Modifier
                                                 .align(Alignment.BottomCenter)
                                                 .fillMaxWidth()
                                                 .background(
                                                     androidx.compose.ui.graphics.Brush.verticalGradient(
                                                         colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                     )
                                                 )
                                                 .padding(4.dp)
                                         ) {
                                             Text(
                                                 text = "Face Restore",
                                                 fontSize = 10.sp,
                                                 fontWeight = FontWeight.SemiBold,
                                                 color = Color.White,
                                                 textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                 modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                                 maxLines = 1,
                                                 overflow = TextOverflow.Ellipsis
                                             )
                                         }
                                         
                                         // Icon Badge
                                         Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                            Icon(Icons.Default.Face, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                         }
                                     }
                                 }
                             }
     
                             item {
                                 // Upscale Image Item
                                 Card(
                                     modifier = Modifier
                                         .clickable { onNavigateToUpscaleImage() },
                                     shape = RoundedCornerShape(12.dp),
                                     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                 ) {
                                     Box(modifier = Modifier
                                         .fillMaxSize()
                                         .aspectRatio(1f)
                                         .background(MaterialTheme.colorScheme.surfaceVariant)) {
                                         // Image
                                         AsyncImage(
                                             model = androidx.compose.ui.platform.LocalContext.current.let { 
                                                 coil.request.ImageRequest.Builder(it)
                                                     .data("https://drawai-api.drawai.site/workflow-image/upscale_image_super_resolution")
                                                     .crossfade(true)
                                                     .build()
                                             },
                                             contentDescription = "Upscale Image",
                                             contentScale = ContentScale.Crop,
                                             modifier = Modifier.fillMaxSize()
                                         )
                                         // Label Overlay (Gradient)
                                         Box(
                                             modifier = Modifier
                                                 .align(Alignment.BottomCenter)
                                                 .fillMaxWidth()
                                                 .background(
                                                     androidx.compose.ui.graphics.Brush.verticalGradient(
                                                         colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                     )
                                                 )
                                                 .padding(4.dp)
                                         ) {
                                             Text(
                                                 text = "Super Upscale",
                                                 fontSize = 10.sp,
                                                 fontWeight = FontWeight.SemiBold,
                                                 color = Color.White,
                                                 textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                 modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                                 maxLines = 1,
                                                 overflow = TextOverflow.Ellipsis
                                             )
                                         }
                                         
                                         // Icon Badge
                                         Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                         }
                                     }
                                 }
                             }
                             
                             item {
                                 // Make Background Item
                                 Card(
                                     modifier = Modifier
                                         .clickable { onNavigateToMakeBackground() },
                                     shape = RoundedCornerShape(12.dp),
                                     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                 ) {
                                     Box(modifier = Modifier
                                         .fillMaxSize()
                                         .aspectRatio(1f)
                                         .background(MaterialTheme.colorScheme.surfaceVariant)) {
                                         // Image
                                         AsyncImage(
                                             model = androidx.compose.ui.platform.LocalContext.current.let { 
                                                 coil.request.ImageRequest.Builder(it)
                                                     .data("https://drawai-api.drawai.site/workflow-image/make_background_v1")
                                                     .crossfade(true)
                                                     .build()
                                             },
                                             contentDescription = "Make Background",
                                             contentScale = ContentScale.Crop,
                                             modifier = Modifier.fillMaxSize()
                                         )
                                         // Label Overlay (Gradient)
                                         Box(
                                             modifier = Modifier
                                                 .align(Alignment.BottomCenter)
                                                 .fillMaxWidth()
                                                 .background(
                                                     androidx.compose.ui.graphics.Brush.verticalGradient(
                                                         colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                     )
                                                 )
                                                 .padding(4.dp)
                                         ) {
                                             Text(
                                                 text = "Make Background",
                                                 fontSize = 10.sp,
                                                 fontWeight = FontWeight.SemiBold,
                                                 color = Color.White,
                                                 textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                 modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                                 maxLines = 1,
                                                 overflow = TextOverflow.Ellipsis
                                             )
                                         }
                                         
                                         // Tag Badge "NEW"
                                         Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                             Text(
                                                 text = "NEW",
                                                 fontSize = 8.sp,
                                                 fontWeight = FontWeight.Bold,
                                                 color = Color.White,
                                                 modifier = Modifier
                                                     .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                                     .padding(horizontal = 4.dp, vertical = 2.dp)
                                             )
                                         }
                                     }
                                 }
                             }

                             item {
                                 // Make Background Advanced Item
                                 Card(
                                     modifier = Modifier
                                         .clickable { onNavigateToMakeBackgroundAdvanced() },
                                     shape = RoundedCornerShape(12.dp),
                                     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                 ) {
                                     Box(modifier = Modifier
                                         .fillMaxSize()
                                         .aspectRatio(1f)
                                         .background(MaterialTheme.colorScheme.surfaceVariant)) {
                                         // Image
                                         AsyncImage(
                                             model = androidx.compose.ui.platform.LocalContext.current.let { 
                                                 coil.request.ImageRequest.Builder(it)
                                                     .data("https://drawai-api.drawai.site/workflow-image/make_background_v2")
                                                     .crossfade(true)
                                                     .build()
                                             },
                                             contentDescription = "Make Background Advanced",
                                             contentScale = ContentScale.Crop,
                                             modifier = Modifier.fillMaxSize()
                                         )
                                         // Label Overlay (Gradient)
                                         Box(
                                             modifier = Modifier
                                                 .align(Alignment.BottomCenter)
                                                 .fillMaxWidth()
                                                 .background(
                                                     androidx.compose.ui.graphics.Brush.verticalGradient(
                                                         colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                     )
                                                 )
                                                 .padding(4.dp)
                                         ) {
                                             Text(
                                                 text = "Adv Make BG",
                                                 fontSize = 10.sp,
                                                 fontWeight = FontWeight.SemiBold,
                                                 color = Color.White,
                                                 textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                 modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                                 maxLines = 1,
                                                 overflow = TextOverflow.Ellipsis
                                             )
                                         }
                                         
                                         // Icon Badge
                                         Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                            Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                         }
                                     }
                                 }
                             }

                             item {
                                 // Sketch to Image Item
                                 Card(
                                     modifier = Modifier
                                         .clickable { onNavigateToSketchToImage() },
                                     shape = RoundedCornerShape(12.dp),
                                     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                 ) {
                                     Box(modifier = Modifier
                                         .fillMaxSize()
                                         .aspectRatio(1f)
                                         .background(MaterialTheme.colorScheme.surfaceVariant)) {
                                         // Image
                                         AsyncImage(
                                             model = androidx.compose.ui.platform.LocalContext.current.let { 
                                                 coil.request.ImageRequest.Builder(it)
                                                     .data("https://drawai-api.drawai.site/workflow-image/sketch_to_image_drawup")
                                                     .crossfade(true)
                                                     .build()
                                             },
                                             contentDescription = "Sketch to Image",
                                             contentScale = ContentScale.Crop,
                                             modifier = Modifier.fillMaxSize()
                                         )
                                         // Label Overlay (Gradient)
                                         Box(
                                             modifier = Modifier
                                                 .align(Alignment.BottomCenter)
                                                 .fillMaxWidth()
                                                 .background(
                                                     androidx.compose.ui.graphics.Brush.verticalGradient(
                                                         colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                     )
                                                 )
                                                 .padding(4.dp)
                                         ) {
                                             Text(
                                                 text = "Sketch to Image",
                                                 fontSize = 10.sp,
                                                 fontWeight = FontWeight.SemiBold,
                                                 color = Color.White,
                                                 textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                 modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                                 maxLines = 1,
                                                 overflow = TextOverflow.Ellipsis
                                             )
                                         }
                                         
                                         // Tag Badge "NEW"
                                         Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                             Text(
                                                 text = "NEW",
                                                 fontSize = 8.sp,
                                                 fontWeight = FontWeight.Bold,
                                                 color = Color.White,
                                                 modifier = Modifier
                                                     .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                                     .padding(horizontal = 4.dp, vertical = 2.dp)
                                             )
                                         }
                                     }
                                 }
                             }
                             
                             item {
                                 // Draw to Image Item
                                 Card(
                                     modifier = Modifier
                                         .clickable { onNavigateToDrawToImage() },
                                     shape = RoundedCornerShape(12.dp),
                                     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                 ) {
                                     Box(modifier = Modifier
                                         .fillMaxSize()
                                         .aspectRatio(1f)
                                         .background(MaterialTheme.colorScheme.surfaceVariant)) {
                                         // Image
                                         AsyncImage(
                                             model = androidx.compose.ui.platform.LocalContext.current.let { 
                                                 coil.request.ImageRequest.Builder(it)
                                                     .data("https://drawai-api.drawai.site/workflow-image/draw_to_image_drawai")
                                                     .crossfade(true)
                                                     .build()
                                             },
                                             contentDescription = "Draw to Image",
                                             contentScale = ContentScale.Crop,
                                             modifier = Modifier.fillMaxSize()
                                         )
                                         // Label Overlay (Gradient)
                                         Box(
                                             modifier = Modifier
                                                 .align(Alignment.BottomCenter)
                                                 .fillMaxWidth()
                                                 .background(
                                                     androidx.compose.ui.graphics.Brush.verticalGradient(
                                                         colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                     )
                                                 )
                                                 .padding(4.dp)
                                         ) {
                                             Text(
                                                 text = "Draw to Image",
                                                 fontSize = 10.sp,
                                                 fontWeight = FontWeight.SemiBold,
                                                 color = Color.White,
                                                 textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                 modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                                 maxLines = 1,
                                                 overflow = TextOverflow.Ellipsis
                                             )
                                         }
                                         
                                         // Icon Badge
                                         Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                            Icon(Icons.Default.Brush, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                         }
                                     }
                                 }
                             }
                        }
                    }
                }

                // Render based on order
                if (isContentFilterSelected) {
                    ContentSectionBlock()
                    Spacer(Modifier.height(24.dp))
                    WorkflowSection()
                } else {
                    WorkflowSection()
                    ContentSectionBlock()
                }
                }
                 // Scroll Indicator (Arrow)
                androidx.compose.animation.AnimatedVisibility(
                    visible = scrollState.canScrollForward && scrollState.value < 300,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    // Bouncing Animation
                     val infiniteTransition = rememberInfiniteTransition(label = "scroll_indicator")
                     val dy by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 10f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dy"
                    )
                    
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                        contentDescription = "Scroll Down",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha=0.8f),
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer { translationY = dy }
                            .background(MaterialTheme.colorScheme.surface.copy(alpha=0.5f), CircleShape)
                    )
                }
            }
            
            // Banner Ad at bottom (only for free users, not too intrusive)
            // Only show if user scrolled and stayed for a while
            if (shouldShowAds) {
                Spacer(modifier = Modifier.height(8.dp))
                com.doyouone.drawai.ads.BannerAdView(
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (showFilterSheet) {
                com.doyouone.drawai.ui.components.FilterBottomSheet(
                    onDismissRequest = { showFilterSheet = false },
                    sheetState = sheetState,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    selectedSortOption = selectedSortOption,
                    onSortOptionSelected = { selectedSortOption = it },
                    onApply = { showFilterSheet = false }
                )
            }
        }
    }
    }


@Composable
fun WorkflowSection(
    title: String,
    subtitle: String,
    workflows: List<Workflow>,
    onWorkflowClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    onFavoriteClick: (String) -> Unit = {},
    favoriteIds: List<String> = emptyList(),
    workflowsMap: Map<String, com.doyouone.drawai.data.model.WorkflowInfo> = emptyMap(),
    isPremium: Boolean = false,
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var gridColumns by remember { mutableIntStateOf(2) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                 // Grid Toggle Button
                IconButton(onClick = { 
                    gridColumns = when(gridColumns) {
                        2 -> 3
                        3 -> 5
                        else -> 2
                    }
                }) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_view), // Built-in icon as placeholder
                        contentDescription = "Change Grid",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                TextButton(onClick = onSeeAllClick) {
                    Text(
                        text = stringResource(R.string.workflows_see_all),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Grid Display of Workflows (Full Screen) - Responsive Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(500.dp) // Adjust height as needed
        ) {
            workflows.forEachIndexed { index, workflow ->
                item(key = workflow.id) {
                    val workflowInfo = workflowsMap[workflow.id]
                    val workflowIsPremium = workflowInfo?.isPremium ?: (workflow.id == "anime_premium_ultra")
                    
                    // Showcase Logic - Target first item of "Most Viewed" (Top Section)
                    val isShowcaseTarget = title.contains("Most Viewed", ignoreCase = true) && index == 0
                    val modifier = if (isShowcaseTarget) 
                        Modifier.showcaseTarget("workflow_item") 
                    else Modifier

                    val context = androidx.compose.ui.platform.LocalContext.current
                    val wrappedOnClick = {
                        if (com.doyouone.drawai.ui.components.ShowcaseManager.step == 1) {
                             com.doyouone.drawai.ui.components.ShowcaseManager.nextStep(context)
                        }
                        onWorkflowClick(workflow.id)
                    }
                    
                    com.doyouone.drawai.ui.components.WorkflowCard(
                        workflow = workflow,
                        modifier = modifier,
                        onClick = wrappedOnClick,
                        onFavoriteClick = onFavoriteClick,
                        isFavorite = workflow.id in favoriteIds,
                        isPremium = workflowIsPremium,
                        userIsPremium = isPremium,
                        onUpgradeClick = onNavigateToSettings,
                        gridColumns = gridColumns
                    )
                }
                
                // Add native ad every 6 items (full width)
                if ((index + 1) % 6 == 0 && index < workflows.size - 1) {
                    item(
                        key = "native_ad_section_$index",
                        span = { androidx.compose.foundation.lazy.grid.GridItemSpan(gridColumns) }
                    ) {
                        com.doyouone.drawai.ads.NativeAdCard(
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

} // End WorkflowSection
