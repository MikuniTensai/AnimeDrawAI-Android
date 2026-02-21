// File: GenerationLimitUIExample.kt
// Contoh implementasi UI untuk generation limit system
// Copy code yang diperlukan ke file Composable Anda

package com.doyouone.drawai.ui.example

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doyouone.drawai.data.model.GenerationLimit
import com.doyouone.drawai.viewmodel.GenerateUiState

/**
 * Badge yang menampilkan sisa generasi user
 */
@Composable
fun GenerationLimitBadge(
    limit: GenerationLimit?,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {}
) {
    if (limit == null) return
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (limit.isPremium) 
                Color(0xFFFFD700) // Gold untuk premium
            else 
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (limit.isPremium) 
                        Icons.Default.Star 
                    else 
                        Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (limit.isPremium) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    if (limit.isPremium) {
                        Text(
                            text = "Premium",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Unlimited generations",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    } else {
                        Text(
                            text = "Free Plan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${limit.getRemainingGenerations()} / ${limit.maxDailyLimit} generations left today",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Show upgrade button for free users
            if (!limit.isPremium) {
                FilledTonalButton(
                    onClick = onUpgradeClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Upgrade,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upgrade", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Linear progress indicator untuk sisa generasi
 */
@Composable
fun GenerationProgressBar(
    limit: GenerationLimit?,
    modifier: Modifier = Modifier
) {
    if (limit == null || limit.isPremium) return
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Daily generations",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${limit.dailyGenerations} / ${limit.maxDailyLimit}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = limit.dailyGenerations.toFloat() / limit.maxDailyLimit.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = if (limit.dailyGenerations >= limit.maxDailyLimit) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Dialog ketika limit tercapai
 */
@Composable
fun GenerationLimitDialog(
    message: String,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Daily Limit Reached")
        },
        text = {
            Column {
                Text(message)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Upgrade to Premium untuk:",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                PremiumFeatureItem("✨ Unlimited daily generations")
                PremiumFeatureItem("🚀 Priority processing queue")
                PremiumFeatureItem("🎨 Access to exclusive workflows")
                PremiumFeatureItem("📱 Ad-free experience")
            }
        },
        confirmButton = {
            Button(onClick = onUpgrade) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upgrade to Premium")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Nanti")
            }
        }
    )
}

@Composable
private fun PremiumFeatureItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Full screen untuk Premium upgrade
 */
@Composable
fun PremiumUpgradeScreen(
    onBack: () -> Unit,
    onSubscribe: (plan: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Upgrade to Premium",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Hero section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFD700)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Unlimited Creativity",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Generate as many images as you want",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Features list
        Text(
            text = "Premium Features",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FeatureCard(
            icon = Icons.Default.AutoAwesome,
            title = "Unlimited Generations",
            description = "Generate as many images as you want, no daily limits"
        )
        
        FeatureCard(
            icon = Icons.Default.Speed,
            title = "Priority Queue",
            description = "Your generations are processed first"
        )
        
        FeatureCard(
            icon = Icons.Default.Palette,
            title = "Exclusive Workflows",
            description = "Access to premium-only art styles"
        )
        
        FeatureCard(
            icon = Icons.Default.Block,
            title = "No Ads",
            description = "Enjoy ad-free experience"
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Subscription plans
        Text(
            text = "Choose Your Plan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SubscriptionPlanCard(
            title = "Monthly",
            price = "Rp 29.000",
            period = "/month",
            features = listOf("Cancel anytime", "All premium features"),
            onSubscribe = { onSubscribe("monthly") }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SubscriptionPlanCard(
            title = "Yearly",
            price = "Rp 399.000",
            period = "/year",
            badge = "Save 32%",
            features = listOf("Best value", "All premium features"),
            highlighted = true,
            onSubscribe = { onSubscribe("yearly") }
        )
    }
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SubscriptionPlanCard(
    title: String,
    price: String,
    period: String,
    features: List<String>,
    badge: String? = null,
    highlighted: Boolean = false,
    onSubscribe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (highlighted) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                badge?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = period,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Subscribe Now")
            }
        }
    }
}

/**
 * Contoh penggunaan di GenerateScreen
 */
@Composable
fun GenerateScreenExample(
    viewModel: com.doyouone.drawai.viewmodel.GenerateViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val generationLimit by viewModel.generationLimit.collectAsState()
    var showUpgradeDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Show generation limit badge
        GenerationLimitBadge(
            limit = generationLimit,
            onUpgradeClick = { showUpgradeDialog = true }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Show progress bar
        GenerationProgressBar(limit = generationLimit)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Your generate UI here...
        Button(
            onClick = {
                viewModel.generateImage(
                    positivePrompt = "anime girl with sword",
                    workflow = "anime_action"
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate Image")
        }
        
        // Handle limit exceeded
        when (val state = uiState) {
            is com.doyouone.drawai.viewmodel.GenerateUiState.LimitExceeded -> {
                GenerationLimitDialog(
                    message = state.message,
                    onDismiss = { viewModel.resetState() },
                    onUpgrade = {
                        viewModel.resetState()
                        showUpgradeDialog = true
                    }
                )
            }
            else -> { /* Handle other states */ }
        }
    }
    
    // Upgrade dialog/screen
    if (showUpgradeDialog) {
        // Show premium upgrade screen
        // Implementation depends on your navigation setup
    }
}
