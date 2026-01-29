package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.data.service.RealAuthService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

fun Route.debugRoutes(authService: RealAuthService, userRepository: com.bhanit.apps.echo.domain.repository.UserRepository) {
    route("/otps") {
        get("") {
            // In a real app, protect this with a specific header or check environment
            val otps = authService.getActiveOtps()
            call.respond(otps.map { mapOf("phone" to it.key, "otp" to it.value) })
        }
    }

    route("/debug/notify") {
        post("") {
            val params = call.receive<Map<String, String>>()
            val targetId = params["userId"]?.let { java.util.UUID.fromString(it) }
            val title = params["title"] ?: "Debug Test"
            val body = params["body"] ?: "This is a test notification"
            
            if (targetId == null) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, "Missing userId")
                return@post
            }
            
            val userRepo = com.bhanit.apps.echo.data.repository.UserRepositoryImpl()
            val (token, platform) = userRepo.getFcmTokenAndPlatform(targetId)
            
            if (token != null) {
                val result = com.bhanit.apps.echo.domain.service.FCMService.sendNotification(
                    token = token,
                    title = title,
                    body = body,
                    data = mapOf("type" to "DEBUG"),
                    platform = platform
                )
                call.respond(mapOf(
                    "status" to "success", 
                    "platform" to (platform ?: "null"),
                    "token" to token,
                    "result" to result.toString()
                ))
            } else {
                call.respond(io.ktor.http.HttpStatusCode.NotFound, mapOf("error" to "User has no FCM token"))
            }
        }
    }
    
    get("/check_user") {
        val phone = call.parameters["phone"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
        
        val variants = mutableListOf<String>()
        variants.add(phone)
        if (!phone.startsWith("+")) variants.add("+$phone")
        if (!phone.startsWith("+91")) variants.add("+91$phone")
        val digits = phone.filter { it.isDigit() }
        variants.add(digits)
        variants.add("+91$digits")
        
        var foundId: java.util.UUID? = null
        for (variant in variants.distinct()) {
            val id = userRepository.findUserByPhone(variant)
            if (id != null) {
                foundId = id
                break
            }
        }
        
        if (foundId != null) {
            call.respond(mapOf("userId" to foundId.toString()))
        } else {
            call.respond(io.ktor.http.HttpStatusCode.NotFound, mapOf("error" to "User not found for phone $phone"))
        }
    }
    
    post("/wipe_data") {
        // TRUNCATE ALL DATA
        org.jetbrains.exposed.sql.transactions.transaction {
            com.bhanit.apps.echo.data.table.Connections.deleteAll()
            com.bhanit.apps.echo.data.table.UserContacts.deleteAll()
            com.bhanit.apps.echo.data.table.Messages.deleteAll()
            com.bhanit.apps.echo.data.table.Otps.deleteAll() // Optional, but clean
            com.bhanit.apps.echo.data.table.DailyStats.deleteAll()
            com.bhanit.apps.echo.data.table.Users.deleteAll()
        }
        call.respondText("All Data Wiped Successfully")
    }

    get("/dump_data") {
        val users = org.jetbrains.exposed.sql.transactions.transaction {
            com.bhanit.apps.echo.data.table.Users.selectAll().map { 
                "${it[com.bhanit.apps.echo.data.table.Users.phoneNumber]} (Hash: ${it[com.bhanit.apps.echo.data.table.Users.phoneHash]})"
            }
        }
        val contacts = org.jetbrains.exposed.sql.transactions.transaction {
            com.bhanit.apps.echo.data.table.UserContacts.selectAll().map {
                "User: ${it[com.bhanit.apps.echo.data.table.UserContacts.userId]} -> Hash: ${it[com.bhanit.apps.echo.data.table.UserContacts.phoneHash]}"
            }
        }
        call.respondText("USERS:\n${users.joinToString("\n")}\n\nUSER_CONTACTS:\n${contacts.joinToString("\n")}")
    }

    get("/test_query") {
        try {
            val userIdStr = call.parameters["userId"] ?: return@get call.respondText("Provide userId param")
            val userId = java.util.UUID.fromString(userIdStr)

            // Replicating ContactRepositoryImpl logic EXACTLY
             val result = org.jetbrains.exposed.sql.transactions.transaction {
                com.bhanit.apps.echo.data.table.UserContacts.join(
                    com.bhanit.apps.echo.data.table.Users, 
                    org.jetbrains.exposed.sql.JoinType.INNER, 
                    onColumn = com.bhanit.apps.echo.data.table.UserContacts.phoneHash, 
                    otherColumn = com.bhanit.apps.echo.data.table.Users.phoneHash
                )
                .selectAll()
                 .where {
                    (com.bhanit.apps.echo.data.table.UserContacts.userId eq userId) and 
                    (com.bhanit.apps.echo.data.table.Users.isActive eq true) and
                    (com.bhanit.apps.echo.data.table.Users.id neq userId)
                }
                .map { 
                    "Found: ${it[com.bhanit.apps.echo.data.table.Users.phoneNumber]} (ID: ${it[com.bhanit.apps.echo.data.table.Users.id]})"
                }
            }
            call.respondText("SQL Query Result: ${result.joinToString(", ")}")
        } catch (e: Exception) {
            e.printStackTrace()
            call.respondText("Error: ${e.message}\nStack: ${e.stackTraceToString()}")
        }
    }

    get("/test_service_available") {
        try {
             val userIdStr = call.parameters["userId"] ?: return@get call.respondText("Provide userId param")
             val userId = java.util.UUID.fromString(userIdStr)
             
             // Call repository directly to utilize the specific logic we fixed
             val repo = com.bhanit.apps.echo.data.repository.ContactRepositoryImpl()
             val result = repo.getAvailableContacts(userId, 100, 0)
             
             call.respondText("Repo Result: ${result.joinToString("\n") { "ID: ${it.id}, Name: ${it.username}, Phone: ${it.phoneNumber}, Status: ${it.status}" }}")
        } catch (e: Exception) {
            e.printStackTrace()
            call.respondText("Error: ${e.message}\n${e.stackTraceToString()}")
        }
    }
}
