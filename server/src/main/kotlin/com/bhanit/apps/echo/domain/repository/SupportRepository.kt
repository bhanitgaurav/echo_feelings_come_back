package com.bhanit.apps.echo.domain.repository

import com.bhanit.apps.echo.data.model.SupportRequest
import java.util.UUID

interface SupportRepository {
    suspend fun createTicket(userId: UUID?, request: SupportRequest): UUID
    suspend fun getTickets(userId: UUID, limit: Int, offset: Long): List<com.bhanit.apps.echo.data.model.SupportTicketDto>
    
    // Admin Methods
    suspend fun getAllTickets(status: String?, priority: String?, limit: Int, offset: Long): List<com.bhanit.apps.echo.data.model.SupportTicketDto>
    suspend fun getTicketDetails(ticketId: UUID): com.bhanit.apps.echo.data.model.SupportTicketDto?
    suspend fun replyToTicket(ticketId: UUID, senderId: UUID, senderType: com.bhanit.apps.echo.data.table.SenderType, content: String)
    suspend fun updateTicketStatus(ticketId: UUID, status: com.bhanit.apps.echo.data.model.TicketStatus)
    suspend fun updateTicketPriority(ticketId: UUID, priority: com.bhanit.apps.echo.data.model.TicketPriority)
    suspend fun getTicketReplies(ticketId: UUID): List<com.bhanit.apps.echo.data.model.TicketReplyDto>
    suspend fun getTicketStats(): Map<String, Int> // Optional: Breakdown by status/priority
    suspend fun checkTicketSpike(threshold: Int, durationMinutes: Long): Int // Returns count in last X mins
}
