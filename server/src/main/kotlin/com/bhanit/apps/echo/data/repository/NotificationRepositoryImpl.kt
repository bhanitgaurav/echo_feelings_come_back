package com.bhanit.apps.echo.data.repository

import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import com.bhanit.apps.echo.data.table.NotificationHistory
import com.bhanit.apps.echo.data.table.NotificationMessages
import com.bhanit.apps.echo.domain.repository.NotificationMessage
import com.bhanit.apps.echo.domain.repository.NotificationRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate
import java.util.UUID

class NotificationRepositoryImpl : NotificationRepository {

    override suspend fun getActiveMessages(category: String, type: String, isRotational: Boolean): List<NotificationMessage> = dbQuery {
        NotificationMessages.selectAll().where {
            (NotificationMessages.category eq category) and
            (NotificationMessages.type eq type) and
            (NotificationMessages.isRotational eq isRotational) and
            (NotificationMessages.isActive eq true)
        }.map {
            NotificationMessage(
                id = it[NotificationMessages.id].value,
                category = it[NotificationMessages.category],
                type = it[NotificationMessages.type],
                messageTemplate = it[NotificationMessages.messageTemplate],
                isRotational = it[NotificationMessages.isRotational]
            )
        }
    }

    override suspend fun getHistoryForToday(userId: UUID, category: String, date: LocalDate): List<UUID> = dbQuery {
        val today = date
        val yesterday = date.minusDays(1)
        
        (NotificationHistory innerJoin NotificationMessages)
            .select(NotificationHistory.messageId)
            .where {
                (NotificationHistory.userId eq userId) and
                ((NotificationHistory.sentAt.date() eq today) or (NotificationHistory.sentAt.date() eq yesterday)) and
                (NotificationMessages.category eq category)
            }
            .map { it[NotificationHistory.messageId].value }
    }

    override suspend fun getLastSentMessageId(userId: UUID, category: String, type: String): UUID? = dbQuery {
        (NotificationHistory innerJoin NotificationMessages)
            .select(NotificationHistory.messageId)
            .where {
                (NotificationHistory.userId eq userId) and
                (NotificationMessages.category eq category) and
                (NotificationMessages.type eq type)
            }
            .orderBy(NotificationHistory.sentAt to SortOrder.DESC)
            .limit(1)
            .map { it[NotificationHistory.messageId].value }
            .singleOrNull()
    }

    override suspend fun logNotification(userId: UUID, messageId: UUID): Unit = dbQuery {
        NotificationHistory.insert {
            it[this.userId] = userId
            it[this.messageId] = messageId
            it[this.sentAt] = java.time.Instant.now()
        }
    }

    override suspend fun seedMessages(messages: List<NotificationMessage>): Unit = dbQuery {
        messages.forEach { msg ->
            // Check if existsByKey (category + type + template) to avoid dupes on restart
            val exists = NotificationMessages.selectAll().where {
                (NotificationMessages.category eq msg.category) and
                (NotificationMessages.type eq msg.type) and
                (NotificationMessages.messageTemplate eq msg.messageTemplate)
            }.count() > 0

            if (!exists) {
                NotificationMessages.insert {
                    it[category] = msg.category
                    it[type] = msg.type
                    it[messageTemplate] = msg.messageTemplate
                    it[isRotational] = msg.isRotational
                    it[isActive] = true
                }
            } else {
                // Update existing record to sync properties like isRotational
                 NotificationMessages.update({
                    (NotificationMessages.category eq msg.category) and
                    (NotificationMessages.type eq msg.type) and
                    (NotificationMessages.messageTemplate eq msg.messageTemplate)
                }) {
                    it[isRotational] = msg.isRotational
                    it[isActive] = true
                }
            }
        }
    }
}
