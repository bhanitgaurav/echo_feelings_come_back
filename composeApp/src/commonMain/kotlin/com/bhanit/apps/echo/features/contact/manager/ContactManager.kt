package com.bhanit.apps.echo.features.contact.manager

import kotlinx.coroutines.flow.Flow

data class DeviceContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val avatar: ByteArray? = null
)

enum class PermissionStatus {
    GRANTED,
    DENIED,
    DENIED_FOREVER
}

interface ContactManager {
    suspend fun getPermissionStatus(): PermissionStatus
    suspend fun fetchContacts(): List<DeviceContact>
    fun openSettings()
}

expect fun createContactManager(): ContactManager
