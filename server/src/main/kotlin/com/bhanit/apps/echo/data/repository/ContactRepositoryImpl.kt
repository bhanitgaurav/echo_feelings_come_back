package com.bhanit.apps.echo.data.repository

import com.bhanit.apps.echo.data.table.Connections
import com.bhanit.apps.echo.data.table.UserContacts
import com.bhanit.apps.echo.data.table.Users
import com.bhanit.apps.echo.domain.repository.AvailableContactRow
import com.bhanit.apps.echo.domain.repository.ContactRepository
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class ContactRepositoryImpl : ContactRepository {

    override suspend fun upsertContacts(userId: UUID, contacts: List<Pair<String, String?>>) {
        val targetUserId = userId // Rename to avoid shadowing/ambiguity in lambda
        transaction {
             // Delete existing
             // Exposed strict deleteWhere
             UserContacts.deleteWhere { UserContacts.userId eq targetUserId }
             
             // Insert new
             UserContacts.batchInsert(contacts) { (hash, name) ->
                 this[UserContacts.userId] = targetUserId
                 this[UserContacts.phoneHash] = hash
                 this[UserContacts.localName] = name
                 this[UserContacts.createdAt] = Instant.now()
             }
        }
    }

    override suspend fun getAvailableContacts(userId: UUID, limit: Int, offset: Int): List<AvailableContactRow> {
        val targetUserId = userId
        return try {
            transaction {
            // Join UserContacts with Users
            // We want to fetch columns from Users mostly
            
            val query = UserContacts.join(Users, JoinType.INNER, onColumn = UserContacts.phoneHash, otherColumn = Users.phoneHash)
                .selectAll()
                 .where {
                    (UserContacts.userId eq targetUserId) and 
                    (Users.isActive eq true) and
                    (Users.id neq targetUserId)
                }
                .limit(limit)
                .offset(offset.toLong())

            query.map { row ->
                val friendId = row[Users.id].value
                
                // Fetch connection status
                val connectionRow = Connections.selectAll().where {
                    ((Connections.userA eq targetUserId) and (Connections.userB eq friendId)) or
                    ((Connections.userA eq friendId) and (Connections.userB eq targetUserId))
                }.firstOrNull()

                val connectionStatus = connectionRow?.get(Connections.status)?.name
                val initiatedBy = connectionRow?.get(Connections.initiatedBy)
                
                AvailableContactRow(
                    id = friendId,
                    username = row[Users.username],
                    phoneNumber = row[Users.phoneNumber],
                    avatarUrl = row[Users.profilePhoto],
                    status = connectionStatus,
                    initiatedBy = initiatedBy?.value
                )
            }.filter { it.id != targetUserId }
            }.filter { it.id != targetUserId }
        } catch (e: Exception) {
            println("ERROR: ContactRepositoryImpl.getAvailableContacts failed: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
}
