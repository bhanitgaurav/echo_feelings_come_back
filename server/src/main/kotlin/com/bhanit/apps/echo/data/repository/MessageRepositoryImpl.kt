package com.bhanit.apps.echo.data.repository

import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import com.bhanit.apps.echo.data.model.MessageDTO
import com.bhanit.apps.echo.data.table.Messages
import com.bhanit.apps.echo.data.table.Replies
import com.bhanit.apps.echo.data.table.Users
import com.bhanit.apps.echo.domain.repository.MessageRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import java.time.Instant
import java.util.UUID

class MessageRepositoryImpl : MessageRepository {

    // Use alias to prevent column ambiguity (Messages.content vs Replies.content)
    private val RepliesAlias = Replies.alias("r")
    private val ReceiversAlias = Users.alias("receivers")

    override suspend fun sendMessage(
        senderId: UUID,
        receiverId: UUID,
        feeling: String,
        content: String,
        isAnonymous: Boolean
    ): UUID = dbQuery {
        Messages.insertAndGetId {
            it[Messages.senderId] = senderId
            it[Messages.receiverId] = receiverId
            it[Messages.feelingId] = feeling // Use feelingId (String)
            it[Messages.content] = content
            it[Messages.isAnonymous] = isAnonymous
            it[Messages.createdAt] = Instant.now()
        }.value
    }

    override suspend fun replyToMessage(
        messageId: UUID,
        senderId: UUID,
        content: String
    ): UUID = dbQuery {
        Replies.insertAndGetId {
            it[Replies.messageId] = messageId
            it[Replies.senderId] = senderId
            it[Replies.content] = content
            it[Replies.createdAt] = Instant.now()
        }.value
    }

    override suspend fun hasReplied(messageId: UUID): Boolean = dbQuery {
        !Replies.selectAll().where { Replies.messageId eq messageId }.empty()
    }

    override suspend fun getMessage(messageId: UUID): MessageDTO? = dbQuery {
        val row = Messages.innerJoin(Users, { Messages.senderId }, { Users.id })
            .join(RepliesAlias, JoinType.LEFT, Messages.id, RepliesAlias[Replies.messageId])
            .join(ReceiversAlias, JoinType.INNER, Messages.receiverId, ReceiversAlias[Users.id])
            .selectAll()
            .where { Messages.id eq messageId }
            .singleOrNull()
        row?.toMessageDTO()
    }

    override suspend fun getHistory(
        userA: UUID,
        userB: UUID,
        limit: Int,
        offset: Int
    ): List<MessageDTO> = dbQuery {
        val condition = (
            (Messages.senderId eq userA) and (Messages.receiverId eq userB)
        ) or (
            (Messages.senderId eq userB) and (Messages.receiverId eq userA)
        )

        val query = Messages.innerJoin(Users, { Messages.senderId }, { Users.id })
            .join(RepliesAlias, JoinType.LEFT, Messages.id, RepliesAlias[Replies.messageId])
            .join(ReceiversAlias, JoinType.INNER, Messages.receiverId, ReceiversAlias[Users.id])
            .selectAll()
            .where { condition }
            .orderBy(Messages.createdAt to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())

        query.map { it.toMessageDTO() }
    }



    override suspend fun getInbox(
        userId: UUID,
        limit: Int,
        offset: Int,
        feelings: List<String>?,
        startDate: Long?,
        endDate: Long?
    ): List<MessageDTO> = dbQuery {
        // Optimize Block Check: Don't fetch IDs. Filter in DB.
        // A user is "blocked" relative to me if:
        // 1. They blocked me (blocker = them, blocked = me)
        // 2. I blocked them (blocker = me, blocked = them)
        
        val blockedByMe = com.bhanit.apps.echo.data.table.UserBlocks
            .select(com.bhanit.apps.echo.data.table.UserBlocks.blockedId)
            .where { com.bhanit.apps.echo.data.table.UserBlocks.blockerId eq userId }
            .map { it[com.bhanit.apps.echo.data.table.UserBlocks.blockedId].value }
            
        val blockedMe = com.bhanit.apps.echo.data.table.UserBlocks
            .select(com.bhanit.apps.echo.data.table.UserBlocks.blockerId)
            .where { com.bhanit.apps.echo.data.table.UserBlocks.blockedId eq userId }
            .map { it[com.bhanit.apps.echo.data.table.UserBlocks.blockerId].value }

        val blockedIds = (blockedByMe + blockedMe).distinct()

        var condition: Op<Boolean> = Messages.receiverId eq userId
        
        // Exclude messages from people I blocked OR who blocked me
        if (blockedIds.isNotEmpty()) {
            condition = condition and (Messages.senderId notInList blockedIds)
        }
        
        if (!feelings.isNullOrEmpty()) {
            condition = condition and (Messages.feelingId inList feelings)
        }
        
        if (startDate != null) {
            condition = condition and (Messages.createdAt greaterEq Instant.ofEpochMilli(startDate))
        }
        
        if (endDate != null) {
            condition = condition and (Messages.createdAt lessEq Instant.ofEpochMilli(endDate))
        }

        // Optimize Select: Only fetch needed columns
        Messages.innerJoin(Users, { Messages.senderId }, { Users.id })
            .join(RepliesAlias, JoinType.LEFT, Messages.id, RepliesAlias[Replies.messageId])
            .join(ReceiversAlias, JoinType.INNER, Messages.receiverId, ReceiversAlias[Users.id])
            .select(Messages.columns + Users.username + RepliesAlias[Replies.content] + RepliesAlias[Replies.createdAt] + ReceiversAlias[Users.username])
            .where { condition }
            .orderBy(Messages.createdAt to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toMessageDTO() }
    }

