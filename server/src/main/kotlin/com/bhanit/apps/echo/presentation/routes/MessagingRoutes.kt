package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.data.model.SendMessageRequest
import com.bhanit.apps.echo.domain.service.MessagingService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*

fun Route.messagingRoutes(messagingService: MessagingService) {
    route("/messages") {
        post("/send") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
            val request = call.receive<SendMessageRequest>()
            
            // Optional: Parse client date for timezone-accurate streaks
            val dateStr = call.request.queryParameters["localDate"]
            val clientDate = try {
                if (dateStr != null) java.time.LocalDate.parse(dateStr) else java.time.LocalDate.now()
            } catch (e: Exception) {
                java.time.LocalDate.now()
            }
            
            val messageId = messagingService.sendMessage(
                senderId = java.util.UUID.fromString(currentUserId),
                receiverId = java.util.UUID.fromString(request.receiverId),
                feeling = request.feeling,
                content = request.content,
                isAnonymous = request.isAnonymous,
                clientDate = clientDate
            )
            
            call.respond(mapOf("status" to "success", "messageId" to messageId.toString()))
        }

        post("/reply") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
            val request = call.receive<com.bhanit.apps.echo.data.model.ReplyMessageRequest>()

            val replyId = messagingService.replyToMessage(
                messageId = java.util.UUID.fromString(request.messageId),
                senderId = java.util.UUID.fromString(currentUserId),
                content = request.content
            )

            call.respond(mapOf("status" to "success", "replyId" to replyId.toString()))
        }

        get("/inbox") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            
            // multiple "feelings" params: ?feelings=LOVE&feelings=ANGER
            // multiple "feelings" params: ?feelings=LOVE&feelings=ANGER (now Strings)
            val feelings = call.request.queryParameters.getAll("feelings")
            
            val startDate = call.request.queryParameters["startDate"]?.toLongOrNull()
            val endDate = call.request.queryParameters["endDate"]?.toLongOrNull()
            
            val messages = messagingService.getInbox(
                java.util.UUID.fromString(currentUserId), 
                page, 
                limit, 
                feelings, 
                startDate, 
                endDate
            )
            call.respond(messages)
        }

        get("/history") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val receiverIdStr = call.request.queryParameters["receiverId"]
            val receiverId = if (receiverIdStr != null) java.util.UUID.fromString(receiverIdStr) else null
            val onlyReplied = call.request.queryParameters["onlyReplied"]?.toBoolean() ?: false

            val messages = messagingService.getSentMessages(java.util.UUID.fromString(currentUserId), receiverId, page, limit, onlyReplied)
            call.respond(messages)
        }
        
        get("/reply-status/{messageId}") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
            val messageIdStr = call.parameters["messageId"] ?: throw IllegalArgumentException("Missing messageId")
            
            val canReply = messagingService.canReply(
                java.util.UUID.fromString(messageIdStr),
                java.util.UUID.fromString(currentUserId)
            )
            
            call.respond(mapOf("canReply" to canReply))
        }

        get("/usage") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("id")?.asString() ?: throw IllegalStateException("User ID not found in token")
            val receiverIdStr = call.request.queryParameters["receiverId"] ?: throw IllegalArgumentException("Missing receiverId")

            val status = messagingService.getUsageStatus(
                java.util.UUID.fromString(currentUserId),
                java.util.UUID.fromString(receiverIdStr)
            )
            call.respond(status)
        }
    }
}
