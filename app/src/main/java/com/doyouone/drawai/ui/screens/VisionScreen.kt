package com.doyouone.drawai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.doyouone.drawai.ui.theme.*
import com.doyouone.drawai.ui.components.AnimeDrawMainTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.res.stringResource
import com.doyouone.drawai.R

data class VisionItem(
    val id: String = "",
    val vision: String,
    val avoid: String,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisionScreen(
    workflowId: String? = null,
    source: String? = null,
    onNavigateToWorkflow: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenDrawer: () -> Unit = {}
) {
    
    var itemsList by remember { mutableStateOf<List<VisionItem>>(emptyList()) }
    var shuffledItems by remember { mutableStateOf<List<VisionItem>>(emptyList()) }
    var currentPage by remember { mutableStateOf(0) }
    val pageSize = 20
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val clipboard = LocalClipboardManager.current
    val db = Firebase.firestore
    
    // Function to fetch data from Firebase with force refresh
    suspend fun fetchVisions(forceRefresh: Boolean = false) {
        try {
            val query = if (forceRefresh) {
                // Force server fetch
                db.collection("visions").get(com.google.firebase.firestore.Source.SERVER).await()
            } else {
                db.collection("visions").get().await()
            }
            
            itemsList = query.documents.mapNotNull { document ->
                try {
                    VisionItem(
                        id = document.id,
                        vision = document.getString("vision") ?: return@mapNotNull null,
                        avoid = document.getString("avoid") ?: return@mapNotNull null,
                        category = document.getString("category") ?: "",
                        tags = document.get("tags") as? List<String> ?: emptyList(),
                        imageUrl = document.getString("imageUrl")
                    )
                } catch (e: Exception) {
                    null
                }
            }
            // Initial shuffle
            shuffledItems = itemsList.shuffled()
            currentPage = 0
            
            errorMessage = null
            
        } catch (e: Exception) {
            errorMessage = "Failed to load visions: ${e.message}"
        }
    }
    
    // Fetch data from Firebase on first load
    LaunchedEffect(Unit) {
        fetchVisions()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        AnimeDrawMainTopBar(
            title = stringResource(R.string.vision_prompts_title),
            subtitle = stringResource(R.string.vision_prompts_subtitle),
            onOpenDrawer = onOpenDrawer,
            actions = {
                IconButton(
                    onClick = { 
                        shuffledItems = itemsList.shuffled()
                        currentPage = 0
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.vision_shuffle),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        } else if (errorMessage != null) {
            // Error State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.vision_failed_to_load),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.vision_tap_to_retry),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    FilledTonalButton(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                isLoading = true
                                fetchVisions(forceRefresh = true)
                                isLoading = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.vision_retry))
                    }
                }
            }
        } else if (itemsList.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.vision_no_visions),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            // Data loaded successfully
            val displayedItems = shuffledItems.drop(currentPage * pageSize).take(pageSize)
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp) // Space for FAB
            ) {
                items(displayedItems) { item ->
                    VisionCard(
                        item = item,
                        clipboard = clipboard,
                        source = source,
                        workflowId = workflowId,
                        onNavigateToWorkflow = onNavigateToWorkflow
                    )
                }
                
                // Next Button at bottom of list
                item(span = { GridItemSpan(2) }) {
                    Column {
                        if ((currentPage + 1) * pageSize < shuffledItems.size) {
                            Button(
                                onClick = { currentPage++ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text(stringResource(R.string.vision_next_page))
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
                            }
                        } else if (shuffledItems.isNotEmpty()) {
                             Button(
                                onClick = { 
                                    shuffledItems = itemsList.shuffled()
                                    currentPage = 0
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text(stringResource(R.string.vision_shuffle))
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Refresh, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisionCard(
    item: VisionItem,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    source: String?,
    workflowId: String?,
    onNavigateToWorkflow: (String, String, String) -> Unit
) {
    var showAvoid by remember { mutableStateOf(false) }
    var copiedText by remember { mutableStateOf("") }
    val copiedMessage = stringResource(R.string.vision_copied)
    
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.5.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp) // Adjusted height for visibility
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    coil.compose.SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Vision Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(), 
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
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = "Error Icon",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    )
                } else {
                    // Fallback Icon
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "Draw Icon",
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Vision Content - Compact
                Text(
                    text = item.vision,
                    fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // Action Row - Copy and Avoid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy button - Small
                TextButton(
                    onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(item.vision))
                        copiedText = copiedMessage
                    },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.vision_copy),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Avoid toggle - if available
                if (item.avoid.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showAvoid = !showAvoid }
                    ) {
                        Text(
                            text = stringResource(R.string.vision_avoid),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                        Icon(
                            imageVector = if (showAvoid) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showAvoid) "Show avoid" else "Hide avoid",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            
            // Avoid content - compact
            AnimatedVisibility(
                visible = showAvoid,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.avoid,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Category and Tags - Compact row
            if (item.category.isNotEmpty() || item.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category - Small chip
                    if (item.category.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = item.category,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    // Tags - Max 2 small chips
                    item.tags.take(2).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            // Use This Vision button - Compact
            if (source == "workflow" && workflowId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        onNavigateToWorkflow(workflowId, item.vision, item.avoid)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.vision_use),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Copy feedback - Small
            AnimatedVisibility(
                visible = copiedText.isNotEmpty(), 
                enter = fadeIn(), 
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✓ $copiedText",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            
            LaunchedEffect(copiedText) {
                if (copiedText.isNotEmpty()) {
                    delay(1200)
                    copiedText = ""
                }
            }
        }
        }
    }
}