    override suspend fun getSentMessages(
        userId: UUID,
        receiverId: UUID?,
        limit: Int,
        offset: Int,
        onlyReplied: Boolean
    ): List<MessageDTO> = dbQuery {
        val baseCondition = Messages.senderId eq userId
        val condition = if (receiverId != null) {
            baseCondition and (Messages.receiverId eq receiverId)
        } else {
            baseCondition
        }
        
        val replyJoinType = if (onlyReplied) JoinType.INNER else JoinType.LEFT

        Messages.innerJoin(Users, { Messages.senderId }, { Users.id })
            .join(RepliesAlias, replyJoinType, Messages.id, RepliesAlias[Replies.messageId])
            .join(ReceiversAlias, JoinType.INNER, Messages.receiverId, ReceiversAlias[Users.id])
            .selectAll()
            .where { condition }
            .orderBy(Messages.createdAt to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toMessageDTO() }
    }
    
    private fun ResultRow.toMessageDTO(): MessageDTO {
        // Use Alias to retrieve Reply info
        val replyContent = this.getOrNull(RepliesAlias[Replies.content])
        val replyTime = this.getOrNull(RepliesAlias[Replies.createdAt])?.toEpochMilli()
        val username = this[Users.username]
        val receiverName = this[ReceiversAlias[Users.username]]
        
        return MessageDTO(
            id = this[Messages.id].value.toString(),
            senderId = this[Messages.senderId].value.toString(),
            senderUsername = username,
            receiverId = this[Messages.receiverId].value.toString(),
            receiverUsername = receiverName,
            feeling = this[Messages.feelingId].toString(),
            content = this[Messages.content],
            isAnonymous = this[Messages.isAnonymous],
            timestamp = this[Messages.createdAt].toEpochMilli(),
            replyContent = replyContent,
            replyTimestamp = replyTime,
            isRead = this[Messages.isRead]
        )
    }

    override suspend fun getDailyMessageCount(senderId: UUID, receiverId: UUID, date: java.time.LocalDate): Long = dbQuery {
         val startOfDay = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
         val nextDay = date.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()

         Messages.selectAll().where {
             (Messages.senderId eq senderId) and
             (Messages.receiverId eq receiverId) and
             (Messages.createdAt greaterEq startOfDay) and
             (Messages.createdAt less nextDay)
         }.count()
    }

    override suspend fun getRelationLimit(senderId: UUID, receiverId: UUID): Int = dbQuery {
        val row = com.bhanit.apps.echo.data.table.UserRelationLimits
            .selectAll().where { 
                (com.bhanit.apps.echo.data.table.UserRelationLimits.senderId eq senderId) and 
                (com.bhanit.apps.echo.data.table.UserRelationLimits.receiverId eq receiverId) 
            }
            .singleOrNull()
            
        if (row == null) return@dbQuery 3 // Default
        
        val baseLimit = row[com.bhanit.apps.echo.data.table.UserRelationLimits.dailyLimit]
        val boostExpiresAt = row[com.bhanit.apps.echo.data.table.UserRelationLimits.boostExpiresAt]
        val boostCount = row[com.bhanit.apps.echo.data.table.UserRelationLimits.boostCount]
        
        val isBoostActive = boostExpiresAt != null && boostExpiresAt.isAfter(Instant.now())
        
        if (isBoostActive) baseLimit + boostCount else baseLimit
    }

    override suspend fun getUsersWithUnreadMessages(minAgeInHours: Int): List<UUID> = dbQuery {
        val cutoff = Instant.now().minusSeconds(minAgeInHours * 3600L)
        
        Messages.selectAll()
            .where { 
                (Messages.isRead eq false) and 
                (Messages.createdAt lessEq cutoff) 
            }
            .withDistinct()
            .map { it[Messages.receiverId].value }
    }
}
