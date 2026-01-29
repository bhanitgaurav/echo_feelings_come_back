package com.bhanit.apps.echo.features.auth.data

import com.bhanit.apps.echo.data.model.AuthResponse
import com.bhanit.apps.echo.features.auth.domain.AuthRepository
import com.bhanit.apps.echo.features.auth.domain.SessionRepository

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val sessionRepository: SessionRepository
) : AuthRepository {
    override suspend fun sendOtp(phone: String): Result<Boolean> {
        return com.bhanit.apps.echo.core.network.safeApiCall {
            api.sendOtp(phone)
            true
        }
    }

    override suspend fun verifyOtp(phone: String, otp: String): Result<AuthResponse> {
        return com.bhanit.apps.echo.core.network.safeApiCall {
            api.verifyOtp(phone, otp)
        }
    }

    override suspend fun logout() {
        try {
            api.logout()
        } catch (e: Exception) {
            // Ignore for now
        }
    }
}
