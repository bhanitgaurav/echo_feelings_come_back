package com.bhanit.apps.echo.domain.repository

import com.bhanit.apps.echo.data.table.AdminRole
import org.jetbrains.exposed.sql.ResultRow
import java.util.UUID

interface AdminRepository {
    suspend fun findAdminByEmail(email: String): ResultRow?
    suspend fun getAdminById(id: UUID): ResultRow?
    suspend fun createAdmin(email: String, passwordHash: String, role: AdminRole): UUID
    suspend fun listAdmins(): List<ResultRow>
    suspend fun updateAdminStatus(id: UUID, isActive: Boolean)
    suspend fun logAudit(adminId: UUID, action: String, targetId: String?, details: String?, ipAddress: String?)
    suspend fun updateLastLogin(id: UUID, ip: String?)
}
