package com.doyouone.drawai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doyouone.drawai.R
import com.doyouone.drawai.data.model.InventoryItem
import com.doyouone.drawai.viewmodel.InventoryUiState
import com.doyouone.drawai.viewmodel.InventoryViewModel
import com.doyouone.drawai.ui.theme.Purple40
import com.doyouone.drawai.ui.theme.Purple80

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var itemToUse by remember { mutableStateOf<InventoryItem?>(null) }
    var showUseDialog by remember { mutableStateOf(false) }

    if (showUseDialog && itemToUse != null) {
        val item = itemToUse!!
        AlertDialog(
            onDismissRequest = { showUseDialog = false },
            title = { Text("Use ${item.name}?") },
            text = { Text("Do you want to activate this item now? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUseDialog = false
                        viewModel.useItem(
                            itemId = item.id,
                            onSuccess = { msg -> 
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show() 
                            },
                            onError = { err -> 
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show() 
                            }
                        )
                    }
                ) { Text("Use") }
            },
            dismissButton = {
                TextButton(onClick = { showUseDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Header matching other screens
        com.doyouone.drawai.ui.components.AnimeDrawMainTopBar(
            title = "Inventory", 
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.loadInventory() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is InventoryUiState.Loading -> {
                     CircularProgressIndicator(color = Purple40)
                }
                is InventoryUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadInventory() }) {
                            Text("Retry")
                        }
                    }
                }
                is InventoryUiState.Success -> {
                    if (state.items.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Empty Inventory",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Claim daily rewards or visit the shop to get items!",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.items) { item ->
                                InventoryItemCard(
                                    item = item,
                                    onClick = {
                                        when (item.id) {
                                            "pro_pass_3d", "outfit_ticket" -> {
                                                itemToUse = item
                                                showUseDialog = true
                                            }
                                            "streak_ice" -> {
                                                Toast.makeText(context, "This protects your streak automatically if you miss a day.", Toast.LENGTH_LONG).show()
                                            }
                                            "candy", "coffee", "rose", "chocolate", "ring" -> {
                                                Toast.makeText(context, "Gift this to your character in Chat!", Toast.LENGTH_SHORT).show()
                                            }
                                            else -> {
                                                // Unknown, maybe try use?
                                                 Toast.makeText(context, "Item info: ${item.description}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemCard(item: InventoryItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Placeholder Icon (In real app, map item.id/iconRes to painter)
            val iconRes = when (item.id) {
                 "candy" ->  R.drawable.ic_launcher_foreground // Placeholder
                 "coffee" -> R.drawable.ic_launcher_foreground
                 "rose" -> R.drawable.ic_launcher_foreground
                 "streak_ice" -> android.R.drawable.ic_lock_idle_lock // Use sys icon for freeze
                 else -> R.drawable.ic_launcher_foreground
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Purple40.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                 // For now using text emoji as icons if real drawables missing
                val emoji = when(item.id) {
                    "candy" -> "🍬"
                    "coffee" -> "☕"
                    "rose" -> "🌹"
                    "chocolate" -> "🍫"
                    "streak_ice" -> "❄️"
                    "pro_pass_3d" -> "🎫"
                    "outfit_ticket" -> "👗"
                    else -> "📦"
                }
                Text(text = emoji, fontSize = 28.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                 maxLines = 1
            )
            
            Text(
                text = "x${item.amount}",
                color = Purple40,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
             Text(
                text = item.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2
            )
            
            // Interaction Hint
             Spacer(modifier = Modifier.height(8.dp))
             Text(
                text = if (item.id == "pro_pass_3d") "Tap to Use" else if (item.id in listOf("candy","rose","coffee", "chocolate")) "Gift in Chat" else "Tap for info",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
