package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.domain.repository.HabitRepository
import com.bhanit.apps.echo.domain.repository.UserRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

import com.bhanit.apps.echo.domain.repository.MessageRepository
import com.bhanit.apps.echo.domain.repository.DailyStatsRepository

class NotificationScheduler(
    private val habitRepository: HabitRepository,
    private val notificationService: NotificationService,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    private val dailyStatsRepository: DailyStatsRepository
) {
    
    fun start() {
        startStreakReminders()
        startUnopenedAppChecks()
    }

    private fun startStreakReminders() {
        GlobalScope.launch {
            while (true) {
                // Target: 8 PM Local Time (Simplification: 8 PM Server Time for MVP, or iterate all users?)
                // Since users are global, "8 PM" is complex. 
                // MVP: Run once every hour, check users whose "Local Time" is approx 8 PM?
                // Or simplification: Run at 8 PM UTC and just ping everyone inactive.
                // Let's go with: Check every hour.
                
                // For this MVP step, let's run it every hour on the hour.
                val now = LocalDateTime.now()
                val nextHour = now.plusHours(1).withMinute(0).withSecond(0)
                val delayMs = Duration.between(now, nextHour).toMillis()
                
                delay(delayMs)
                
                try {
                    println("NotificationScheduler: Running Streak Reminders...")
                    runStreakReminderLogic()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun startUnopenedAppChecks() {
        GlobalScope.launch {
            while (true) {
                // Check hourly
                val now = LocalDateTime.now()
                val nextHour = now.plusHours(1).withMinute(0).withSecond(0)
                val delayMs = Duration.between(now, nextHour).toMillis()
                
                delay(delayMs)
                
                try {
                    println("NotificationScheduler: Running Unopened App & Engagement Checks...")
                    runUnopenedAppLogic()
                    runEngagementPromptLogic()
                    
                    println("NotificationScheduler: Running Season Start Checks...")
                    notificationService.checkSeasonStartNotifications()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun runUnopenedAppLogic() {
        // Users with unread messages older than 24 hours
        val targets = messageRepository.getUsersWithUnreadMessages(minAgeInHours = 24)
        
        targets.forEach { userId ->
             val userDetails = userRepository.getUserDetails(userId)
             val timezone = userDetails?.get(com.bhanit.apps.echo.data.table.Users.timezone) ?: "UTC"
             
             try {
                 val zoneId = java.time.ZoneId.of(timezone)
                 val userTime = java.time.ZonedDateTime.now(zoneId)
                 
                 // Target 10 PM
                 if (userTime.hour == 22) {
                     val sent = notificationService.sendUnopenedAppReminder(userId)
                     if (sent) println("NotificationScheduler: Sent unopened app reminder to $userId")
                 }
             } catch (e: Exception) {
                 println("WARN: Invalid timezone for user $userId: $timezone")
             }
        }
    }

    private suspend fun runEngagementPromptLogic() {
        // Active users (e.g. active in last 7 days)
        val targets = userRepository.findUserIdsByFilter(daysInactive = null, hasCompletedOnboarding = null, minVersion = null, platform = null)
        // findUserIdsByFilter with nulls returns all active users.
        // We probably want to limit to RECENTLY active users to avoid spamming ghosts. 
        // But for MVP, let's iterate all active (isActive=true) users.
        
        targets.forEach { userId ->
             val userDetails = userRepository.getUserDetails(userId)
             val timezone = userDetails?.get(com.bhanit.apps.echo.data.table.Users.timezone) ?: "UTC"
             
             try {
                 val zoneId = java.time.ZoneId.of(timezone)
                 val userTime = java.time.ZonedDateTime.now(zoneId)
                 
                 // Target 8 PM (Window 7:30 - 8:30 covered by hourly check at 8:00)
                 if (userTime.hour == 20) {
                     val sent = notificationService.sendEngagementPrompt(userId)
                     if (sent) println("NotificationScheduler: Sent engagement prompt to $userId")
                 }
             } catch (e: Exception) {
                 println("WARN: Invalid timezone for user $userId: $timezone")
             }
        }
    }
    
    private suspend fun runStreakReminderLogic() {
        val targets = habitRepository.getUsersForStreakReminder()
        val category = "STREAK"
        val type = "STREAK_REMINDER"

        targets.forEach { target ->
            // Check if we sent this category today
            if (notificationService.hasSentNotificationToday(target.userId, category)) {
                return@forEach
            }

            // Get Rotational Message
            val messagePair = notificationService.getRotationalMessageAndId(target.userId, category, type)
            if (messagePair == null) return@forEach
            
            val (messageText, messageId) = messagePair

            // Send FCM
            try {
                val (token, platform) = userRepository.getFcmTokenAndPlatform(target.userId)
                
                if (token != null) {
                    val result = com.bhanit.apps.echo.domain.service.FCMService.sendNotification(
                        token = token,
                        title = "Echo Streak",
                        body = messageText,
                        data = mapOf("type" to "STREAK_REMINDER"),
                        platform = platform
                    )
                    
                    if (result is com.bhanit.apps.echo.domain.service.FCMService.FCMResult.InvalidToken) {
                        userRepository.clearFcmToken(target.userId)
                    }
                }
                
                // Log History
                notificationService.logNotificationSent(target.userId, messageId)
                println("NotificationScheduler: Sent streak reminder to ${target.userId}")
            } catch (e: Exception) {
                println("NotificationScheduler: Failed to send to ${target.userId}: ${e.message}")
            }
        }
    }
}
