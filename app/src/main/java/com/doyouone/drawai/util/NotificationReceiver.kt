package com.doyouone.drawai.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.doyouone.drawai.MainActivity

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val helper = NotificationHelper(context)
        // Show local push notification
        helper.showDailyReminderNotification()
    }
}
