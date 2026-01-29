package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.data.model.LoginRequest
import com.bhanit.apps.echo.data.model.VerifyOtpRequest
import com.bhanit.apps.echo.domain.service.AuthService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/send-otp") {
            val request = call.receive<LoginRequest>()

            // Let StatusPages handle exceptions
            val success = authService.sendOtp(request)
            if (success) {
                call.respond(mapOf("status" to "success"))
            } else {
                // If false returned without exception (unlikely with current service logic), throw generic error
                throw com.bhanit.apps.echo.core.domain.model.ServiceException(com.bhanit.apps.echo.core.util.ErrorCode.SERVER_ERROR)
            }
        }
        
        post("/verify-otp") {
            val request = call.receive<VerifyOtpRequest>()
            try {
                val response = authService.verifyOtp(request)
                call.respond(response)
            } catch (e: IllegalArgumentException) {
                call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Invalid OTP")
            }
        }
        
        authenticate("auth-jwt") {
            post("/logout") {
                val principal = call.principal<JWTPrincipal>()
                val id = principal?.payload?.getClaim("id")?.asString()
                
                if (id != null) {
                    authService.logout(id)
                    call.respondText("Logged out")
                } else {
                    call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Invalid Token")
                }
            }
        }
    }
}
