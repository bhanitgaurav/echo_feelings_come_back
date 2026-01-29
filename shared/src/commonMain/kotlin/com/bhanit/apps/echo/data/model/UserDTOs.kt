package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val username: String? = null,
    val gender: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val allowQrAutoConnect: Boolean? = null
)

@Serializable
data class UserProfileResponse(
    val id: String,
    val phoneNumber: String,
    val fullName: String?,
    val username: String,
    val gender: String?,
    val email: String?,
    val photoUrl: String?,
    val credits: Int,
    val referralCode: String?,
    val referralUrl: String? = null,
    val allowQrAutoConnect: Boolean = true
)

@Serializable
data class BlockedUserResponse(
    val id: String,
    val username: String?,
    val phoneHash: String
)

@Serializable
data class DeviceSyncRequest(
    val platform: String,
    val appVersion: String,
    val osVersion: String,
    val deviceModel: String,
    val locale: String,
    val timezone: String
)
