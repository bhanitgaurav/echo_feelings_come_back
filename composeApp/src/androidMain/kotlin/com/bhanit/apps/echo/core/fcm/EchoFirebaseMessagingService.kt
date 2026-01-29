package com.bhanit.apps.echo.core.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bhanit.apps.echo.features.messaging.domain.MessagingRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class EchoFirebaseMessagingService : FirebaseMessagingService() {

    private val messagingRepository: MessagingRepository by inject()
    private val sessionRepository: com.bhanit.apps.echo.features.auth.domain.SessionRepository by inject()
    private val analytics: com.bhanit.apps.echo.shared.analytics.EchoAnalytics by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ... (onNewToken remains same)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // Log Receipt
        analytics.logEvent(
            com.bhanit.apps.echo.shared.analytics.EchoEvents.NOTIFICATION_RECEIVED,
            mapOf(
                com.bhanit.apps.echo.shared.analytics.EchoParams.NOTIFICATION_TYPE to (remoteMessage.data["type"] ?: "unknown")
            )
        )
        
        // 1. Safety Check: If user is NOT logged in, suppress notification
        // Note: runBlocking is generally bad in Main Thread, but onMessageReceived runs on worker thread.
        val isLoggedIn = kotlinx.coroutines.runBlocking { 
            sessionRepository.isLoggedIn.firstOrNull() == true
        }

        if (!isLoggedIn) {
            println("WARN: Notification received but user logged out. Suppressing.")
            return
        }
        

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Echo"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"]
        
        if (body != null) {
            val type = remoteMessage.data["type"] ?: "unknown"
            if (shouldShowNotification(type)) {
                showNotification(title, body, remoteMessage.data)
            } else {
            }
        }
    }

    private fun shouldShowNotification(type: String): Boolean {
        // ... (existing code)
        val result = kotlinx.coroutines.runBlocking {
             // ... (existing match)
             when (type.uppercase()) {
                "ECHO", "REPLY", "FEELING", "CONNECTION" -> sessionRepository.notificationsFeelings.firstOrNull() ?: true
                "CHECKIN", "INACTIVE" -> sessionRepository.notificationsCheckins.firstOrNull() ?: true
                "REFLECTION" -> sessionRepository.notificationsReflections.firstOrNull() ?: true
                "REWARD", "STREAK", "REFERRAL_WELCOME", "REFERRAL_JOINED" -> sessionRepository.notificationsRewards.firstOrNull() ?: true
                "SYSTEM", "UPDATE" -> sessionRepository.notificationsUpdates.firstOrNull() ?: true
                else -> true 
            }
        }
        return result
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        // Use server-provided category or infer from type
        val type = data["type"] ?: "GENERAL"
        val category = data["category"] ?: inferCategoryFromType(type)
        
        val channelId = when (category) {
            "REWARD" -> "channel_rewards"
            "SOCIAL" -> "channel_social"
            "REFLECTION" -> "channel_reflections"
            "SYSTEM" -> "channel_updates"
            else -> "channel_general"
        }
        
        val notificationGroupKey = "com.bhanit.apps.echo.notification.$category"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Ensure Channel Exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager, channelId, category)
        }

        val intent = android.content.Intent(this, com.bhanit.apps.echo.MainActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = android.net.Uri.parse("android.resource://" + packageName + "/" + com.bhanit.apps.echo.R.raw.echo_notification)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(com.bhanit.apps.echo.R.drawable.ic_notification_logo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(notificationGroupKey)
            .setSound(soundUri) // Default sound, channel overrides on O+
            
        // Vibration pattern (Subtle)
        notificationBuilder.setVibrate(longArrayOf(0, 250, 250, 250))

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        
        // Summary Notification for Grouping
        val summaryNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Echo Updates")
            .setSmallIcon(com.bhanit.apps.echo.R.drawable.ic_notification_logo)
            .setStyle(NotificationCompat.InboxStyle().setSummaryText("New updates"))
            .setGroup(notificationGroupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .build()
            
        val summaryId = category.hashCode()
        notificationManager.notify(summaryId, summaryNotification)
    }
    
    private fun inferCategoryFromType(type: String): String {
        return when {
             type.startsWith("REWARD") || type.startsWith("SEASON") || type.contains("BONUS") -> "REWARD"
             type.startsWith("SOCIAL") || type == "CONNECTION" || type == "MESSAGE" -> "SOCIAL"
             type == "REFLECTION" || type == "CHECKIN" || type.contains("REMINDER") -> "REFLECTION"
             type == "SYSTEM" || type == "UPDATE" -> "SYSTEM"
             else -> "GENERAL"
        }
    }

    private fun createNotificationChannel(manager: NotificationManager, channelId: String, category: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(channelId) == null) {
                val (name, importance, desc) = when (channelId) {
                    "channel_rewards" -> Triple("Rewards & Achievements", NotificationManager.IMPORTANCE_HIGH, "Updates about your streaks, bonuses, and seasonal rewards")
                    "channel_social" -> Triple("Social & Connections", NotificationManager.IMPORTANCE_HIGH, "Messages and connection requests")
                    "channel_reflections" -> Triple("Reflections & Reminders", NotificationManager.IMPORTANCE_DEFAULT, "Daily prompts and check-in reminders")
                    "channel_updates" -> Triple("System Updates", NotificationManager.IMPORTANCE_LOW, "App updates and maintenance info")
                    else -> Triple("General", NotificationManager.IMPORTANCE_DEFAULT, "General notifications")
                }

                val channel = NotificationChannel(channelId, name, importance).apply {
                    description = desc
                    enableVibration(true)
                    val soundUri = android.net.Uri.parse("android.resource://" + packageName + "/" + com.bhanit.apps.echo.R.raw.echo_notification)
                    
                    val audioAttributes = android.media.AudioAttributes.Builder()
                         .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                         .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                         .build()
                    setSound(soundUri, audioAttributes)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
