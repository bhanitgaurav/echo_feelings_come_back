package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val phone: String
)

@Serializable
data class VerifyOtpRequest(
    val phone: String,
    val otp: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val isNewUser: Boolean,
    val isOnboardingCompleted: Boolean
)
