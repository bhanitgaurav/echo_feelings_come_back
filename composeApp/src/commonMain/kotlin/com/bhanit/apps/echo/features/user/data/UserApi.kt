package com.bhanit.apps.echo.features.user.data

import com.bhanit.apps.echo.data.model.BlockedUserResponse
import com.bhanit.apps.echo.data.model.UpdateProfileRequest
import com.bhanit.apps.echo.data.model.UserProfileResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class UserApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    // private val baseUrl = UrlConstants.BASE_URL // Removed hardcoded constant

    suspend fun getProfile(): UserProfileResponse {
        return client.get("$baseUrl/user/profile").body()
    }

    suspend fun updateProfile(request: UpdateProfileRequest) {
        client.put("$baseUrl/user/profile") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getBlockedUsers(): List<BlockedUserResponse> {
        return client.get("$baseUrl/user/blocks").body()
    }

    suspend fun blockUser(userId: String) {
        client.post("$baseUrl/user/block") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("userId" to userId))
        }
    }

    suspend fun unblockUser(userId: String) {
        client.delete("$baseUrl/user/block/$userId")
    }

    suspend fun completeOnboarding(username: String, referralCode: String?, photoUrl: String?) {
        val body = buildMap {
            put("username", username)
            if (referralCode != null) put("referralCode", referralCode)
            if (photoUrl != null) put("photoUrl", photoUrl)
        }
        client.post("$baseUrl/user/onboarding") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun getCredits(): Int {
        val map = client.get("$baseUrl/user/credits").body<Map<String, Int>>()
        return map["credits"] ?: 0
    }

    suspend fun getMilestones(): List<com.bhanit.apps.echo.data.model.MilestoneStatusDto> {
        return client.get("$baseUrl/user/credits/milestones").body()
    }

    suspend fun getCreditHistory(
        page: Int,
        pageSize: Int,
        query: String?,
        filter: com.bhanit.apps.echo.data.model.TransactionFilter?,
        startDate: Long?,
        endDate: Long?
    ): List<com.bhanit.apps.echo.data.model.CreditTransaction> {
        return client.get("$baseUrl/user/credits/history") {
            url {
                parameters.append("page", page.toString())
                parameters.append("limit", pageSize.toString())
                if (query != null) parameters.append("q", query)
                if (filter != null) parameters.append("filter", filter.name)
                if (startDate != null) parameters.append("startDate", startDate.toString())
                if (endDate != null) parameters.append("endDate", endDate.toString())
            }
        }.body()
    }

    suspend fun purchaseItem(request: com.bhanit.apps.echo.data.model.PurchaseRequest) {
        client.post("$baseUrl/user/purchase") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun saveFcmToken(token: String) {
        client.post("$baseUrl/user/fcm-token") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("token" to token))
        }
    }

    suspend fun syncDeviceDetails(request: com.bhanit.apps.echo.data.model.DeviceSyncRequest) {
        client.post("$baseUrl/user/device") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun generateUsername(inspiration: String?): String {
        val map = client.get("$baseUrl/user/generate-username") {
            url {
                if (inspiration != null) parameters.append("inspiration", inspiration)
            }
        }.body<Map<String, String>>()
        return map["username"] ?: throw Exception("No username returned")
    }

    suspend fun deleteAccount() {
        client.delete("$baseUrl/user/account")
    }
    
    suspend fun updateNotificationSettings(
        feelings: Boolean? = null,
        checkins: Boolean? = null,
        reflections: Boolean? = null,
        rewards: Boolean? = null,
        inactiveReminders: Boolean? = null
    ) {
        // Build map manually to avoid creating a new DTO class in shared module if not already there
        // Or if DTO exists, use it. Server defines it inline inside route.
        // For Client, we can just send Map or create a local DTO. DTO is safer.
        // Let's us Map for simplicity matching server reception logic if it was dynamic, 
        // BUT Server expects `NotificationSettingsRequest` Serializable.
        // We should add a Serializable DTO in this file or shared.
        // Since this file is in Feature layer, we can define a private DTO or just Map if server uses ContentNegotiation to parse Map -> Object (it doesn't automatically).
        // Server code used: call.receive<NotificationSettingsRequest>()
        // So we MUST send JSON matching that structure.
        
        @kotlinx.serialization.Serializable
        data class NotificationSettingsRequest(
            val feelings: Boolean? = null,
            val checkins: Boolean? = null,
            val reflections: Boolean? = null,
            val rewards: Boolean? = null,
            val inactiveReminders: Boolean? = null
        )
        
        client.post("$baseUrl/user/settings/notifications") {
            contentType(ContentType.Application.Json)
            setBody(NotificationSettingsRequest(feelings, checkins, reflections, rewards, inactiveReminders))
        }
    }
}
