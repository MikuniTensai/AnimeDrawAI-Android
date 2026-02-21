package com.doyouone.drawai.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import com.doyouone.drawai.viewmodel.GenerateViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Lock
import com.doyouone.drawai.ui.components.ProcessingModal
import com.doyouone.drawai.ui.components.MaintenancePopup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawToImageScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGallery: (() -> Unit)? = null,
    viewModel: GenerateViewModel = viewModel()
) {
    var currentStep by remember { mutableStateOf(DrawStep.DRAWING) }
    
    // Shared Drawing State
    // We capture the bitmap to pass it to the config screen
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    when(currentStep) {
        DrawStep.DRAWING -> {
            DrawingEditorScreen(
                onNavigateBack = onNavigateBack,
                onContinue = { bitmap -> 
                    capturedBitmap = bitmap
                    currentStep = DrawStep.CONFIG 
                }
            )
        }
        DrawStep.CONFIG -> {
            ConfigAndGenerateScreen(
                capturedBitmap = capturedBitmap,
                onBackToDraw = { currentStep = DrawStep.DRAWING },
                onNavigateBack = onNavigateBack,
                onNavigateToGallery = onNavigateToGallery,
                viewModel = viewModel
            )
        }
    }
}

enum class DrawStep { DRAWING, CONFIG }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingEditorScreen(
    onNavigateBack: () -> Unit,
    onContinue: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var photoEditor by remember { mutableStateOf<ja.burhanrashid52.photoeditor.PhotoEditor?>(null) }
    var photoEditorView by remember { mutableStateOf<ja.burhanrashid52.photoeditor.PhotoEditorView?>(null) }
    var selectedTool by remember { mutableStateOf(EditorTool.NONE) }
    var selectedColor by remember { mutableStateOf(android.graphics.Color.BLACK) }
    var brushSize by remember { mutableFloatStateOf(12f) }
    var brushOpacity by remember { mutableIntStateOf(100) } // 0-100
    
    // Dialogs
    var showTextDialog by remember { mutableStateOf(false) }
    var showBrushDialog by remember { mutableStateOf(false) }
    var showEmojiDialog by remember { mutableStateOf(false) }
    var showShapesDialog by remember { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }
    var showEraserDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var textColor by remember { mutableStateOf(android.graphics.Color.BLACK) }
    
    // Shape settings (using Brush as default)
    var shapeSize by remember { mutableFloatStateOf(12f) }
    
    // Eraser settings
    var eraserSize by remember { mutableFloatStateOf(20f) }
    
    // Image Pickers
    var hasLoadedImage by remember { mutableStateOf(false) }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                photoEditorView?.source?.setImageBitmap(bitmap)
                hasLoadedImage = true
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Failed to load image", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val stickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                photoEditor?.addImage(bitmap)
                android.widget.Toast.makeText(context, "Sticker added!", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Failed to load sticker", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            photoEditorView?.source?.setImageBitmap(it)
            hasLoadedImage = true
        }
    }
    
    // Apply brush with opacity support
    fun applyBrushSettings() {
        photoEditor?.let { editor ->
            editor.setBrushDrawingMode(true)
            val shapeBuilder = ja.burhanrashid52.photoeditor.shape.ShapeBuilder()
                .withShapeType(ja.burhanrashid52.photoeditor.shape.ShapeType.Brush)
                .withShapeSize(brushSize)
                .withShapeOpacity(brushOpacity)
                .withShapeColor(selectedColor)
            editor.setShape(shapeBuilder)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // PhotoEditorView
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                val view = ja.burhanrashid52.photoeditor.PhotoEditorView(ctx)
                photoEditorView = view
                
                // Create editor
                val editor = ja.burhanrashid52.photoeditor.PhotoEditor.Builder(ctx, view)
                    .setPinchTextScalable(true)
                    .setClipSourceImage(false)
                    .build()
                    
                // Initial blank canvas
                if (!hasLoadedImage) {
                    val bmp = Bitmap.createBitmap(1536, 1536, Bitmap.Config.ARGB_8888)
                    val cv = android.graphics.Canvas(bmp)
                    cv.drawColor(android.graphics.Color.WHITE)
                    view.source.setImageBitmap(bmp)
                }
                
                photoEditor = editor
                
                view
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Detect orientation
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
            
            Text("Photo Editor", color = Color.White, fontWeight = FontWeight.Bold)
            
            // Two separate buttons: Download and Next
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Download button - save to gallery
                IconButton(
                    onClick = {
                        photoEditor?.saveAsBitmap(object : ja.burhanrashid52.photoeditor.OnSaveBitmap {
                            override fun onBitmapReady(saveBitmap: Bitmap) {
                                scope.launch {
                                    try {
                                        val file = File(context.cacheDir, "edited_drawing_${System.currentTimeMillis()}.png")
                                        val out = FileOutputStream(file)
                                        saveBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                        out.close()
                                        
                                        // Save to gallery
                                        val storage = com.doyouone.drawai.data.local.ImageStorage(context)
                                        storage.saveImage(saveBitmap, "Photo Edit", "", "photo_editor")
                                        
                                        android.widget.Toast.makeText(context, "Saved to Gallery!", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Failed to save", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })
                    },
                    modifier = Modifier
                        .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Download, "Save", tint = Color.White)
                }
                
                // Next button - continue to config screen
                IconButton(
                    onClick = {
                        photoEditor?.saveAsBitmap(object : ja.burhanrashid52.photoeditor.OnSaveBitmap {
                            override fun onBitmapReady(saveBitmap: Bitmap) {
                                onContinue(saveBitmap)
                            }
                        })
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", tint = Color.White)
                }
            }
        }
        
        // Responsive Toolbar - Portrait: Bottom Horizontal, Landscape: Left Vertical
        if (isLandscape) {
            // Landscape: Vertical toolbar on left side
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxHeight()
                    .padding(top = 56.dp) // Account for top bar
            ) {
                // Tools Column (Vertical)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(Color(0xFF1C1C1E))
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CompactToolButton(Icons.Default.Brush, "Brush", selectedTool == EditorTool.BRUSH) { 
                        selectedTool = EditorTool.BRUSH
                        showBrushDialog = true
                    }
                    CompactToolButton(Icons.Default.TextFields, "Text", selectedTool == EditorTool.TEXT) { 
                        selectedTool = EditorTool.TEXT
                        showTextDialog = true
                    }
                    CompactToolButton(Icons.Default.Category, "Shape", selectedTool == EditorTool.SHAPES) { 
                        selectedTool = EditorTool.SHAPES
                        showShapesDialog = true
                    }
                    CompactToolButton(Icons.Default.FilterVintage, "Filter", selectedTool == EditorTool.FILTERS) { 
                        selectedTool = EditorTool.FILTERS
                        showFiltersDialog = true
                    }
                    CompactToolButton(Icons.Default.AutoFixHigh, "Erase", selectedTool == EditorTool.ERASER) { 
                        selectedTool = EditorTool.ERASER
                        showEraserDialog = true
                    }
                    CompactToolButton(Icons.Default.Image, "Stick", selectedTool == EditorTool.STICKER) { 
                        selectedTool = EditorTool.STICKER
                        stickerLauncher.launch("image/*")
                    }
                    CompactToolButton(Icons.Default.EmojiEmotions, "Emoji", selectedTool == EditorTool.EMOJI) { 
                        selectedTool = EditorTool.EMOJI
                        showEmojiDialog = true
                    }
                    
                    Spacer(Modifier.weight(1f))
                    
                    // Actions (Compact)
                    HorizontalDivider(color = Color.Gray, modifier = Modifier.width(40.dp))
                    Spacer(Modifier.height(4.dp))
                    
                    IconButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Image, "Gallery", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.CameraAlt, "Camera", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = Color.Gray, modifier = Modifier.width(40.dp))
                    Spacer(Modifier.height(4.dp))
                    
                    IconButton(onClick = { photoEditor?.undo() }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { photoEditor?.redo() }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Redo", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        } else {
            // Portrait: Horizontal toolbar at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1E))
            ) {
                // Tools Row (7 tools) - Horizontal Scrollable for Different Screen Sizes
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    item {
                        PhotoEditorToolButton(
                            icon = Icons.Default.Brush,
                            label = "Brush",
                            isSelected = selectedTool == EditorTool.BRUSH,
                            onClick = { 
                                selectedTool = EditorTool.BRUSH
                                showBrushDialog = true
                            }
                        )
                    }
                    
                    item {
                        PhotoEditorToolButton(
                            icon = Icons.Default.TextFields,
                            label = "Text",
                            isSelected = selectedTool == EditorTool.TEXT,
                            onClick = { 
                                selectedTool = EditorTool.TEXT
                                showTextDialog = true 
                            }
                        )
                    }
                    
                    item {
                        PhotoEditorToolButton(
                            icon = Icons.Default.Category,
                            label = "Shapes",
                            isSelected = selectedTool == EditorTool.SHAPES,
                            onClick = { 
                                selectedTool = EditorTool.SHAPES
                                showShapesDialog = true
                            }
                        )
                    }
                    
                    item {
                        PhotoEditorToolButton(
                            icon = Icons.Default.FilterVintage,
                            label = "Filters",
                            isSelected = selectedTool == EditorTool.FILTERS,
                            onClick = { 
                                selectedTool = EditorTool.FILTERS
                                showFiltersDialog = true
                            }
                        )
                    }
                    
                    item {
                        PhotoEditorToolButton(
                            icon = Icons.Default.AutoFixHigh,
                            label = "Eraser",
                            isSelected = selectedTool == EditorTool.ERASER,
                            onClick = { 
                                selectedTool = EditorTool.ERASER
                                showEraserDialog = true
                            }
                        )
                    }
                    
                    item {
                        PhotoEditorToolButton(
                            icon = Icons.Default.Image,
                            label = "Sticker",
                            isSelected = selectedTool == EditorTool.STICKER,
                            onClick = { 
                                selectedTool = EditorTool.STICKER
                                stickerLauncher.launch("image/*")
                            }
                        )
                    }
                    
                    item {
                        PhotoEditorToolButton(
                            icon = Icons.Default.EmojiEmotions,
                            label = "Emoji",
                            isSelected = selectedTool == EditorTool.EMOJI,
                            onClick = { 
                                selectedTool = EditorTool.EMOJI
                                showEmojiDialog = true
                            }
                        )
                    }
                }
                
                // Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Load Image Buttons
                    Row {
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.Image, "Gallery", tint = Color.White)
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier
                                .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, "Camera", tint = Color.White)
                        }
                    }
                    
                    // Undo/Redo
                    Row {
                        IconButton(onClick = { photoEditor?.undo() }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = Color.White)
                        }
                        
                        IconButton(onClick = { photoEditor?.redo() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Redo", tint = Color.White, modifier = Modifier.graphicsLayer(scaleX = -1f))
                        }
                    }
                }
            }
        }
    }
    
    // Brush Dialog with Anime-optimized presets + Ink Styles
    if (showBrushDialog) {
        AlertDialog(
            onDismissRequest = { showBrushDialog = false },
            title = { Text("Brush & Ink Settings") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Ink Style Presets (Professional Manga Tools)
                    Text("🖊️ Ink Styles:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("Professional manga pen tools", fontSize = 10.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // G-Pen - Solid black ink, perfect for line art
                            OutlinedButton(
                                onClick = { 
                                    brushSize = 5f
                                    brushOpacity = 100
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("G-Pen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("5px • 100%", fontSize = 8.sp, color = Color.Gray)
                                    Text("Solid Ink", fontSize = 8.sp, color = Color.Gray)
                                }
                            }
                            
                            // Maru Pen - Fine detail work
                            OutlinedButton(
                                onClick = { 
                                    brushSize = 2f
                                    brushOpacity = 100
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Maru", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("2px • 100%", fontSize = 8.sp, color = Color.Gray)
                                    Text("Fine Line", fontSize = 8.sp, color = Color.Gray)
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Marker - Semi-transparent for shading
                            OutlinedButton(
                                onClick = { 
                                    brushSize = 20f
                                    brushOpacity = 40
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Marker", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("20px • 40%", fontSize = 8.sp, color = Color.Gray)
                                    Text("Shading", fontSize = 8.sp, color = Color.Gray)
                                }
                            }
                            
                            // Airbrush - Very soft, for gradients
                            OutlinedButton(
                                onClick = { 
                                    brushSize = 30f
                                    brushOpacity = 20
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Airbrush", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("30px • 20%", fontSize = 8.sp, color = Color.Gray)
                                    Text("Soft", fontSize = 8.sp, color = Color.Gray)
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Ink Brush - Variable pressure simulation
                            OutlinedButton(
                                onClick = { 
                                    brushSize = 15f
                                    brushOpacity = 80
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Ink", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("15px • 80%", fontSize = 8.sp, color = Color.Gray)
                                    Text("Variable", fontSize = 8.sp, color = Color.Gray)
                                }
                            }
                            
                            // Screen Tone effect
                            OutlinedButton(
                                onClick = { 
                                    brushSize = 40f
                                    brushOpacity = 15
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Tone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("40px • 15%", fontSize = 8.sp, color = Color.Gray)
                                    Text("Light Fill", fontSize = 8.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Quick Size Presets
                    Text("📏 Quick Sizes:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { brushSize = 3f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) { 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("3px", fontSize = 9.sp)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { brushSize = 8f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) { 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("8px", fontSize = 9.sp)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { brushSize = 20f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) { 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("20px", fontSize = 9.sp)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { brushSize = 40f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) { 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("40px", fontSize = 9.sp)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Custom Controls
                    Text("⚙️ Custom Settings:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    
                    // Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Size:", fontWeight = FontWeight.Bold)
                        Text("${brushSize.toInt()}px", 
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = brushSize,
                        onValueChange = { brushSize = it },
                        valueRange = 1f..60f
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Opacity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Opacity:", fontWeight = FontWeight.Bold)
                        Text("${brushOpacity}%", 
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = brushOpacity.toFloat(),
                        onValueChange = { brushOpacity = it.toInt() },
                        valueRange = 5f..100f
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Color Grid
                    Text("🎨 Color Palette:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    
                    // Extended Color Grid for Anime - Horizontal Scrollable
                    val colors = listOf(
                        // Basic colors
                        android.graphics.Color.BLACK to "Black",
                        android.graphics.Color.WHITE to "White",
                        android.graphics.Color.GRAY to "Gray",
                        android.graphics.Color.LTGRAY to "Light Gray",
                        
                        // Anime common colors
                        android.graphics.Color.RED to "Red",
                        android.graphics.Color.rgb(255, 182, 193) to "Pink",
                        android.graphics.Color.rgb(255, 165, 0) to "Orange",
                        android.graphics.Color.YELLOW to "Yellow",
                        
                        android.graphics.Color.GREEN to "Green",
                        android.graphics.Color.rgb(144, 238, 144) to "Light Green",
                        android.graphics.Color.CYAN to "Cyan",
                        android.graphics.Color.BLUE to "Blue",
                        
                        android.graphics.Color.rgb(100, 149, 237) to "Sky Blue",
                        android.graphics.Color.MAGENTA to "Magenta",
                        android.graphics.Color.rgb(138, 43, 226) to "Purple",
                        android.graphics.Color.rgb(139, 69, 19) to "Brown"
                    )
                    
                    // Horizontal scrollable grid with 4 rows for even layout (4x4 = 16 colors)
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(colors.chunked(4)) { column ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                column.forEach { (color, _) ->
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(Color(color), CircleShape)
                                            .clickable { selectedColor = color }
                                            .then(
                                                if (selectedColor == color) 
                                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                else Modifier
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    applyBrushSettings()
                    showBrushDialog = false
                }) {
                    Text("Apply & Draw")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBrushDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Text Dialog
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Add Text") },
            text = {
                Column {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Enter text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Text Color:")
                    
                    val textColors = listOf(
                        android.graphics.Color.BLACK,
                        android.graphics.Color.WHITE,
                        android.graphics.Color.RED,
                        android.graphics.Color.BLUE,
                        android.graphics.Color.GREEN,
                        android.graphics.Color.YELLOW
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        textColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(color), CircleShape)
                                    .clickable { textColor = color }
                                    .then(
                                        if (textColor == color) 
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textInput.isNotEmpty()) {
                        photoEditor?.addText(textInput, textColor)
                        textInput = ""
                        showTextDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Emoji Dialog
    if (showEmojiDialog) {
        AlertDialog(
            onDismissRequest = { showEmojiDialog = false },
            title = { Text("Select Emoji") },
            text = {
                Column {
                    val emojis = listOf(
                        "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆",
                        "😉", "😊", "😋", "😎", "😍", "😘", "🥰", "😗",
                        "😙", "😚", "🙂", "🤗", "🤩", "🤔", "🤨", "😐",
                        "😑", "😶", "🙄", "😏", "😣", "😥", "😮", "🤐",
                        "😯", "😪", "😫", "🥱", "😴", "😌", "😛", "😜",
                        "😝", "🤤", "😒", "😓", "😔", "😕", "🙃", "🤑",
                        "😲", "☹️", "🙁", "😖", "😞", "😟", "😤", "😢",
                        "😭", "😦", "😧", "😨", "😩", "🤯", "😬", "😰",
                        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
                        "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙"
                    )
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(emojis.size) { index ->
                            Text(
                                text = emojis[index],
                                fontSize = 32.sp,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clickable {
                                        photoEditor?.addEmoji(emojis[index])
                                        showEmojiDialog = false
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEmojiDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Shapes Dialog (Simplified - Size Presets)
    if (showShapesDialog) {
        AlertDialog(
            onDismissRequest = { showShapesDialog = false },
            title = { Text("Draw Geometric Shapes") },
            text = {
                Column {
                    Text("Quick Size Presets:", fontWeight = FontWeight.Bold)
                    Text("Use these sizes for drawing shapes manually", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    
                    // Size Presets for Shapes
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { shapeSize = 3f },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Fine", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("3px", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                            
                            OutlinedButton(
                                onClick = { shapeSize = 8f },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Outline", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("8px", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { shapeSize = 15f },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Bold", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("15px", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                            
                            OutlinedButton(
                                onClick = { shapeSize = 25f },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Thick", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("25px", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    
                    Text("Custom Size: ${shapeSize.toInt()}px", fontWeight = FontWeight.Bold)
                    Slider(
                        value = shapeSize,
                        onValueChange = { shapeSize = it },
                        valueRange = 3f..30f
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    Text("Color:", fontWeight = FontWeight.Bold)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            android.graphics.Color.BLACK,
                            android.graphics.Color.RED,
                            android.graphics.Color.BLUE,
                            android.graphics.Color.GREEN
                        ).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(color), CircleShape)
                                    .clickable { selectedColor = color }
                                    .then(
                                        if (selectedColor == color)
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    photoEditor?.let { editor ->
                        val shapeBuilder = ja.burhanrashid52.photoeditor.shape.ShapeBuilder()
                            .withShapeType(ja.burhanrashid52.photoeditor.shape.ShapeType.Brush)
                            .withShapeSize(shapeSize)
                            .withShapeColor(selectedColor)
                            .withShapeOpacity(100)
                        editor.setShape(shapeBuilder)
                        editor.setBrushDrawingMode(true)
                    }
                    showShapesDialog = false
                }) {
                    Text("Start Drawing")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShapesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Filters Dialog
    if (showFiltersDialog) {
        AlertDialog(
            onDismissRequest = { showFiltersDialog = false },
            title = { Text("Apply Filter") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Manga & B/W Effects:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    
                    // Manga essential filters
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterButton("Black & White", ja.burhanrashid52.photoeditor.PhotoFilter.BLACK_WHITE) { photoEditor?.setFilterEffect(it) }
                        FilterButton("Gray Scale", ja.burhanrashid52.photoeditor.PhotoFilter.GRAY_SCALE) { photoEditor?.setFilterEffect(it) }
                        FilterButton("Brightness +", ja.burhanrashid52.photoeditor.PhotoFilter.BRIGHTNESS) { photoEditor?.setFilterEffect(it) }
                        FilterButton("Contrast +", ja.burhanrashid52.photoeditor.PhotoFilter.CONTRAST) { photoEditor?.setFilterEffect(it) }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    
                    Text("Artistic Filters:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterButton("Fill Light", ja.burhanrashid52.photoeditor.PhotoFilter.FILL_LIGHT) { photoEditor?.setFilterEffect(it) }
                        FilterButton("Sepia (Vintage)", ja.burhanrashid52.photoeditor.PhotoFilter.SEPIA) { photoEditor?.setFilterEffect(it) }
                        FilterButton("Cross Process", ja.burhanrashid52.photoeditor.PhotoFilter.CROSS_PROCESS) { photoEditor?.setFilterEffect(it) }
                        FilterButton("Documentary", ja.burhanrashid52.photoeditor.PhotoFilter.DOCUMENTARY) { photoEditor?.setFilterEffect(it) }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    
                    Text("Color Effects:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterButton("Auto Fix", ja.burhanrashid52.photoeditor.PhotoFilter.AUTO_FIX) { photoEditor?.setFilterEffect(it) }
                        FilterButton("Saturation +", ja.burhanrashid52.photoeditor.PhotoFilter.SATURATE) { photoEditor?.setFilterEffect(it) }
                        FilterButton("Temperature", ja.burhanrashid52.photoeditor.PhotoFilter.TEMPERATURE) { photoEditor?.setFilterEffect(it) }
                        FilterButton("Tint", ja.burhanrashid52.photoeditor.PhotoFilter.TINT) { photoEditor?.setFilterEffect(it) }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    Text("💡 Tip: Filters are applied to the entire image", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Reset to no filter
                    photoEditor?.setFilterEffect(ja.burhanrashid52.photoeditor.PhotoFilter.NONE)
                    showFiltersDialog = false
                }) {
                    Text("Clear Filter")
                }
            }
        )
    }
    
    // Eraser Dialog
    if (showEraserDialog) {
        AlertDialog(
            onDismissRequest = { showEraserDialog = false },
            title = { Text("Eraser Settings") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Choose Eraser Mode:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    
                    // Auto Remove Option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Clear all drawing (reset canvas)
                                photoEditor?.clearAllViews()
                                showEraserDialog = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Clear All",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Remove all drawings instantly",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Manual Erase Mode
                    Text("Manual Eraser:", fontWeight = FontWeight.Bold)
                    Text("Brush to erase specific areas", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    
                    // Quick Size Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { eraserSize = 15f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.7f), CircleShape)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("15px", fontSize = 9.sp)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { eraserSize = 35f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.7f), CircleShape)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("35px", fontSize = 9.sp)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { eraserSize = 70f },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.7f), CircleShape)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("70px", fontSize = 9.sp)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Custom Size
                    Text("Custom Size: ${eraserSize.toInt()}px", fontWeight = FontWeight.Bold)
                    Slider(
                        value = eraserSize,
                        onValueChange = { eraserSize = it },
                        valueRange = 10f..100f
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    Text("💡 Tip: Larger size for quick cleaning, smaller for precision", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = {
                    photoEditor?.brushEraser()
                    // Apply eraser size via ShapeBuilder
                    val shapeBuilder = ja.burhanrashid52.photoeditor.shape.ShapeBuilder()
                        .withShapeSize(eraserSize)
                    photoEditor?.setShape(shapeBuilder)
                    showEraserDialog = false
                }) {
                    Text("Start Erasing")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraserDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FilterButton(
    label: String,
    filter: ja.burhanrashid52.photoeditor.PhotoFilter,
    onClick: (ja.burhanrashid52.photoeditor.PhotoFilter) -> Unit
) {
    OutlinedButton(
        onClick = { onClick(filter) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, modifier = Modifier.fillMaxWidth())
    }
}

enum class EditorTool {
    NONE, BRUSH, TEXT, SHAPES, FILTERS, ERASER, STICKER, EMOJI
}

// Compact tool button for landscape mode (Icon only)
@Composable
fun CompactToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun PhotoEditorToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(28.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigAndGenerateScreen(
    capturedBitmap: Bitmap?,
    onBackToDraw: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToGallery: (() -> Unit)?,
    viewModel: GenerateViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Premium Logic
    val generationLimit by viewModel.generationLimit.collectAsState()
    val isPremium = remember(generationLimit) {
        generationLimit?.subscriptionType != "free" && generationLimit?.isSubscriptionExpired() == false
    }
    
    // States
    var resultImageUrl by remember { mutableStateOf<String?>(null) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var blockedWord by remember { mutableStateOf("") }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    
    // Config
    var positivePrompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    var controlnetStrength by remember { mutableFloatStateOf(1.0f) }
    var cfgScale by remember { mutableFloatStateOf(7.0f) }
    var denoise by remember { mutableFloatStateOf(0.65f) }  // Lower for drawing preservation (0.6-0.7 recommended)
    var steps by remember { mutableIntStateOf(20) }
    var width by remember { mutableIntStateOf(1024) }
    var height by remember { mutableIntStateOf(1024) }
    var seed by remember { mutableStateOf("763189886868065") }
    
    // Enforce limits
    LaunchedEffect(isPremium) {
        if (!isPremium) {
            steps = 20
            width = 1024
            height = 1024
        }
    }

    // Dropdowns
    var selectedSampler by remember { mutableStateOf("dpmpp_2m_sde") }
    var selectedScheduler by remember { mutableStateOf("karras") }
    var selectedCheckpoint by remember { mutableStateOf("wildcardxXLTURBO_wildcardxXLTURBOV10.safetensors") }
    
    var expandedSampler by remember { mutableStateOf(false) }
    var expandedScheduler by remember { mutableStateOf(false) }
    var expandedCheckpoint by remember { mutableStateOf(false) }
    var expandedWidth by remember { mutableStateOf(false) }
    var expandedHeight by remember { mutableStateOf(false) }
    var isConfigExpanded by remember { mutableStateOf(true) }

    // Options Lists (Reused)
    val samplerOptions = listOf("euler", "euler_a", "heun", "dpm_2", "dpm_2_a", "lms", "dpm_fast", "dpm_adaptive", "dpmpp_2s_a", "dpmpp_sde", "dpmpp_2m", "dpmpp_2m_sde", "ddim", "uni_pc")
    val schedulerOptions = listOf("normal", "karras", "exponential", "simple", "ddim_uniform")
    val checkpointOptions = listOf(
        "BSSEquinoxILSemi_v30.safetensors",
        "miaomiaoPixel_vPred11.safetensors",
        "akashicpulse_v41.safetensors",
        "ramthrustsNSFWPINK_alchemyMix176.safetensors",
        "astreapixieRadiance_v16.safetensors",
        "catCarrier_v70.safetensors",
        "animeCelestialMagic_v30.safetensors",
        "animeChangefulXL_v10ReleasedCandidate.safetensors",
        "animecollection_v420.safetensors",
        "corepomMIX2_sakuraMochi.safetensors",
        "dollamor_v10.safetensors",
        "flatAnimix_v20.safetensors",
        "floraEclipse_v10.safetensors",
        "hotaruBlend_vPredV20.safetensors",
        "hyperpixel_v10.safetensors",
        "lakeLakeSeriesOfModels_renoLakeK1S.safetensors",
        "lizmix_version18.safetensors",
        "lilithsLullaby_v10.safetensors",
        "endlustriaLUMICA_v2.safetensors",
        "meinamix_meinaV11.safetensors",
        "mritualIllustrious_v20.safetensors",
        "novaAnimeXL_ilV125.safetensors",
        "nova3DCGXL_ilV70.safetensors",
        "novaFurryXL_ilV120.safetensors",
        "novaMatureXL_v35.safetensors",
        "novaOrangeXL_reV30.safetensors",
        "novaRetroXL_v20.safetensors",
        "novaUnrealXL_v100.safetensors",
        "scyraxPastelCore_v10.safetensors",
        "perfectdeliberate_v20A.safetensors",
        "wildcardxXLTURBO_wildcardxXLTURBOV10.safetensors",
        "PVCStyleModelMovable_epsIll11.safetensors",
        "AnimeRealPantheon_k1ssBakedvae.safetensors",
        "redLilyIllu_v10.safetensors",
        "silenceMix_v7.safetensors",
        "softMixKR_v23.safetensors",
        "3dStock3dAnimeStyle_v30.safetensors",
        "tinyNovaMerge_v25.safetensors",
        "trattoNero_vitta.safetensors",
        "dvine_v60.safetensors",
        "xeHentaiAnimePDXL_02.safetensors",
        "asianBlendIllustrious_v10.safetensors",
        "asianBlendPDXLPony_v1.safetensors",
        "margaretAsianWomanPony_v10.safetensors",
        "ponyAsianRealismAlpha_v05.safetensors",
        "iniverseMixSFWNSFW_realXLV1.safetensors",
        "intorealismUltra_v10.safetensors",
        "novaRealityXL_ilV901.safetensors",
        "realcosplay_realchenkinv10.safetensors",
        "ultra_v11.safetensors"
    )

    val checkpointFriendlyNames = mapOf(
        "BSSEquinoxILSemi_v30.safetensors" to "Animal Equinox",
        "miaomiaoPixel_vPred11.safetensors" to "Anime Ah Pixel",
        "akashicpulse_v41.safetensors" to "Anime Akashic Pulse",
        "ramthrustsNSFWPINK_alchemyMix176.safetensors" to "Anime Alchemy Mix",
        "astreapixieRadiance_v16.safetensors" to "Anime Astrea Pixie Radiance",
        "catCarrier_v70.safetensors" to "Anime Cat Carrier",
        "animeCelestialMagic_v30.safetensors" to "Anime Celestial Magic",
        "animeChangefulXL_v10ReleasedCandidate.safetensors" to "Anime Change Full Perform",
        "animecollection_v420.safetensors" to "Anime Collection",
        "corepomMIX2_sakuraMochi.safetensors" to "Anime Corepom Mix 2",
        "dollamor_v10.safetensors" to "Anime Doll Doll",
        "flatAnimix_v20.safetensors" to "Anime Flat Animix",
        "floraEclipse_v10.safetensors" to "Anime Flora Eclipse",
        "hotaruBlend_vPredV20.safetensors" to "Anime Hotaru Blend",
        "hyperpixel_v10.safetensors" to "Anime Hyper Pixel",
        "lakeLakeSeriesOfModels_renoLakeK1S.safetensors" to "Anime Lake Reno Series",
        "lizmix_version18.safetensors" to "Anime Lizmix",
        "lilithsLullaby_v10.safetensors" to "Anime Lulaby",
        "endlustriaLUMICA_v2.safetensors" to "Anime Lumica Illusion",
        "meinamix_meinaV11.safetensors" to "Anime Main Mix",
        "mritualIllustrious_v20.safetensors" to "Anime Mritual Ilustrious",
        "novaAnimeXL_ilV125.safetensors" to "Anime Nova",
        "nova3DCGXL_ilV70.safetensors" to "Anime Nova 3D",
        "novaFurryXL_ilV120.safetensors" to "Anime Nova Furry",
        "novaMatureXL_v35.safetensors" to "Anime Nova Mature",
        "novaOrangeXL_reV30.safetensors" to "Anime Nova Orange",
        "novaRetroXL_v20.safetensors" to "Anime Nova Retro",
        "novaUnrealXL_v100.safetensors" to "Anime Nova Unreal",
        "scyraxPastelCore_v10.safetensors" to "Anime Pastel Core",
        "perfectdeliberate_v20A.safetensors" to "Anime Perfect Deliberate",
        "wildcardxXLTURBO_wildcardxXLTURBOV10.safetensors" to "Anime Premium Ultra",
        "PVCStyleModelMovable_epsIll11.safetensors" to "Anime PVC Moveable",
        "AnimeRealPantheon_k1ssBakedvae.safetensors" to "Anime Real Pantheon",
        "redLilyIllu_v10.safetensors" to "Anime Red Lily",
        "silenceMix_v7.safetensors" to "Anime Silence Mix",
        "softMixKR_v23.safetensors" to "Anime Soft Mix",
        "3dStock3dAnimeStyle_v30.safetensors" to "Anime Stock Style",
        "tinyNovaMerge_v25.safetensors" to "Anime Tiny Merge",
        "trattoNero_vitta.safetensors" to "Anime Tratto Nero",
        "dvine_v60.safetensors" to "Anime Well Vell",
        "xeHentaiAnimePDXL_02.safetensors" to "Anime Xe",
        "asianBlendIllustrious_v10.safetensors" to "General Asia Blend Illustrious",
        "asianBlendPDXLPony_v1.safetensors" to "General Asia Blend Pony",
        "margaretAsianWomanPony_v10.safetensors" to "General Asia Margaret Pony",
        "ponyAsianRealismAlpha_v05.safetensors" to "General Asia Realism Alpha",
        "iniverseMixSFWNSFW_realXLV1.safetensors" to "General Iniverse Mix",
        "intorealismUltra_v10.safetensors" to "General Intorealism Ultra",
        "novaRealityXL_ilV901.safetensors" to "General Nova Reality",
        "realcosplay_realchenkinv10.safetensors" to "General Real Cosplay",
        "ultra_v11.safetensors" to "General Ultra"
    )
    val resolutionOptions = listOf(512, 768, 1024, 1536, 2048)
    

    val appPreferences = remember { com.doyouone.drawai.data.preferences.AppPreferences(context) }
    val isRestrictedContentEnabled by appPreferences.isRestrictedContentEnabled.collectAsState(initial = false)

    val filteredCheckpointOptions = remember(isRestrictedContentEnabled) {
        if (isRestrictedContentEnabled) {
            checkpointOptions
        } else {
            checkpointOptions.filter { 
                !it.contains("nsfw", ignoreCase = true) && 
                !it.contains("hentai", ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onBackToDraw() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "Generate Art", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Turn your drawing into art", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Preview
                Text(
                    "Your Drawing Preview", 
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedBitmap != null) {
                            AsyncImage(
                                model = capturedBitmap,
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            
            // Error
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
            }

            // Config Form (simplified reuse)
            // WORKFLOWS
             Text("Workflows", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
             Spacer(Modifier.height(8.dp))
             Box {
                 OutlinedTextField(
                     value = checkpointFriendlyNames[selectedCheckpoint] ?: selectedCheckpoint,
                     onValueChange = {},
                                        readOnly = true,
                                        enabled = isPremium,
                                        leadingIcon = {
                                            val friendlyName = checkpointFriendlyNames[selectedCheckpoint] ?: selectedCheckpoint
                                            val imageId = friendlyName.lowercase().replace(" ", "_").replace("(", "").replace(")", "")
                                            AsyncImage(
                                                model = "${com.doyouone.drawai.data.api.RetrofitClient.getBaseUrl()}workflow-image/$imageId",
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentScale = ContentScale.Crop
                                            )
                                        },
                                        trailingIcon = { 
                                             if (!isPremium) Icon(Icons.Default.Lock, "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                                             else IconButton(onClick = { expandedCheckpoint = !expandedCheckpoint }) { Icon(Icons.Default.ArrowDropDown, "") }
                                        },
                                        modifier = Modifier.fillMaxWidth().clickable { if(isPremium) expandedCheckpoint = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        )

                 Spacer(Modifier.height(16.dp))
                 DropdownMenu(expanded = expandedCheckpoint, onDismissRequest = { expandedCheckpoint = false }) {
                     filteredCheckpointOptions.forEach { 
                         DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val friendlyName = checkpointFriendlyNames[it] ?: it
                                    val imageId = friendlyName.lowercase().replace(" ", "_").replace("(", "").replace(")", "")
                                    AsyncImage(
                                        model = "${com.doyouone.drawai.data.api.RetrofitClient.getBaseUrl()}workflow-image/$imageId",
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(friendlyName)
                                }
                            }, 
                            onClick = { selectedCheckpoint = it; expandedCheckpoint = false }
                        )
                     }
                 }
             }
             if(!isPremium) Text("Upgrade to generic more styles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
             Spacer(Modifier.height(16.dp))
             
             // PROMPTS
             Text("Vision", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
             OutlinedTextField(
                 value = positivePrompt,
                 onValueChange = { positivePrompt = it },
                 placeholder = { Text("e.g. anime girl, colorful, detailed") },
                 modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                 shape = RoundedCornerShape(12.dp),
                 minLines = 2
             )
             Spacer(Modifier.height(8.dp))
             
             Text("Avoid", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
             OutlinedTextField(
                 value = negativePrompt,
                 onValueChange = { negativePrompt = it },
                 placeholder = { Text("e.g. blurry, low quality, ugly") },
                 modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                 shape = RoundedCornerShape(12.dp),
                 minLines = 2
             )
             Spacer(Modifier.height(16.dp))
             
             // SLIDERS
             Text("ControlNet Strength: ${String.format("%.1f", controlnetStrength)}")
             Slider(value = controlnetStrength, onValueChange = { controlnetStrength = it }, valueRange = 0.5f..2.0f)
             
             Text("Denoise: ${String.format("%.2f", denoise)}")
             Slider(value = denoise, onValueChange = { denoise = it }, valueRange = 0.1f..1.0f)
             
             Text("Steps: $steps ${if(!isPremium) "(Locked)" else ""}")
             Slider(value = steps.toFloat(), onValueChange = { if(isPremium) steps = it.toInt() }, valueRange = 10f..50f, enabled = isPremium)
             
             Spacer(Modifier.height(16.dp))
             
             // Seed
             OutlinedTextField(
                 value = seed, onValueChange = { if(it.all{c->c.isDigit()}) seed = it },
                 label = { Text("Seed (Empty = Random)") },
                 keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                 modifier = Modifier.fillMaxWidth(),
                 trailingIcon = { if(seed.isNotEmpty()) IconButton(onClick={seed=""}){Icon(Icons.Default.Close,"")} else Icon(Icons.Default.Shuffle,"") }
             )
             
             Spacer(Modifier.height(32.dp))
             
             // GENERATE BUTTON (Hidden when processing)
             if (isProcessing) {
                 CircularProgressIndicator()
                 Text(processingMessage)
             }
             
             if (!isProcessing) {
                 Button(
                     onClick = {
                         if (capturedBitmap == null) return@Button
                         scope.launch {
                             isProcessing = true
                             errorMessage = null
                             processingMessage = "Preparing..."
                             
                             // 1. NSFW / Safety Filter (Client-Side)
                             val nsfwKeywords = listOf(
                                 "nsfw", "nude", "naked", "sex", "porn", "adult", "mature", "explicit",
                                 "lewd", "waifu", "hentai", "ecchi", "loli", "shota", "undress",
                                 "n4k3d", "s3x", "p0rn", "fuck", "dick", "vagina", "pussy", "breast"
                             )
                             
                             val promptToCheck = (positivePrompt + " " + negativePrompt).lowercase()
                             val foundKeyword = nsfwKeywords.firstOrNull { promptToCheck.contains(it) }
                             
                             if (foundKeyword != null && !isPremium) {
                                 blockedWord = foundKeyword
                                 showBlockedDialog = true
                                 isProcessing = false
                                 return@launch
                             }
                             
                             try {
                                 // Convert captured bitmap to file
                                 val file = File(context.cacheDir, "temp_draw_upload.png")
                                 val out = FileOutputStream(file)
                                 capturedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                 out.close()
                                 
                                 val apiService = com.doyouone.drawai.data.api.RetrofitClient.apiService
                                 val requestBody = file.asRequestBody("image/png".toMediaTypeOrNull())
                                 val imagePart = MultipartBody.Part.createFormData("image", file.name, requestBody)
                                 
                                 val options = mapOf(
                                     "positive_prompt" to positivePrompt.toRequestBody(),
                                     "negative_prompt" to negativePrompt.toRequestBody(),
                                     "controlnet_strength" to controlnetStrength.toString().toRequestBody(),
                                     "cfg_scale" to cfgScale.toString().toRequestBody(),
                                     "denoise" to denoise.toString().toRequestBody(),
                                     "steps" to steps.toString().toRequestBody(),
                                     "width" to width.toString().toRequestBody(),
                                     "height" to height.toString().toRequestBody(),
                                     "sampler" to selectedSampler.toRequestBody(),
                                     "scheduler" to selectedScheduler.toRequestBody(),
                                     "checkpoint" to selectedCheckpoint.toRequestBody(),
                                     "seed" to (if(seed.isEmpty()) "-1" else seed).toRequestBody()
                                 )
                                 
                                 processingMessage = "Uploading..."
                                 val response = apiService.drawToImage(imagePart, options)
                                 if (!response.isSuccessful) throw Exception("Server Error: ${response.code()}")
                                 
                                 val taskId = response.body()?.taskId ?: throw Exception("No Task ID")
                                 processingMessage = "Generating..."
                                 
                                 // Poll
                                 var attempts = 0
                                 while(attempts < 60) {
                                     delay(2000)
                                     attempts++
                                     val statusRes = apiService.getTaskStatus(taskId)
                                     val status = statusRes.body()?.status
                                     if (status == "completed") {
                                         val files = statusRes.body()?.resultFiles ?: emptyList()
                                         if (files.isNotEmpty()) {
                                              val fname = files[0].substringAfterLast("/").substringAfterLast("\\")
                                              resultImageUrl = "https://drawai-api.drawai.site/download/$fname"
                                              saveImageToGallery(context, fname, "Draw to Image")
                                         }
                                         // Show Ad
                                         if (com.doyouone.drawai.ads.AdManager.isInterstitialAdReady()) {
                                             com.doyouone.drawai.ads.AdManager.showInterstitialAd(
                                                 activity = (context as? android.app.Activity)!!,
                                                 onAdDismissed = { onNavigateToGallery?.invoke() }
                                             )
                                         } else {
                                             onNavigateToGallery?.invoke()
                                         }
                                         break
                                     } else if (status == "failed") {
                                         throw Exception(statusRes.body()?.error ?: "Failed")
                                     }
                                 }
                                 

                             } catch (e: Exception) {
                                 if (e.message?.contains("530") == true || e.message?.contains("Maintenance", ignoreCase = true) == true) {
                                     showMaintenanceDialog = true
                                 } else if (e.message?.contains("403") == true || e.message?.contains("limit", ignoreCase = true) == true) {
                                     showLimitDialog = true
                                 } else {
                                     errorMessage = e.message
                                 }
                                 isProcessing = false
                             }
                         }
                     },
                     modifier = Modifier.fillMaxWidth().height(56.dp),
                     shape = RoundedCornerShape(16.dp)
                 ) {
                     Icon(Icons.Default.AutoAwesome, null)
                     Spacer(Modifier.width(12.dp))
                     Text("Generate Art", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                 }
             }
             }
         }
     }


    // Copy the Limit Dialog Logic from previous implementation
    if (showLimitDialog) {
        com.doyouone.drawai.ui.components.RewardedAdDialog(
            remainingGenerations = 0,
            onDismiss = { showLimitDialog = false },
            onWatchAd = {
                if (context is android.app.Activity) {
                    com.doyouone.drawai.ads.AdManager.showRewardedAd(
                        context,
                        onUserEarnedReward = {
                            scope.launch {
                                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                if (userId != null) {
                                   val repo = com.doyouone.drawai.data.repository.FirebaseGenerationRepository()
                                   repo.addBonusGeneration(userId)
                                   android.widget.Toast.makeText(context, "Bonus generation added!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            showLimitDialog = false
                        }
                    )
                }
            },
            onUpgradeToPremium = { showLimitDialog = false }
        )
    }
    
    // Blocked Word Dialog
    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Restricted Content") },
            text = { Text("Your prompt contains a restricted word: \"$blockedWord\"\n\nPlease remove it to comply with safety guidelines.") },
            confirmButton = {
                TextButton(onClick = { showBlockedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    if (showMaintenanceDialog) {
        MaintenancePopup(
            onGoToGalleryClick = { 
                showMaintenanceDialog = false
                onNavigateToGallery?.invoke()
            },
            onRetry = {
                showMaintenanceDialog = false
            }
        )
    }
    
    // Processing Modal - Blocking dialog
    ProcessingModal(
        isProcessing = isProcessing,
        message = if (processingMessage.isNotEmpty()) processingMessage else "Processing...",
        detail = "Please wait while we generate your image."
    )
}

// Helpers
private fun String.toRequestBody(): RequestBody = this.toRequestBody("text/plain".toMediaTypeOrNull())

private suspend fun saveImageToGallery(context: Context, filename: String, prompt: String) {
    try {
        val repo = com.doyouone.drawai.data.repository.DrawAIRepository()
        val storage = com.doyouone.drawai.data.local.ImageStorage(context)
        val tempFile = File(context.cacheDir, filename)
        val result = repo.downloadImage(filename, tempFile)
        if (result.isSuccess) {
            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
            if (bitmap != null) {
                storage.saveImage(bitmap, prompt, "", "draw_to_image")
            }
        }
    } catch (e: Exception) { }
}
