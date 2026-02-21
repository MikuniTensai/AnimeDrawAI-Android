package com.doyouone.drawai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doyouone.drawai.ui.navigation.BottomNavItem
import com.doyouone.drawai.ui.navigation.Screen
import com.doyouone.drawai.ui.theme.AccentPurple
import com.doyouone.drawai.ui.theme.Purple40
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.ui.res.stringResource
import com.doyouone.drawai.R
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Language
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit,
    userEmail: String = "Guest",
    userDisplayName: String = "Guest",
    generationLimit: com.doyouone.drawai.data.model.GenerationLimit? = null,
    characterCount: Int = 0,
    isPremium: Boolean = false,
    onNavigateToSubscription: () -> Unit = {},
    onColorSelected: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    val context = LocalContext.current
    
    // Animation for Pro users
    val infiniteTransition = rememberInfiniteTransition(label = "pro_gradient")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
        // Drawer Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp) // Reduced height
                .background(
                    if (isPremium) {
                        // Pro: Animated Gradient
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary, // Or use a shade of primary
                                MaterialTheme.colorScheme.primary
                            ),
                            start = androidx.compose.ui.geometry.Offset(animatedOffset, 0f),
                            end = androidx.compose.ui.geometry.Offset(animatedOffset + 500f, 500f),
                            tileMode = androidx.compose.ui.graphics.TileMode.Mirror
                        )
                    } else if (generationLimit?.subscriptionType == "basic") {
                        // Basic: Static Gradient
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    } else {
                        // Free: Darker/Different Gradient
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha=0.8f), // Darker Purple
                                MaterialTheme.colorScheme.primary.copy(alpha=0.6f)  // Deep Purple
                            )
                        )
                    }
                )
                .statusBarsPadding()
                .padding(12.dp), // Reduced padding
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                // User Info
                // Profile Header
                Text(
                    text = if (userDisplayName.isNotBlank() && userDisplayName != "Guest") userDisplayName else stringResource(R.string.drawer_guest),
                    fontSize = 18.sp, // Reduced font size
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))

                // User Info
                Text(
                    text = userEmail,
                    fontSize = 12.sp, // Fixed smaller font size
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Limit and Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Limit
                    generationLimit?.let { limit ->
                        val remaining = limit.getRemainingGenerations()
                        val max = if (limit.subscriptionType == "free") {
                            limit.maxDailyLimit 
                        } else {
                            // Fallback if subscriptionLimit is 0 (migration issue)
                            if (limit.subscriptionLimit > 0) limit.subscriptionLimit 
                            else when(limit.subscriptionType) {
                                "basic" -> 200
                                "pro" -> 600
                                else -> limit.maxDailyLimit // Fallback to daily limit
                            }
                        }
                        
                        Column {
                            Text(
                                text = stringResource(R.string.subscription_limit_format, remaining, max),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            
                            // Expired Date - Use app's locale, not system locale
                            if (limit.subscriptionType != "free" && limit.subscriptionEndDate != null) {
                                val locale = context.resources.configuration.locales[0]
                                val dateFormat = SimpleDateFormat("dd MMM yyyy", locale)
                                val dateStr = dateFormat.format(limit.subscriptionEndDate.toDate())
                                Text(
                                    text = stringResource(R.string.subscription_exp_format, dateStr),
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            
                            // Chat Limit
                            Text(
                                text = stringResource(R.string.subscription_chat_format, "$characterCount/${limit.maxChatLimit}"),
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    // Status Badge
                    val statusText = when {
                        isPremium -> stringResource(R.string.subscription_status_pro)
                        generationLimit?.subscriptionType == "basic" -> stringResource(R.string.subscription_status_basic)
                        else -> stringResource(R.string.subscription_status_free)
                    }
                    
                    val statusColor = when {
                        isPremium -> Color(0xFFFFD700) // Gold
                        generationLimit?.subscriptionType == "basic" -> Color(0xFFE0E0E0) // Silver
                        else -> Color.White.copy(alpha = 0.7f)
                    }
                    
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .align(Alignment.Top)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer
        
        // Watch Ad Button for Free Users
        if (!isPremium && generationLimit?.subscriptionType == "free") {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val authManager = remember { com.doyouone.drawai.auth.AuthManager(context) }
            val userId = authManager.getCurrentUserId() ?: ""
            val subscriptionRepository = remember(userId) { com.doyouone.drawai.data.repository.SubscriptionRepository(userId) }
            val activity = context as? android.app.Activity
            
            Button(
                onClick = { 
                    if (activity != null) {
                        com.doyouone.drawai.ads.AdManager.showRewardedAd(
                            activity = activity,
                            onUserEarnedReward = { _ ->
                                scope.launch {
                                    if (userId.isNotEmpty()) {
                                        subscriptionRepository.addBonusGeneration(1)
                                        Toast.makeText(context, "+1 Generation Added!", Toast.LENGTH_SHORT).show()
                                        onClose()
                                    }
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Watch Ad (+1 Gen)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        
        // Drawer Items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp) // Reduced padding
        ) {
            
            DrawerItem(
                icon = Icons.Default.Home,
                label = stringResource(R.string.menu_home),
                isSelected = currentRoute == BottomNavItem.Home.route,
                onClick = { 
                    onNavigate(BottomNavItem.Home.route)
                    onClose()
                }
            )
            
            // Collapsible More Features Section
            var isMoreFeaturesExpanded by remember { mutableStateOf(false) }

            NavigationDrawerItem(
                icon = { 
                    Icon(
                        if (isMoreFeaturesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                        contentDescription = null
                    ) 
                },
                label = { Text("More Features", fontSize = 14.sp) },
                selected = false,
                onClick = { isMoreFeaturesExpanded = !isMoreFeaturesExpanded },
                modifier = Modifier.padding(vertical = 2.dp),
                shape = RoundedCornerShape(12.dp)
            )

            if (isMoreFeaturesExpanded) {
                 Column(modifier = Modifier.padding(start = 16.dp)) {
                    DrawerItem(
                        icon = Icons.Default.Inventory, // Inventory Icon
                        label = "Inventory", // TODO: Localize
                        isSelected = currentRoute == Screen.Inventory.route,
                        onClick = { 
                            onNavigate(Screen.Inventory.route)
                            onClose()
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.Info, // Vision (safe icon)
                        label = stringResource(R.string.menu_vision),
                        isSelected = currentRoute == Screen.Vision.route,
                        onClick = { 
                            onNavigate(Screen.Vision.route)
                            onClose()
                        }
                    )
                    
                    DrawerItem(
                        icon = Icons.Default.Search, // Explore (safe icon)
                        label = stringResource(R.string.menu_explore),
                        isSelected = currentRoute == BottomNavItem.Explore.route,
                        onClick = { 
                            onNavigate(BottomNavItem.Explore.route)
                            onClose()
                        }
                    )
                    
                    DrawerItem(
                        icon = Icons.Default.Star, // Chat
                        label = stringResource(R.string.menu_chat),
                        isSelected = currentRoute == BottomNavItem.Chat.route,
                        onClick = { 
                            if (userEmail == "Guest" || userEmail.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.drawer_login_to_chat), Toast.LENGTH_SHORT).show()
                            } else {
                                onNavigate(BottomNavItem.Chat.route)
                                onClose()
                            }
                        }
                    )
                    
                    DrawerItem(
                        icon = Icons.Default.PhotoLibrary, // Gallery
                        label = stringResource(R.string.menu_gallery),
                        isSelected = currentRoute == Screen.Gallery.route,
                        onClick = { 
                            onNavigate(Screen.Gallery.route)
                            onClose()
                        }
                    )
                    
                    DrawerItem(
                        icon = Icons.Default.Favorite,
                        label = stringResource(R.string.menu_favorites),
                        isSelected = currentRoute == Screen.Favorites.route,
                        onClick = { 
                            onNavigate(Screen.Favorites.route)
                            onClose()
                        }
                    )
                 }
            }
            
            // Subscription Item (Special Styling)
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color.White) },
                label = { Text(stringResource(R.string.menu_subscription), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }, // Reduced font
                selected = false,
                onClick = { 
                    onNavigateToSubscription()
                    onClose()
                },
                modifier = Modifier
                    .padding(vertical = 2.dp) // Reduced padding
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .height(48.dp), // Explicit slightly smaller height
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = Color.White,
                    unselectedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            // Language Selector
            var showLanguageDialog by remember { mutableStateOf(false) }
            val currentLanguage = getCurrentLanguage(context)
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Language, contentDescription = null) },
                label = { 
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.drawer_current_language), fontSize = 14.sp)
                    }
                },
                selected = false,
                onClick = { showLanguageDialog = true },
                modifier = Modifier.padding(vertical = 2.dp), // Reduced padding
                shape = RoundedCornerShape(12.dp)
            )
            
            if (showLanguageDialog) {
                LanguageSelectorDialog(
                    onDismiss = { showLanguageDialog = false },
                    currentLanguage = currentLanguage
                )
            }
            
            // Theme Color Picker (5x1 Grid)
            Spacer(modifier = Modifier.height(4.dp)) // Reduced spacer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp) // Reduced height
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val colors = listOf(
                    com.doyouone.drawai.ui.theme.AccentPurple, // Base Purple
                    com.doyouone.drawai.ui.theme.ThemePink,    // Pink
                    com.doyouone.drawai.ui.theme.ThemeIndigo,  // Indigo/Deep Blue
                    com.doyouone.drawai.ui.theme.ThemeBlue,    // Blue
                    com.doyouone.drawai.ui.theme.ThemeCyan,    // Cyan
                    com.doyouone.drawai.ui.theme.ThemeTeal,    // Teal
                    com.doyouone.drawai.ui.theme.ThemeSunset   // Sunset
                )
                
                colors.forEach { color ->
                     Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .clickable { onColorSelected(color) }
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // Reduced padding
            
            
            DrawerItem(
                icon = Icons.Default.EmojiEvents,
                label = stringResource(R.string.menu_leaderboard),
                isSelected = currentRoute == Screen.Leaderboard.route,
                onClick = { 
                    onNavigate(Screen.Leaderboard.route)
                    onClose()
                }
            )

            DrawerItem(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.menu_settings),
                isSelected = currentRoute == Screen.Settings.route,
                onClick = { 
                    onNavigate(Screen.Settings.route)
                    onClose()
                }
            )
            
            DrawerItem(
                icon = Icons.Default.Info, // Or Notifications
                label = stringResource(R.string.menu_events_updates),
                isSelected = currentRoute == Screen.News.route,
                onClick = { 
                    onNavigate(Screen.News.route)
                    onClose()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Events Promo Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .clickable { 
                        onNavigate(Screen.News.route)
                        onClose()
                    }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.drawer_promo_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = stringResource(R.string.drawer_promo_desc),
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label, fontSize = 14.sp) }, // Reduced font size
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 2.dp), // Reduced padding
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
