package com.bhanit.apps.echo

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import com.bhanit.apps.echo.data.db.DatabaseFactory
import com.bhanit.apps.echo.presentation.routes.authRoutes
import com.bhanit.apps.echo.presentation.routes.contactRoutes
import com.bhanit.apps.echo.presentation.routes.messagingRoutes
import com.bhanit.apps.echo.presentation.routes.dashboardRoutes
import com.bhanit.apps.echo.presentation.routes.userRoutes
import com.bhanit.apps.echo.presentation.routes.emotionRoutes
import com.bhanit.apps.echo.presentation.routes.creditsRoutes
import com.bhanit.apps.echo.presentation.routes.mediaRoutes
import com.bhanit.apps.echo.presentation.routes.supportRoutes
import com.bhanit.apps.echo.presentation.routes.habitRoutes
import com.bhanit.apps.echo.presentation.routes.notificationRoutes
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.bhanit.apps.echo.presentation.routes.debugRoutes
import com.bhanit.apps.echo.presentation.routes.adminRoutes
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import com.bhanit.apps.echo.presentation.routes.contentRoutes
import com.bhanit.apps.echo.presentation.routes.adminContentRoutes
import com.bhanit.apps.echo.presentation.routes.shortLinkRoutes

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}
fun Application.module() {
    DatabaseFactory.init(environment.config)
    com.bhanit.apps.echo.domain.service.FCMService.init(environment.config)
    // ... rest of file structure ...
    
    install(ContentNegotiation) {
        json(kotlinx.serialization.json.Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    

    install(io.ktor.server.plugins.cors.routing.CORS) {
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowMethod(io.ktor.http.HttpMethod.Patch)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowCredentials = true
        anyHost()
    }
    
    install(io.ktor.server.plugins.statuspages.StatusPages) {
        exception<com.bhanit.apps.echo.core.domain.model.ServiceException> { call, cause ->
            println("DEBUG: StatusPages caught ServiceException: ${cause.errorCode}")
            val status = when (cause.errorCode) {
                com.bhanit.apps.echo.core.util.ErrorCode.INVALID_OTP -> io.ktor.http.HttpStatusCode.Unauthorized
                com.bhanit.apps.echo.core.util.ErrorCode.OTP_RATE_LIMIT_EXCEEDED,
                com.bhanit.apps.echo.core.util.ErrorCode.OTP_MAX_ATTEMPTS_BLOCKED -> io.ktor.http.HttpStatusCode.TooManyRequests
                com.bhanit.apps.echo.core.util.ErrorCode.SERVER_ERROR -> io.ktor.http.HttpStatusCode.InternalServerError
                else -> io.ktor.http.HttpStatusCode.BadRequest
            }
            
            try {
                call.respond(
                    status,
                    com.bhanit.apps.echo.data.model.ApiErrorDTO(
                        errorCode = cause.errorCode.name,
                        errorMessage = cause.errorCode.message,
                        errorDescription = cause.message
                    )
                )
            } catch (e: Exception) {
                println("DEBUG: StatusPages Serialization Failed: ${e.message}")
                e.printStackTrace()
                // Fallback to manual JSON
                call.respondText(
                    text = """{"errorCode": "${cause.errorCode.name}", "errorMessage": "${cause.errorCode.message}", "errorDescription": "${cause.message ?: ""}"}""",
                    contentType = io.ktor.http.ContentType.Application.Json,
                    status = status
                )
            }
        }
        exception<Throwable> { call, cause ->
            println("DEBUG: StatusPages caught Throwable: ${cause::class.simpleName}")
            cause.printStackTrace()
            val dto = com.bhanit.apps.echo.data.model.ApiErrorDTO(
                errorCode = "INTERNAL_SERVER_ERROR",
                errorMessage = "An unexpected error occurred. Please try again later.",
                errorDescription = cause.localizedMessage
            )
            call.respond(io.ktor.http.HttpStatusCode.InternalServerError, dto)
        }
    }

    val jwtService = com.bhanit.apps.echo.data.service.JwtService(
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        secret = environment.config.property("jwt.secret").getString()
    )

    // Admin Module (Moved up for Auth)
    val adminRepository = com.bhanit.apps.echo.data.repository.AdminRepositoryImpl()
    val adminService = com.bhanit.apps.echo.domain.service.AdminService(adminRepository, jwtService)


    org.jetbrains.exposed.sql.transactions.transaction {
        org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns(
            com.bhanit.apps.echo.data.table.Users,
            com.bhanit.apps.echo.data.table.Emotions,
            com.bhanit.apps.echo.data.table.Connections,
            com.bhanit.apps.echo.data.table.Messages,
            com.bhanit.apps.echo.data.table.Replies,
            com.bhanit.apps.echo.data.table.DailyStats,
            com.bhanit.apps.echo.data.table.Referrals,
            com.bhanit.apps.echo.data.table.UserBlocks,
            com.bhanit.apps.echo.data.table.Reports,
            com.bhanit.apps.echo.data.table.Otps,
            com.bhanit.apps.echo.data.table.UserContacts,
            com.bhanit.apps.echo.data.table.UserUnlockedEmotions,
            com.bhanit.apps.echo.data.table.UserRelationLimits,
            com.bhanit.apps.echo.data.table.CreditTransactions,
            com.bhanit.apps.echo.data.table.SupportTickets,
            com.bhanit.apps.echo.data.table.SupportTicketHistory,
            com.bhanit.apps.echo.data.table.SupportTicketHistory,
            com.bhanit.apps.echo.data.table.UserStreaks,
            com.bhanit.apps.echo.data.table.NotificationMessages,
            com.bhanit.apps.echo.data.table.NotificationHistory,
            com.bhanit.apps.echo.data.table.AdminUsers,
            com.bhanit.apps.echo.data.table.AuditLogs,
            com.bhanit.apps.echo.data.table.TicketReplies,
            com.bhanit.apps.echo.data.table.AppContents,
            com.bhanit.apps.echo.data.table.UserActivityMeta,
            com.bhanit.apps.echo.data.table.SystemConfig,
            com.bhanit.apps.echo.data.table.SeasonalEvents,
            com.bhanit.apps.echo.data.table.RewardHistory
        )
        // Migration: Rename MANUAL_GRANT to MANUAL_ADJUSTMENT (Dev approved)
        try {
            exec("UPDATE credit_transactions SET type = 'MANUAL_ADJUSTMENT' WHERE type = 'MANUAL_GRANT'")
        } catch (e: Exception) {
            println("Migration: Failed to update legacy transaction types: ${e.message}")
        }
        
        // Migration: Clean up Historical Logs (Sent/Received Echos should be INTERNAL)
        try {
            // New columns default to VISIBLE/REWARD currently. We need to fix logs.
            // intent = 'LOG' (2), visibility = 'INTERNAL' (1)
            // Note: Enum ordinal or string depends on Exposed config. 
            // Schema.kt uses enumerationByName (String).
            
            val sql = """
                UPDATE credit_transactions 
                SET visibility = 'INTERNAL', intent = 'LOG', reward_source = 'SYSTEM' 
                WHERE type IN ('SENT_ECHO', 'RECEIVED_ECHO')
            """.trimIndent()
            exec(sql)
            println("Migration: Updated historical ECHO logs to INTERNAL visibility.")
        } catch (e: Exception) {
             println("Migration: Failed to update historical logs: ${e.message}")
        }
        
        // Migration: Allow Guest Tickets
        try {
            exec("ALTER TABLE support_tickets ALTER COLUMN user_id DROP NOT NULL")
            exec("ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255)")
        } catch (e: Exception) {
            println("Migration: Failed to update support_tickets: ${e.message}")
        }
        
        // Migration: Drop Legacy Reflection Streak Columns (Cleanup)
        try {
            exec("ALTER TABLE user_streaks DROP COLUMN IF EXISTS reflection_streak")
            exec("ALTER TABLE user_streaks DROP COLUMN IF EXISTS reflection_last_active_at")
            exec("ALTER TABLE user_streaks DROP COLUMN IF EXISTS acknowledgement_streak") // Cleanup reverted rename
            exec("ALTER TABLE user_streaks DROP COLUMN IF EXISTS acknowledgement_last_active_at")
             println("Migration: Dropped legacy columns.")
        } catch (e: Exception) {
             println("Migration: Failed to drop legacy columns: ${e.message}")
        }


    }
    
    val userRepository = com.bhanit.apps.echo.data.repository.UserRepositoryImpl()
    val supportRepository = com.bhanit.apps.echo.data.repository.SupportRepositoryImpl()

    val jwtRealm = environment.config.property("jwt.realm").getString()
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(jwtService.getVerifier())
            validate { credential ->
                val idClaim = credential.payload.getClaim("id").asString()
                val versionClaim = credential.payload.getClaim("token_version").asInt()
                
                println("DEBUG: Auth Validate - ID: $idClaim, VersionClaim: $versionClaim")
                
                if (idClaim != null && idClaim != "") {
                    // Check DB for current version
                    try {
                        val userId = java.util.UUID.fromString(idClaim)
                        val currentVersion = userRepository.getUserTokenVersion(userId)
                        println("DEBUG: Auth Validate - DB Version: $currentVersion")

                        if (versionClaim != null && versionClaim == currentVersion) {
                             println("DEBUG: Auth Validate - SUCCESS")
                             JWTPrincipal(credential.payload)
                        } else {
                            println("DEBUG: Auth Validate - FAILURE (Version Mismatch). Token: $versionClaim, DB: $currentVersion")
                            // Token version mismatch (logged out elsewhere)
                            null
                        }
                    } catch (e: Exception) {
                         println("DEBUG: Auth Validate - FAILURE (Exception): ${e.message}")
                         e.printStackTrace()
                         // Do NOT return null here, or it triggers 401 Logout.
                         // Throwing exception triggers 500, which Client retry logic should handle.
                         throw com.bhanit.apps.echo.core.domain.model.ServiceException(
                             com.bhanit.apps.echo.core.util.ErrorCode.SERVER_ERROR
                         )
                    }
                } else {
                    println("DEBUG: Auth Validate - FAILURE (Invalid ID Claim)")
                    null
                }
            }
            
            challenge { defaultScheme, realm ->
                call.respond(
                    io.ktor.http.HttpStatusCode.Unauthorized,
                    com.bhanit.apps.echo.data.model.ApiErrorDTO(
                        errorCode = "UNAUTHORIZED",
                        errorMessage = "You are not logged in. Please log in to continue.",
                        errorDescription = "Missing or invalid authentication token"
                    )
                )
            }
        }
        
        jwt("auth-admin-jwt") {
            realm = jwtRealm
            verifier(jwtService.getVerifier())
            validate { credential ->
                val idClaim = credential.payload.getClaim("id").asString()
                val roleClaim = credential.payload.getClaim("role").asString()
                
                if (idClaim != null && !roleClaim.isNullOrBlank()) {
                     val adminId = java.util.UUID.fromString(idClaim)
                     val admin = adminRepository.getAdminById(adminId) 
                     if (admin != null && admin[com.bhanit.apps.echo.data.table.AdminUsers.isActive]) {
                         JWTPrincipal(credential.payload)
                     } else {
                         null
                     }
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Invalid Admin Credentials")
            }
        }
    }

    val connectionRepository = com.bhanit.apps.echo.data.repository.ConnectionRepositoryImpl()
    val contactRepository = com.bhanit.apps.echo.data.repository.ContactRepositoryImpl()
    val emotionRepository = com.bhanit.apps.echo.data.repository.EmotionRepositoryImpl()
    val messageRepository = com.bhanit.apps.echo.data.repository.MessageRepositoryImpl()
    val dailyStatsRepository = com.bhanit.apps.echo.data.repository.DailyStatsRepositoryImpl()
    
    val habitRepository = com.bhanit.apps.echo.data.repository.HabitRepositoryImpl()
    val notificationRepository = com.bhanit.apps.echo.data.repository.NotificationRepositoryImpl()
    
    val seasonalEventRepository = com.bhanit.apps.echo.data.repository.SeasonalEventRepositoryImpl()
    
    val notificationService = com.bhanit.apps.echo.domain.service.NotificationService(notificationRepository, dailyStatsRepository, userRepository, seasonalEventRepository)
    val creditService = com.bhanit.apps.echo.data.service.CreditServiceImpl(seasonalEventRepository, notificationService)
    
    val client = HttpClient(OkHttp) {
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 5000
            socketTimeoutMillis = 15000
        }
        install(ClientContentNegotiation) {
            json(kotlinx.serialization.json.Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    val otpSender = com.bhanit.apps.echo.data.service.BaileysOtpSenderImpl(client)
    
    val authService = com.bhanit.apps.echo.data.service.RealAuthService(userRepository, jwtService, otpSender, creditService)
    val contactService = com.bhanit.apps.echo.domain.service.ContactService(userRepository, connectionRepository, contactRepository)
    val emotionService = com.bhanit.apps.echo.domain.service.EmotionService(emotionRepository)
    val habitService = com.bhanit.apps.echo.domain.service.HabitService(habitRepository, creditService)
    val messagingService = com.bhanit.apps.echo.domain.service.MessagingService(messageRepository, dailyStatsRepository, userRepository, connectionRepository, creditService, emotionRepository, habitService)
    val supportService = com.bhanit.apps.echo.domain.service.SupportService(supportRepository)
    val userService = com.bhanit.apps.echo.domain.service.UserService(userRepository, contactService)
    // notificationService moved up
    val notificationScheduler = com.bhanit.apps.echo.domain.service.NotificationScheduler(habitRepository, notificationService, userRepository, messageRepository, dailyStatsRepository)
    val cloudinaryService = com.bhanit.apps.echo.domain.service.CloudinaryService()

    // Admin Module


    
    // Create Initial Admin (Only for dev/first run if no admins exist)
    // In production, this should be done via migration or separate script.
    // For now, checks are inside 'createInitialAdmin' or we can just rely on manual insert via SQL.
    
    val contentRepository = com.bhanit.apps.echo.data.repository.ContentRepositoryImpl()
    val contentService = com.bhanit.apps.echo.domain.service.ContentService(contentRepository)

    // Seed Emotions & Notifications & Content
    kotlinx.coroutines.runBlocking {
        emotionService.seedDefaults()
        notificationService.seedDefaults()
        contentService.seedDefaults() // Added seeding
        
        // Seed Initial Admin
        try {
            if (adminRepository.findAdminByEmail("admin@echo.com") == null) {
                adminService.createInitialAdmin("admin@echo.com", "admin123")
                println("DEBUG: Created initial admin user: admin@echo.com")
            }
        } catch (e: Exception) {
             println("DEBUG: Failed to seed admin: ${e.message}")
        }
        
        // Seed Seasonal Events (Valentine 2026)
        try {
            val vId = "VALENTINE_2026"
            if (seasonalEventRepository.getEvent(vId) == null) {
                val rules = listOf(
                    com.bhanit.apps.echo.util.SeasonalRule(com.bhanit.apps.echo.util.SeasonalRuleType.SEND_POSITIVE, bonusCredits = 2, dailyCap = 1, maxTotal = 10),
                    com.bhanit.apps.echo.util.SeasonalRule(com.bhanit.apps.echo.util.SeasonalRuleType.RESPOND, bonusCredits = 1, maxTotal = 5),
                    com.bhanit.apps.echo.util.SeasonalRule(com.bhanit.apps.echo.util.SeasonalRuleType.COMEBACK, bonusCredits = 3, oncePerSeason = true)
                )
                val event = com.bhanit.apps.echo.util.SeasonalEvent(
                    id = vId,
                    name = "Valentine's Week",
                    colorHex = "0xFFE91E63",
                    startMonth = java.time.Month.FEBRUARY,
                    startDay = 6,
                    endMonth = java.time.Month.FEBRUARY,
                    endDay = 21,
                    rules = rules
                )
                seasonalEventRepository.createEvent(event, 2026)
                println("DEBUG: Seeded Valentine's 2026 (Feb 6 - Feb 21)")
            }
        } catch (e: Exception) {
            println("DEBUG: Failed to seed seasonal event: ${e.message}")
        }
    }
    
    // Start Schedulers
    notificationScheduler.start()
    
    // Background Job: Cleanup Expired OTPs daily at 12 AM
    kotlinx.coroutines.GlobalScope.launch {
        while (true) {
            val now = java.time.LocalDateTime.now()
            var nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            
            // If it's exactly midnight (rare), schedule for next day
            if (now.isEqual(nextMidnight)) {
                nextMidnight = nextMidnight.plusDays(1)
            }
            
            val delayMillis = java.time.Duration.between(now, nextMidnight).toMillis()
            println("Cleanup: Scheduling next OTP cleanup in ${delayMillis / 1000 / 60} minutes")
            
            kotlinx.coroutines.delay(delayMillis)
            
            try {
                println("Cleanup: Running daily OTP cleanup...")
                authService.cleanupExpiredOtps()
            } catch (e: Exception) {
                println("Cleanup: Failed to run OTP cleanup: ${e.message}")
            }
        }
    }
    
    routing {
        // Static web routes moved to separate React frontend
        
        get("/") {
            call.respondText("Echo API is running.")
        }

        get("/logo.png") {
             val resource = java.io.File("src/main/resources/logo.png")
             if (resource.exists()) call.respondFile(resource) 
             else call.respond(io.ktor.http.HttpStatusCode.NotFound)
        }

        contentRoutes(contentService) // Public API
        authRoutes(authService)
        // debugRoutes(authService, userRepository) // Moved to Admin-only
        
        authenticate("auth-jwt") {
            authRoutes(authService)
            userRoutes(userRepository, notificationService, userService)
            contactRoutes(contactService)
            emotionRoutes(emotionService)
            messagingRoutes(messagingService)
            dashboardRoutes(dailyStatsRepository, creditService)
            creditsRoutes(userRepository, emotionRepository, creditService, habitRepository, seasonalEventRepository)
            habitRoutes(habitService, userRepository)
            notificationRoutes(notificationService)
            mediaRoutes(cloudinaryService)
        }
        
        
        supportRoutes(supportService)
        
        adminRoutes(adminService, adminRepository, notificationService, userRepository, dailyStatsRepository, supportRepository, seasonalEventRepository)
        
        shortLinkRoutes()

        authenticate("auth-admin-jwt") {
            adminContentRoutes(contentService)
            debugRoutes(authService, userRepository)
        }
    }
}