package com.bhanit.apps.echo.core.base

expect class BiometricManager {
    suspend fun canAuthenticate(): Boolean
    suspend fun authenticate(title: String, subtitle: String): Boolean
}
