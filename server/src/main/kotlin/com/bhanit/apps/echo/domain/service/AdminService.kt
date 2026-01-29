package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.data.service.JwtService
import com.bhanit.apps.echo.data.table.AdminRole
import com.bhanit.apps.echo.data.util.CryptoUtils
import com.bhanit.apps.echo.domain.repository.AdminRepository
import java.util.UUID

class AdminService(
    private val adminRepository: AdminRepository,
    private val jwtService: JwtService
) {
    suspend fun login(email: String, password: String, ip: String?): String? {
        val admin = adminRepository.findAdminByEmail(email) ?: return null
        
        // Check Password
        val hash = CryptoUtils.sha256(password)
        // Ideally checking against stored hash. 
        // Note: admin[AdminUsers.passwordHash] access requires ResultRow mapping or passing specific col.
        // Repository returns ResultRow.
        val storedHash = admin[com.bhanit.apps.echo.data.table.AdminUsers.passwordHash]
        
        if (hash == storedHash) {
            val id = admin[com.bhanit.apps.echo.data.table.AdminUsers.id].value
            val role = admin[com.bhanit.apps.echo.data.table.AdminUsers.role]
            
            if (!admin[com.bhanit.apps.echo.data.table.AdminUsers.isActive]) return null

            adminRepository.updateLastLogin(id, ip)
            
            // Log Audit
            adminRepository.logAudit(id, "LOGIN", null, "Admin Logged In", ip)
            
            // Generate Admin Token
            return jwtService.generateAdminToken(id, role.name) 
        }
        return null
    }

    suspend fun createInitialAdmin(email: String, password: String): UUID {
        // Only if no admins exist? Or used for seeding.
        // Password Hash
        val hash = CryptoUtils.sha256(password)
        return adminRepository.createAdmin(email, hash, AdminRole.ADMIN)
    }
    
    suspend fun createAdmin(creatorId: UUID, email: String, password: String, role: AdminRole): UUID {
        // Verify Creator is ADMIN (Done at Route level usually, but can double check if we fetch creator role)
        // ...
        val hash = CryptoUtils.sha256(password)
        val newId = adminRepository.createAdmin(email, hash, role)
        adminRepository.logAudit(creatorId, "CREATE_ADMIN", newId.toString(), "Created admin with role $role", null)
        return newId
    }

    suspend fun getAdmin(id: UUID) = adminRepository.getAdminById(id)
    
    suspend fun listAdmins() = adminRepository.listAdmins()
    
    suspend fun toggleAdminStatus(actorId: UUID, targetId: UUID, isActive: Boolean) {
        if (actorId == targetId) throw IllegalArgumentException("Cannot change your own status")
        adminRepository.updateAdminStatus(targetId, isActive)
        adminRepository.logAudit(actorId, "UPDATE_ADMIN_STATUS", targetId.toString(), "Set active to $isActive", null)
    }

    suspend fun logAction(adminId: UUID, action: String, targetId: String?, details: String?, ip: String?) {
        adminRepository.logAudit(adminId, action, targetId, details, ip)
    }
}
