package com.doyouone.drawai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.doyouone.drawai.R
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Genshin Impact-style Summoning Animation Screen
 * Shows animated reveal based on character rarity (deadly sins count)
 */
@Composable
fun SummoningAnimationScreen(
    characterImageUrl: String,
    sinCount: Int,
    rarity: String,
    onAnimationComplete: () -> Unit
) {
    var animationPhase by remember { mutableStateOf(0) }
    
    // Get colors based on sin count
    val colors = remember(sinCount) { getRarityColors(sinCount) }
    
    // Animation timing
    LaunchedEffect(Unit) {
        delay(300) // Initial delay
        animationPhase = 1 // Fade in
        delay(500)
        animationPhase = 2 // Particle burst
        delay(700)
        animationPhase = 3 // Color reveal
        delay(500)
        animationPhase = 4 // Image reveal
        delay(300)
        animationPhase = 5 // Rarity badge
        delay(800)
        animationPhase = 6 // Hold
        delay(300)
        onAnimationComplete() // Fade out and navigate
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Phase 1-2: Particle effects
        if (animationPhase >= 2) {
            ParticleEffect(
                colors = colors,
                isActive = animationPhase in 2..5
            )
        }
        
        // Phase 3: Color gradient reveal
        if (animationPhase >= 3) {
            ColorRevealEffect(
                colors = colors,
                progress = if (animationPhase >= 3) 1f else 0f
            )
        }
        
        // Phase 4: Character image
        if (animationPhase >= 4) {
            CharacterImageReveal(
                imageUrl = characterImageUrl,
                colors = colors,
                progress = if (animationPhase >= 4) 1f else 0f
            )
        }
        
        // Phase 5: Rarity badge
        if (animationPhase >= 5) {
            RarityBadge(
                sinCount = sinCount,
                rarity = rarity,
                colors = colors,
                alpha = if (animationPhase >= 5) 1f else 0f
            )
        }
    }
}

/**
 * Get color scheme based on sin count
 */
data class RarityColors(
    val primary: Color,
    val secondary: Color,
    val particle: Color
)

private fun getRarityColors(sinCount: Int): RarityColors {
    return when (sinCount) {
        1 -> RarityColors( // Common - Bronze
            primary = Color(0xFFCD7F32),
            secondary = Color(0xFF8B4513),
            particle = Color(0xFFDEB887)
        )
        2 -> RarityColors( // Rare - Purple
            primary = Color(0xFF9370DB),
            secondary = Color(0xFF8A2BE2),
            particle = Color(0xFFDDA0DD)
        )
        3 -> RarityColors( // Epic - Silver
            primary = Color(0xFFC0C0C0),
            secondary = Color(0xFFE8E8E8),
            particle = Color(0xFFFFFFFF)
        )
        else -> RarityColors( // Legendary (4-7) - Gold
            primary = Color(0xFFFFD700),
            secondary = Color(0xFFFFA500),
            particle = Color(0xFFFFE4B5)
        )
    }
}

/**
 * Animated particle effect
 */
@Composable
private fun ParticleEffect(
    colors: RarityColors,
    isActive: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val particles = remember {
        List(30) {
            Particle(
                angle = Random.nextFloat() * 360f,
                distance = Random.nextFloat() * 300f + 100f,
                size = Random.nextFloat() * 8f + 4f,
                speed = Random.nextFloat() * 0.5f + 0.5f
            )
        }
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (isActive) {
            particles.forEach { particle ->
                val animatedAngle = particle.angle + (rotation * particle.speed)
                val x = center.x + cos(Math.toRadians(animatedAngle.toDouble())).toFloat() * particle.distance
                val y = center.y + sin(Math.toRadians(animatedAngle.toDouble())).toFloat() * particle.distance
                
                drawCircle(
                    color = colors.particle.copy(alpha = 0.8f),
                    radius = particle.size,
                    center = Offset(x, y)
                )
                
                // Draw star shape for legendary
                if (particle.size > 8f) {
                    drawStar(
                        center = Offset(x, y),
                        radius = particle.size * 1.5f,
                        color = colors.primary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private data class Particle(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val speed: Float
)

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val points = 5
    val outerRadius = radius
    val innerRadius = radius * 0.4f
    
    for (i in 0 until points * 2) {
        val angle = (i * 36 - 90) * Math.PI / 180
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        
        if (i == 0) path.moveTo(x, y)
        else path.lineTo(x, y)
    }
    path.close()
    
    drawPath(path, color)
}

/**
 * Color gradient reveal effect
 */
@Composable
private fun ColorRevealEffect(
    colors: RarityColors,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "colorReveal"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(animatedProgress)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = 0.3f),
                        colors.secondary.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    radius = 800f * animatedProgress
                )
            )
    )
}

/**
 * Character image reveal with glow effect
 */
@Composable
private fun CharacterImageReveal(
    imageUrl: String,
    colors: RarityColors,
    progress: Float
) {
    val scale by animateFloatAsState(
        targetValue = if (progress > 0f) 1f else 0.5f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "imageScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (progress > 0f) 1f else 0f,
        animationSpec = tween(500),
        label = "imageAlpha"
    )
    
    Box(
        modifier = Modifier
            .size(300.dp)
            .scale(scale)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Character image
        AsyncImage(
            model = imageUrl,
            contentDescription = stringResource(R.string.summon_content_desc_character),
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Rarity badge showing sin count
 */
@Composable
private fun RarityBadge(
    sinCount: Int,
    rarity: String,
    colors: RarityColors,
    alpha: Float
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(300),
        label = "badgeAlpha"
    )
    
    Column(
        modifier = Modifier
            .offset(y = 200.dp)
            .alpha(animatedAlpha),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sin count with stars
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(minOf(sinCount, 7)) {
                Text(
                    text = "★",
                    fontSize = 32.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Rarity text
        Text(
            text = rarity,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        
        // Sin count text
        Text(
            text = "$sinCount " + stringResource(R.string.summon_deadly_sins),
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
