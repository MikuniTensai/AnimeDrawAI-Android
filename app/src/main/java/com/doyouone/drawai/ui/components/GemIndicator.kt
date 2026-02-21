package com.doyouone.drawai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GemIndicator(
    gemCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // Gem Color: Purple/Pinkish to look premium
    val gemColor = Color(0xFFE91E63) // Pink 500
    // Use Material 3 colorScheme
    val backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)

    Row(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Diamond, // Ensure using standard icon
            contentDescription = "Gems",
            tint = gemColor,
            modifier = Modifier.size(18.dp)
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        Text(
            text = gemCount.toString(),
            // Use Material 3 typography
            style = MaterialTheme.typography.labelLarge, 
            fontWeight = FontWeight.Bold,
            // Use Material 3 onSurface
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        )
    }
}
