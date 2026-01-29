package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.data.model.AuthResponse
import com.bhanit.apps.echo.data.model.LoginRequest
import com.bhanit.apps.echo.data.model.VerifyOtpRequest

interface AuthService {
    suspend fun sendOtp(request: LoginRequest): Boolean
    suspend fun verifyOtp(request: VerifyOtpRequest): AuthResponse
    suspend fun logout(userId: String)
    suspend fun deleteAccount(userId: String)
    suspend fun cleanupExpiredOtps()
}
