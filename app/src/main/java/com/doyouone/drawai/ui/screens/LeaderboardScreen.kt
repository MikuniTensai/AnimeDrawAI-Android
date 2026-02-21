package com.doyouone.drawai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.doyouone.drawai.data.repository.LeaderboardEntry
import androidx.compose.ui.res.stringResource
import com.doyouone.drawai.R
import com.doyouone.drawai.ui.viewmodel.LeaderboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    onOpenDrawer: () -> Unit // Callback to open drawer
) {
    val topCreators by viewModel.topCreators.collectAsState()
    val topCreatorsWeekly by viewModel.topCreatorsWeekly.collectAsState()
    val topCreatorsMonthly by viewModel.topCreatorsMonthly.collectAsState()
    
    val topRomancers by viewModel.topRomancers.collectAsState()
    val communityMVPs by viewModel.communityMVPs.collectAsState()
    val risingStars by viewModel.risingStars.collectAsState()
    
    val categoryLikes by viewModel.categoryLikes.collectAsState()
    val categoryDownloads by viewModel.categoryDownloads.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.leaderboard_tab_architect),
        stringResource(R.string.leaderboard_tab_romancer),
        stringResource(R.string.leaderboard_tab_mvp),
        stringResource(R.string.leaderboard_tab_fame),
        stringResource(R.string.leaderboard_tab_rising)
    )
    
    // For Hall of Fame filter
    var selectedCategory by remember { mutableStateOf("anime") }
    val categories = listOf("anime", "background", "animal", "flower", "food", "general")
    
    // For Master Architect filter
    var selectedTimeFrame by remember { mutableStateOf(0) } // 0=All, 1=Weekly, 2=Monthly
    val timeFrames = listOf(
        stringResource(R.string.leaderboard_filter_all_time),
        stringResource(R.string.leaderboard_filter_weekly),
        stringResource(R.string.leaderboard_filter_monthly)
    )

    // State
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Compact Header (matching AnimeDrawMainTopBar spacing)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu Button (Left)
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Title
            Text(
                text = stringResource(R.string.leaderboard_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Modern Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { 
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
            }
        }

        // Content with Pull-to-Refresh
        var isRefreshing by remember { mutableStateOf(false) }
        
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading && !isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null && !isRefreshing) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.leaderboard_error_prefix, error ?: ""),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        viewModel.loadAllData() 
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        val description = stringResource(R.string.leaderboard_architect_desc)
                        
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Time Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                timeFrames.forEachIndexed { index, label ->
                                    FilterChip(
                                        selected = selectedTimeFrame == index,
                                        onClick = { selectedTimeFrame = index },
                                        label = { Text(label) },
                                        leadingIcon = if (selectedTimeFrame == index) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                            
                            val entries = when(selectedTimeFrame) {
                                1 -> topCreatorsWeekly
                                2 -> topCreatorsMonthly
                                else -> topCreators
                            }
                            
                            LeaderboardList(
                                entries = entries, 
                                metricLabel = stringResource(R.string.leaderboard_metric_generations),
                                headerTitle = stringResource(R.string.leaderboard_architect_title),
                                headerDesc = description,
                                headerIcon = Icons.Default.Create,
                                viewModel = viewModel
                            )
                        }
                    }
                    1 -> LeaderboardList(
                        entries = topRomancers, 
                        metricLabel = stringResource(R.string.leaderboard_metric_affection),
                        headerTitle = stringResource(R.string.leaderboard_romancer_title),
                        headerDesc = stringResource(R.string.leaderboard_romancer_desc),
                        headerIcon = Icons.Default.Favorite,
                        viewModel = viewModel
                    )
                    2 -> LeaderboardList(
                        entries = communityMVPs, 
                        metricLabel = stringResource(R.string.leaderboard_metric_shares),
                        headerTitle = stringResource(R.string.leaderboard_mvp_title),
                        headerDesc = stringResource(R.string.leaderboard_mvp_desc),
                        headerIcon = Icons.Default.Share,
                        viewModel = viewModel
                    )
                    3 -> {
                         Column(modifier = Modifier.fillMaxSize()) {
                            // Category Filter
                            ScrollableTabRow(
                                selectedTabIndex = categories.indexOf(selectedCategory),
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.secondary,
                                edgePadding = 16.dp,
                                indicator = {},
                                divider = {}
                            ) {
                                categories.forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { 
                                            selectedCategory = cat 
                                            viewModel.loadCategoryData(cat)
                                        },
                                        label = { Text(cat.replaceFirstChar { it.uppercase() }) },
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            
                            // Toggle Likes vs Downloads
                            var showDownloads by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !showDownloads,
                                    onClick = { showDownloads = false },
                                    label = { Text(stringResource(R.string.leaderboard_filter_most_liked)) },
                                    leadingIcon = { Icon(Icons.Default.ThumbUp, null, Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = showDownloads,
                                    onClick = { showDownloads = true },
                                    label = { Text(stringResource(R.string.leaderboard_filter_most_downloaded)) },
                                    leadingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp)) }
                                )
                            }
                            
                            LeaderboardList(
                                entries = if (showDownloads) categoryDownloads else categoryLikes, 
                                metricLabel = if (showDownloads) stringResource(R.string.leaderboard_metric_downloads) else stringResource(R.string.leaderboard_metric_likes),
                                headerTitle = stringResource(R.string.leaderboard_fame_title),
                                headerDesc = stringResource(R.string.leaderboard_fame_desc),
                                headerIcon = Icons.Default.Star,
                                viewModel = viewModel
                            )
                        }
                    }
                    4 -> LeaderboardList(
                        entries = risingStars, 
                        metricLabel = stringResource(R.string.leaderboard_metric_recent_likes),
                        headerTitle = stringResource(R.string.leaderboard_rising_title),
                        headerDesc = stringResource(R.string.leaderboard_rising_desc),
                        headerIcon = Icons.Default.TrendingUp,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardList(
    entries: List<LeaderboardEntry>, 
    metricLabel: String,
    headerTitle: String,
    headerDesc: String,
    headerIcon: androidx.compose.ui.graphics.vector.ImageVector,
    viewModel: LeaderboardViewModel? = null
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Header Item
        item {
            LeaderboardInfoCard(title = headerTitle, description = headerDesc, icon = headerIcon)
        }
        
        if (entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.leaderboard_no_rankings),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(entries) { index, entry ->
                LeaderboardItem(rank = index + 1, entry = entry, metricLabel = metricLabel)
            }
        }
    }
}

@Composable
fun LeaderboardInfoCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun LeaderboardItem(rank: Int, entry: LeaderboardEntry, metricLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Number
        Text(
            text = "$rank",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = when (rank) {
                1 -> Color(0xFFFFD700) // Gold
                2 -> Color(0xFFC0C0C0) // Silver
                3 -> Color(0xFFCD7F32) // Bronze
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Card Content
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                AsyncImage(
                    model = entry.userPhoto ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${entry.userId}", 
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Name & Metric
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${entry.score.toInt()} $metricLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
