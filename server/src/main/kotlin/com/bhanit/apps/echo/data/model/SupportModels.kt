package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SupportRequest(
    val category: TicketCategory,
    val subject: String,
    val description: String,
    val email: String? = null // Optional for internal, used for guests if passed
)

@Serializable
data class PublicSupportRequest(
    val email: String,
    val subject: String,
    val description: String,
    val category: TicketCategory = TicketCategory.GENERAL
)

@Serializable
enum class TicketCategory {
    GENERAL, BUG, FEEDBACK, ACCOUNT, CONTENT, OTHER
}

@Serializable
enum class TicketStatus {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED, WAITING_FOR_USER
}

@Serializable
enum class TicketPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

@Serializable
data class SupportResponse(
    val ticketId: String,
    val message: String
)

@Serializable
data class SupportTicketDto(
    val id: String,
    val userId: String?, // Nullable for guests
    val contactEmail: String?, // For guests
    val category: TicketCategory,
    val subject: String,
    val description: String,
    val status: TicketStatus,
    val priority: TicketPriority,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SupportHistoryResponse(
    val tickets: List<SupportTicketDto>
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
