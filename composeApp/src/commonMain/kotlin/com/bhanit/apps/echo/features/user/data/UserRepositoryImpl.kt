package com.bhanit.apps.echo.features.user.data

import com.bhanit.apps.echo.data.model.BlockedUserResponse
import com.bhanit.apps.echo.data.model.UpdateProfileRequest
import com.bhanit.apps.echo.data.model.UserProfileResponse
import com.bhanit.apps.echo.features.user.domain.UserRepository
import com.bhanit.apps.echo.core.network.safeApiCall

class UserRepositoryImpl(private val api: UserApi) : UserRepository {
    override suspend fun getProfile(): Result<UserProfileResponse> {
        return safeApiCall {
            api.getProfile()
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): Result<Unit> {
        return safeApiCall {
            api.updateProfile(request)
            Unit
        }
    }

    override suspend fun getBlockedUsers(): Result<List<BlockedUserResponse>> {
         return safeApiCall {
            api.getBlockedUsers()
        }
    }

    override suspend fun blockUser(userId: String): Result<Unit> {
         return safeApiCall {
            api.blockUser(userId)
            Unit
        }
    }

    override suspend fun unblockUser(userId: String): Result<Unit> {
         return safeApiCall {
            api.unblockUser(userId)
            Unit
        }
    }

    override suspend fun completeOnboarding(username: String, referralCode: String?, photoUrl: String?): Result<Unit> {
        return safeApiCall {
            api.completeOnboarding(username, referralCode, photoUrl)
            Unit
        }
    }

    override suspend fun getCredits(): Result<Int> {
        return safeApiCall {
            api.getCredits()
        }
    }

    override suspend fun getMilestones(): Result<List<com.bhanit.apps.echo.data.model.MilestoneStatusDto>> {
        return safeApiCall {
            api.getMilestones()
        }
    }

    override suspend fun getCreditHistory(
        page: Int,
        pageSize: Int,
        query: String?,
        filter: com.bhanit.apps.echo.data.model.TransactionFilter?,
        startDate: Long?,
        endDate: Long?
    ): Result<List<com.bhanit.apps.echo.data.model.CreditTransaction>> {
        return safeApiCall {
            api.getCreditHistory(page, pageSize, query, filter, startDate, endDate)
        }
    }

    override suspend fun purchaseItem(request: com.bhanit.apps.echo.data.model.PurchaseRequest): Result<Unit> {
        return safeApiCall {
            api.purchaseItem(request)
            Unit
        }
    }

    override suspend fun saveFcmToken(token: String): Result<Unit> {
        return safeApiCall {
            api.saveFcmToken(token)
            Unit
        }
    }

    override suspend fun syncDeviceDetails(
        platform: String,
        appVersion: String,
        osVersion: String,
        deviceModel: String,
        locale: String,
        timezone: String
    ): Result<Unit> {
        return safeApiCall {
            api.syncDeviceDetails(
                com.bhanit.apps.echo.data.model.DeviceSyncRequest(
                    platform = platform,
                    appVersion = appVersion,
                    osVersion = osVersion,
                    deviceModel = deviceModel,
                    locale = locale,
                    timezone = timezone
                )
            )
            Unit
        }
    }
    
    override suspend fun generateUsername(inspiration: com.bhanit.apps.echo.shared.domain.model.Inspiration?): Result<String> {
        return safeApiCall {
            api.generateUsername(inspiration?.name)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return safeApiCall {
            api.deleteAccount()
            Unit
        }
    }

    override suspend fun updateNotificationSettings(
        feelings: Boolean?,
        checkins: Boolean?,
        reflections: Boolean?,
        rewards: Boolean?,
        inactiveReminders: Boolean?
    ): Result<Unit> {
        return safeApiCall {
            api.updateNotificationSettings(feelings, checkins, reflections, rewards, inactiveReminders)
            Unit
        }
    }
}
