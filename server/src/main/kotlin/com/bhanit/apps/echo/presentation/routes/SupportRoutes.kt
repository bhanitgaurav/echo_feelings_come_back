package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.data.model.SupportRequest
import com.bhanit.apps.echo.data.model.SupportResponse
import com.bhanit.apps.echo.domain.service.SupportService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.supportRoutes(supportService: SupportService) {
    // Public Route for Guests
    post("/public/support") {
        val request = call.receive<com.bhanit.apps.echo.data.model.PublicSupportRequest>()
        
        if (request.email.isBlank() || request.subject.isBlank() || request.description.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Email, Subject and Description cannot be empty")
            return@post
        }

        // Convert Public Request to Internal Request
        val internalRequest = SupportRequest(
            category = request.category,
            subject = request.subject,
            description = request.description,
            email = request.email
        )

        val ticketId = supportService.submitTicket(null, internalRequest)
        
        call.respond(HttpStatusCode.Created, SupportResponse(ticketId.toString(), "Ticket created successfully"))
    }

    // Authenticated Routes for App
    authenticate("auth-jwt") {
        post("/support") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }
            
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid User")
                return@post
            }

            val request = call.receive<SupportRequest>()
            
            if (request.subject.isBlank() || request.description.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Subject and Description cannot be empty")
                return@post
            }

            val ticketId = supportService.submitTicket(userId, request)
            
            call.respond(HttpStatusCode.Created, SupportResponse(ticketId.toString(), "Ticket created successfully"))
        }

        get("/support") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }

            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid User")
                return@get
            }

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            
            val tickets = supportService.getTickets(userId, page, limit)
            
            call.respond(HttpStatusCode.OK, com.bhanit.apps.echo.data.model.SupportHistoryResponse(tickets))
        }
    }
}
