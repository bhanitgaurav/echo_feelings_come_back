package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.domain.service.HabitService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

import com.bhanit.apps.echo.domain.repository.UserRepository
import com.bhanit.apps.echo.data.table.Users

fun Route.habitRoutes(habitService: HabitService, userRepository: UserRepository) {
    route("/habits") {
        // Record Activity (e.g. App Open)
        post("/activity") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) } ?: return@post
            
            val params = call.receive<Map<String, String>>()
            val typeStr = params["type"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest, "Missing type")
            
            try {
                val type = HabitService.ActivityType.valueOf(typeStr)
                val dateStr = params["date"]
                
                val activityDate = if (dateStr != null) {
                    java.time.LocalDate.parse(dateStr) 
                } else {
                    val user = userRepository.getUser(java.util.UUID.fromString(userId.toString()))
                    val zoneId = try {
                         if (user?.get(Users.timezone) != null) java.time.ZoneId.of(user[Users.timezone]) else java.time.ZoneId.systemDefault()
                    } catch (e: Exception) { java.time.ZoneId.systemDefault() }
                    java.time.LocalDate.now(zoneId)
                }
                
                // "MESSAGE_SENT" should generally be called internally, but maybe client wants to force sync?
                // Letting client record OPEN events.
                if (type == HabitService.ActivityType.MESSAGE_SENT) {
                     return@post call.respond(io.ktor.http.HttpStatusCode.Forbidden, "Message activity is recorded automatically.")
                }
                
                habitService.recordActivity(userId, type, date = activityDate)
                call.respond(mapOf("status" to "success"))
            } catch (e: IllegalArgumentException) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, "Invalid activity type")
            }
        }

        // Get Status (Streaks)
        get("/status") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) } ?: return@get
            
            val status = habitService.getHabitStatus(userId)
            if (status != null) {
                call.respond(status)
            } else {
                // Return zeros if no record
                // Return zeros if no record
                call.respond(com.bhanit.apps.echo.domain.repository.UserStreakData(
                    userId = userId,
                    presenceStreak = 0,
                    presenceLastActiveAt = null,
                    kindnessStreak = 0,
                    kindnessLastActiveAt = null,
                    responseStreak = 0,
                    responseLastActiveAt = null,
                    gracePeriodUsedAt = null,
                    updatedAt = java.time.Instant.now()
                ))
            }
        }
    }
}
