package com.bhanit.apps.echo.data.service

import com.bhanit.apps.echo.domain.service.OtpSender
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class BaileysOtpSenderImpl(
    private val client: HttpClient
) : OtpSender {

    @Serializable
    private data class OtpRequest(
        val phone: String,
        val otp: String
    )

    override suspend fun sendOtp(phoneNumber: String, otp: String): Boolean {
        val baseUrl = System.getenv("BAILEYS_SERVICE_URL") ?: "http://localhost:3000"
        return try {
            // Assumes the sidecar is running on localhost:3000 or defined env var
            val response = client.post("$baseUrl/send-otp") {
                contentType(ContentType.Application.Json)
                setBody(OtpRequest(phone = phoneNumber, otp = otp))
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            // detailed stack trace only if it's NOT a connection refused error
            if (e.message?.contains("Failed to connect") == true || e is java.net.ConnectException) {
                println("INFO: WhatsApp Sidecar unreachable ($baseUrl). OTP will only be logged to console.")
            } else {
                e.printStackTrace()
            }
            false
        }
    }
}
