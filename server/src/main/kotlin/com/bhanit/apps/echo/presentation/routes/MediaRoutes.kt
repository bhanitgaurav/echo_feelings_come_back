package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.domain.service.CloudinaryService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.mediaRoutes(cloudinaryService: CloudinaryService) {
    authenticate("auth-jwt") {
        route("/media") {
            get("/params") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asString() 
                
                if (userId == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Unauthorized,
                        com.bhanit.apps.echo.data.model.ApiErrorDTO(
                            errorCode = "UNAUTHORIZED",
                            errorMessage = "User not authorized",
                            errorDescription = "Missing or invalid user ID claim"
                        )
                    )
                    return@get
                }
                
                // Let StatusPages handle exceptions (returns ApiErrorDTO)
                val params = cloudinaryService.generateUploadParams(userId)
                call.respond(params)
            }
        }
    }
}
