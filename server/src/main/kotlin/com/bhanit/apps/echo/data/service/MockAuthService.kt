package com.bhanit.apps.echo.data.service

import com.bhanit.apps.echo.data.model.AuthResponse
import com.bhanit.apps.echo.data.model.LoginRequest
import com.bhanit.apps.echo.data.model.VerifyOtpRequest
import com.bhanit.apps.echo.domain.repository.UserRepository
import com.bhanit.apps.echo.domain.service.AuthService
import java.util.UUID

class MockAuthService(private val userRepository: UserRepository) : AuthService {
    private val otpStorage = mutableMapOf<String, String>() // Phone -> OTP

    override suspend fun sendOtp(request: LoginRequest): Boolean {
        // Mock: Always send 123456
        otpStorage[request.phone] = "123456"
        println("Mock OTP for ${request.phone}: 123456")
        return true
    }

    override suspend fun verifyOtp(request: VerifyOtpRequest): AuthResponse {
        val validOtp = otpStorage[request.phone]
        if (validOtp == request.otp) {
            otpStorage.remove(request.phone)
            
            var userId = userRepository.findUserByPhone(request.phone)
            var isNew = false
            
            if (userId == null) {
                userId = userRepository.createUser(request.phone)
                isNew = true
            }
            
            // TODO: Generate Real JWT
            val token = "mock.jwt.token.${userId}" 
            
            val isOnboardingCompleted = userRepository.isOnboardingCompleted(userId)
            
            return AuthResponse(token, userId.toString(), isNew, isOnboardingCompleted)
        } else {
            throw IllegalArgumentException("Invalid OTP")
        }
    }

    override suspend fun logout(userId: String) {
        // No-op for mock
    }

    override suspend fun deleteAccount(userId: String) {
        // No-op for mock
    }

    override suspend fun cleanupExpiredOtps() {
        // No-op for mock
        println("MockAuthService: cleanupExpiredOtps called")
    }
}
