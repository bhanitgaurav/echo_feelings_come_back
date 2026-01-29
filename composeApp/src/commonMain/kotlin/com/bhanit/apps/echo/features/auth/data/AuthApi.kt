package com.bhanit.apps.echo.features.auth.data

import com.bhanit.apps.echo.data.model.AuthResponse
import com.bhanit.apps.echo.data.model.LoginRequest
import com.bhanit.apps.echo.data.model.VerifyOtpRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApi(
    private val client: HttpClient,
    private val BASE_URL: String
) {
    // private val BASE_URL = UrlConstants.BASE_URL // Removed hardcoded constant
    
    suspend fun sendOtp(phone: String) {
        client.post("$BASE_URL/auth/send-otp") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(phone))
        }
    }
    
    suspend fun verifyOtp(phone: String, otp: String): AuthResponse {
        return client.post("$BASE_URL/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(VerifyOtpRequest(phone, otp))
        }.body()
    }
    
    suspend fun logout() {
        client.post("$BASE_URL/auth/logout") {
            contentType(ContentType.Application.Json)
        }
    }
}
