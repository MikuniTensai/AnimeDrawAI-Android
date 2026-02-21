package com.doyouone.drawai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.stringResource
import com.doyouone.drawai.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.doyouone.drawai.data.repository.UsageStatisticsRepository
import com.doyouone.drawai.ui.theme.*
import com.doyouone.drawai.ui.components.AnimeDrawTopAppBar
import com.doyouone.drawai.ui.components.AnimeDrawAppBarTitle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatisticsScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { com.doyouone.drawai.auth.AuthManager(context) }
    val userId = authManager.getCurrentUserId() ?: ""
    
    val statsRepo = remember(userId) {
        if (userId.isNotEmpty()) UsageStatisticsRepository(userId)
        else null
    }
    
    var stats by remember { mutableStateOf(UsageStatisticsRepository.UsageStats()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(statsRepo) {
        isLoading = true
        statsRepo?.let { repo ->
            stats = repo.getUsageStats()
        }
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            AnimeDrawTopAppBar(
                title = {
                    AnimeDrawAppBarTitle(stringResource(R.string.stats_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.stats_content_desc_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                // Total Generations Card
                StatCard(
                    icon = "🎨",
                    title = stringResource(R.string.stats_total_generations),
                    value = stats.totalGenerations.toString(),
                    subtitle = if (stats.lastGenerationDate.isNotEmpty()) 
                        stringResource(R.string.stats_last_generation, stats.lastGenerationDate)
                    else 
                        stringResource(R.string.stats_no_generations),
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Total Saves Card
                StatCard(
                    icon = "💾",
                    title = stringResource(R.string.stats_total_saves),
                    value = stats.totalSaves.toString(),
                    subtitle = stringResource(R.string.stats_saves_desc),
                    color = AccentBlue
                )
                
                // Total Favorites Card
                StatCard(
                    icon = "❤️",
                    title = stringResource(R.string.stats_total_favorites),
                    value = stats.totalFavorites.toString(),
                    subtitle = stringResource(R.string.stats_favorites_desc),
                    color = ErrorRed
                )
                
                // Member Since Card
                if (stats.firstGenerationDate.isNotEmpty()) {
                    StatCard(
                        icon = "📅",
                        title = stringResource(R.string.stats_member_since),
                        value = stats.firstGenerationDate.split(" ")[0],
                        subtitle = stringResource(R.string.stats_member_desc),
                        color = AccentGreen
                    )
                }
                
                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.stats_about_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = stringResource(R.string.stats_about_desc),
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: String,
    title: String,
    value: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 28.sp
                )
            }
            
            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
