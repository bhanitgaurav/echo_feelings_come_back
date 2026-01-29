package com.bhanit.apps.echo.domain.repository

import java.util.UUID

data class NotificationMessage(
    val id: UUID,
    val category: String,
    val type: String,
    val messageTemplate: String,
    val isRotational: Boolean
)

interface NotificationRepository {
    suspend fun getActiveMessages(category: String, type: String, isRotational: Boolean): List<NotificationMessage>
    suspend fun getHistoryForToday(userId: UUID, category: String, date: java.time.LocalDate = java.time.LocalDate.now()): List<UUID> // Messages sent "today" (passed date) for this category
    suspend fun getLastSentMessageId(userId: UUID, category: String, type: String): UUID?
    suspend fun logNotification(userId: UUID, messageId: UUID)
    suspend fun seedMessages(messages: List<NotificationMessage>) // For startup seeding
}
