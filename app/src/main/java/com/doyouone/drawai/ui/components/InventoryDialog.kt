package com.doyouone.drawai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.doyouone.drawai.data.model.InventoryItem
import com.doyouone.drawai.ui.theme.Purple40
import com.doyouone.drawai.ui.theme.Purple80

@Composable
fun InventoryDialog(
    inventory: List<InventoryItem>,
    onItemClick: (InventoryItem) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E) // Deep Dark Blue/Purple
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .border(2.dp, Brush.verticalGradient(listOf(Purple80, Purple40)), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Bag",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (inventory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Your bag is empty.\nLogin daily to earn gifts! 🎁",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 75.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f, fill = false) // Limit height
                    ) {
                        items(inventory) { item ->
                            InventoryItemCard(item, onItemClick)
                        }
                    }
                }
            }
        }
    }
}

// Helper to get emoji based on item ID
fun getEmojiForItem(itemId: String): String {
    return when (itemId.lowercase()) {
        "candy" -> "🍬"
        "coffee" -> "☕"
        "rose" -> "🌹"
        "chocolate" -> "🍫"
        "ring" -> "💍"
        "streak_ice" -> "❄️"
        "pro_pass_3d" -> "🎫"
        else -> "🎁"
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onClick: (InventoryItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth() // Adapt to grid cell width
            .background(Color(0xFF2D2D44), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF4A4A6A), RoundedCornerShape(12.dp))
            .clickable { onClick(item) }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Purple40.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getEmojiForItem(item.id),
                fontSize = 24.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = item.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        
        Text(
            text = "x${item.amount}",
            color = Color.Gray,
            fontSize = 10.sp
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
             Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF4081), modifier = Modifier.size(10.dp))
             Spacer(modifier = Modifier.width(2.dp))
             Text(
                text = "+${item.affectionValue}",
                color = Color(0xFFFF4081), // Pink
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
