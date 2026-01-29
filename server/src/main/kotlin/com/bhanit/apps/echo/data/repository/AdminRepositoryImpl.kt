package com.bhanit.apps.echo.data.repository

import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import com.bhanit.apps.echo.data.table.AdminUsers
import com.bhanit.apps.echo.data.table.AuditLogs
import com.bhanit.apps.echo.data.table.AdminRole
import com.bhanit.apps.echo.domain.repository.AdminRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class AdminRepositoryImpl : AdminRepository {
    override suspend fun findAdminByEmail(email: String): ResultRow? = dbQuery {
        AdminUsers.selectAll().where { AdminUsers.email eq email }.singleOrNull()
    }

    override suspend fun getAdminById(id: UUID): ResultRow? = dbQuery {
        AdminUsers.selectAll().where { AdminUsers.id eq id }.singleOrNull()
    }

    override suspend fun createAdmin(email: String, passwordHash: String, role: AdminRole): UUID = dbQuery {
        AdminUsers.insertAndGetId {
            it[AdminUsers.email] = email
            it[AdminUsers.passwordHash] = passwordHash
            it[AdminUsers.role] = role
            it[AdminUsers.isActive] = true
            it[AdminUsers.createdAt] = Instant.now()
        }.value
    }

    override suspend fun listAdmins(): List<ResultRow> = dbQuery {
        AdminUsers.selectAll().orderBy(AdminUsers.createdAt, SortOrder.DESC).toList()
    }

    override suspend fun updateAdminStatus(id: UUID, isActive: Boolean) {
        dbQuery {
            AdminUsers.update({ AdminUsers.id eq id }) {
                it[AdminUsers.isActive] = isActive
            }
        }
    }

    override suspend fun logAudit(adminId: UUID, action: String, targetId: String?, details: String?, ipAddress: String?) {
        dbQuery {
            AuditLogs.insert {
                it[AuditLogs.adminId] = adminId
                it[AuditLogs.action] = action
                it[AuditLogs.targetId] = targetId
                it[AuditLogs.details] = details
                it[AuditLogs.ipAddress] = ipAddress
                it[AuditLogs.createdAt] = Instant.now()
            }
        }
    }

    override suspend fun updateLastLogin(id: UUID, ip: String?) {
        dbQuery {
            AdminUsers.update({ AdminUsers.id eq id }) {
                it[AdminUsers.lastLoginAt] = Instant.now()
                if (ip != null) it[AdminUsers.lastLoginIp] = ip
            }
        }
    }
}
