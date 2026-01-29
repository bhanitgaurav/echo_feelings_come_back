package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.data.model.SupportRequest
import com.bhanit.apps.echo.domain.repository.SupportRepository
import java.util.UUID

class SupportService(private val supportRepository: SupportRepository) {

    suspend fun submitTicket(userId: UUID?, request: SupportRequest): UUID {
        val ticketId = supportRepository.createTicket(userId, request)
        
        // Trigger Email Notification (Mock)
        sendSupportEmail(userId, ticketId, request)
        
        return ticketId
    }

    suspend fun getTickets(userId: UUID, page: Int, limit: Int): List<com.bhanit.apps.echo.data.model.SupportTicketDto> {
        val offset = ((page - 1) * limit).toLong()
        return supportRepository.getTickets(userId, limit, offset)
    }

    private fun sendSupportEmail(userId: UUID?, ticketId: UUID, request: SupportRequest) {
        // In a real app, integrate with SendGrid, AWS SES, or just standard JavaMailSender
        println("========== [EMAIL TRIGGER] SUPPORT TICKET CREATED ==========")
        println("To: admin@echo.com")
        println("Subject: [${request.category}] New Ticket #$ticketId")
        println("From User: ${userId ?: "GUEST (${request.email ?: "No Email"})"}")
        println("Issue: ${request.subject}")
        println("Description: ${request.description}")
        println("============================================================")
    }
}
