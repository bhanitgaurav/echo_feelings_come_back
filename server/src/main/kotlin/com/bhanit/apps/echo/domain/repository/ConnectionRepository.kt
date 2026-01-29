package com.bhanit.apps.echo.domain.repository

import com.bhanit.apps.echo.data.table.ConnectionStatus
import java.util.UUID

interface ConnectionRepository {
    suspend fun createConnectionRequest(fromUserId: UUID, toUserId: UUID)
    suspend fun createConnection(initiatorId: UUID, targetId: UUID, status: ConnectionStatus)
    suspend fun updateConnectionStatus(userA: UUID, userB: UUID, status: ConnectionStatus)
    suspend fun getConnectionStatus(userA: UUID, userB: UUID): ConnectionStatus?
    suspend fun getConnections(userId: UUID, limit: Int, offset: Int): List<org.jetbrains.exposed.sql.ResultRow>
    suspend fun getPendingRequests(userId: UUID): List<org.jetbrains.exposed.sql.ResultRow>
    suspend fun getSentRequests(userId: UUID): List<org.jetbrains.exposed.sql.ResultRow>
    suspend fun deleteConnection(userA: UUID, userB: UUID)
    suspend fun isBlocked(blockerId: UUID, blockedId: UUID): Boolean
    suspend fun areBlocked(userA: UUID, userB: UUID): Boolean
}
