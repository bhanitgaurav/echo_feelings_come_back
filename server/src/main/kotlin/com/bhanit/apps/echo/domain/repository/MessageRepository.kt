package com.bhanit.apps.echo.domain.repository

import com.bhanit.apps.echo.data.model.MessageDTO
import java.util.UUID

interface MessageRepository {
    suspend fun sendMessage(
        senderId: UUID,
        receiverId: UUID,
        feeling: String, // String ID
        content: String,
        isAnonymous: Boolean
    ): UUID

    suspend fun replyToMessage(
        messageId: UUID,
        senderId: UUID, // The one replying (original receiver)
        content: String
    ): UUID

    suspend fun getHistory(
        userA: UUID, 
        userB: UUID, 
        limit: Int, 
        offset: Int
    ): List<MessageDTO>
    
    suspend fun getInbox(
        userId: UUID, 
        limit: Int, 
        offset: Int,
        feelings: List<String>? = null, // List<String>
        startDate: Long? = null, 
        endDate: Long? = null
    ): List<MessageDTO>

    suspend fun getSentMessages(
        userId: UUID,
        receiverId: UUID? = null,
        limit: Int,
        offset: Int,
        onlyReplied: Boolean = false
    ): List<MessageDTO>
    
    suspend fun getMessage(messageId: UUID): MessageDTO?
    suspend fun hasReplied(messageId: UUID): Boolean
    
    suspend fun getDailyMessageCount(senderId: UUID, receiverId: UUID, date: java.time.LocalDate): Long
    suspend fun getRelationLimit(senderId: UUID, receiverId: UUID): Int
    
    suspend fun getUsersWithUnreadMessages(minAgeInHours: Int): List<UUID>
}
