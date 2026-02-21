package com.doyouone.drawai.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.doyouone.drawai.R
import com.doyouone.drawai.ui.theme.AccentWhite

// Global State Manager for Interactive Tutorial
object ShowcaseManager {
    var step by mutableStateOf(0) // 0 = Inactive
    var activeTargetKey by mutableStateOf<String?>(null)
    
    // Map of target keys to their bounds on screen
    val targets = mutableStateMapOf<String, Rect>()
    
    fun startTutorial(context: android.content.Context) {
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val isShown = prefs.getBoolean("tutorial_shown", false)
        
        if (!isShown) {
            step = 1
            activeTargetKey = "workflow_item"
        }
    }
    
    fun nextStep(context: android.content.Context) {
        step++
        targets.clear() // Clear old targets when moving to new step
        
        updateActiveKey()
        
        // If finished
        if (step == 0) {
             markTutorialCompleted(context)
        }
    }
    
    fun skipTutorial(context: android.content.Context) {
        step = 0
        activeTargetKey = null
        markTutorialCompleted(context)
    }
    
    private fun markTutorialCompleted(context: android.content.Context) {
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("tutorial_shown", true).apply()
    }
    
    fun endTutorial(context: android.content.Context) {
        skipTutorial(context)
    }
    
    private fun updateActiveKey() {
        activeTargetKey = when (step) {
            1 -> "workflow_item"
            2 -> "prompt_input"
            3 -> "generate_button"
            else -> null // End
        }
        if (activeTargetKey == null) step = 0
    }


    fun updateTarget(key: String, coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            val newBounds = coordinates.boundsInRoot()
            val oldBounds = targets[key]
            
            // Update only if changed significantly (> 1.0f) to avoid jitter loops
            if (oldBounds == null || 
                kotlin.math.abs(oldBounds.left - newBounds.left) > 1f ||
                kotlin.math.abs(oldBounds.top - newBounds.top) > 1f ||
                kotlin.math.abs(oldBounds.width - newBounds.width) > 1f ||
                kotlin.math.abs(oldBounds.height - newBounds.height) > 1f
            ) {
                 targets[key] = newBounds
            }
        }
    }
}

fun Modifier.showcaseTarget(key: String): Modifier = onGloballyPositioned { coordinates ->
    ShowcaseManager.updateTarget(key, coordinates)
}

@Composable
fun ShowcaseOverlay(onTutorialComplete: () -> Unit = {}) {
    val currentStep = ShowcaseManager.step
    val activeKey = ShowcaseManager.activeTargetKey
    val targetRect = ShowcaseManager.targets[activeKey]
    
    // Debug logging
    LaunchedEffect(currentStep, activeKey, targetRect) {
        android.util.Log.d("ShowcaseOverlay", "=== Showcase State ===")
        android.util.Log.d("ShowcaseOverlay", "currentStep: $currentStep")
        android.util.Log.d("ShowcaseOverlay", "activeKey: $activeKey")
        android.util.Log.d("ShowcaseOverlay", "targetRect: $targetRect")
        android.util.Log.d("ShowcaseOverlay", "targets map: ${ShowcaseManager.targets}")
    }

    if (currentStep > 0 && targetRect != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f) // Crucial for BlendMode.Clear to work
        ) {
            // Dark Overlay with Hole
            Canvas(
                modifier = Modifier.fillMaxSize()
                // Visual only, allow clicks to pass through
            ) {
                val padding = 8.dp.toPx()
                
                // 1. Draw Dimmed Background
                drawRect(Color.Black.copy(alpha = 0.75f))

                // 2. Draw Hole (Clear Mode)
                drawRect(
                    color = Color.Transparent,
                    topLeft = targetRect.topLeft.minus(Offset(padding, padding)),
                    size = androidx.compose.ui.geometry.Size(
                        targetRect.width + padding * 2,
                        targetRect.height + padding * 2
                    ),
                    blendMode = BlendMode.Clear
                )
            }

            // Tooltip / Instruction Text
            val density = LocalDensity.current
            val screenHeight = 
                androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
            val targetBottom = with(density) { targetRect.bottom.toDp() }
            
            val tooltipTopPadding = if (targetBottom > screenHeight / 2) {
                // Target is in bottom half -> Show tooltip ABOVE
                with(density) { (targetRect.top.toDp() - 100.dp) }
            } else {
                // Target is in top half -> Show tooltip BELOW
                with(density) { (targetRect.bottom.toDp() + 24.dp) }
            }

            Box(
                modifier = Modifier
                    .offset(
                        y = maxOf(0.dp, tooltipTopPadding)
                    )
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val stepTitle = when (currentStep) {
                        1 -> stringResource(R.string.tutorial_step1_title)
                        2 -> stringResource(R.string.tutorial_step2_title)
                        3 -> stringResource(R.string.tutorial_step3_title)
                        else -> ""
                    }
                    
                    val stepDesc = when (currentStep) {
                        1 -> stringResource(R.string.tutorial_step1_desc)
                        2 -> stringResource(R.string.tutorial_step2_desc)
                        3 -> stringResource(R.string.tutorial_step3_desc)
                        else -> ""
                    }
                    
                    Text(
                        text = stepTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stepDesc,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Next Button
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = { 
                            if (currentStep == 3) {
                                onTutorialComplete()
                            }
                            ShowcaseManager.nextStep(context) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (currentStep == 3) stringResource(R.string.tutorial_btn_finish) else stringResource(R.string.tutorial_btn_next),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
