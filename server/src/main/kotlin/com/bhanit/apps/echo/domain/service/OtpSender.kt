package com.bhanit.apps.echo.domain.service

interface OtpSender {
    suspend fun sendOtp(phoneNumber: String, otp: String): Boolean
}
