package com.bhanit.apps.echo.core.permission

enum class PermissionStatus {
    GRANTED,
    DENIED_FOREVER,
    NOT_DETERMINED
}

interface PermissionHandler {
    suspend fun requestNotificationPermission()
    suspend fun hasNotificationPermission(): Boolean
    suspend fun getPermissionStatus(): PermissionStatus
    fun openAppSettings()
}
