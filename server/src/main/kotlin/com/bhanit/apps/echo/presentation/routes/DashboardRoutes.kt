package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.data.model.TimeRange
import com.bhanit.apps.echo.domain.repository.DailyStatsRepository
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.*
import java.util.UUID


import com.bhanit.apps.echo.domain.service.CreditService

fun Route.dashboardRoutes(
    dailyStatsRepository: DailyStatsRepository,
    creditService: CreditService
) {

    // authenticate("auth-jwt") block removed as it is handled in Application.kt
    get("/dashboard/analytics") {
        val principal = call.principal<JWTPrincipal>()
        val userIdStr = principal?.payload?.getClaim("id")?.asString()
        
        if (userIdStr == null) {
            call.respond(io.ktor.http.HttpStatusCode.Unauthorized)
            return@get
        }
        
        val userId = UUID.fromString(userIdStr)
        
        // Trigger Weekly Reflection Reward (asynchronously to not block main thread?)
        // Ktor handlers are suspending, so we can just call it. It's fast (one DB read/write).
        creditService.checkAndAwardWeeklyReflection(userId)

        val rangeParam = call.request.queryParameters["range"] ?: "WEEK"
        val timeRange = try {
            TimeRange.valueOf(rangeParam.uppercase())
        } catch (e: Exception) {
            TimeRange.WEEK
        }

        // Parse client date for timezone-accurate analytics
        val dateStr = call.request.queryParameters["localDate"]
        val clientDate = try {
            if (dateStr != null) java.time.LocalDate.parse(dateStr) else java.time.LocalDate.now()
        } catch (e: Exception) {
            java.time.LocalDate.now()
        }

        try {
            val analytics = dailyStatsRepository.getAnalytics(userId, timeRange, clientDate)
            call.respond(analytics)
        } catch (e: Exception) {
            call.respond(io.ktor.http.HttpStatusCode.InternalServerError, "Error fetching analytics")
        }
    }
}
