package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.data.table.ContentType
import com.bhanit.apps.echo.domain.service.ContentService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.contentRoutes(contentService: ContentService) {
    route("/content") {
        // Public Endpoint
        get("/{type}") {
            val typeStr = call.parameters["type"]?.uppercase()
            try {
                val type = ContentType.valueOf(typeStr ?: "")
                val content = contentService.getContent(type)
                call.respond(mapOf("content" to content))
            } catch (e: IllegalArgumentException) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, "Invalid content type")
            }
        }
    }
}

fun Route.adminContentRoutes(contentService: ContentService) {
     route("/content") {
        post {
            // Admin only - validation relies on 'authenticate("auth-admin-jwt")' wrapper in Application.kt
            val params = call.receive<Map<String, String>>()
            val typeStr = params["type"]?.uppercase()
            val content = params["content"]
            
            if (typeStr != null && content != null) {
                try {
                     val type = ContentType.valueOf(typeStr)
                     contentService.updateContent(type, content)
                     call.respond(io.ktor.http.HttpStatusCode.OK, "Updated")
                } catch (e: IllegalArgumentException) {
                     call.respond(io.ktor.http.HttpStatusCode.BadRequest, "Invalid type")
                }
            } else {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, "Missing type or content")
            }
        }
     }
}
