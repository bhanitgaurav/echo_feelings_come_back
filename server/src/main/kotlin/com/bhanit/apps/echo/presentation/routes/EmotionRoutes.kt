package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.domain.service.EmotionService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.UUID

fun Route.emotionRoutes(emotionService: EmotionService) {
    route("/emotions") {
        get {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }
            
            if (userId != null) {
                val emotions = emotionService.getEmotionsForUser(userId)
                call.respond(emotions)
            } else {
                 // Fallback or Unauthorized, currently just public implementation logic if auth failed but route reached
                 // (Though this route is inside authenticate block usually)
                 call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Invalid User ID")
            }
        }
    }
}
