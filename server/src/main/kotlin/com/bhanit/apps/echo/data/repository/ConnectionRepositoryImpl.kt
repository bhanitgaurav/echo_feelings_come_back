package com.bhanit.apps.echo.data.repository

import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import com.bhanit.apps.echo.data.table.ConnectionStatus
import com.bhanit.apps.echo.data.table.Connections
import com.bhanit.apps.echo.domain.repository.ConnectionRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.Op
import java.time.Instant
import java.util.UUID

class ConnectionRepositoryImpl : ConnectionRepository {
    override suspend fun createConnectionRequest(fromUserId: UUID, toUserId: UUID) = dbQuery {
        // Sort IDs to ensure A < B uniqueness logic
        val idA = if (fromUserId < toUserId) fromUserId else toUserId
        val idB = if (fromUserId < toUserId) toUserId else fromUserId
        
        // Check if exists
        val exists = Connections.selectAll().where {
            (Connections.userA eq idA) and (Connections.userB eq idB)
        }.any()

        if (!exists) {
            Connections.insert {
                it[userA] = idA
                it[userB] = idB
                it[status] = ConnectionStatus.PENDING
                it[initiatedBy] = fromUserId
                it[createdAt] = Instant.now()
            }
            Unit
        }
    }

    override suspend fun createConnection(initiatorId: UUID, targetId: UUID, status: ConnectionStatus) = dbQuery {
        val idA = if (initiatorId < targetId) initiatorId else targetId
        val idB = if (initiatorId < targetId) targetId else initiatorId
        
        // Check if exists
        val exists = Connections.selectAll().where {
            (Connections.userA eq idA) and (Connections.userB eq idB)
        }.any()

        if (exists) {
            Connections.update({ (Connections.userA eq idA) and (Connections.userB eq idB) }) {
                it[Connections.status] = status
            }
        } else {
             Connections.insert {
                it[userA] = idA
                it[userB] = idB
                it[Connections.status] = status
                it[initiatedBy] = initiatorId
                it[createdAt] = Instant.now()
            }
        }
        Unit
    }

    override suspend fun updateConnectionStatus(userA: UUID, userB: UUID, status: ConnectionStatus) = dbQuery {
        try {
            val idA = if (userA < userB) userA else userB
            val idB = if (userA < userB) userB else userA

            Connections.update({ (Connections.userA eq idA) and (Connections.userB eq idB) }) {
                it[Connections.status] = status
            }
        } catch (e: Exception) {
            println("ERROR: ConnectionRepositoryImpl.updateConnectionStatus failed for $userA, $userB: ${e.message}")
            e.printStackTrace()
            throw e
        }
        Unit
    }

    override suspend fun getConnectionStatus(userA: UUID, userB: UUID): ConnectionStatus? = dbQuery {
        val idA = if (userA < userB) userA else userB
        val idB = if (userA < userB) userB else userA
        
        Connections.selectAll().where { (Connections.userA eq idA) and (Connections.userB eq idB) }
            .map { it[Connections.status] }
            .singleOrNull()
    }

    private fun getBlockedUserIds(userId: UUID): List<UUID> {
        return com.bhanit.apps.echo.data.table.UserBlocks.selectAll().where {
            (com.bhanit.apps.echo.data.table.UserBlocks.blockerId eq userId) or
            (com.bhanit.apps.echo.data.table.UserBlocks.blockedId eq userId)
        }.flatMap {
            listOf(
                it[com.bhanit.apps.echo.data.table.UserBlocks.blockerId].value,
                it[com.bhanit.apps.echo.data.table.UserBlocks.blockedId].value
            )
        }.filter { it != userId }.distinct()
    }

    override suspend fun getConnections(userId: UUID, limit: Int, offset: Int): List<org.jetbrains.exposed.sql.ResultRow> = dbQuery {
        val blockedIds = getBlockedUserIds(userId)
        
        var condition: Op<Boolean> = ((Connections.userA eq userId) or (Connections.userB eq userId)) and
                (Connections.status eq ConnectionStatus.CONNECTED)

        if (blockedIds.isNotEmpty()) {
            condition = condition and (Connections.userA notInList blockedIds) and (Connections.userB notInList blockedIds)
        }

        Connections.selectAll().where { condition }
        .limit(limit)
        .offset(offset.toLong())
        .toList()
    }

    override suspend fun getPendingRequests(userId: UUID): List<org.jetbrains.exposed.sql.ResultRow> = dbQuery {
        val blockedIds = getBlockedUserIds(userId)

        var condition: Op<Boolean> = ((Connections.userA eq userId) or (Connections.userB eq userId)) and
                (Connections.status eq ConnectionStatus.PENDING) and
                (Connections.initiatedBy neq userId)

        if (blockedIds.isNotEmpty()) {
            condition = condition and (Connections.userA notInList blockedIds) and (Connections.userB notInList blockedIds)
        }

        Connections.selectAll().where { condition }.toList()
    }

    override suspend fun getSentRequests(userId: UUID): List<org.jetbrains.exposed.sql.ResultRow> = dbQuery {
        val blockedIds = getBlockedUserIds(userId)
        
        var condition: Op<Boolean> = (Connections.initiatedBy eq userId) and
                (Connections.status eq ConnectionStatus.PENDING)
                
        // For sent requests, we need to ensure the RECIPIENT is not blocked.
        // Recipient is the 'other' user in (userA, userB).
        // Since we don't know if we are A or B for a specific row without checking, simpler to just check both A and B are not blocked.
        // One of them is US (and we are not in blockedIds), so this works safely.
        
        if (blockedIds.isNotEmpty()) {
            condition = condition and (Connections.userA notInList blockedIds) and (Connections.userB notInList blockedIds)
        }

        Connections.selectAll().where { condition }.toList()
    }

    override suspend fun deleteConnection(userA: UUID, userB: UUID) = dbQuery {
        val idA = if (userA < userB) userA else userB
        val idB = if (userA < userB) userB else userA
        
        Connections.deleteWhere {
            (Connections.userA eq idA) and (Connections.userB eq idB)
        }
        
        // REMOVED: UserBlocks deletion. 
        // deleteConnection should only remove the connection (Unfriend).
        // Unblocking is handled explicitly by UserRepository.unblockUser.
        
        Unit
    }

    override suspend fun isBlocked(blockerId: UUID, blockedId: UUID): Boolean = dbQuery {
        com.bhanit.apps.echo.data.table.UserBlocks.selectAll().where {
            (com.bhanit.apps.echo.data.table.UserBlocks.blockerId eq blockerId) and
            (com.bhanit.apps.echo.data.table.UserBlocks.blockedId eq blockedId)
        }.count() > 0
    }

    override suspend fun areBlocked(userA: UUID, userB: UUID): Boolean = dbQuery {
        com.bhanit.apps.echo.data.table.UserBlocks.selectAll().where {
            ((com.bhanit.apps.echo.data.table.UserBlocks.blockerId eq userA) and (com.bhanit.apps.echo.data.table.UserBlocks.blockedId eq userB)) or
            ((com.bhanit.apps.echo.data.table.UserBlocks.blockerId eq userB) and (com.bhanit.apps.echo.data.table.UserBlocks.blockedId eq userA))
        }.count() > 0
    }
}
