package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.data.model.SyncContactsRequest
import com.bhanit.apps.echo.domain.service.ContactService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.auth.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.contactRoutes(contactService: ContactService) {
    route("/contacts") {
        post("/sync") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("id")?.asString() // Optional for unauthenticated sync? 
            // Actually usually sync is authenticated if we want to persist. 
            // Schema implies user_id is required for UserContacts.
            // If principal is null, we can't persist.
            
            val request = call.receive<SyncContactsRequest>()
            val userIdUuid = currentUserId?.let { java.util.UUID.fromString(it) }
            
            val response = contactService.syncContacts(request, userIdUuid)
            call.respond(response)
        }

        get("/available") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found")
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val offset = (page - 1) * limit
            
            val contacts = contactService.getAvailableContacts(currentUserId, limit, offset)
            call.respond(contacts)
        }

        get("/search") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
            val query = call.request.queryParameters["query"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val offset = ((page - 1) * limit).toLong()

            if (query.isNullOrBlank()) {
                call.respond(emptyList<com.bhanit.apps.echo.data.model.ConnectionResponse>())
                return@get
            }

            val results = contactService.searchUsers(currentUserId, query, limit, offset)
            call.respond(results)
        }

        get("/connections") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val query = call.request.queryParameters["query"]
            val offset = (page - 1) * limit
            
            val connections = contactService.getConnections(currentUserId, limit, offset, query)
            call.respond(connections)
        }
        
        post("/connect/{userId}") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
            val targetId = call.parameters["userId"] ?: throw IllegalArgumentException("Missing userId")
            contactService.requestConnection(currentUserId, targetId)
            call.respond(io.ktor.http.HttpStatusCode.OK)
        }
        
        post("/connect/{userId}/block") {
             val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
             val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
             val targetId = call.parameters["userId"] ?: throw IllegalArgumentException("Missing userId")
             contactService.blockUser(currentUserId, targetId)
             call.respond(io.ktor.http.HttpStatusCode.OK)
        }

        post("/connect/{userId}/remove") {
             val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
             val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
             val targetId = call.parameters["userId"] ?: throw IllegalArgumentException("Missing userId")
             contactService.removeConnection(currentUserId, targetId)
             call.respond(io.ktor.http.HttpStatusCode.OK)
        }
        
        post("/accept/{userId}") {
             val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
             val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
             val targetId = call.parameters["userId"] ?: throw IllegalArgumentException("Missing userId")
             contactService.acceptConnection(targetId, currentUserId) // sender=target, acceptor=me
             call.respond(io.ktor.http.HttpStatusCode.OK)
        }
        
        post("/deny/{userId}") {
             val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
             val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
             val targetId = call.parameters["userId"] ?: throw IllegalArgumentException("Missing userId")
             contactService.denyConnection(targetId, currentUserId) // sender=target, denier=me
             call.respond(io.ktor.http.HttpStatusCode.OK)
        }
        
        get("/requests") {
             val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
             val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
             val requests = contactService.getPendingRequests(currentUserId)
             call.respond(requests)
        }
        
        get("/requests/sent") {
             val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
             val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
             val requests = contactService.getSentRequests(currentUserId)
             call.respond(requests)
        }

        post("/cancel/{userId}") {
             val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
             val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
             val targetId = call.parameters["userId"] ?: throw IllegalArgumentException("Missing userId")
             contactService.cancelConnectionRequest(currentUserId, targetId)
             call.respond(io.ktor.http.HttpStatusCode.OK)
        }

        post("/referral") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
            
            val params = call.receive<Map<String, String>>()
            val code = params["code"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            
            try {
                val result = contactService.connectByReferral(currentUserId, code)
                call.respond(result)
            } catch (e: IllegalArgumentException) {
                call.respond(io.ktor.http.HttpStatusCode.NotFound, "User not found")
            } catch (e: Exception) {
                call.respond(io.ktor.http.HttpStatusCode.InternalServerError)
            }
        }
    }
}
