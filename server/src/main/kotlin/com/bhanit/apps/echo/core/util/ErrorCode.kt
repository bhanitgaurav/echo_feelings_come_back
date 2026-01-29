package com.bhanit.apps.echo.core.util

enum class ErrorCode(val code: String, val message: String) {
    OTP_RATE_LIMIT_EXCEEDED("AUTH_001", "Please wait 30 seconds before requesting a new OTP."),
    OTP_MAX_ATTEMPTS_BLOCKED("AUTH_002", "Too many attempts. You are blocked for 5 minutes."),
    INVALID_OTP("AUTH_003", "Invalid OTP. Please try again."),
    SERVER_ERROR("GEN_001", "Something went wrong. Please try again.")
}
