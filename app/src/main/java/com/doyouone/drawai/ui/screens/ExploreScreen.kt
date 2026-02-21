package com.doyouone.drawai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.doyouone.drawai.data.model.CommunityPost
import com.doyouone.drawai.data.model.SortType
import com.doyouone.drawai.ui.components.PremiumOnlyFeature
import com.doyouone.drawai.ui.theme.*
import com.doyouone.drawai.ui.components.AnimeDrawMainTopBar
import com.doyouone.drawai.viewmodel.CommunityViewModel
import com.doyouone.drawai.R

import androidx.compose.material.icons.filled.Menu

@Composable
fun ExploreScreen(
    onPostClick: (CommunityPost) -> Unit = {},
    onBackClick: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPreferences = remember { com.doyouone.drawai.data.preferences.UserPreferences(context) }
    // Use null as initial value to distinguish between "not loaded yet" and "not premium"
    val isPremiumState = userPreferences.isPremium.collectAsState(initial = null)
    
    // Debug logging
    LaunchedEffect(isPremiumState.value) {
        android.util.Log.d("ExploreScreen", "🔍 isPremium state: ${isPremiumState.value}")
    }
    
    when (isPremiumState.value) {
        null -> {
            // Loading state - show spinner
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        true -> {
            // Show full explore screen for premium users
            ExploreScreenContent(
                onPostClick = onPostClick,
                onBackClick = onBackClick,
                onOpenDrawer = onOpenDrawer
            )
        }
        false -> {
            // Show upgrade prompt for free users
            PremiumOnlyFeature(
                featureName = "Community Explore",
                onBackClick = onBackClick,
                onUpgradeClick = onNavigateToSubscription
            )
        }
    }
}

@Composable
private fun ExploreScreenContent(
    onPostClick: (CommunityPost) -> Unit,
    onBackClick: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { CommunityViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        AnimeDrawMainTopBar(
            title = stringResource(R.string.explore_title),
            subtitle = stringResource(R.string.explore_subtitle),
            onOpenDrawer = onOpenDrawer
        )
        
        // Sort Tabs
        SortTabs(
            selectedSort = sortType,
            onSortSelected = { viewModel.changeSortType(it) }
        )
        
        Box(modifier = Modifier.fillMaxSize()) {
            // Content
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.error != null -> {
                    ErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.loadPosts(refresh = true) }
                    )
                }
                uiState.posts.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    PostsGrid(
                        posts = uiState.posts,
                        onPostClick = onPostClick,
                        onLikeClick = { viewModel.toggleLike(it) }
                    )
                }
            }
            
            // Refresh FAB
            if (uiState.isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                FloatingActionButton(
                    onClick = { viewModel.refreshPosts() },
                    modifier = Modifier
                       .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.explore_refresh),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SortTabs(
    selectedSort: SortType,
    onSortSelected: (SortType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SortTab(
            text = stringResource(R.string.explore_popular),
            isSelected = selectedSort == SortType.POPULAR,
            onClick = { onSortSelected(SortType.POPULAR) }
        )
        SortTab(
            text = stringResource(R.string.explore_recent),
            isSelected = selectedSort == SortType.RECENT,
            onClick = { onSortSelected(SortType.RECENT) }
        )
        SortTab(
            text = stringResource(R.string.explore_trending),
            isSelected = selectedSort == SortType.TRENDING,
            onClick = { onSortSelected(SortType.TRENDING) }
        )
        SortTab(
            text = stringResource(R.string.explore_mine),
            isSelected = selectedSort == SortType.MY_POSTS,
            onClick = { onSortSelected(SortType.MY_POSTS) }
        )
    }
}

@Composable
private fun RowScope.SortTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            // Removed weight(1f) to allow natural width
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PostsGrid(
    posts: List<CommunityPost>,
    onPostClick: (CommunityPost) -> Unit,
    onLikeClick: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val columns = when {
        configuration.screenWidthDp >= 900 -> 4
        configuration.screenWidthDp >= 600 -> 3
        else -> 2
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(posts) { post ->
            CommunityPostCard(
                post = post,
                onClick = { onPostClick(post) },
                onLikeClick = { onLikeClick(post.id) }
            )
        }
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPost,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image
            AsyncImage(
                model = post.imageUrl,
                contentDescription = post.prompt,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
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
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                // Username
                Text(
                    text = post.username,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentWhite.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Prompt
                Text(
                    text = post.prompt,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Workflow badge
                    Text(
                        text = post.workflow.replace("_", " ").take(12),
                        fontSize = 9.sp,
                        color = AccentWhite,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                    
                    // Engagement stats
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Likes
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onLikeClick)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "♥",
                                fontSize = 14.sp,
                                color = Color(0xFFFF6B9D) // Always pink
                            )
                            Text(
                                text = formatCount(post.likes),
                                fontSize = 10.sp,
                                color = AccentWhite,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Downloads
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📥",
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatCount(post.downloads),
                                fontSize = 10.sp,
                                color = AccentWhite.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    val configuration = LocalConfiguration.current
    val columns = when {
        configuration.screenWidthDp >= 900 -> 4
        configuration.screenWidthDp >= 600 -> 3
        else -> 2
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(6) { // Show 6 skeleton cards
            SkeletonPostCard()
        }
    }
}

@Composable
private fun SkeletonPostCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Skeleton image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceLight.copy(alpha = alpha))
            )
            
            // Skeleton info overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                // Skeleton username
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(10.dp)
                        .background(
                            AccentWhite.copy(alpha = alpha * 0.5f),
                            RoundedCornerShape(4.dp)
                        )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Skeleton prompt lines
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp)
                        .background(
                            AccentWhite.copy(alpha = alpha * 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                        .background(
                            AccentWhite.copy(alpha = alpha * 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🌟",
                fontSize = 48.sp
            )
            Text(
                text = stringResource(R.string.explore_no_posts),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.explore_be_first),
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "❌",
                fontSize = 48.sp
            )
            Text(
                text = stringResource(R.string.explore_error_oops),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = error,
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.explore_retry))
            }
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> String.format("%.1fM", count / 1000000.0)
        count >= 1000 -> String.format("%.1fK", count / 1000.0)
        else -> count.toString()
    }
}
