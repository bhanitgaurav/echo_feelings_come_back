package com.bhanit.apps.echo.core.fcm

interface FcmTokenProvider {
    suspend fun getToken(): String?
}
