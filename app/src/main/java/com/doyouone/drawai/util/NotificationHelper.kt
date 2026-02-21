package com.doyouone.drawai.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.doyouone.drawai.MainActivity
import com.doyouone.drawai.R
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

/**
 * NotificationHelper - Manages app notifications
 */
class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "anime_draw_ai_channel"
        private const val CHANNEL_NAME = "Draw AI"
        private const val CHANNEL_DESCRIPTION = "Notifications for image generation"
        private const val NOTIFICATION_ID_GENERATION = 1001
        private const val NOTIFICATION_ID_DOWNLOAD = 1002
        private const val NOTIFICATION_ID_DAILY_REMINDER = 1003
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show notification when generation completes
     */
    fun showGenerationCompleteNotification(
        imageUrl: String? = null,
        success: Boolean = true
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_gallery", true)
            imageUrl?.let { putExtra("image_url", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = if (success) "✨ Generation Complete!" else "❌ Generation Failed"
        val text = if (success) 
            "Your anime artwork is ready! Tap to view." 
        else 
            "Something went wrong. Please try again."
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Replace with app icon
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
        
        if (success) {
            builder.setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your anime artwork has been generated successfully! Tap to view your creation in the gallery."))
        }
        
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID_GENERATION, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted, silently fail
            android.util.Log.w("NotificationHelper", "Notification permission not granted")
        }
    }
    
    /**
     * Show notification when download completes
     */
    fun showDownloadCompleteNotification(fileName: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("💾 Download Complete")
            .setContentText("$fileName saved to gallery")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
        
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID_DOWNLOAD, builder.build())
            }
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "Notification permission not granted")
        }
    }
    
    /**
     * Show progress notification for generation
     */
    fun showGenerationProgressNotification(progress: Int) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🎨 Generating...")
            .setContentText("Creating your anime artwork")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
        
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID_GENERATION, builder.build())
            }
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "Notification permission not granted")
        }
    }
    
    /**
     * Cancel generation progress notification
     */
    fun cancelGenerationNotification() {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID_GENERATION)
        }
    }
    
    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true // Permission not required for older versions
        }
    }
    
    /**
     * Show daily reminder notification
     */
    fun showDailyReminderNotification() {
        var charName: String? = null
        try {
            // Read active characters preferences synchronously
            kotlinx.coroutines.runBlocking {
                val prefs = com.doyouone.drawai.data.preferences.AppPreferences(context)
                val chars = prefs.activeReminderCharacters.first()
                if (chars.isNotEmpty()) {
                    charName = chars.random()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error reading prefs for notification", e)
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_DAILY_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = if (charName != null) "$charName" else "☀️ Good Morning!"
        val text = if (charName != null) "Hey! Don't forget your daily login! 🌟" else "Don't forget to claim your daily reward and keep your streak!"
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon in real app
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID_DAILY_REMINDER, builder.build())
            }
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "Notification permission not granted")
        }
    }
}
