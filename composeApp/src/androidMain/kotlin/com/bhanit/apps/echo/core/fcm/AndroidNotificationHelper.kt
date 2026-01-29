package com.bhanit.apps.echo.core.fcm

import android.app.NotificationManager
import android.content.Context

class AndroidNotificationHelper(private val context: Context) : NotificationHelper {
    override fun cancelAll() {
        println("DEBUG: AndroidNotificationHelper - Cancelling all notifications")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        println("DEBUG: AndroidNotificationHelper - Cancelled")
    }
}
