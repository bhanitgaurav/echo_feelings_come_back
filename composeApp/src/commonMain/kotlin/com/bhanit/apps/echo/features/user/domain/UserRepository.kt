package com.bhanit.apps.echo.features.user.domain

import com.bhanit.apps.echo.data.model.BlockedUserResponse
import com.bhanit.apps.echo.data.model.UpdateProfileRequest
import com.bhanit.apps.echo.data.model.UserProfileResponse

interface UserRepository {
    suspend fun getProfile(): Result<UserProfileResponse>
    suspend fun updateProfile(request: UpdateProfileRequest): Result<Unit>
    suspend fun getBlockedUsers(): Result<List<BlockedUserResponse>>
    suspend fun blockUser(userId: String): Result<Unit>
    suspend fun unblockUser(userId: String): Result<Unit>
    suspend fun completeOnboarding(username: String, referralCode: String?, photoUrl: String? = null): Result<Unit>
    suspend fun getCredits(): Result<Int>
    suspend fun getMilestones(): Result<List<com.bhanit.apps.echo.data.model.MilestoneStatusDto>>
    suspend fun getCreditHistory(
        page: Int,
        pageSize: Int,
        query: String? = null,
        filter: com.bhanit.apps.echo.data.model.TransactionFilter? = com.bhanit.apps.echo.data.model.TransactionFilter.ALL,
        startDate: Long? = null,
        endDate: Long? = null
    ): Result<List<com.bhanit.apps.echo.data.model.CreditTransaction>>
    suspend fun purchaseItem(request: com.bhanit.apps.echo.data.model.PurchaseRequest): Result<Unit>
    suspend fun saveFcmToken(token: String): Result<Unit>
    suspend fun syncDeviceDetails(
        platform: String,
        appVersion: String,
        osVersion: String,
        deviceModel: String,
        locale: String,
        timezone: String
    ): Result<Unit>
    
    suspend fun generateUsername(inspiration: com.bhanit.apps.echo.shared.domain.model.Inspiration? = null): Result<String>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun updateNotificationSettings(
        feelings: Boolean? = null,
        checkins: Boolean? = null,
        reflections: Boolean? = null,
        rewards: Boolean? = null,
        inactiveReminders: Boolean? = null
    ): Result<Unit>
}
