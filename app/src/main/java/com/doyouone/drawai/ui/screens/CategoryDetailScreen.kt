package com.doyouone.drawai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import com.doyouone.drawai.data.model.Workflow
import com.doyouone.drawai.data.api.RetrofitClient
import com.doyouone.drawai.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.res.stringResource
import com.doyouone.drawai.R
import com.doyouone.drawai.data.repository.FavoritesRepository
import com.doyouone.drawai.data.local.LocalFavoritesManager
import androidx.compose.material.icons.filled.Tune
import kotlinx.coroutines.launch

// Fungsi untuk mengacak urutan workflows
fun List<Workflow>.shuffled(): List<Workflow> {
    return this.shuffled(java.util.Random())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    category: String,
    workflows: List<Workflow>,
    onWorkflowSelected: (String) -> Unit,
    onNavigateToSubscription: () -> Unit = {},
    onBackPressed: () -> Unit,
    userId: String = "" // Added for Favorites sync
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { com.doyouone.drawai.data.preferences.UserPreferences(context) }

    val userIsPremium by userPreferences.isPremium.collectAsState(initial = false)
    
    // Get generation limit to check subscription type (same logic as HomeScreen)
    val viewModel: com.doyouone.drawai.viewmodel.GenerateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val generationLimit by viewModel.generationLimit.collectAsState()
    
    // Determine if user has premium access
    val userHasPremiumAccess = remember(userIsPremium, generationLimit) {
        val subscriptionType = generationLimit?.subscriptionType ?: "free"
        userIsPremium || subscriptionType == "basic" || subscriptionType == "pro"
    }
    
    // Load generation limit on first composition
    LaunchedEffect(Unit) {
        viewModel.loadGenerationLimit()
    }
    
    // Category titles
    val categoryTitle = when (category) {
        "most_viewed" -> stringResource(R.string.workflows_most_viewed)
        "newest" -> stringResource(R.string.workflows_newest)
        "most_popular" -> stringResource(R.string.workflows_most_popular)
        "all" -> "All Workflows" // Handle 'all' title
        else -> category
    }
    
    val categorySubtitle = when (category) {
        "most_viewed" -> stringResource(R.string.category_most_viewed_subtitle)
        "newest" -> stringResource(R.string.category_newest_subtitle)
        "most_popular" -> stringResource(R.string.category_most_popular_subtitle)
        "all" -> "Browse and search all available workflows" // Handle 'all' subtitle
        else -> stringResource(R.string.category_browse_all)
    }
    
    // Favorites Logic
    val favoritesRepo = remember(userId) { 
        if (userId.isNotEmpty()) FavoritesRepository(userId) else null 
    }
    val favoriteIds by favoritesRepo?.getFavorites()?.collectAsState(initial = emptyList()) 
        ?: remember { mutableStateOf(emptyList()) }
    val localFavoriteIds by LocalFavoritesManager.favoriteIds.collectAsState()
    val allFavoriteIds = remember(favoriteIds, localFavoriteIds) {
        (favoriteIds + localFavoriteIds).toSet().toList()
    }

    // State
    var searchQuery by remember { mutableStateOf("") }
    var gridColumns by remember { mutableIntStateOf(2) }
    
    // Filter & Sort State
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedSortOption by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
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
    
    // Filter Labels
    // Filter Labels
    val filterAll = stringResource(R.string.filter_all)
    val filterFavorites = stringResource(R.string.filter_favorites)
    val filterAnime = stringResource(R.string.filter_anime)
    val filterGeneral = stringResource(R.string.filter_general)
    val filterAnimal = stringResource(R.string.filter_animal)
    val filterFlower = stringResource(R.string.filter_flower)
    val filterFood = stringResource(R.string.filter_food)
    val filterBackground = stringResource(R.string.filter_background)
    
    val mostViewedLabel = stringResource(R.string.workflows_most_viewed)
    val newestLabel = stringResource(R.string.workflows_newest)
    val mostPopularLabel = stringResource(R.string.workflows_most_popular)
    
    val randomSeed = remember { System.currentTimeMillis() }

    // Filter logic
    val filteredWorkflows = remember(workflows, searchQuery, selectedFilter, selectedSortOption, allFavoriteIds) {
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // .statusBarsPadding() - Already handled by NavHost
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.category_content_desc_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = categorySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            
            // Grid Toggle
            IconButton(onClick = { 
                gridColumns = when(gridColumns) {
                    2 -> 3
                    3 -> 5
                    else -> 2
                }
            }) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_view),
                    contentDescription = stringResource(R.string.category_content_desc_grid),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Search Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_in_category)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
            )
        )
        
        FilledTonalIconButton(
            onClick = { showFilterSheet = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(56.dp),
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
        
        // Workflow Count
        Text(
            text = stringResource(R.string.workflows_count, filteredWorkflows.size),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Grid Content (LazyVerticalGrid matching Home)
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            filteredWorkflows.forEachIndexed { index, workflow ->
                item(key = workflow.id) {
                    com.doyouone.drawai.ui.components.WorkflowCard(
                        workflow = workflow,
                        userIsPremium = userHasPremiumAccess,
                        isPremium = workflow.isPremium,
                        onClick = { onWorkflowSelected(workflow.id) },
                        onUpgradeClick = onNavigateToSubscription,
                        gridColumns = gridColumns,
                        isFavorite = workflow.id in allFavoriteIds,
                        onFavoriteClick = { id ->
                            LocalFavoritesManager.toggleFavorite(id)
                            scope.launch { favoritesRepo?.toggleFavorite(id) }
                        }
                    )
                }

                // Add native ad every 6 items (full width) - ONLY IF NOT PREMIUM
                if (!userHasPremiumAccess && (index + 1) % 6 == 0 && index < filteredWorkflows.size - 1) {
                    item(
                        key = "native_ad_category_$index",
                        span = { androidx.compose.foundation.lazy.grid.GridItemSpan(gridColumns) }
                    ) {
                        com.doyouone.drawai.ads.NativeAdCard(
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
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

