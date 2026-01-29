package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.domain.repository.NotificationRepository
import com.bhanit.apps.echo.domain.repository.NotificationMessage
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll


data class TargetFilters(
    val platform: String? = null,
    val minVersion: String? = null,
    val daysInactive: Int? = null,
    val hasCompletedOnboarding: Boolean? = null
)

class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val dailyStatsRepository: com.bhanit.apps.echo.domain.repository.DailyStatsRepository,
    private val userRepository: com.bhanit.apps.echo.domain.repository.UserRepository,
    private val seasonalEventRepository: com.bhanit.apps.echo.domain.repository.SeasonalEventRepository
) {

    suspend fun broadcastNotification(
        title: String,
        body: String,
        filters: TargetFilters,
        dryRun: Boolean
    ): com.bhanit.apps.echo.presentation.routes.BroadcastResultDTO {
        // 1. Find Target Users
        val targetUserIds = userRepository.findUserIdsByFilter(
            daysInactive = filters.daysInactive,
            hasCompletedOnboarding = filters.hasCompletedOnboarding,
            minVersion = filters.minVersion,
            platform = filters.platform
        )

        if (dryRun) {
            return com.bhanit.apps.echo.presentation.routes.BroadcastResultDTO(
                count = targetUserIds.size,
                sample = targetUserIds.take(5).map { it.toString() }
            )
        }

        // 2. Send Notifications
        var successCount = 0
        var failCount = 0

        targetUserIds.forEach { uid ->
            val userDetails = userRepository.getUserDetails(uid)
            val token = userDetails?.get(com.bhanit.apps.echo.data.table.Users.fcmToken)
            val platform = userDetails?.get(com.bhanit.apps.echo.data.table.Users.platform)

            if (token != null) {
                // Using "BROADCAST" as generic type
                val result = FCMService.sendNotification(
                    token = token,
                    title = title,
                    body = body,
                    data = mapOf("type" to "SYSTEM"),
                    platform = platform
                )

                if (result is FCMService.FCMResult.Success) {
                    successCount++
                } else {
                    failCount++
                    if (result is FCMService.FCMResult.InvalidToken) {
                        try {
                            userRepository.clearFcmToken(uid)
                            userRepository.setUserInactive(uid)
                        } catch (e: Exception) {
                            // Ignore db errors during broadcast
                        }
                    }
                }
            } else {
                failCount++ // No token = Failed delivery attempt
            }
        }
        return com.bhanit.apps.echo.presentation.routes.BroadcastResultDTO(
            sent = successCount,
            failed = failCount,
            total = targetUserIds.size
        )


    }

    suspend fun sendUnopenedAppReminder(userId: UUID): Boolean {
        // 0. Check Preference
        val userDetails = userRepository.getUserDetails(userId)
        val enabled = userDetails?.get(com.bhanit.apps.echo.data.table.Users.notifyInactiveReminders) ?: true
        if (!enabled) return false

        // 1. Rate Limit: Check if we sent this notification today
        if (hasSentNotificationToday(
                userId,
                "SOCIAL"
            )
        ) return false // "SOCIAL" covers Unopened & Engagement to avoid overlap spam

        // 2. Pick Message
        // "UNOPENED_REMINDER_GENERIC" or "UNOPENED_REMINDER_POSITIVE"
        val type =
            if (Math.random() > 0.5) "UNOPENED_REMINDER_POSITIVE" else "UNOPENED_REMINDER_GENERIC"

        val messagePair = getRotationalMessageAndId(userId, "SOCIAL", type) ?: return false
        val (text, id) = messagePair

        // 3. Send
        return sendRetentionNotification(userId, "Unopened Echo", text, "UNOPENED_REMINDER", id, "REFLECTION") // Category: REFLECTION (matches client inference)
    }

    suspend fun sendEngagementPrompt(userId: UUID): Boolean {
        // 0. Check Preference
        val userDetails = userRepository.getUserDetails(userId)
        val enabled = userDetails?.get(com.bhanit.apps.echo.data.table.Users.notifyCheckins) ?: true
        if (!enabled) return false

        // 1. Rate Limit
        if (hasSentNotificationToday(userId, "SOCIAL")) return false

        // 2. Check Logic: Sent Count == 0
        val stats = dailyStatsRepository.getDailyStats(userId, java.time.LocalDate.now())
        val sentCount = stats?.sentCount ?: 0

        if (sentCount > 0) return false // Already engaged, no prompt needed

        // 3. Pick Message
        val messagePair =
            getRotationalMessageAndId(userId, "SOCIAL", "SOCIAL_PROMPT") ?: return false
        val (text, id) = messagePair

        // 4. Send
        return sendRetentionNotification(userId, "Warm Connection", text, "SOCIAL_PROMPT", id, "SOCIAL")
    }

    private suspend fun sendRetentionNotification(
        userId: UUID,
        title: String,
        body: String,
        type: String,
        messageId: UUID,
        category: String = "GENERAL"
    ): Boolean {
        try {
            val userDetails = userRepository.getUserDetails(userId)
            val token = userDetails?.get(com.bhanit.apps.echo.data.table.Users.fcmToken)
            val platform = userDetails?.get(com.bhanit.apps.echo.data.table.Users.platform)
            
            if (token != null) {
                val result = FCMService.sendNotification(
                    token = token,
                    title = title,
                    body = body,
                    data = mapOf("type" to type, "category" to category),
                    platform = platform
                )

                if (result is FCMService.FCMResult.InvalidToken) {
                    userRepository.clearFcmToken(userId)
                    return false
                }

                logNotificationSent(userId, messageId)
                return true
            }
        } catch (e: Exception) {
            println("WARN: Failed to send retention notification to $userId: ${e.message}")
        }
        return false
    }

    suspend fun getInAppNotification(
        userId: UUID,
        date: java.time.LocalDate = java.time.LocalDate.now()
    ): NotificationMessage? {
        // 1. Check if we already showed a reflection today (Client Date)
        if (hasSentNotificationToday(userId, "REFLECTION", date)) {
            val historyIds = notificationRepository.getHistoryForToday(userId, "REFLECTION", date)
            val existingId = historyIds.firstOrNull()

            if (existingId != null) {
                return null
            }
            return null
        }

        // 2. Check Daily Stats for "Same-Day Delayed" context
        val stats = dailyStatsRepository.getDailyStats(userId, date)

        var typePrefix = "UNIVERSAL_ONE_LINER" // Default

        if (stats != null) {
            if (stats.sentCount > 0) {
                val hasHeavy = stats.sentSentiments.contains("HEAVY")
                val hasDifficult = stats.sentSentiments.contains("DIFFICULT")
                val hasPositive =
                    stats.sentSentiments.contains("POSITIVE") || stats.sentSentiments.contains("REFLECTIVE")

                typePrefix = when {
                    hasHeavy -> "SAME_DAY_SENT_HEAVY"
                    hasDifficult -> "SAME_DAY_SENT_DIFFICULT"
                    hasPositive -> "SAME_DAY_SENT_POSITIVE"
                    else -> "SAME_DAY_SENT_POSITIVE"
                }
            } else if (stats.receivedCount > 0) {
                val hasHeavy = stats.receivedSentiments.contains("HEAVY")
                val hasDifficult = stats.receivedSentiments.contains("DIFFICULT")
                val hasPositive =
                    stats.receivedSentiments.contains("POSITIVE") || stats.receivedSentiments.contains(
                        "REFLECTIVE"
                    )

                typePrefix = when {
                    hasHeavy -> "SAME_DAY_RECEIVED_HEAVY"
                    hasDifficult -> "SAME_DAY_RECEIVED_DIFFICULT"
                    hasPositive -> "SAME_DAY_RECEIVED_POSITIVE"
                    else -> "SAME_DAY_RECEIVED_POSITIVE"
                }
            }
        }

        // 3. Fetch Message
        val messagePair = getRotationalMessageAndId(userId, "REFLECTION", typePrefix)

        if (messagePair != null) {
            val (text, id) = messagePair
            logNotificationSent(userId, id)
            return NotificationMessage(id, "REFLECTION", typePrefix, text, true)
        }

        return null
    }

    suspend fun getRotationalMessageAndId(
        userId: UUID,
        category: String,
        type: String
    ): Pair<String, UUID>? {
        val messages = notificationRepository.getActiveMessages(category, type, isRotational = true)
        if (messages.isEmpty()) return null

        val lastSentId = notificationRepository.getLastSentMessageId(userId, category, type)

        val available = if (messages.size > 1 && lastSentId != null) {
            messages.filter { it.id != lastSentId }
        } else {
            messages
        }

        val selected = available.random()
        return selected.messageTemplate to selected.id
    }

    suspend fun getOperationalMessage(
        type: String,
        params: Map<String, String>,
        category: String = "SOCIAL"
    ): String? {
        val messages = notificationRepository.getActiveMessages(
            category = category,
            type = type,
            isRotational = false
        )
        val template = messages.firstOrNull()?.messageTemplate ?: return null

        var result = template
        params.forEach { (key, value) ->
            result = result.replace("{${key}}", value)
        }
        return result
    }

    suspend fun logNotificationSent(userId: UUID, messageId: UUID) {
        notificationRepository.logNotification(userId, messageId)
    }

    suspend fun hasSentNotificationToday(
        userId: UUID,
        category: String,
        date: java.time.LocalDate = java.time.LocalDate.now()
    ): Boolean {
        val history = notificationRepository.getHistoryForToday(userId, category, date)
        return history.isNotEmpty()
    }

    private fun shouldSendPush(type: com.bhanit.apps.echo.data.model.TransactionType): Boolean {
        // Echo-Safe Rule: meaningfully rare moments get a push. Frequent ones stay quiet (in-app).
        return when (type) {
            com.bhanit.apps.echo.data.model.TransactionType.FIRST_TIME_BONUS,
            com.bhanit.apps.echo.data.model.TransactionType.SEASON_REWARD -> true

            com.bhanit.apps.echo.data.model.TransactionType.STREAK_REWARD,
            com.bhanit.apps.echo.data.model.TransactionType.BALANCED_ACTIVITY_BONUS -> false // Quiet appreciation (In-App History)
            
            else -> false
        }
    }

    suspend fun sendRewardNotification(
        userId: UUID,
        type: com.bhanit.apps.echo.data.model.TransactionType,
        amount: Int,
        relatedId: String?,
        description: String?
    ) {
        if (amount <= 0) return
        
        // 0. Check User Preference (Server-Side Sync)
        val userDetails = userRepository.getUserDetails(userId)
        val rewardsEnabled = userDetails?.get(com.bhanit.apps.echo.data.table.Users.notifyRewards) ?: true
        
        if (!rewardsEnabled) {
            println("DEBUG: Skipped reward notification for $userId (notify_rewards=false)")
            return
        }

        // 1. Check Push Eligibility (In-App Policy)
        if (!shouldSendPush(type)) {
             println("DEBUG: Skipped reward push for $userId (Type $type is In-App Only)")
             return 
        }

        // 2. Determine Template Key
        var templateKey = "REWARD_GENERIC"
        var seasonName = "Season" // Default for seasonal formatting

        when (type) {
            com.bhanit.apps.echo.data.model.TransactionType.STREAK_REWARD -> templateKey = "REWARD_STREAK"
            com.bhanit.apps.echo.data.model.TransactionType.SEASON_REWARD -> {
                templateKey = "REWARD_SEASONAL"
                // Extract Season Name from Description (e.g. "Valentine's Week Appreciation" -> "Valentine's Week")
                if (!description.isNullOrBlank()) {
                    seasonName = description.replace(" Appreciation", "").replace(" Reward", "").trim()
                } else if (relatedId?.contains("VALENTINE") == true) {
                     seasonName = "Valentine's"
                }
            }
            com.bhanit.apps.echo.data.model.TransactionType.BALANCED_ACTIVITY_BONUS -> {
                templateKey = "REWARD_CONSISTENCY"
            }
            com.bhanit.apps.echo.data.model.TransactionType.FIRST_TIME_BONUS -> {
                if (relatedId == "FIRST_TIME_ECHO") templateKey = "REWARD_FIRST_ECHO"
                else if (relatedId == "FIRST_TIME_RESPONSE") templateKey = "REWARD_FIRST_RESPONSE"
            }
            com.bhanit.apps.echo.data.model.TransactionType.REFERRAL_REWARD -> return 
            com.bhanit.apps.echo.data.model.TransactionType.SIGNUP_BONUS -> return
            else -> templateKey = "REWARD_GENERIC"
        }
        
        // 3. Fetch Template
        val message = getOperationalMessage(templateKey, emptyMap(), category = "REWARDS") ?: return
        
        // 4. Format Message (Echo-Safe Logic)
        val formattedBody = message
            .replace("{amount}", amount.toString())
            .replace("{season_name}", seasonName)
            
        // 5. Send
        try {
            val token = userDetails?.get(com.bhanit.apps.echo.data.table.Users.fcmToken) 
            val platform = userDetails?.get(com.bhanit.apps.echo.data.table.Users.platform)

            if (token != null) {
                // Determine Title from Key (Echo-Safe Mapping)
                val title = when(templateKey) {
                    "REWARD_STREAK" -> "You kept showing up"
                    "REWARD_CONSISTENCY" -> "Consistency noticed"
                    "REWARD_SEASONAL" -> "$seasonName Moment 💖"
                    "REWARD_FIRST_ECHO" -> "Your first echo"
                    "REWARD_FIRST_RESPONSE" -> "Your response mattered"
                    else -> "A small moment of appreciation"
                }

                val result = FCMService.sendNotification(
                    token = token,
                    title = title,
                    body = formattedBody,
                    data = mapOf(
                        "type" to templateKey, // Specific Reward Type e.g. REWARD_SEASONAL
                        "category" to "REWARD", // High Level Category
                        "amount" to amount.toString(),
                        "source" to type.name, // Tracking Source e.g. SEASON_REWARD
                        "season" to if (type == com.bhanit.apps.echo.data.model.TransactionType.SEASON_REWARD) seasonName else "",
                        "navigate_to" to "JOURNEY" // Deep link
                    ),
                    platform = platform
                )

                if (result is FCMService.FCMResult.InvalidToken) {
                    userRepository.clearFcmToken(userId)
                }
            }
        } catch (e: Exception) {
            println("WARN: Failed to send reward notification ($templateKey) to $userId: ${e.message}")
        }
    }

    suspend fun seedDefaults() {
        val defaults = NotificationDefaults.getInitialMessages()
        notificationRepository.seedMessages(defaults)
    }

    suspend fun checkSeasonStartNotifications() = kotlinx.coroutines.coroutineScope {
        val scope = this
        val todayUtc = java.time.LocalDate.now() 
        val allEvents = seasonalEventRepository.getEventsByYear(todayUtc.year)
        
        val activeUsers = userRepository.getActiveUsersWithMetadata()
        val nowUtc = java.time.ZonedDateTime.now(java.time.ZoneId.of("UTC"))
        
        // Process in batches to control concurrency
        activeUsers.chunked(50).forEach { batch ->
            batch.map { user ->
                val (userId, timezoneStr, metaJson) = user
                scope.launch {
                    try {
                        val zoneId = try { java.time.ZoneId.of(timezoneStr) } catch(e: Exception) { return@launch }
                        val userTime = nowUtc.withZoneSameInstant(zoneId)
                        val userHour = userTime.hour
                        
                        // 8AM - 12PM Window
                        if (userHour in 8..11) { 
                            val userToday = userTime.toLocalDate()
                            
                            val startingSeasons = allEvents.filter { 
                                val start = java.time.LocalDate.of(it.year ?: todayUtc.year, it.startMonth, it.startDay)
                                start == userToday && it.isActive 
                            }
                            
                            if (startingSeasons.isNotEmpty()) {
                                // Check Preference inside loop (Optimization: Batch fetch later?)
                                val userDetails = userRepository.getUserDetails(userId)
                                val rewardsEnabled = userDetails?.get(com.bhanit.apps.echo.data.table.Users.notifyRewards) ?: true
                                
                                if (rewardsEnabled) {
                                    startingSeasons.forEach { season ->
                                         if (metaJson == null || !metaJson.contains("\"${season.id}\"")) { 
                                             val msg = getOperationalMessage("SEASON_START", mapOf("season_name" to season.name), "REWARDS")
                                             if (msg != null) {
                                                 val token = userDetails?.get(com.bhanit.apps.echo.data.table.Users.fcmToken)
                                                 val platform = userDetails?.get(com.bhanit.apps.echo.data.table.Users.platform)
                                                 
                                                 if (token != null) {
                                                      val result = com.bhanit.apps.echo.domain.service.FCMService.sendNotification(
                                                          token, 
                                                          "${season.name} Starts Today", 
                                                          msg, 
                                                          mapOf("type" to "SEASON_START", "result_id" to season.id, "navigate_to" to "JOURNEY", "category" to "REWARD"),
                                                          platform
                                                      )
                                                      
                                                      if (result is com.bhanit.apps.echo.domain.service.FCMService.FCMResult.Success) {
                                                          userRepository.markSeasonAnnounced(userId, season.id)
                                                      } else if (result is com.bhanit.apps.echo.domain.service.FCMService.FCMResult.InvalidToken) {
                                                          userRepository.clearFcmToken(userId)
                                                      }
                                                 }
                                             }
                                         }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Error processing season start for user $userId: ${e.message}")
                    }
                }
            }.joinAll() // Wait for batch
        }
    }
}
