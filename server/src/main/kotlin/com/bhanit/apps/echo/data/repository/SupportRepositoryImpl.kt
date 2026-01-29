package com.bhanit.apps.echo.data.repository

import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import com.bhanit.apps.echo.data.model.SupportRequest
import com.bhanit.apps.echo.data.table.SupportTickets
import com.bhanit.apps.echo.domain.repository.SupportRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.UUID

class SupportRepositoryImpl : SupportRepository {
    override suspend fun createTicket(userId: UUID?, request: SupportRequest): UUID = dbQuery {
        SupportTickets.insertAndGetId {
            it[this.userId] = userId
            it[contactEmail] = request.email // Save email if provided (guest or user)
            it[category] = com.bhanit.apps.echo.data.model.TicketCategory.valueOf(request.category.name)
            it[subject] = request.subject
            it[description] = request.description
            it[status] = com.bhanit.apps.echo.data.model.TicketStatus.OPEN
            it[priority] = com.bhanit.apps.echo.data.model.TicketPriority.MEDIUM
            it[createdAt] = Instant.now()
        }.value
    }

    override suspend fun getTickets(userId: UUID, limit: Int, offset: Long): List<com.bhanit.apps.echo.data.model.SupportTicketDto> = dbQuery {
        SupportTickets.selectAll().where { SupportTickets.userId eq userId }
            .orderBy(SupportTickets.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(limit, offset = offset)
            .map {
                com.bhanit.apps.echo.data.model.SupportTicketDto(
                    id = it[SupportTickets.id].value.toString(),
                    userId = it[SupportTickets.userId]?.value?.toString(),
                    contactEmail = it[SupportTickets.contactEmail],
                    category = it[SupportTickets.category],
                    subject = it[SupportTickets.subject],
                    description = it[SupportTickets.description],
                    status = it[SupportTickets.status],
                    priority = it[SupportTickets.priority],
                    createdAt = it[SupportTickets.createdAt].toEpochMilli(),
                    updatedAt = it[SupportTickets.updatedAt].toEpochMilli()
                )
            }
    }

    override suspend fun getAllTickets(status: String?, priority: String?, limit: Int, offset: Long): List<com.bhanit.apps.echo.data.model.SupportTicketDto> = dbQuery {
        val query = SupportTickets.selectAll()
        
        if (!status.isNullOrBlank()) {
             try {
                 val statusEnum = com.bhanit.apps.echo.data.model.TicketStatus.valueOf(status)
                 query.adjustWhere {
                     val old = this
                     val op = SupportTickets.status eq statusEnum
                     old?.and(op) ?: op
                 }
             } catch (e: Exception) { }
        }
        
        if (!priority.isNullOrBlank()) {
             try {
                 val priorityEnum = com.bhanit.apps.echo.data.model.TicketPriority.valueOf(priority)
                 query.adjustWhere {
                     val old = this
                     val op = SupportTickets.priority eq priorityEnum
                     old?.and(op) ?: op
                 }
             } catch (e: Exception) { }
        }
        
        query
            .orderBy(SupportTickets.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(limit, offset = offset)
            .map {
                com.bhanit.apps.echo.data.model.SupportTicketDto(
                    id = it[SupportTickets.id].value.toString(),
                    userId = it[SupportTickets.userId]?.value?.toString(),
                    contactEmail = it[SupportTickets.contactEmail],
                    category = it[SupportTickets.category],
                    subject = it[SupportTickets.subject],
                    description = it[SupportTickets.description],
                    status = it[SupportTickets.status],
                    priority = it[SupportTickets.priority],
                    createdAt = it[SupportTickets.createdAt].toEpochMilli(),
                    updatedAt = it[SupportTickets.updatedAt].toEpochMilli()
                )
            }
    }

    override suspend fun getTicketStats(): Map<String, Int> = dbQuery {
        // Simple breakdown by status
        // More complex group by queries if needed. 
        // For simple dashboard: Total Open, Total Critical.
        val openCount = SupportTickets.select(SupportTickets.id).where { SupportTickets.status eq com.bhanit.apps.echo.data.model.TicketStatus.OPEN }.count()
        val criticalCount = SupportTickets.select(SupportTickets.id).where { SupportTickets.priority eq com.bhanit.apps.echo.data.model.TicketPriority.CRITICAL }.count()
        
        mapOf(
            "open" to openCount.toInt(),
            "critical" to criticalCount.toInt()
        )
    }

    override suspend fun checkTicketSpike(threshold: Int, durationMinutes: Long): Int = dbQuery {
        val cutoff = java.time.Instant.now().minusSeconds(durationMinutes * 60)
        SupportTickets.select(SupportTickets.id)
            .where { SupportTickets.createdAt greaterEq cutoff }
            .count().toInt()
    }

    override suspend fun getTicketDetails(ticketId: UUID): com.bhanit.apps.echo.data.model.SupportTicketDto? = dbQuery {
        SupportTickets.selectAll().where { SupportTickets.id eq ticketId }
            .map {
                com.bhanit.apps.echo.data.model.SupportTicketDto(
                    id = it[SupportTickets.id].value.toString(),
                    userId = it[SupportTickets.userId]?.value?.toString(),
                    contactEmail = it[SupportTickets.contactEmail],
                    category = it[SupportTickets.category],
                    subject = it[SupportTickets.subject],
                    description = it[SupportTickets.description],
                    status = it[SupportTickets.status],
                    priority = it[SupportTickets.priority],
                    createdAt = it[SupportTickets.createdAt].toEpochMilli(),
                    updatedAt = it[SupportTickets.updatedAt].toEpochMilli()
                )
            }
            .singleOrNull()
    }

    override suspend fun replyToTicket(ticketId: UUID, senderId: UUID, senderType: com.bhanit.apps.echo.data.table.SenderType, content: String) {
        dbQuery {
            val currentStatus = SupportTickets.select(SupportTickets.status)
                .where { SupportTickets.id eq ticketId }
                .singleOrNull()?.get(SupportTickets.status)

            if (currentStatus == com.bhanit.apps.echo.data.model.TicketStatus.CLOSED) {
                throw IllegalStateException("Cannot reply to a CLOSED ticket.")
            }

            com.bhanit.apps.echo.data.table.TicketReplies.insert {
                it[this.ticketId] = ticketId
                it[this.senderId] = senderId
                it[this.senderType] = senderType
                it[this.content] = content
                it[this.createdAt] = Instant.now()
            }
            
            // Validate Update Status if needed (e.g., if Admin replies -> pending user action? Or simple reply)
            SupportTickets.update({ SupportTickets.id eq ticketId }) {
                it[updatedAt] = Instant.now()
            }
        }
    }

    override suspend fun updateTicketStatus(ticketId: UUID, status: com.bhanit.apps.echo.data.model.TicketStatus) {
        dbQuery {
            val currentStatus = SupportTickets.select(SupportTickets.status)
                .where { SupportTickets.id eq ticketId }
                .singleOrNull()?.get(SupportTickets.status)

            if (currentStatus == com.bhanit.apps.echo.data.model.TicketStatus.CLOSED) {
                throw IllegalStateException("Cannot modify a CLOSED ticket.")
            }

            SupportTickets.update({ SupportTickets.id eq ticketId }) {
                it[this.status] = status
                it[updatedAt] = Instant.now()
            }
        }
    }

    override suspend fun updateTicketPriority(ticketId: UUID, priority: com.bhanit.apps.echo.data.model.TicketPriority) {
        dbQuery {
            val currentStatus = SupportTickets.select(SupportTickets.status)
                .where { SupportTickets.id eq ticketId }
                .singleOrNull()?.get(SupportTickets.status)

            if (currentStatus == com.bhanit.apps.echo.data.model.TicketStatus.CLOSED) {
                throw IllegalStateException("Cannot modify a CLOSED ticket.")
            }

            SupportTickets.update({ SupportTickets.id eq ticketId }) {
                it[this.priority] = priority
                it[updatedAt] = Instant.now()
            }
        }
    }

    override suspend fun getTicketReplies(ticketId: UUID): List<com.bhanit.apps.echo.data.model.TicketReplyDto> = dbQuery {
        com.bhanit.apps.echo.data.table.TicketReplies.selectAll()
            .where { com.bhanit.apps.echo.data.table.TicketReplies.ticketId eq ticketId }
            .orderBy(com.bhanit.apps.echo.data.table.TicketReplies.createdAt to org.jetbrains.exposed.sql.SortOrder.ASC)
            .map {
                com.bhanit.apps.echo.data.model.TicketReplyDto(
                    id = it[com.bhanit.apps.echo.data.table.TicketReplies.id].value.toString(),
                    ticketId = it[com.bhanit.apps.echo.data.table.TicketReplies.ticketId].value.toString(),
                    senderId = it[com.bhanit.apps.echo.data.table.TicketReplies.senderId].toString(),
                    senderType = it[com.bhanit.apps.echo.data.table.TicketReplies.senderType].name,
                    content = it[com.bhanit.apps.echo.data.table.TicketReplies.content],
                    createdAt = it[com.bhanit.apps.echo.data.table.TicketReplies.createdAt].toEpochMilli()
                )
            }
    }
}
