package com.doyouone.drawai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.doyouone.drawai.R
import com.doyouone.drawai.ui.theme.Purple40
import com.doyouone.drawai.ui.theme.Purple80

// Hardcoded for UI visualization, should ideally match Server Cycle A
data class DailyRewardUi(
    val day: Int,
    val label: String,
    val icon: ImageVector,
    val isBig: Boolean = false
)

// Week A: Resource Focus (Gold/Gems)
private val REWARDS_CYCLE_A = listOf(
    DailyRewardUi(1, "Candy", Icons.Default.Star),
    DailyRewardUi(2, "100 Gems", Icons.Default.Diamond),
    DailyRewardUi(3, "Coffee", Icons.Default.Star),
    DailyRewardUi(4, "200 Gems", Icons.Default.Diamond),
    DailyRewardUi(5, "Rose", Icons.Default.Star),
    DailyRewardUi(6, "500 Gems", Icons.Default.Diamond),
    DailyRewardUi(7, "Pro 3D", Icons.Default.Star, true)
)

// Week B: Item Focus (Fragments/Tickets)
private val REWARDS_CYCLE_B = listOf(
    DailyRewardUi(1, "Coffee", Icons.Default.Star),
    DailyRewardUi(2, "100 Gems", Icons.Default.Diamond),
    DailyRewardUi(3, "Chocolate", Icons.Default.Star),
    DailyRewardUi(4, "200 Gems", Icons.Default.Diamond),
    DailyRewardUi(5, "Outfit Tkt", Icons.Default.Star),
    DailyRewardUi(6, "500 Gems", Icons.Default.Diamond),
    DailyRewardUi(7, "Streak Ice", Icons.Default.Lock, true) // Placeholder icon
)

@Composable
fun DailyLoginDialog(
    currentStreak: Int,
    isClaimable: Boolean,
    nextDayIndex: Int, // The day user is ABOUT to claim (1-7)
    rewardCycle: String = "A", // "A" (Resource) or "B" (Items)
    streakSaved: Boolean = false,
    onClaim: () -> Unit,
    onDismiss: () -> Unit
) {
    // Select rewards based on cycle
    val currentRewards = remember(rewardCycle) {
        if (rewardCycle == "B") REWARDS_CYCLE_B else REWARDS_CYCLE_A
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E) // Deep Dark Blue/Purple
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, Brush.verticalGradient(listOf(Purple80, Purple40)), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = stringResource(R.string.daily_reward_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Streak Badge
                Surface(
                    color = Purple40.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Purple40)
                ) {
                    Text(
                        text = "🔥 Streak: $currentStreak Days", // TODO: Localize "Streak" later
                        color = Purple80,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                if (streakSaved) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "❄️ Streak Saved by Freeze!",
                        color = Color(0xFF00E5FF), // Cyan/Ice color
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Rewards Grid (Custom Spans)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4), // Fixed 4 columns
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp) // Allow scrolling with limit
                ) {
                    items(currentRewards, span = { item ->
                        val spanCount = if (item.day == 7) 2 else 1 // Day 7 spans 2 columns
                        GridItemSpan(spanCount)
                    }) { rewardItem ->
                        DayItem(
                            day = rewardItem.day,
                            reward = rewardItem,
                            currentDayTarget = nextDayIndex
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Claim Button
                Button(
                    onClick = onClaim,
                    enabled = isClaimable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple40,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isClaimable) {
                        Text(stringResource(R.string.daily_reward_claim), fontWeight = FontWeight.Bold)
                    } else {
                        // Using 'Claimed' string as fallback for 'Come Back Tomorrow'
                        Text(stringResource(R.string.daily_reward_claimed), fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun DayItem(
    day: Int,
    reward: DailyRewardUi,
    currentDayTarget: Int,
    modifier: Modifier = Modifier
) {
    // Determine state
    // "Past": day < currentDayTarget
    // "Today": day == currentDayTarget
    // "Future": day > currentDayTarget
    
    val isToday = day == currentDayTarget
    val isPast = day < currentDayTarget
    val isFuture = day > currentDayTarget
    val isBigReward = reward.isBig

    val backgroundColor = when {
        isToday -> Purple40
        isPast -> Color(0xFF2D2D44) // Darker grey-purple
        else -> Color.Transparent // Outline only
    }
    
    val borderColor = when {
        isToday -> Purple80
        isPast -> Color.Transparent
        else -> Color(0xFF4A4A6A)
    }

    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Day Label
        Text(
            text = stringResource(R.string.daily_reward_day, day),
            color = if (isToday) Color.White else Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Icon
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if(isPast) Icons.Default.Check else if(isFuture && !isBigReward) Icons.Default.Lock else reward.icon,
                contentDescription = null,
                tint = if (isToday) Color.White else if (isBigReward && isFuture) Purple80 else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Reward Label Localized
        val rewardLabel = when {
            reward.label.contains("Gems") -> {
                val amount = reward.label.filter { it.isDigit() }.toIntOrNull() ?: 0
                stringResource(R.string.daily_reward_gems, amount)
            }
            reward.label == "Candy" -> stringResource(R.string.shop_item_candy)
            reward.label == "Coffee" -> stringResource(R.string.shop_item_coffee)
            reward.label == "Rose" -> stringResource(R.string.shop_item_rose)
            else -> reward.label
        }

        Text(
            text = rewardLabel,
            color = if (isToday) Color.White else Color.Gray,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            lineHeight = 10.sp
        )
    }
}
