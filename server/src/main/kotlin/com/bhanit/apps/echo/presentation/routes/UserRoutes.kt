package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.data.model.BlockedUserResponse
import com.bhanit.apps.echo.data.model.UpdateProfileRequest
import com.bhanit.apps.echo.data.model.UserProfileResponse
import com.bhanit.apps.echo.data.table.Users
import com.bhanit.apps.echo.domain.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

import com.bhanit.apps.echo.domain.service.NotificationService
import com.bhanit.apps.echo.domain.service.FCMService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import com.bhanit.apps.echo.domain.service.UserService
import com.bhanit.apps.echo.shared.domain.model.Inspiration

fun Route.userRoutes(userRepository: UserRepository, notificationService: NotificationService, userService: UserService) {
    route("/user") {
        
        get("/generate-username") {
             val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
             if (principal == null) {
                 return@get call.respond(HttpStatusCode.Unauthorized)
             }
             
             val inspirationStr = call.request.queryParameters["inspiration"]
             val inspiration = try {
                 if (inspirationStr != null) Inspiration.valueOf(inspirationStr) else null
             } catch (e: Exception) { null }
             
             try {
                 val username = userService.generateUniqueUsername(inspiration)
                 call.respond(mapOf("username" to username))
             } catch (e: Exception) {
                 call.respond(HttpStatusCode.InternalServerError, "Failed to generate username")
             }
        }
        
        get("/profile") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val userId = if (userIdString != null) UUID.fromString(userIdString) else null

            if (userId == null) {
                 return@get call.respond(HttpStatusCode.Unauthorized, "Invalid Token Claims")
            }

            val user = userRepository.getUser(userId)
            if (user != null) {
                call.respond(
                    UserProfileResponse(
                        id = user[Users.id].value.toString(),
                        phoneNumber = user[Users.phoneNumber],
                        fullName = user[Users.fullName],
                        username = user[Users.username],
                        gender = user[Users.gender],
                        email = user[Users.email],
                        photoUrl = user[Users.profilePhoto],
                        credits = user[Users.credits],
                        referralCode = user[Users.referralCode],
                        referralUrl = user[Users.referralCode]?.let { code ->
                             val baseUrl = userRepository.getReferralBaseUrl()
                             // Ensure no double slash if user added one
                             val cleanBase = baseUrl.trimEnd('/')
                             // Format: https://api.echo.com/r/CODE
                             // Clients will append ?m=...
                             "$cleanBase/r/$code"
                        },
                        allowQrAutoConnect = user[Users.allowQrAutoConnect]
                    )
                )
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        put("/profile") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val currentUserId = if (userIdString != null) UUID.fromString(userIdString) else return@put call.respond(HttpStatusCode.Unauthorized)

            val request = call.receive<UpdateProfileRequest>()
            try {
                userRepository.updateProfile(
                    id = currentUserId,
                    fullName = request.fullName,
                    username = request.username,
                    gender = request.gender,
                    email = request.email,
                    photoUrl = request.photoUrl,
                    allowQrAutoConnect = request.allowQrAutoConnect
                )
                call.respond(HttpStatusCode.OK)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid Request")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to update profile")
            }
        }

        get("/blocks") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val currentUserId = if (userIdString != null) UUID.fromString(userIdString) else return@get call.respond(HttpStatusCode.Unauthorized)

            val blocks = userRepository.getBlockedUsers(currentUserId)
            call.respond(blocks.map { 
                BlockedUserResponse(
                    id = it[Users.id].value.toString(),
                    username = it[Users.username],
                    phoneHash = it[Users.phoneHash]
                )
            })
        }

        post("/block") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val currentUserId = if (userIdString != null) UUID.fromString(userIdString) else return@post call.respond(HttpStatusCode.Unauthorized)
            
            val request = call.receive<Map<String, String>>()
            val targetId = request["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing userId")
            userRepository.blockUser(currentUserId, UUID.fromString(targetId))
            call.respond(HttpStatusCode.OK)
        }

        delete("/block/{userId}") {
             val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
             val userIdString = principal?.payload?.getClaim("id")?.asString()
             val currentUserId = if (userIdString != null) UUID.fromString(userIdString) else return@delete call.respond(HttpStatusCode.Unauthorized)

             val targetId = call.parameters["userId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
             userRepository.unblockUser(currentUserId, UUID.fromString(targetId))
             call.respond(HttpStatusCode.OK)
        }
        
        post("/fcm-token") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val currentUserId = if (userIdString != null) UUID.fromString(userIdString) else return@post call.respond(HttpStatusCode.Unauthorized)

            val request = call.receive<Map<String, String>>()
            val token = request["token"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing token")
            
            try {
                userRepository.saveFcmToken(currentUserId, token)
                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to save FCM token")
            }
        }
        
        post("/settings/notifications") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val currentUserId = if (userIdString != null) UUID.fromString(userIdString) else return@post call.respond(HttpStatusCode.Unauthorized)
            
            @kotlinx.serialization.Serializable
            data class NotificationSettingsRequest(
                val feelings: Boolean? = null,
                val checkins: Boolean? = null,
                val reflections: Boolean? = null,
                val rewards: Boolean? = null,
                val inactiveReminders: Boolean? = null
            )
            
            try {
                val req = call.receive<NotificationSettingsRequest>()
                userRepository.updateNotificationSettings(
                    userId = currentUserId,
                    notifyFeelings = req.feelings,
                    notifyCheckins = req.checkins,
                    notifyReflections = req.reflections,
                    notifyRewards = req.rewards,
                    notifyInactiveReminders = req.inactiveReminders
                )
                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, com.bhanit.apps.echo.data.model.ApiErrorDTO(errorMessage = "Failed to update notification settings"))
            }
        }
        
        post("/device") {
             val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
             val userIdString = principal?.payload?.getClaim("id")?.asString()
             val currentUserId = if (userIdString != null) UUID.fromString(userIdString) else return@post call.respond(HttpStatusCode.Unauthorized)
             
             @kotlinx.serialization.Serializable
             data class DeviceSyncRequest(
                 val platform: String,
                 val appVersion: String,
                 val osVersion: String,
                 val deviceModel: String,
                 val locale: String,
                 val timezone: String
             )
             
             try {
                 val req = call.receive<DeviceSyncRequest>()
                 userRepository.updateDeviceMetadata(
                     userId = currentUserId,
                     platform = req.platform,
                     appVersion = req.appVersion,
                     osVersion = req.osVersion,
                     deviceModel = req.deviceModel,
                     locale = req.locale,
                     timezone = req.timezone
                 )
                 call.respond(HttpStatusCode.OK)
             } catch (e: Exception) {
                 call.respond(HttpStatusCode.InternalServerError, "Failed to sync device info: ${e.message}")
             }
        }

        post("/onboarding") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val currentUserId = if (userIdString != null) UUID.fromString(userIdString) else return@post call.respond(HttpStatusCode.Unauthorized)
            
            @kotlinx.serialization.Serializable
            data class OnboardingRequest(val username: String, val referralCode: String? = null, val photoUrl: String? = null)
            
            try {
                val rawRequest = call.receive<OnboardingRequest>()
                // Normalize referral code to Uppercase to match DB
                val request = rawRequest.copy(referralCode = rawRequest.referralCode?.uppercase())

                val (success, referrerId) = userRepository.completeOnboarding(currentUserId, request.username, request.referralCode, request.photoUrl)
                if (success) {
                    GlobalScope.launch {
                        try {
                            // 0. Auto-Connect if Referred
                            if (referrerId != null) {
                                try {
                                    userService.connectOnboardingReferral(referrerId, currentUserId)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            // 1. Notify Self
                            val (myToken, myPlatform) = userRepository.getFcmTokenAndPlatform(currentUserId)
                            if (myToken != null) {
                                // Only send "Received Credits" msg if actually referred
                                if (referrerId != null) { 
                                    val msg = notificationService.getOperationalMessage("REFERRAL_WELCOME", emptyMap(), category = "REWARDS")
                                    if (msg != null) {
                                        val result = FCMService.sendNotification(myToken, "Welcome to Echo", msg, mapOf("type" to "REFERRAL_WELCOME"), platform = myPlatform)
                                        if (result is FCMService.FCMResult.InvalidToken) {
                                            userRepository.clearFcmToken(currentUserId)
                                        }
                                    }
                                }
                            }
                            
                            // 2. Notify Referrer
                            if (referrerId != null) {
                                    val (referrerToken, referrerPlatform) = userRepository.getFcmTokenAndPlatform(referrerId)
                                    if (referrerToken != null) {
                                        val params = mapOf("name" to request.username)
                                        val msg = notificationService.getOperationalMessage("REFERRAL_JOINED", params, category = "REWARDS")
                                        if (msg != null) {
                                            val result = FCMService.sendNotification(referrerToken, "New Referral", msg, mapOf("type" to "REFERRAL_JOINED"), platform = referrerPlatform)
                                            if (result is FCMService.FCMResult.InvalidToken) {
                                                userRepository.clearFcmToken(referrerId)
                                            }
                                        }
                                    }
                            }
                        } catch(e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.BadRequest, com.bhanit.apps.echo.data.model.ApiErrorDTO(errorMessage = "Onboarding failed"))
                }
            } catch (e: IllegalArgumentException) {
                 call.respond(HttpStatusCode.BadRequest, com.bhanit.apps.echo.data.model.ApiErrorDTO(errorCode = "INVALID_REQUEST", errorMessage = e.message ?: "Invalid Request"))
            } catch (e: Exception) {
                // Log the full stack trace for 500 errors
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, com.bhanit.apps.echo.data.model.ApiErrorDTO(errorCode = "ONBOARDING_FAILED", errorMessage = "Failed to complete onboarding", errorDescription = "Please contact support if this persists."))
            }
        }
        
        delete("/account") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val currentUserId = if (userIdString != null) UUID.fromString(userIdString) else return@delete call.respond(HttpStatusCode.Unauthorized)
            
            try {
                userRepository.deleteUser(currentUserId)
                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to delete account")
            }
        }
        
        // Credits & Transactions
        get("/credits") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val userId = if (userIdString != null) UUID.fromString(userIdString) else return@get call.respond(HttpStatusCode.Unauthorized)

            val user = userRepository.getUser(userId)
            val credits = user?.get(Users.credits) ?: 0
            call.respond(mapOf("credits" to credits))
        }

        get("/credits/history") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val userId = if (userIdString != null) UUID.fromString(userIdString) else return@get call.respond(HttpStatusCode.Unauthorized)

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val query = call.request.queryParameters["q"]
            val filterStr = call.request.queryParameters["filter"]
            val filter = try { 
                if (filterStr != null) com.bhanit.apps.echo.data.model.TransactionFilter.valueOf(filterStr) else null 
            } catch(e: Exception) { null }
            val startDate = call.request.queryParameters["startDate"]?.toLongOrNull()
            val endDate = call.request.queryParameters["endDate"]?.toLongOrNull()

            val history = userRepository.getCreditHistory(userId, page, pageSize, query, filter, startDate, endDate)
            call.respond(history)
        }



        // DEBUG - Wipe Data
        post("/reset-data") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val currentUserId = if (userIdString != null) UUID.fromString(userIdString) else return@post call.respond(HttpStatusCode.Unauthorized)
            
            val success = userRepository.resetUserData(currentUserId)
            if (success) {
                call.respond(HttpStatusCode.OK, "User data reset for testing onboarding.")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to reset data.")
            }
        }
    }

    // DEBUG ENDPOINT - REMOVE LATER
    get("/debug/check_user") {
        val phone = call.parameters["phone"] ?: return@get call.respond(HttpStatusCode.BadRequest)

        // Checks:
        // 1. As provided
        // 2. Normalized (+91)
        // 3. Raw (digits)

        val variants = mutableListOf<String>()
        variants.add(phone)
        if (!phone.startsWith("+")) variants.add("+$phone")
        if (!phone.startsWith("+91")) variants.add("+91$phone")
        val digits = phone.filter { it.isDigit() }
        variants.add(digits)
        variants.add("+91$digits")

        val results = mutableListOf<String>()

        variants.distinct().forEach { variant ->
            val id = userRepository.findUserByPhone(variant)
            if (id != null) {
                results.add("Found user for '$variant': ID=$id")
            } else {
                results.add("No user for '$variant'")
            }
        }
        call.respond(results.joinToString("\n"))
    }
}
