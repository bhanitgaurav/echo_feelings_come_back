package com.bhanit.apps.echo.features.contact.domain

data class Contact(
    val name: String,
    val phoneNumber: String,
    val isRegistered: Boolean = false,
    val userId: String? = null,
    val photoUrl: String? = null,
    val status: String? = null // PENDING_INCOMING, PENDING_OUTGOING, CONNECTED, NOT_CONNECTED
)
