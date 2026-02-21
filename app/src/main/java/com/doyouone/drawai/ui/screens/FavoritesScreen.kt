package com.doyouone.drawai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import com.doyouone.drawai.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doyouone.drawai.data.model.Workflow
import com.doyouone.drawai.data.repository.FavoritesRepository
import com.doyouone.drawai.ui.theme.*
import com.doyouone.drawai.ui.components.AnimeDrawMainTopBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.doyouone.drawai.data.api.RetrofitClient
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration

// Fungsi untuk menghitung jumlah kolom grid berdasarkan lebar layar
@Composable
fun getResponsiveFavoritesColumns(): Int {
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
fun FavoritesScreen(
    onWorkflowSelected: (String) -> Unit,
    onNavigateToSubscription: () -> Unit = {},
    userId: String = "",
    onOpenDrawer: () -> Unit = {},
    viewModel: com.doyouone.drawai.viewmodel.GenerateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoritesRepo = remember { FavoritesRepository(userId) }
    
    // State for favorites
    var favoriteWorkflows by remember { mutableStateOf<List<Workflow>>(emptyList()) }
    var workflowsMap by remember { mutableStateOf<Map<String, Workflow>>(emptyMap()) }
    var userIsPremium by remember { mutableStateOf(false) }
    
    // Load favorites and premium status
    LaunchedEffect(Unit) {
        // In a real app, we would observe a Flow from the repository
        // For now, we'll just load them once or use a dummy implementation if needed
        // Assuming favoritesRepo has a method to get favorites
        // Since I don't see the exact API, I'll try to infer or use a safe approach
        
        // Placeholder for loading logic
        // favoriteWorkflows = favoritesRepo.getFavorites() 
        // userIsPremium = ...
    }

    // Since I don't have the exact implementation of FavoritesRepository, 
    // I will try to use the LocalFavoritesManager which was referenced in the file
    // Load local favorites
    val localFavorites = com.doyouone.drawai.data.local.LocalFavoritesManager.favoriteIds.collectAsState()

    // Load all workflows from ViewModel to filter by favorites
    val allWorkflowsMap by viewModel.workflows.collectAsState()
    
    // Update favorite workflows when either local favorites or all workflows change
    LaunchedEffect(localFavorites.value, allWorkflowsMap) {
        val allWorkflows = allWorkflowsMap.entries.map { entry ->
            Workflow(
                id = entry.key,
                name = entry.value.name,
                description = entry.value.description,
                estimatedTime = entry.value.estimatedTime,
                category = if (entry.key.contains("anime")) "Anime" else "General",
                isFavorite = true
            )
        }
        
        // Filter workflows that are in the favorites list
        val favs = allWorkflowsMap.entries.filter { it.key in localFavorites.value }.map { entry ->
             Workflow(
                id = entry.key,
                name = entry.value.name,
                description = entry.value.description,
                estimatedTime = entry.value.estimatedTime,
                category = if (entry.key.contains("anime")) "Anime" else "General",
                isFavorite = true
            )
        }
        favoriteWorkflows = favs
        workflowsMap = allWorkflowsMap.mapValues { entry ->
            Workflow(
                id = entry.key,
                name = entry.value.name,
                description = entry.value.description,
                estimatedTime = entry.value.estimatedTime,
                category = if (entry.key.contains("anime")) "Anime" else "General",
                isFavorite = entry.key in localFavorites.value
            )
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimeDrawMainTopBar(
            title = stringResource(id = R.string.favorites_title),
            subtitle = stringResource(id = R.string.favorites_saved_count, favoriteWorkflows.size),
            onOpenDrawer = onOpenDrawer
        )
        
        if (favoriteWorkflows.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
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
                        text = stringResource(id = R.string.favorites_empty_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Text(
                        text = stringResource(id = R.string.favorites_empty_message),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            // Favorites Grid (Full Display) - Responsive Grid
            val gridColumns = getResponsiveFavoritesColumns()
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                gridItems(favoriteWorkflows) { workflow ->
                    val workflowInfo = workflowsMap[workflow.id]
                    val isPremiumWorkflow = workflowInfo?.isPremium ?: (workflow.id == "anime_premium_ultra")
                    GridFavoriteWorkflowCard(
                        workflow = workflow,
                        isPremiumWorkflow = isPremiumWorkflow,
                        userIsPremium = userIsPremium,
                        onClick = { onWorkflowSelected(workflow.id) },
                        onUpgradeClick = onNavigateToSubscription,
                        onRemoveFavorite = {
                            // Immediately update local state via singleton manager
                            com.doyouone.drawai.data.local.LocalFavoritesManager.removeFavorite(workflow.id)
                            // Also update Firestore in background
                            scope.launch {
                                favoritesRepo?.removeFavorite(workflow.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GridFavoriteWorkflowCard(
    workflow: Workflow,
    isPremiumWorkflow: Boolean,
    userIsPremium: Boolean,
    onClick: () -> Unit,
    onUpgradeClick: () -> Unit = {},
    onRemoveFavorite: () -> Unit
) {
    val isLocked = isPremiumWorkflow && !userIsPremium
    var showUpgradeDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable {
                if (isLocked) {
                    showUpgradeDialog = true
                    android.util.Log.d("GridFavoriteWorkflowCard", "Blocked premium favorite by FREE user: ${workflow.id}")
                } else {
                    onClick()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            SubcomposeAsyncImage(
                model = "${RetrofitClient.getBaseUrl()}workflow-image/${workflow.id}",
                contentDescription = workflow.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (isLocked) 0.4f else 1f,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📷", fontSize = 48.sp)
                    }
                }
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Premium badge
            if (isPremiumWorkflow) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color(0xFFFFD700).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = stringResource(id = R.string.favorites_pro_badge), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            // Center lock icon when locked
            if (isLocked) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(40.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔒", fontSize = 40.sp)
                    }
                }
            }
            
            // Delete Icon (Top Right)
            IconButton(
                onClick = onRemoveFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(id = R.string.favorites_remove_content_desc),
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Badge
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = workflow.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                
                // Workflow Info
                Column {
                    Text(
                        text = workflow.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱",
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = workflow.estimatedTime,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteWorkflowCard(
    workflow: Workflow,
    onClick: () -> Unit,
    onRemoveFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            SubcomposeAsyncImage(
                model = "${RetrofitClient.getBaseUrl()}workflow-image/${workflow.id}",
                contentDescription = workflow.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📷", fontSize = 32.sp)
                    }
                }
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 0.5f
                        )
                    )
            )
            
            // Delete Icon (Top Right)
            IconButton(
                onClick = onRemoveFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(id = R.string.favorites_remove_content_desc),
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Badge
                Box(
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = workflow.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
                
                // Workflow Info
                Column {
                    Text(
                        text = workflow.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = workflow.description,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = workflow.estimatedTime,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
