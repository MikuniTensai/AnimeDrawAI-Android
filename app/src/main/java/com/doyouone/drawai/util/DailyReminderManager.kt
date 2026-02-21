package com.doyouone.drawai.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.util.Calendar

/**
 * Manages scheduling of daily reminders via AlarmManager
 */
class DailyReminderManager(private val context: Context) {

    fun scheduleReminder(hour: Int = 8, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Exact Alarm permission check for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // If we can't schedule exact alarms, just return/log or handle elsewhere (UI usually prompts)
                // For now, we will try best effort or use inexact
                android.util.Log.w("DailyReminder", "Cannot schedule exact alarms, checking permission")
            }
        }

        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // If time has passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            // Use setRepeating or setExactAndAllowWhileIdle depending on needs
            // setInexactRepeating is battery friendly but less precise
            // For "Morning Call", precision isn't critical down to the second, but "exact" is preferred by users
            // However, starting with Android 12, exact alarms need permission. 
            // We'll use setExactAndAllowWhileIdle for the next occurrence, and reschedule in receiver if needed,
            // OR simply use setInexactRepeating for simplicity if exactness isn't strict.
            // Given "Local Push Notification" concept, standard repeating is fine.
            
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            android.util.Log.d("DailyReminder", "Scheduled reminder for ${calendar.time}")
            
        } catch (e: SecurityException) {
            android.util.Log.e("DailyReminder", "Failed to schedule alarm: ${e.message}")
        }
    }

    fun cancelReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        android.util.Log.d("DailyReminder", "Cancelled reminder")
    }
}
