package com.bhanit.apps.echo.features.messaging.domain

import kotlinx.coroutines.flow.Flow

interface MessagingRepository {
    suspend fun sendMessage(receiverId: String, emotion: Emotion, content: String, isAnonymous: Boolean): Result<String>
    suspend fun replyToMessage(messageId: String, content: String): Result<String>
    suspend fun getInbox(
        page: Int, 
        limit: Int,
        feelings: List<Emotion>? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Result<List<Message>>
    suspend fun getSentMessages(receiverId: String? = null, page: Int, limit: Int, onlyReplied: Boolean = false): Result<List<Message>>
    suspend fun checkReplyStatus(messageId: String): Result<Boolean>
    suspend fun updateFcmToken(token: String): Result<Unit>
    suspend fun getDailyUsage(receiverId: String): Result<Map<String, Int>>
}
