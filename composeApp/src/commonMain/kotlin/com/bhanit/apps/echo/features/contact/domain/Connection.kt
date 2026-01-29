package com.bhanit.apps.echo.features.contact.domain

enum class ConnectionStatus {
    PENDING, PENDING_INCOMING, PENDING_OUTGOING, CONNECTED, BLOCKED, NONE, NOT_CONNECTED
}

data class Connection(
    val userId: String,
    val username: String,
    val phoneNumber: String, // Added field
    val status: ConnectionStatus,
    val avatarUrl: String? = null
)
