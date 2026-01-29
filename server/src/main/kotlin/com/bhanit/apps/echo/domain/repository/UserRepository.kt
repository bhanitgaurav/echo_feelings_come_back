package com.bhanit.apps.echo.domain.repository

import com.bhanit.apps.echo.data.table.Users
import java.util.UUID

interface UserRepository {
    suspend fun findUserByPhone(phone: String): UUID?
    suspend fun findUsersByHashes(hashes: List<String>): List<org.jetbrains.exposed.sql.ResultRow>
    suspend fun createUser(phone: String): UUID
    suspend fun getUser(id: UUID): org.jetbrains.exposed.sql.ResultRow?
    suspend fun updateProfile(id: UUID, fullName: String?, username: String?, gender: String?, email: String?, photoUrl: String?, allowQrAutoConnect: Boolean? = null)
    suspend fun getBlockedUsers(userId: UUID): List<org.jetbrains.exposed.sql.ResultRow>
    suspend fun blockUser(blockerId: UUID, blockedId: UUID)
    suspend fun unblockUser(blockerId: UUID, blockedId: UUID)
    suspend fun incrementTokenVersion(userId: UUID): Int
    suspend fun getUserTokenVersion(userId: UUID): Int
    suspend fun findUsersByIds(ids: List<UUID>): List<org.jetbrains.exposed.sql.ResultRow>
    suspend fun isUsernameTaken(username: String): Boolean
    suspend fun saveFcmToken(userId: UUID, token: String)
    suspend fun getFcmToken(userId: UUID): String?
    suspend fun getFcmTokenAndPlatform(userId: UUID): Pair<String?, String?>
    suspend fun clearFcmToken(userId: UUID)
    suspend fun setUserInactive(userId: UUID)
    suspend fun deleteUser(userId: UUID)
    suspend fun completeOnboarding(userId: UUID, username: String, referralCode: String?, photoUrl: String?): Pair<Boolean, UUID?>
    suspend fun getCreditHistory(
        userId: UUID,
        page: Int,
        pageSize: Int,
        query: String?,
        filter: com.bhanit.apps.echo.data.model.TransactionFilter?,
        startDate: Long?,
        endDate: Long?
    ): List<com.bhanit.apps.echo.data.model.CreditTransaction>
    suspend fun purchaseItem(userId: UUID, request: com.bhanit.apps.echo.data.model.PurchaseRequest, cost: Int): Boolean
    suspend fun isOnboardingCompleted(userId: UUID): Boolean
    suspend fun updateConsent(userId: UUID)
    suspend fun resetUserData(userId: UUID): Boolean
    
    // Admin Methods
    suspend fun searchUsers(query: String?, limit: Int, offset: Long, includePhone: Boolean = false, exactMatch: Boolean = false, currentUserId: UUID? = null): List<org.jetbrains.exposed.sql.ResultRow>
    suspend fun getBlockedBy(userId: UUID): List<org.jetbrains.exposed.sql.ResultRow>
    suspend fun grantCredits(adminId: UUID, targetUserId: UUID, amount: Int, reason: String, notes: String?): UUID
    suspend fun getUserDetails(userId: UUID): org.jetbrains.exposed.sql.ResultRow?
    suspend fun findUserIdsByFilter(daysInactive: Int?, hasCompletedOnboarding: Boolean?, minVersion: String?, platform: String?): List<UUID>
    suspend fun updateDeviceMetadata(userId: UUID, platform: String, appVersion: String, osVersion: String, deviceModel: String, locale: String, timezone: String)
    suspend fun findUserByReferralCode(code: String): UUID?
    suspend fun getReferralBaseUrl(): String
    suspend fun recordTransaction(
        userId: UUID, 
        type: com.bhanit.apps.echo.data.model.TransactionType, 
        amount: Int, 
        relatedId: String? = null, 
        notes: String? = null,
        visibility: com.bhanit.apps.echo.data.table.TransactionVisibility = com.bhanit.apps.echo.data.table.TransactionVisibility.INTERNAL, 
        source: com.bhanit.apps.echo.data.table.RewardSource = com.bhanit.apps.echo.data.table.RewardSource.SYSTEM,
        intent: com.bhanit.apps.echo.data.table.TransactionIntent = com.bhanit.apps.echo.data.table.TransactionIntent.LOG,
        metadata: String? = null
    ): UUID
    suspend fun getActiveUsersWithMetadata(): List<Triple<UUID, String, String?>>
    suspend fun markSeasonAnnounced(userId: UUID, seasonId: String)
    suspend fun updateNotificationSettings(
        userId: UUID, 
        notifyFeelings: Boolean?, 
        notifyCheckins: Boolean?, 
        notifyReflections: Boolean?, 
        notifyRewards: Boolean?, 
        notifyInactiveReminders: Boolean?
    )
    suspend fun resetNotificationSettings(userId: UUID)
}
