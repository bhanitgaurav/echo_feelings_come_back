package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncContactsRequest(
    val hashedNumbers: List<String>
)

@Serializable
data class SyncContactsResponse(
    val registeredUsers: List<UserSummary>,
    val inviteSuggestions: List<String> // Optional: Hashes that are not registered
)

@Serializable
data class UserSummary(
    val id: String,
    val username: String?,
    val phoneHash: String
)

@Serializable
data class ConnectionRequest(
    val targetUserId: String
)

@Serializable
data class ConnectionResponse(
    val userId: String,
    val username: String?,
    val phoneNumber: String, // Added field
    val status: String,
    val avatarUrl: String?
)

@Serializable
data class ReferralResponse(
    val status: String, // CONNECTED, PENDING, ALREADY_CONNECTED, REQUEST_SENT, SELF
    val userId: String,
    val username: String,
    val avatarUrl: String?
)
