package com.doyouone.drawai.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import com.doyouone.drawai.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.doyouone.drawai.data.model.CommunityPost
import com.doyouone.drawai.ui.components.ZoomableImage
import com.doyouone.drawai.ui.components.AnimeDrawTopAppBar
import com.doyouone.drawai.viewmodel.CommunityViewModel
import com.doyouone.drawai.data.repository.ReportRepository
import com.doyouone.drawai.auth.AuthManager
import kotlinx.coroutines.launch
import java.io.OutputStream

import androidx.compose.foundation.text.selection.SelectionContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    post: CommunityPost,
    viewModel: CommunityViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // Track local like state to update UI immediately
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(post.likes) }
    var downloadCount by remember { mutableStateOf(post.downloads) }
    var isDownloading by remember { mutableStateOf(false) }
    
    // Report state
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var isReporting by remember { mutableStateOf(false) }
    val reportRepository = remember { ReportRepository() }
    val authManager = remember { AuthManager(context) }
    
    // Check if user has liked this post
    LaunchedEffect(post.id) {
        isLiked = viewModel.hasLiked(post.id)
        viewModel.viewPost(post.id) // Track view
    }

    Scaffold(
        topBar = {
            AnimeDrawTopAppBar(
                title = { 
                    Column {
                        Text(
                            text = post.username.ifEmpty { stringResource(R.string.post_anonymous) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.createdAt != null) {
                            Text(
                                text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                    .format(post.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.post_content_desc_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.post_content_desc_info))
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like Button
                    IconButton(
                        onClick = {
                            // Optimistic update
                            isLiked = !isLiked
                            likeCount = if (isLiked) likeCount + 1 else likeCount - 1
                            viewModel.toggleLike(post.id)
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.post_content_desc_like),
                                tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$likeCount",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    // Download Button
                    IconButton(
                        onClick = {
                            if (!isDownloading) {
                                // Check if already downloaded in this session
                                if (viewModel.isPostDownloaded(post.id)) {
                                    Toast.makeText(context, context.getString(R.string.post_downloaded_already), Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }

                                isDownloading = true
                                scope.launch {
                                    downloadImage(context, post.imageUrl, post.id) { success ->
                                        isDownloading = false
                                        if (success) {
                                            downloadCount++
                                            viewModel.downloadPost(post)
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.ArrowDropDown, // Using generic download icon equivalent
                                    contentDescription = stringResource(R.string.post_content_desc_download)
                                )
                            }
                            Text(
                                text = "$downloadCount",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    // Share Button
                    IconButton(
                        onClick = {
                            // Reuse share logic or implement new
                            Toast.makeText(context, context.getString(R.string.post_sharing), Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.post_share))
                            Text(
                                text = stringResource(R.string.post_share),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Main Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Square or dynamic based on image
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ZoomableImage(
                    imageUrl = post.imageUrl,
                    contentDescription = post.prompt,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Details
            Column(modifier = Modifier.padding(16.dp)) {
                // Prompt Section
                Text(
                    text = stringResource(R.string.post_prompt),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        text = post.prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (post.negativePrompt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.post_negative_prompt),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectionContainer {
                        Text(
                            text = post.negativePrompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Workflow / Model Info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.post_workflow_model),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = post.workflow.ifEmpty { stringResource(R.string.post_unknown) },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(stringResource(R.string.post_report_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.post_report_desc))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text(stringResource(R.string.post_report_reason)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReason.isNotBlank()) {
                            isReporting = true
                            scope.launch {
                                val currentUser = authManager.currentUser.value
                                val reportedBy = currentUser?.uid ?: "anonymous"
                                
                                val result = reportRepository.reportImage(
                                    imageId = post.id,
                                    prompt = post.prompt,
                                    negativePrompt = post.negativePrompt,
                                    workflow = post.workflow,
                                    imageUrl = post.imageUrl,
                                    reportReason = reportReason,
                                    reportedBy = reportedBy
                                )
                                
                                result.onSuccess {
                                    Toast.makeText(context, context.getString(R.string.post_report_success), Toast.LENGTH_SHORT).show()
                                    showReportDialog = false
                                    reportReason = ""
                                }.onFailure { e ->
                                    Toast.makeText(context, context.getString(R.string.post_report_failed) + ": ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                                isReporting = false
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.post_report_provide_reason), Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isReporting
                ) {
                    if (isReporting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.post_report_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(stringResource(R.string.post_report_cancel))
                }
            }
        )
    }
}

// Helper function to download image
private suspend fun downloadImage(
    context: Context,
    imageUrl: String,
    fileName: String,
    onResult: (Boolean) -> Unit
) {
    try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false) // Disable hardware bitmaps for saving
            .build()

        val result = (loader.execute(request) as? SuccessResult)?.drawable
        val bitmap = (result as? BitmapDrawable)?.bitmap

        if (bitmap != null) {
            saveBitmapToGallery(context, bitmap, "AnimeDraw_$fileName")
            Toast.makeText(context, context.getString(R.string.post_saved_gallery), Toast.LENGTH_SHORT).show()
            onResult(true)
        } else {
            Toast.makeText(context, context.getString(R.string.post_download_failed), Toast.LENGTH_SHORT).show()
            onResult(false)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        onResult(false)
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String) {
    val filename = "$title.jpg"
    var fos: OutputStream? = null
    
    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AnimeDrawAI")
        }
        
        val imageUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        
        fos = imageUri?.let { context.contentResolver.openOutputStream(it) }
        fos?.let {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        fos?.close()
    }
}
