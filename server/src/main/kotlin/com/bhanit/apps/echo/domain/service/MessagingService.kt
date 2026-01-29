package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.data.model.MessageDTO
import com.bhanit.apps.echo.domain.repository.MessageRepository
import com.bhanit.apps.echo.data.table.Users
import java.util.UUID

class MessagingService(
    private val messageRepository: MessageRepository,
    private val dailyStatsRepository: com.bhanit.apps.echo.domain.repository.DailyStatsRepository,
    private val userRepository: com.bhanit.apps.echo.domain.repository.UserRepository,
    private val connectionRepository: com.bhanit.apps.echo.domain.repository.ConnectionRepository,
    private val creditService: com.bhanit.apps.echo.domain.service.CreditService, // Injected for Seasonal/Streak rewards
    private val emotionRepository: com.bhanit.apps.echo.data.repository.EmotionRepository? = null,
    private val habitService: com.bhanit.apps.echo.domain.service.HabitService? = null
) {
    suspend fun sendMessage(
        senderId: UUID,
        receiverId: UUID,
        feeling: String, // String ID
        content: String,
        isAnonymous: Boolean,
        clientDate: java.time.LocalDate = java.time.LocalDate.now()
    ): UUID {
        // 1. Check Daily Limit
        val today = clientDate
        val dailyCount = messageRepository.getDailyMessageCount(senderId, receiverId, today)
        val limit = messageRepository.getRelationLimit(senderId, receiverId)
        
        if (dailyCount >= limit) {
             throw IllegalArgumentException("Daily message limit exceeded for this person.")
        }
        
        // 2. Check Blocks (Strict Privacy) - Optimized
        if (connectionRepository.areBlocked(senderId, receiverId)) {
             throw IllegalArgumentException("You cannot send messages to this user.")
        }

        try {
            val id =
                messageRepository.sendMessage(senderId, receiverId, feeling, content, isAnonymous)
                
            // Record Usage (Credits)
            emotionRepository?.recordEmotionUsage(senderId, feeling)
            
            // Record Transaction for Seasonal Tracking
            try {
                userRepository.recordTransaction(
                    userId = senderId,
                    type = com.bhanit.apps.echo.data.model.TransactionType.SENT_ECHO,
                    amount = 0, // Zero amount, purely for tracking stats
                    relatedId = receiverId.toString(),
                    notes = "Sent Echo to user"
                )
            } catch (e: Exception) {
                println("WARN: Failed to record SENT_ECHO transaction: ${e.message}")
            }
            
            // Check & Grant Seasonal Rewards
            try {
                creditService.checkAndAwardSeasonal(
                    userId = senderId,
                    date = today,
                    ruleType = com.bhanit.apps.echo.util.SeasonalRuleType.SEND_POSITIVE,
                    relatedSourceId = id.toString()
                )
            } catch (e: Exception) {
                println("WARN: Failed to check seasonal rewards (SEND_POSITIVE): ${e.message}")
            }

            // Update stats
            val today = java.time.LocalDate.now()
            dailyStatsRepository.incrementSentCount(senderId, today)
            dailyStatsRepository.incrementSentCount(senderId, today)
            dailyStatsRepository.incrementReceivedCount(receiverId, today)
            
            // Record Habit Activity
            try {
                habitService?.recordActivity(
                    senderId, 
                    com.bhanit.apps.echo.domain.service.HabitService.ActivityType.MESSAGE_SENT, 
                    feeling,
                    date = today // Use clientDate (assigned to 'today' above)
                )
            } catch (e: Exception) {
                println("ERROR: MessagingService -> Failed to record habit: ${e.message}")
            }

            // Trigger FCM Notification
            try {
                val (receiverToken, receiverPlatform) = userRepository.getFcmTokenAndPlatform(receiverId)
                if (!receiverToken.isNullOrBlank()) {
                    val title = if (isAnonymous) "New Echo" else "New Echo"
                    // Privacy: Don't show specific emotion or name on lock screen.
                    val body = "Someone shared a feeling with you."
                    val data = mapOf(
                        "type" to "ECHO"
                    )
                    val result = FCMService.sendNotification(receiverToken, title, body, data, platform = receiverPlatform)
                    if (result is FCMService.FCMResult.InvalidToken) {
                        println("WARN: Invalid FCM token for user $receiverId. Clearing token.")
                        userRepository.clearFcmToken(receiverId)
                    }
                } else {
                    println("DEBUG: FCM -> No token for receiver $receiverId")
                }
            } catch (e: Exception) {
                println("ERROR: FCM -> Failed to send message notification: ${e.message}")
            }
            return id
        } catch (e: Exception) {
            println("ERROR: MessagingService.sendMessage failed: ${e.stackTraceToString()}")
            throw e
        }
    }

    suspend fun replyToMessage(
        messageId: UUID,
        senderId: UUID, // The one replying
        content: String
    ): UUID {
        if (messageRepository.hasReplied(messageId)) {
            throw IllegalArgumentException("You can only reply once to a message.")
        }
        
        val message = messageRepository.getMessage(messageId) 
            ?: throw IllegalArgumentException("Message not found")
            
        if (message.receiverId != senderId.toString()) {
             throw IllegalArgumentException("You are not the recipient of this message")
        }

        val id = messageRepository.replyToMessage(messageId, senderId, content)
        
        // Trigger FCM Notification for Original Sender
        try {
            val originalSenderId = UUID.fromString(message.senderId)
            val (senderToken, senderPlatform) = userRepository.getFcmTokenAndPlatform(originalSenderId)
            if (!senderToken.isNullOrBlank()) {
                 // We need the replier's username for the deep link
                 val replierRow = userRepository.getUser(senderId)
                 val replierName = replierRow?.get(Users.username) ?: "Someone"
                 
                 val data = mapOf(
                     "type" to "REPLY",
                     "userId" to senderId.toString(),
                     "username" to replierName
                 )
                 val result = FCMService.sendNotification(senderToken, "Echo Reply", "$replierName replied to your echo!", data, platform = senderPlatform)
                 if (result is FCMService.FCMResult.InvalidToken) {
                     println("WARN: Invalid FCM token for user $originalSenderId. Clearing token.")
                     userRepository.clearFcmToken(originalSenderId)
                 }
            } else {
                 println("DEBUG: FCM -> No token for sender $originalSenderId")
            }
        } catch (e: Exception) {
             println("ERROR: FCM -> Failed to send reply notification: ${e.message}")
        }
        
        // Record Response Streak (ECHO_BACK_SENT)
        try {
            habitService?.recordActivity(
                senderId, 
                com.bhanit.apps.echo.domain.service.HabitService.ActivityType.ECHO_BACK_SENT, 
                emotionId = null,
                date = java.time.LocalDate.now()
            )
        } catch (e: Exception) {
            println("ERROR: MessagingService -> Failed to record Response Streak: ${e.message}")
        }
        
        // Record Transaction for Seasonal Tracking (Respond Rule)
        try {
            userRepository.recordTransaction(
                userId = senderId,
                type = com.bhanit.apps.echo.data.model.TransactionType.RECEIVED_ECHO, // Interpreted as "Response Action" for Seasonal Rules
                amount = 0,
                relatedId = messageId.toString(),
                notes = "Replied to echo"
            )
        } catch (e: Exception) {
            println("WARN: Failed to record RECEIVED_ECHO transaction for reply: ${e.message}")
        }
        
        // Check & Grant Seasonal Rewards (Respond)
        try {
            creditService.checkAndAwardSeasonal(
                 userId = senderId,
                 date = java.time.LocalDate.now(),
                 ruleType = com.bhanit.apps.echo.util.SeasonalRuleType.RESPOND,
                 relatedSourceId = messageId.toString()
            )
        } catch (e: Exception) {
             println("WARN: Failed to check seasonal rewards (RESPOND): ${e.message}")
        }
        
        return id
    }

    suspend fun getHistory(userA: UUID, userB: UUID, page: Int, limit: Int): List<MessageDTO> {
        val offset = (page - 1) * limit
        return messageRepository.getHistory(userA, userB, limit, offset)
    }
    
    suspend fun getSentMessages(userId: UUID, receiverId: UUID? = null, page: Int, limit: Int, onlyReplied: Boolean = false): List<MessageDTO> {
        val offset = (page - 1) * limit
        return messageRepository.getSentMessages(userId, receiverId, limit, offset, onlyReplied)
    }

    suspend fun getInbox(
        userId: UUID, 
        page: Int, 
        limit: Int,
        feelings: List<String>? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): List<MessageDTO> {
        val offset = (page - 1) * limit
        return messageRepository.getInbox(userId, limit, offset, feelings, startDate, endDate)
    }
    
    suspend fun canReply(messageId: UUID, userId: UUID): Boolean {
        if (messageRepository.hasReplied(messageId)) return false
        val message = messageRepository.getMessage(messageId) ?: return false
        return message.receiverId == userId.toString()
    }
    
    suspend fun getUsageStatus(senderId: UUID, receiverId: UUID): Map<String, Int> {
        val today = java.time.LocalDate.now()
        val count = messageRepository.getDailyMessageCount(senderId, receiverId, today).toInt()
        val limit = messageRepository.getRelationLimit(senderId, receiverId)
        return mapOf(
            "used" to count,
            "limit" to limit,
            "remaining" to (limit - count).coerceAtLeast(0)
        )
    }
}
