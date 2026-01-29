package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TicketStatus { OPEN, IN_REVIEW, RESOLVED, CLOSED }

@Serializable
enum class TicketPriority { LOW, MEDIUM, HIGH, CRITICAL }

@Serializable
enum class TicketCategory { BUG, FEEDBACK, ACCOUNT, REQUEST_EMOTION, OTHER }

@Serializable
data class SupportRequest(
    val category: TicketCategory,
    val subject: String,
    val description: String,
    val priority: TicketPriority = TicketPriority.MEDIUM
)

@Serializable
data class SupportTicketDto(
    val id: String,
    val category: TicketCategory,
    val subject: String,
    val description: String,
    val status: TicketStatus,
    val priority: TicketPriority,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class TicketReplyDto(
    val id: String,
    val ticketId: String,
    val senderId: String,
    val senderType: String,
    val content: String,
    val createdAt: Long
)

@Serializable
data class SupportHistoryResponse(
    val tickets: List<SupportTicketDto>
)

@Serializable
data class SupportResponse(
    val ticketId: String,
    val message: String
)
