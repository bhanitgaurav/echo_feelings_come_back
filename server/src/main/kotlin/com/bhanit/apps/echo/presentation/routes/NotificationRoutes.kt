package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.domain.service.NotificationService
import com.bhanit.apps.echo.domain.repository.NotificationMessage
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.notificationRoutes(notificationService: NotificationService) {
    route("/notifications") {
        get("/in-app") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) } ?: return@get
            
            // Parse client date or default to Server Today
            val dateStr = call.request.queryParameters["date"]
            val date = try {
                if (dateStr != null) java.time.LocalDate.parse(dateStr) else java.time.LocalDate.now()
            } catch (e: Exception) {
                java.time.LocalDate.now()
            }

            val notification = notificationService.getInAppNotification(userId, date)
            if (notification != null) {
                // Derive sentiment/color key
                val sentiment = when {
                    notification.type.contains("POSITIVE") -> "POSITIVE"
                    notification.type.contains("HEAVY") -> "HEAVY"
                    notification.type.contains("DIFFICULT") -> "DIFFICULT"
                    notification.category == "STREAK" -> "STREAK"
                    else -> "NEUTRAL"
                }

                // Map to Client DTO
                call.respond(mapOf(
                    "id" to notification.id.toString(),
                    "category" to notification.category,
                    "type" to notification.type,
                    "message" to notification.messageTemplate,
                    "sentiment" to sentiment
                ))
            } else {
                call.respond(io.ktor.http.HttpStatusCode.NoContent)
            }
        }
    }
}
