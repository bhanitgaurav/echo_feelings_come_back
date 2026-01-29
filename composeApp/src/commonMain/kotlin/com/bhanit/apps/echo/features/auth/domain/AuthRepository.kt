package com.bhanit.apps.echo.features.auth.domain

import com.bhanit.apps.echo.data.model.AuthResponse

interface AuthRepository {
    suspend fun sendOtp(phone: String): Result<Boolean>
    suspend fun verifyOtp(phone: String, otp: String): Result<AuthResponse>
    suspend fun logout()
}
