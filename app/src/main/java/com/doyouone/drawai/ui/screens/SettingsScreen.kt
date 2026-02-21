package com.doyouone.drawai.ui.screens

import android.content.Intent
import android.net.Uri
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doyouone.drawai.ui.theme.*
import com.doyouone.drawai.R
import androidx.compose.ui.res.stringResource
import com.doyouone.drawai.ui.components.AnimeDrawMainTopBar
import com.doyouone.drawai.ui.components.SettingsItem
import com.doyouone.drawai.ui.components.SettingsSection
import com.doyouone.drawai.ui.components.SettingsToggleItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, coil.annotation.ExperimentalCoilApi::class)
@Composable
fun SettingsScreen(
    themePreferences: com.doyouone.drawai.data.preferences.ThemePreferences? = null,
    onOpenDrawer: () -> Unit = {},
    onNavigateToNews: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    viewModel: com.doyouone.drawai.viewmodel.GenerateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPreferences = remember { com.doyouone.drawai.data.preferences.AppPreferences(context) }
    val generationRepository = remember { com.doyouone.drawai.data.repository.FirebaseGenerationRepository() }
    val reminderManager = remember { com.doyouone.drawai.util.DailyReminderManager(context) }
    
    // State
    var generationLimit by remember { mutableStateOf<com.doyouone.drawai.data.model.GenerationLimit?>(null) }
    
    var isDarkMode by remember { mutableStateOf(false) }
    var isNotificationsEnabled by remember { mutableStateOf(true) }
    var reminderTimeHour by remember { mutableStateOf(8) }
    var reminderTimeMinute by remember { mutableStateOf(0) }
    
    var autoSaveEnabled by remember { mutableStateOf(true) }
    var galleryLockEnabled by remember { mutableStateOf(false) }
    var restrictedContentEnabled by remember { mutableStateOf(false) }
    var ageVerified by remember { mutableStateOf(false) }
    
    var cacheCleared by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    var showContentWarningDialog by remember { mutableStateOf(false) }
    var showAgeVerificationDialog by remember { mutableStateOf(false) }
    var showMathChallengeDialog by remember { mutableStateOf(false) }
    var showGalleryPinSetupDialog by remember { mutableStateOf(false) }
    var showMoreUpgradeDialog by remember { mutableStateOf(false) }
    var showMoreRequestDialog by remember { mutableStateOf(false) }

    
    // App Version
    val appVersionName = try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "1.0.34"
    } catch (e: Exception) {
        "1.0.34"
    }

    // Load data
    LaunchedEffect(Unit) {
        // Load user info
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val isAnonymous = user?.isAnonymous ?: true
        
        // Load preferences
        if (themePreferences != null) {
            launch { themePreferences.isDarkMode.collect { isDarkMode = it } }
        }
        launch { appPreferences.isNotificationsEnabled.collect { isNotificationsEnabled = it } }
        launch { appPreferences.isAutoSaveEnabled.collect { autoSaveEnabled = it } }
        launch { appPreferences.isGalleryLockEnabled.collect { galleryLockEnabled = it } }
        launch { appPreferences.isRestrictedContentEnabled.collect { restrictedContentEnabled = it } }

        launch { appPreferences.isAgeVerified.collect { ageVerified = it } }
        
        // Load generation limit (subscription status)
        if (user != null) {
            launch {
                generationRepository.getGenerationLimitFlow(user.uid).collect { limit ->
                    generationLimit = limit
                }
            }
        }
        
        launch { appPreferences.reminderHour.collect { reminderTimeHour = it } }
        launch { appPreferences.reminderMinute.collect { reminderTimeMinute = it } }
    }
    

    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        AnimeDrawMainTopBar(
            title = stringResource(R.string.settings_title),
            onOpenDrawer = onOpenDrawer
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.settings_subtitle),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
        )
        
        // Preferences Section
        SettingsSection(title = stringResource(R.string.settings_section_preferences)) {
            SettingsToggleItem(
                title = stringResource(R.string.settings_dark_mode),
                subtitle = stringResource(R.string.settings_dark_mode_desc),
                isChecked = isDarkMode,
                onToggle = { enabled ->
                    scope.launch {
                        themePreferences?.setDarkMode(enabled)
                    }
                }
            )
            
            SettingsToggleItem(
                title = "Daily Check-in Reminder", // TODO: Localize
                subtitle = if (isNotificationsEnabled) "Reminder set for %02d:%02d".format(reminderTimeHour, reminderTimeMinute) else "Enable daily reminders to keep streak",
                isChecked = isNotificationsEnabled,
                onToggle = { enabled ->
                    scope.launch {
                        appPreferences.setNotificationsEnabled(enabled)
                        if (enabled) {
                            reminderManager.scheduleReminder(reminderTimeHour, reminderTimeMinute)
                        } else {
                            reminderManager.cancelReminder()
                        }
                    }
                }
            )
            
            if (isNotificationsEnabled) {
                // Time Picker Trigger
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Show System TimePicker
                            android.app.TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    reminderTimeHour = hour
                                    reminderTimeMinute = minute
                                    // Save preferences -> In a real app save to DataStore
                                    // Here we just re-schedule
                                    scope.launch {
                                        appPreferences.setReminderTime(hour, minute)
                                        reminderManager.scheduleReminder(hour, minute)
                                        
                                        // Update state (though flow collection will likely do it too)
                                        reminderTimeHour = hour
                                        reminderTimeMinute = minute
                                    }
                                },
                                reminderTimeHour,
                                reminderTimeMinute,
                                true // 24h format
                            ).show()
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reminder Time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "%02d:%02d".format(reminderTimeHour, reminderTimeMinute),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            SettingsToggleItem(
                title = stringResource(R.string.settings_auto_save),
                subtitle = stringResource(R.string.settings_auto_save_desc),
                isChecked = autoSaveEnabled,
                onToggle = { enabled ->
                    scope.launch {
                        appPreferences.setAutoSaveEnabled(enabled)
                    }
                }
            )
            
            SettingsToggleItem(
                title = stringResource(R.string.settings_gallery_lock),
                subtitle = stringResource(R.string.settings_gallery_lock_desc),
                isChecked = galleryLockEnabled,
                onToggle = { enabled ->
                    if (enabled) {
                        showGalleryPinSetupDialog = true
                    } else {
                        scope.launch {
                            appPreferences.setGalleryLockEnabled(false)
                            appPreferences.setGalleryPin("")
                        }
                    }
                }
            )
            
            // Only show restricted content toggle for registered users (not guests)
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val isAnonymous = user?.isAnonymous ?: true
            if (!isAnonymous) {
                SettingsToggleItem(
                    title = stringResource(R.string.settings_restricted_content),
                    subtitle = stringResource(R.string.settings_restricted_content_desc),
                    isChecked = restrictedContentEnabled,
                    onToggle = { enabled ->
                        if (enabled && !ageVerified) {
                            showContentWarningDialog = true
                        } else if (enabled && ageVerified) {
                            scope.launch {
                                appPreferences.setRestrictedContentEnabled(true)
                                // Workflows will auto-reload via Flow observation
                            }
                        } else {
                            scope.launch {
                                appPreferences.setRestrictedContentEnabled(false)
                                // Workflows will auto-reload via Flow observation
                            }
                        }
                    }
                )
                
                // MORE Content Toggle (Only visible if Restricted Content is ON)
                if (restrictedContentEnabled) {
                    val isMoreEnabled = generationLimit?.moreEnabled == true
                    
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_more),
                        subtitle = stringResource(R.string.settings_more_desc),
                        isChecked = isMoreEnabled,
                        onToggle = { enabled ->
                            val userUid = user?.uid ?: return@SettingsToggleItem
                            
                            if (enabled) {
                                // User trying to enable
                                if (!isMoreEnabled) {
                                    // Check Subscription
                                    if (generationLimit?.subscriptionType == "free") {
                                        showMoreUpgradeDialog = true
                                    } else {
                                        // Check Request Status
                                        if (generationLimit?.moreRequestStatus == "pending") {
                                            android.widget.Toast.makeText(context, context.getString(R.string.settings_request_pending), android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            showMoreRequestDialog = true
                                        }
                                    }
                                }
                            } else {
                                // User trying to disable
                                if (isMoreEnabled) {
                                    // RESTRICTION: User cannot disable "More" once enabled
                                    android.widget.Toast.makeText(context, context.getString(R.string.settings_cannot_disable), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Rewards Section
        SettingsSection(title = "Rewards") {
            // Redeem Code Item
            var showRedeemDialog by remember { mutableStateOf(false) }
            SettingsItem(
                 title = "Redeem Promo Code", // TODO: Localize
                 subtitle = "Enter code to get rewards", // TODO: Localize
                 onClick = { showRedeemDialog = true }
            )

            if (showRedeemDialog) {
                 var codeInput by remember { mutableStateOf("") }
                 var isRedeeming by remember { mutableStateOf(false) }

                 AlertDialog(
                     onDismissRequest = { showRedeemDialog = false },
                     title = { Text("Redeem Code") },
                     text = {
                         Column {
                             Text("Enter your promo code below:")
                             Spacer(Modifier.height(8.dp))
                             OutlinedTextField(
                                 value = codeInput,
                                 onValueChange = { codeInput = it },
                                 singleLine = true,
                                 placeholder = { Text("Code") },
                                 modifier = Modifier.fillMaxWidth()
                             )
                         }
                     },
                     confirmButton = {
                         Button(
                             onClick = {
                                 if (codeInput.isNotBlank()) {
                                     isRedeeming = true
                                     viewModel.redeemCode(codeInput) { success, msg ->
                                         isRedeeming = false
                                         if (success) {
                                             android.widget.Toast.makeText(context, msg ?: "Success!", android.widget.Toast.LENGTH_LONG).show()
                                             showRedeemDialog = false
                                         } else {
                                             android.widget.Toast.makeText(context, msg ?: "Failed to redeem", android.widget.Toast.LENGTH_SHORT).show()
                                         }
                                     }
                                 }
                             },
                             enabled = !isRedeeming
                         ) {
                             if (isRedeeming) {
                                 CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                             } else {
                                 Text("Redeem")
                             }
                         }
                     },
                     dismissButton = {
                         TextButton(onClick = { showRedeemDialog = false }) {
                             Text("Cancel")
                         }
                     }
                 )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Storage Section
        SettingsSection(title = stringResource(R.string.settings_section_storage)) {
            SettingsItem(
                title = stringResource(R.string.settings_clear_cache),
                subtitle = if (cacheCleared) stringResource(R.string.settings_cache_cleared) else stringResource(R.string.settings_clear_cache_desc),
                onClick = { showClearCacheDialog = true }
            )
            
            // Check if user has premium access (basic/pro plan or premium status)
            val hasPremiumAccess = generationLimit?.let { limit ->
                limit.isPremium || limit.subscriptionType == "basic" || limit.subscriptionType == "pro"
            } ?: false
            
            SettingsItem(
                title = if (hasPremiumAccess) stringResource(R.string.settings_export_gallery) else stringResource(R.string.settings_export_gallery_locked),
                subtitle = if (hasPremiumAccess) stringResource(R.string.settings_export_gallery_desc) else stringResource(R.string.settings_export_gallery_premium),
                onClick = {
                    if (hasPremiumAccess) {
                        showExportDialog = true
                    } else {
                        // Redirect to subscription screen
                        onNavigateToSubscription()
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // About & Legal Section
        SettingsSection(title = stringResource(R.string.settings_section_about)) {
            SettingsItem(
                title = stringResource(R.string.settings_privacy_policy),
                subtitle = stringResource(R.string.settings_privacy_policy_desc),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/document/d/1QE2DRsvw2p0_bpwM8gbGGN2ySVYo92UOIj7vSUXJUhk/edit?usp=sharing"))
                    context.startActivity(intent)
                }
            )
            
            SettingsItem(
                title = stringResource(R.string.settings_terms_service),
                subtitle = stringResource(R.string.settings_terms_service_desc),
                onClick = {
                    // TODO: Replace with your actual Google Docs Terms of Service URL
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/document/d/YOUR_TERMS_DOCUMENT_ID/edit?usp=sharing"))
                    context.startActivity(intent)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Support Section
        SettingsSection(title = stringResource(R.string.settings_section_support)) {
            SettingsItem(
                title = stringResource(R.string.settings_help_support),
                subtitle = stringResource(R.string.settings_help_support_desc),
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:nitedreamworks@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Draw AI - Support Request")
                        putExtra(Intent.EXTRA_TEXT, "\n\n---\nApp Version: $appVersionName\nDevice: ${android.os.Build.MODEL}\nAndroid: ${android.os.Build.VERSION.RELEASE}")
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Send Email"))
                    } catch (e: Exception) {
                        // Handle no email app
                    }
                }
            )
            
            SettingsItem(
                title = stringResource(R.string.settings_rate_app),
                subtitle = stringResource(R.string.settings_rate_app_desc),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.doyouone.drawai"))
                    context.startActivity(intent)
                }
            )
            
            SettingsItem(
                title = stringResource(R.string.settings_app_version),
                subtitle = "$appVersionName",
                onClick = { onNavigateToNews() }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        

        
        Text(
            text = "Draw AI v$appVersionName",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        // Bottom spacing for navigation
        Spacer(modifier = Modifier.height(100.dp))
    }
    
    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.settings_clear_cache_title)) },
            text = { Text(stringResource(R.string.settings_clear_cache_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                // Clear Coil image cache
                                coil.ImageLoader(context).memoryCache?.clear()
                                coil.ImageLoader(context).diskCache?.clear()
                                
                                // Clear app cache
                                context.cacheDir.deleteRecursively()
                                
                                cacheCleared = true
                                showClearCacheDialog = false
                                
                                // Reset after 3 seconds
                                kotlinx.coroutines.delay(3000)
                                cacheCleared = false
                            } catch (e: Exception) {
                                android.util.Log.e("SettingsScreen", "Error clearing cache: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_btn_clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.settings_btn_cancel))
                }
            }
        )
    }
    
    // Export Gallery Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.settings_export_dialog_title)) },
            text = { 
                Column {
                    Text(stringResource(R.string.settings_export_warning_1))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_export_warning_2))
                    Text(stringResource(R.string.settings_export_warning_3))
                    Text(stringResource(R.string.settings_export_warning_4))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_export_confirm), fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        scope.launch {
                            exportAllToGallery(context)
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_btn_export_all), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.settings_btn_cancel))
                }
            }
        )
    }
    
    // Delete Account Dialog

    
    // Age Verification Dialogs
    if (showContentWarningDialog) {
        com.doyouone.drawai.ui.components.ContentWarningDialog(
            onAgeVerification = {
                showContentWarningDialog = false
                showAgeVerificationDialog = true
            },
            onProceed = {
                showContentWarningDialog = false
                scope.launch {
                    appPreferences.setRestrictedContentEnabled(true)
                    // Workflows will auto-reload via Flow observation
                }
            },
            onDismiss = {
                showContentWarningDialog = false
            }
        )
    }
    
    if (showAgeVerificationDialog) {
        com.doyouone.drawai.ui.components.AgeVerificationDialog(
            onVerified = { isVerified ->
                showAgeVerificationDialog = false
                if (isVerified) {
                    scope.launch {
                        appPreferences.setAgeVerified(true)
                        appPreferences.setRestrictedContentEnabled(true)
                        // Workflows will auto-reload via Flow observation
                    }
                } else {
                    showMathChallengeDialog = true
                }
            },
            onDismiss = {
                showAgeVerificationDialog = false
            }
        )
    }
    
    if (showMathChallengeDialog) {
        com.doyouone.drawai.ui.components.MathChallengeDialog(
            onVerified = { isVerified ->
                showMathChallengeDialog = false
                if (isVerified) {
                    scope.launch {
                        appPreferences.setAgeVerified(true)
                        appPreferences.setRestrictedContentEnabled(true)
                        // Workflows will auto-reload via Flow observation
                    }
                }
                // If failed, just close the dialog
            },
            onDismiss = {
                showMathChallengeDialog = false
            }
        )
    }
    
    // PIN Setup Dialog
    if (showGalleryPinSetupDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { 
                showGalleryPinSetupDialog = false
                pinInput = ""
                confirmPinInput = ""
                showError = false
            },
            title = { Text(stringResource(R.string.settings_pin_setup_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_pin_setup_msg))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { 
                            pinInput = it.take(4)
                            showError = false
                        },
                        label = { Text(stringResource(R.string.settings_pin_enter)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { 
                            confirmPinInput = it.take(4)
                            showError = false
                        },
                        label = { Text(stringResource(R.string.settings_pin_confirm)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    
                    if (showError) {
                        Text(
                            text = stringResource(R.string.settings_pin_error),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput.length == 4 && pinInput == confirmPinInput) {
                            scope.launch {
                                appPreferences.setGalleryPin(pinInput)
                                appPreferences.setGalleryLockEnabled(true)
                                showGalleryPinSetupDialog = false
                                pinInput = ""
                                confirmPinInput = ""
                                showError = false
                            }
                        } else {
                            showError = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_btn_set_pin))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showGalleryPinSetupDialog = false
                        pinInput = ""
                        confirmPinInput = ""
                        showError = false
                    }
                ) {
                    Text(stringResource(R.string.settings_btn_cancel))
                }
            }
        )
    }

    
    // More Content Feature Dialogs
    if (showMoreUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showMoreUpgradeDialog = false },
            title = { Text(stringResource(R.string.settings_upgrade_required_title)) },
            text = { Text(stringResource(R.string.settings_upgrade_required_msg)) },
            confirmButton = {
                TextButton(onClick = { showMoreUpgradeDialog = false }) {
                    Text(stringResource(R.string.settings_btn_ok))
                }
            }
        )
    }

    if (showMoreRequestDialog) {
        AlertDialog(
            onDismissRequest = { showMoreRequestDialog = false },
            title = { Text(stringResource(R.string.settings_request_access_title)) },
            text = { Text(stringResource(R.string.settings_request_access_msg)) },
            confirmButton = {
                TextButton(onClick = { 
                    scope.launch {
                        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        val userUid = user?.uid
                        if (userUid != null) {
                            generationRepository.requestMoreAccess(userUid)
                            android.widget.Toast.makeText(context, context.getString(R.string.settings_request_sent), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    showMoreRequestDialog = false
                }) {
                    Text(stringResource(R.string.settings_btn_send_request))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoreRequestDialog = false }) {
                    Text(stringResource(R.string.settings_btn_cancel))
                }
            }
        )
    }


}

/**
 * Export all generated images to main gallery with deletion warning
 */


suspend fun exportAllToGallery(context: android.content.Context) {
    try {
        // Get all generated images
        val imageStorage = com.doyouone.drawai.data.local.ImageStorage(context)
        val allImages = imageStorage.getAllImages()
        
        if (allImages.isEmpty()) {
            android.widget.Toast.makeText(context, context.getString(R.string.settings_export_empty), android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        var exportedCount = 0
        var failedCount = 0
        
        // Export each image to main gallery
        for (image in allImages) {
            try {
                val imageFile = java.io.File(image.imageUrl)
                if (imageFile.exists()) {
                    // Load bitmap from file
                    val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
                    if (bitmap != null) {
                        // Save to main gallery using MediaStore
                        val values = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "${image.workflow}_${image.id}.jpg")
                            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            put(android.provider.MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                            put(android.provider.MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                            
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DrawAI")
                                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                            }
                        }
                        
                        val uri = context.contentResolver.insert(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            values
                        )
                        
                        if (uri != null) {
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                            }
                            
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                values.clear()
                                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                                context.contentResolver.update(uri, values, null, null)
                            }
                            
                            // Delete original file after successful export
                            imageFile.delete()
                            exportedCount++
                        } else {
                            failedCount++
                        }
                    } else {
                        failedCount++
                    }
                } else {
                    failedCount++
                }
            } catch (e: Exception) {
                android.util.Log.e("ExportGallery", "Failed to export image: ${image.id}", e)
                failedCount++
            }
        }
        
        // Clear metadata after export
        imageStorage.clearAll()
        
        // Show result
        val message = if (failedCount > 0) {
            context.getString(R.string.settings_export_partial, exportedCount, failedCount)
        } else {
            context.getString(R.string.settings_export_success, exportedCount)
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        
    } catch (e: Exception) {
        android.util.Log.e("ExportGallery", "Export failed", e)
        android.widget.Toast.makeText(context, context.getString(R.string.settings_export_failed, e.message), android.widget.Toast.LENGTH_LONG).show()
    }
}



