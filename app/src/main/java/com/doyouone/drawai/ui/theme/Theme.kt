package com.doyouone.drawai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurpleDark,
    secondary = Purple60,
    tertiary = AccentLavender,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = TextOnPurple,
    onSecondary = TextOnPurple,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = AccentPurple,
    secondary = Purple60,
    tertiary = AccentPurple,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = TextOnPurple,
    onSecondary = TextOnPurple,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun AnimeDrawAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled to use custom theme
    dynamicColor: Boolean = false,
    customPrimaryColor: androidx.compose.ui.graphics.Color? = null,
    content: @Composable () -> Unit
) {
    val baseColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val colorScheme = when {
        customPrimaryColor != null -> {
            baseColorScheme.copy(
                primary = customPrimaryColor,
                tertiary = customPrimaryColor,
                secondary = customPrimaryColor,
                primaryContainer = customPrimaryColor.copy(alpha = 0.1f),
                secondaryContainer = customPrimaryColor.copy(alpha = 0.1f),
                tertiaryContainer = customPrimaryColor.copy(alpha = 0.1f),
                // Ensure text on containers is correct (usually primary color for light containers)
                onPrimaryContainer = customPrimaryColor,
                onSecondaryContainer = customPrimaryColor,
                onTertiaryContainer = customPrimaryColor
            )
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> baseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}