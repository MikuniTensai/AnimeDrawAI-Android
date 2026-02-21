package com.doyouone.drawai.ui.components

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class Language(
    val code: String,
    val nativeName: String,
    val flag: String
)

val availableLanguages = listOf(
    Language("en", "English", "🇬🇧"),
    Language("in", "Bahasa Indonesia", "🇮🇩"),
    Language("es", "Español", "🇪🇸"),
    Language("pt", "Português", "🇵🇹"),
    Language("fr", "Français", "🇫🇷"),
    Language("de", "Deutsch", "🇩🇪"),
    Language("th", "ไทย", "🇹🇭"),
    Language("ar", "العربية", "🇸🇦"),
    Language("tr", "Türkçe", "🇹🇷"),
    Language("it", "Italiano", "🇮🇹"),
    Language("ru", "Русский", "🇷🇺"),
    Language("vi", "Tiếng Việt", "🇻🇳"),
    Language("zh", "中文", "🇨🇳"),
    Language("ja", "日本語", "🇯🇵"),
    Language("ko", "한국어", "🇰🇷"),
    Language("hi", "हिन्दी", "🇮🇳")
)

@Composable
fun LanguageSelectorDialog(
    onDismiss: () -> Unit,
    currentLanguage: String = "en"
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Select Language",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(availableLanguages) { language ->
                    LanguageItem(
                        language = language,
                        isSelected = currentLanguage == language.code,
                        onClick = {
                            setLocale(context, language.code)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.flag,
                    fontSize = 24.sp
                )
                Text(
                    text = language.nativeName,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

fun setLocale(context: Context, languageCode: String) {
    // Save language preference
    val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    prefs.edit().putString("selected_language", languageCode).apply()
    
    // Update locale with proper country codes for date formatting
    val locale = when (languageCode) {
        "en" -> Locale("en", "US")
        "in" -> Locale("id", "ID")  // Indonesian uses 'id' as language code
        "es" -> Locale("es", "ES")
        "pt" -> Locale("pt", "PT")
        "fr" -> Locale("fr", "FR")
        "de" -> Locale("de", "DE")
        "zh" -> Locale("zh", "CN")
        "ja" -> Locale("ja", "JP")
        "ko" -> Locale("ko", "KR")
        "hi" -> Locale("hi", "IN")
        "th" -> Locale("th", "TH")
        "ar" -> Locale("ar", "SA")
        "tr" -> Locale("tr", "TR")
        "it" -> Locale("it", "IT")
        "ru" -> Locale("ru", "RU")
        "vi" -> Locale("vi", "VN")
        else -> Locale("en", "US")
    }
    Locale.setDefault(locale)
    
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    @Suppress("DEPRECATION")
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
    
    // Recreate activity to apply changes
    if (context is Activity) {
        context.recreate()
    }
}

fun getCurrentLanguage(context: Context): String {
    val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    return prefs.getString("selected_language", "en") ?: "en"
}
