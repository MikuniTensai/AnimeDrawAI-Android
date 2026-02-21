package com.doyouone.drawai.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.doyouone.drawai.R
import com.doyouone.drawai.data.model.ShopItem
import com.doyouone.drawai.data.repository.DrawAIRepository
import com.doyouone.drawai.ui.theme.Purple40
import com.doyouone.drawai.ui.theme.Purple80
import kotlinx.coroutines.launch

@Composable
fun ShopDialog(
    onDismiss: () -> Unit,
    onPurchaseBooster: ( (Boolean, String?) -> Unit ) -> Unit = {}, // Callback with result callback
    repository: DrawAIRepository = DrawAIRepository() // Injected
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var shopItems by remember { mutableStateOf<List<ShopItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isPurchasing by remember { mutableStateOf(false) }

    // Initial Load
    LaunchedEffect(Unit) {
        isLoading = true
        val result = repository.getShopItems()
        if (result.isSuccess) {
            val apiItems = result.getOrNull() ?: emptyList()
            
            // Inject Special Items
            val boosterItem = ShopItem(
                id = "daily_booster",
                name = "Daily Limit Booster",
                description = "+5 Daily Generations (Expires at midnight)",
                costGems = 50,
                costUsd = null,
                imageUrl = null,
                type = "item",
                amount = 5,
                itemId = "daily_gen_boost",
                durationDays = null
            )
            
            // Add to top of list
            shopItems = listOf(boosterItem) + apiItems
        } else {
            error = result.exceptionOrNull()?.message
        }
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E) // Deep Dark Blue/Purple
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = Color(0xFFE91E63), // Same pink color as GemIndicator
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.gem_store_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Purple40)
                    }
                } else if (error != null) {
                     Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Error loading shop", color = Color.Red)
                            Text(text = error!!, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f, fill = false) 
                    ) {
                        items(shopItems) { item ->
                            ShopItemCard(
                                item = item,
                                isPurchasing = isPurchasing,
                                onBuy = {
                                    if (item.id == "daily_booster") {
                                        isPurchasing = true
                                        onPurchaseBooster { success, msg ->
                                            isPurchasing = false
                                            if (success) {
                                                android.widget.Toast.makeText(context, "Booster Activated! +5 Generations", android.widget.Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            } else {
                                                android.widget.Toast.makeText(context, "Failed: ${msg ?: "Unknown error"}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        scope.launch {
                                            isPurchasing = true
                                            val buyResult = repository.purchaseShopItem(item.id)
                                            isPurchasing = false
                                            
                                            if (buyResult.isSuccess) {
                                                val resp = buyResult.getOrNull()
                                                if (resp?.success == true) {
                                                    // We can't access stringResource in a suspend function directly easily unless passing Context
                                                    // Or we can use the context we captured earlier
                                                    android.widget.Toast.makeText(context, resp.message ?: "Purchase successful!", android.widget.Toast.LENGTH_SHORT).show()
                                                    onDismiss() // Close shop on success
                                                } else {
                                                    android.widget.Toast.makeText(context, "Failed: ${resp?.error}", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                 android.widget.Toast.makeText(context, "Error: ${buyResult.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
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

@Composable
fun ShopItemCard(
    item: ShopItem,
    isPurchasing: Boolean,
    onBuy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Item Icon - Show emoji for items
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                 if (item.imageUrl != null) {
                     Image(
                         painter = rememberAsyncImagePainter(item.imageUrl),
                         contentDescription = null,
                         modifier = Modifier.fillMaxSize()
                     )
                 } else {
                     // Show emoji based on item ID
                     val emoji = when (item.id) {
                         "daily_booster" -> "🚀"
                         "candy" -> "🍬"
                         "coffee" -> "☕"
                         "rose" -> "🌹"
                         "chocolate" -> "🍫"
                         "streak_ice" -> "❄"
                         "pro_pass_3d" -> "🎫"
                         "outfit_ticket" -> "👗"
                         else -> "🎁"
                     }
                     Text(
                         text = emoji,
                         fontSize = 36.sp,
                         textAlign = TextAlign.Center
                     )
                 }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val localizedName = when(item.id) {
                "candy" -> stringResource(R.string.shop_item_candy)
                "coffee" -> stringResource(R.string.shop_item_coffee)
                "rose" -> stringResource(R.string.shop_item_rose)
                else -> item.name
            }

            Text(
                text = localizedName,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1
            )
            
            Text(
                text = item.description,
                color = Color.Gray,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                modifier = Modifier.height(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onBuy,
                enabled = !isPurchasing,
                colors = ButtonDefaults.buttonColors(containerColor = Purple40),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                if (item.costGems != null) {
                    // Show gem cost with icon
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${item.costGems}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else if (item.costUsd != null) {
                    Text(text = "$${item.costUsd}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(text = stringResource(R.string.shop_buy_button), fontSize = 12.sp)
                }
            }
        }
    }
}
