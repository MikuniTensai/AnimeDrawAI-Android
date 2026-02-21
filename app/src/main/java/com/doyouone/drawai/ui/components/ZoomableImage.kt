package com.doyouone.drawai.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * Zoomable image composable with pinch-to-zoom, pan, and double-tap support
 * Provides smooth zooming and panning experience for fullscreen image viewing
 */
@Composable
fun ZoomableImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    minScale: Float = 1f,
    maxScale: Float = 5f
) {
    // Zoom and pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // Transformable state for pinch-to-zoom
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        
        // Only allow panning when zoomed in
        val newOffset = if (newScale > 1f) {
            offset + panChange
        } else {
            Offset.Zero
        }
        
        scale = newScale
        offset = newOffset
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformableState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            // Double-tap to zoom in/out
                            if (scale > 1f) {
                                // Reset zoom
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                // Zoom to 2x
                                scale = 2f
                                // Center zoom on tap location
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                offset = Offset(
                                    x = (centerX - tapOffset.x) * scale,
                                    y = (centerY - tapOffset.y) * scale
                                )
                            }
                        },
                        onTap = {
                            // Single tap
                            onTap?.invoke()
                        }
                    )
                },
            contentScale = ContentScale.Fit
        )
    }
}
