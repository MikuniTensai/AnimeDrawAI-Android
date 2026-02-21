package com.doyouone.drawai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.doyouone.drawai.R
import com.doyouone.drawai.data.api.RetrofitClient
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import kotlinx.coroutines.launch
import com.doyouone.drawai.data.model.Workflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowCard(
    workflow: Workflow,
    modifier: Modifier = Modifier,
    userIsPremium: Boolean,
    isPremium: Boolean, // Pass explicitly if the workflow itself is premium
    onClick: () -> Unit,
    onUpgradeClick: () -> Unit = {},
    gridColumns: Int = 2,
    isFavorite: Boolean = false,
    onFavoriteClick: (String) -> Unit = {}
) {
    val isLocked = isPremium && !userIsPremium
    var showUpgradeDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable {
                if (isLocked) {
                    showUpgradeDialog = true
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
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
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
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎨", fontSize = 32.sp)
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

            // Lock Icon
            if (isLocked && gridColumns != 5) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👑", fontSize = 32.sp)
                    }
                }
            }
            
            // Premium Badge - Hide if gridColumns == 5 (too small)
            if (isPremium && gridColumns != 5) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "PRO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
            
            // Favorite Icon
            if (!isLocked && gridColumns != 5) {
                IconButton(
                    onClick = { onFavoriteClick(workflow.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                    ) {
                    Icon(
                            imageVector = if (isFavorite) 
                                Icons.Filled.Favorite 
                            else 
                                Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                }
            }
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (gridColumns > 2) 4.dp else 8.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Workflow Info Group
                if (gridColumns != 5) {
                    Column(
                        modifier = Modifier.align(Alignment.Start),
                        verticalArrangement = Arrangement.spacedBy(0.dp) // No spacing between items
                    ) {
                        // Category Badge - Hide if gridColumns > 2
                        if (gridColumns <= 2) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = getCategoryString(workflow.category),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    lineHeight = 9.sp
                                )
                            }
                        }

                        Text(
                            text = workflow.name,
                            fontSize = if (gridColumns > 2) 12.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = if (gridColumns > 2) 12.sp else 14.sp // Tight line height
                        )
                        
                        if (gridColumns <= 2) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "👁 ${compactFormat(workflow.viewCount)}", 
                                    fontSize = 10.sp, 
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                    lineHeight = 10.sp
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = "⚡ ${compactFormat(workflow.generationCount)}", 
                                    fontSize = 10.sp, 
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                    lineHeight = 10.sp
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = "⏱ ${workflow.estimatedTime}", 
                                    fontSize = 10.sp, 
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showUpgradeDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        ModalBottomSheet(
            onDismissRequest = { showUpgradeDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PremiumOnlyFeature(
                featureName = "Premium Workflows",
                onBackClick = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showUpgradeDialog = false
                        }
                    }
                },
                onUpgradeClick = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                         if (!sheetState.isVisible) {
                            showUpgradeDialog = false
                        }
                        onUpgradeClick()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
    }
}

@Composable
fun getCategoryString(category: String): String {
    return stringResource(
        when (category) {
            "Anime" -> R.string.category_anime
            "General" -> R.string.category_general
            "Animal" -> R.string.category_animal
            "Flower" -> R.string.category_flower
            "Food" -> R.string.category_food
            "Background" -> R.string.category_background
            else -> R.string.category_general
        }
    )
}

private fun compactFormat(number: Long): String {
    if (number < 1000) return number.toString()
    val exp = (Math.log(number.toDouble()) / Math.log(1000.0)).toInt()
    return String.format("%.1f%c", number / Math.pow(1000.0, exp.toDouble()), "kMGTPE"[exp - 1])
}
