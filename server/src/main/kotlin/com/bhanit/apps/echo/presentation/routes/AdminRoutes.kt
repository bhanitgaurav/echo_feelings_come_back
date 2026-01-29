package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.domain.service.AdminService
import com.bhanit.apps.echo.domain.repository.AdminRepository
import com.bhanit.apps.echo.domain.service.NotificationService
import com.bhanit.apps.echo.domain.service.TargetFilters
import com.bhanit.apps.echo.domain.repository.UserRepository
import com.bhanit.apps.echo.domain.repository.DailyStatsRepository
import com.bhanit.apps.echo.domain.repository.SupportRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.util.UUID
import io.ktor.server.routing.*
import com.bhanit.apps.echo.data.table.SystemConfig
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class MetricsDTO(
    val spikeCountHour: Int,
    val spikeCountDay: Int,
    val spikeCountMonth: Int,
    val openTickets: Long,
    val criticalTickets: Long
)

@Serializable
data class AlertsResponseDTO(
    val alerts: List<String>,
    val metrics: MetricsDTO
)

@Serializable
data class TargetFiltersDTO(
    val platform: String? = null,
    val minVersion: String? = null,
    val daysInactive: Int? = null,
    val hasCompletedOnboarding: Boolean? = null
)

@Serializable
data class BroadcastResultDTO(
    val sent: Int = 0,
    val failed: Int = 0,
    val total: Int = 0,
    val count: Int? = null,
    val sample: List<String>? = null
)

@Serializable
data class BroadcastRequestDTO(
    val title: String,
    val body: String,
    val dryRun: Boolean = false,
    val filters: TargetFiltersDTO? = null
)

@Serializable
data class AdminSummaryDTO(
    val id: String,
    val email: String,
    val role: String,
    val isActive: Boolean,
    val lastLogin: String?,
    val lastLoginIp: String?,
    val createdAt: String
)

@Serializable
data class UserSummaryDTO(
    val id: String,
    val username: String,
    val phoneNumber: String,
    val credits: Int,
    val isActive: Boolean,
    val referralCode: String?,
    val createdAt: String
)

@Serializable
data class TicketDetailResponseDTO(
    val ticket: com.bhanit.apps.echo.data.model.SupportTicketDto,
    val replies: List<com.bhanit.apps.echo.data.model.TicketReplyDto>
)

@Serializable
data class UserDetailDTO(
    val id: String,
    val username: String,
    val email: String?,
    val phoneNumber: String,
    val credits: Int,
    val isActive: Boolean,
    val referralCode: String?,
    val createdAt: String
)

@Serializable
data class TransactionHistoryDTO(
    val id: String,
    val amount: Int,
    val type: String,
    val description: String,
    val timestamp: Long
)

private fun maskPhoneNumber(phone: String): String {
    return if (phone.length > 5) {
        "*".repeat(phone.length - 5) + phone.takeLast(5)
    } else {
        phone
    }
}

fun Route.adminRoutes(
    adminService: AdminService,
    adminRepository: AdminRepository,
    notificationService: NotificationService,
    userRepository: UserRepository,
    dailyStatsRepository: DailyStatsRepository,
    supportRepository: SupportRepository,
    seasonalEventRepository: com.bhanit.apps.echo.domain.repository.SeasonalEventRepository
) {
    route("/api/admin") {
        
        post("/auth/login") {
            val params = call.receive<Map<String, String>>()
            val email = params["email"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Email required")
            val password = params["password"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Password required")

            val token = adminService.login(email, password, call.request.local.remoteHost)
            if (token != null) {
                // Get Admin Details for UI
                val admin = adminRepository.findAdminByEmail(email)
                val role = admin?.get(com.bhanit.apps.echo.data.table.AdminUsers.role)
                
                call.respond(mapOf(
                    "token" to token, 
                    "role" to role?.name,
                    "email" to email
                ))
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials or inactive account")
            }
        }

        authenticate("auth-admin-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val id = principal?.payload?.getClaim("id")?.asString()
                val role = principal?.payload?.getClaim("role")?.asString()
                call.respond(mapOf("id" to id, "role" to role))
            }
            
            // --- Admin Management ---
            route("/admins") {
                // List Admins
                get {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN") return@get call.respond(HttpStatusCode.Forbidden, "Requires ADMIN role")
                    
                    val admins = adminService.listAdmins().map { row ->
                         AdminSummaryDTO(
                             id = row[com.bhanit.apps.echo.data.table.AdminUsers.id].toString(),
                             email = row[com.bhanit.apps.echo.data.table.AdminUsers.email],
                             role = row[com.bhanit.apps.echo.data.table.AdminUsers.role].toString(),
                             isActive = row[com.bhanit.apps.echo.data.table.AdminUsers.isActive],
                             lastLogin = row[com.bhanit.apps.echo.data.table.AdminUsers.lastLoginAt]?.toString(),
                             lastLoginIp = row[com.bhanit.apps.echo.data.table.AdminUsers.lastLoginIp],
                             createdAt = row[com.bhanit.apps.echo.data.table.AdminUsers.createdAt].toString()
                         )
                    }
                    call.respond(admins)
                }

                // Create Admin
                post {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN") return@post call.respond(HttpStatusCode.Forbidden, "Requires ADMIN role")
                    
                    val params = call.receive<Map<String, String>>()
                    val email = params["email"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Email required")
                    val password = params["password"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Password required")
                    val newRoleStr = params["role"] ?: "SUPPORT"
                    val newRole = try { com.bhanit.apps.echo.data.table.AdminRole.valueOf(newRoleStr) } catch(e: Exception) { com.bhanit.apps.echo.data.table.AdminRole.SUPPORT }

                    try {
                        val creatorId = UUID.fromString(call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asString())
                        val newId = adminService.createAdmin(creatorId, email, password, newRole)
                        call.respond(HttpStatusCode.Created, mapOf("id" to newId.toString()))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to create admin: ${e.message}")
                    }
                }
                
                // Update Admin Status
                patch("/{id}") {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN") return@patch call.respond(HttpStatusCode.Forbidden, "Requires ADMIN role")
                    
                    val idStr = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
                    val params = call.receive<Map<String, Boolean>>()
                    val isActive = params["isActive"] ?: return@patch call.respond(HttpStatusCode.BadRequest, "isActive required")
                    
                    try {
                        val actorId = UUID.fromString(call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asString())
                        val targetId = UUID.fromString(idStr)
                        adminService.toggleAdminStatus(actorId, targetId, isActive)
                        call.respond(HttpStatusCode.OK)
                    } catch (e: IllegalArgumentException) {
                         call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid Request")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to update admin")
                    }
                }
            }

            route("/config") {
                get {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN") return@get call.respond(HttpStatusCode.Forbidden, "Requires ADMIN role")

                    val config = org.jetbrains.exposed.sql.transactions.transaction {
                        SystemConfig.selectAll().associate {
                            it[SystemConfig.key] to it[SystemConfig.value]
                        }
                    }
                    call.respond(config)
                }

                post {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN") return@post call.respond(HttpStatusCode.Forbidden, "Requires ADMIN role")

                    val params = call.receive<Map<String, String>>()
                    val key = params["key"]
                    val value = params["value"]

                    if (key != null && value != null) {
                         try {
                              org.jetbrains.exposed.sql.transactions.transaction {
                                  // Upsert logic
                                  val existing = SystemConfig.selectAll().where { SystemConfig.key eq key }.singleOrNull()
                                  if (existing != null) {
                                      SystemConfig.update({ SystemConfig.key eq key }) {
                                          it[SystemConfig.value] = value
                                          it[updatedAt] = Instant.now()
                                      }
                                  } else {
                                      SystemConfig.insert {
                                          it[SystemConfig.key] = key
                                          it[SystemConfig.value] = value
                                          it[updatedAt] = Instant.now()
                                      }
                                  }
                              }
                              call.respond(HttpStatusCode.OK)
                         } catch (e: Exception) {
                              call.respond(HttpStatusCode.InternalServerError, "Failed to update config")
                         }
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Key and Value required")
                    }
                }
            }

            // --- User Management ---


                route("/users") {
                    get {
                    val query = call.request.queryParameters["query"]
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    val canView = role == "ADMIN" || role == "SUPPORT"
                    if (!canView) return@get call.respond(HttpStatusCode.Forbidden)

                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                    val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0L
                    
                    val users = userRepository.searchUsers(query, limit, offset, includePhone = true).map {
                        UserSummaryDTO(
                            id = it[com.bhanit.apps.echo.data.table.Users.id].toString(),
                            username = it[com.bhanit.apps.echo.data.table.Users.username],
                            phoneNumber = maskPhoneNumber(it[com.bhanit.apps.echo.data.table.Users.phoneNumber]),
                            credits = it[com.bhanit.apps.echo.data.table.Users.credits],
                            isActive = it[com.bhanit.apps.echo.data.table.Users.isActive],
                            referralCode = it[com.bhanit.apps.echo.data.table.Users.referralCode],
                            createdAt = it[com.bhanit.apps.echo.data.table.Users.createdAt].toString()
                        )
                    }
                    call.respond(users)
                }

                // ...
                
                get("/{id}/transactions") {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN") return@get call.respond(HttpStatusCode.Forbidden)
                    
                    val idStr = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val filterStr = call.request.queryParameters["filter"]
                    
                    val filter = if (filterStr != null) {
                        try {
                            com.bhanit.apps.echo.data.model.TransactionFilter.valueOf(filterStr)
                        } catch(e: Exception) { null }
                    } else null

                    try {
                         val history = userRepository.getCreditHistory(
                             userId = UUID.fromString(idStr),
                             page = page,
                             pageSize = limit,
                             query = null,
                             filter = filter,
                             startDate = null,
                             endDate = null
                         )
                         
                         call.respond(history.map { 
                             TransactionHistoryDTO(
                                 id = it.id,
                                 amount = it.amount,
                                 type = it.type.name,
                                 description = it.description,
                                 timestamp = it.timestamp
                             )
                         })
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to fetch transactions")
                    }
                }



                get("/{id}") {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN" && role != "SUPPORT") return@get call.respond(HttpStatusCode.Forbidden)

                    val idStr = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val user = userRepository.getUserDetails(UUID.fromString(idStr))
                    if (user != null) {
                        call.respond(UserDetailDTO(
                            id = user[com.bhanit.apps.echo.data.table.Users.id].toString(),
                            username = user[com.bhanit.apps.echo.data.table.Users.username],
                            email = user[com.bhanit.apps.echo.data.table.Users.email],
                            phoneNumber = maskPhoneNumber(user[com.bhanit.apps.echo.data.table.Users.phoneNumber]),
                            credits = user[com.bhanit.apps.echo.data.table.Users.credits],
                            isActive = user[com.bhanit.apps.echo.data.table.Users.isActive],
                            referralCode = user[com.bhanit.apps.echo.data.table.Users.referralCode],
                            createdAt = user[com.bhanit.apps.echo.data.table.Users.createdAt].toString()
                        ))
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                
                post("/{id}/credits") {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN") return@post call.respond(HttpStatusCode.Forbidden, "Requires ADMIN role")
                    
                    val idStr = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val adminId = UUID.fromString(call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asString())
                    
                    val params = call.receive<Map<String, String>>()
                    val amount = params["amount"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, "Amount required")
                    val reason = params["reason"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Reason required")
                    val notes = params["notes"]
                    
                    try {
                        val txId = userRepository.grantCredits(adminId, UUID.fromString(idStr), amount, reason, notes)
                        
                        // Determine type for audit based on amount
                        val type = if (amount >= 0) "GRANT_CREDITS" else "REVOKE_CREDITS"
                        adminService.logAction(adminId, type, idStr, "Amount: $amount, Reason: $reason", call.request.local.remoteHost)
                        
                        call.respond(HttpStatusCode.OK, mapOf("transactionId" to txId.toString()))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to grant credits")
                    }
                }
            }

            // --- Tickets ---
            route("/tickets") {
                get {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN" && role != "SUPPORT") return@get call.respond(HttpStatusCode.Forbidden)
                    
                    val status = call.request.queryParameters["status"]
                    val priority = call.request.queryParameters["priority"]
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                    val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0L
                    
                    val tickets = supportRepository.getAllTickets(status, priority, limit, offset)
                    call.respond(tickets)
                }
                
                get("/{id}") {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    val idStr = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val ticketId = UUID.fromString(idStr)
                    
                    val ticket = supportRepository.getTicketDetails(ticketId)
                    if (ticket != null) {
                        val replies = supportRepository.getTicketReplies(ticketId)
                        call.respond(TicketDetailResponseDTO(
                            ticket = ticket,
                            replies = replies
                        ))
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                
                post("/{id}/reply") {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN" && role != "SUPPORT") return@post call.respond(HttpStatusCode.Forbidden)
                    
                    val idStr = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val adminId = UUID.fromString(call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asString())
                    val params = call.receive<Map<String, String>>()
                    val content = params["content"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    
                    try {
                         supportRepository.replyToTicket(UUID.fromString(idStr), adminId, com.bhanit.apps.echo.data.table.SenderType.ADMIN, content)
                         call.respond(HttpStatusCode.OK)
                    } catch (e: Exception) {
                         call.respond(HttpStatusCode.InternalServerError)
                    }
                }
                
                // Update Status or Priority (PATCH)
                patch("/{id}") {
                     val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                     val idStr = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
                     val params = call.receive<Map<String, String>>()
                     val statusStr = params["status"]
                     val priorityStr = params["priority"]
                     
                     try {
                         val ticketId = UUID.fromString(idStr)
                         
                         if (statusStr != null) {
                             val status = com.bhanit.apps.echo.data.model.TicketStatus.valueOf(statusStr)
                             supportRepository.updateTicketStatus(ticketId, status)
                         }
                         
                         if (priorityStr != null) {
                             val priority = com.bhanit.apps.echo.data.model.TicketPriority.valueOf(priorityStr)
                             supportRepository.updateTicketPriority(ticketId, priority)
                         }
                         // I will skip priority update for now unless required.
                         // But I should probably add it for "Incidents" management.
                         // I'll leave it as Status Only for now to save time, or just update logic if priority is passed.
                         // I will just return OK.
                         
                         call.respond(HttpStatusCode.OK)
                     } catch (e: Exception) {
                         call.respond(HttpStatusCode.InternalServerError)
                     }
                }
            }

            // --- Stats ---
            route("/stats") {
                get {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN" && role != "SUPPORT") return@get call.respond(HttpStatusCode.Forbidden)

                    val rangeStr = call.request.queryParameters["range"] ?: "WEEK"
                    val range = try {
                        com.bhanit.apps.echo.data.model.TimeRange.valueOf(rangeStr)
                    } catch (e: Exception) {
                        com.bhanit.apps.echo.data.model.TimeRange.WEEK
                    }

                    try {
                        val stats = dailyStatsRepository.getGlobalAnalytics(range)
                        call.respond(stats)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        call.respond(HttpStatusCode.InternalServerError, "Failed to fetch stats: ${e.message}")
                    }
                }
            }
            
            // --- Alerts ---
            route("/alerts") {
                get {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN") return@get call.respond(HttpStatusCode.Forbidden)
                    
                    val ticketStats = supportRepository.getTicketStats()
                    
                    val spikeCountHour = supportRepository.checkTicketSpike(10, 60)
                    val spikeCountDay = supportRepository.checkTicketSpike(20, 1440) 
                    val spikeCountMonth = supportRepository.checkTicketSpike(50, 43200)

                    val alerts = mutableListOf<String>()
                    if (spikeCountHour > 10) alerts.add("TICKET_SPIKE_DETECTED: $spikeCountHour tickets in last hour.")
                    if ((ticketStats["critical"] ?: 0) > 5) alerts.add("HIGH_CRITICAL_TICKET_COUNT: ${ticketStats["critical"]} critical tickets open.")

                    call.respond(AlertsResponseDTO(
                        alerts = alerts,
                        metrics = MetricsDTO(
                            spikeCountHour = spikeCountHour,
                            spikeCountDay = spikeCountDay,
                            spikeCountMonth = spikeCountMonth,
                            openTickets = (ticketStats["open"] ?: 0).toLong(),
                            criticalTickets = (ticketStats["critical"] ?: 0).toLong()
                        )
                    ))

                }
            }

            // --- Seasonal Events ---
            route("/seasons") {
                get {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN") return@get call.respond(HttpStatusCode.Forbidden)
                    
                    val year = call.request.queryParameters["year"]?.toIntOrNull() ?: java.time.LocalDate.now().year
                    val events = seasonalEventRepository.getEventsByYear(year)
                    call.respond(events)
                }

                post {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN") return@post call.respond(HttpStatusCode.Forbidden)

                    try {
                        val event = call.receive<com.bhanit.apps.echo.util.SeasonalEvent>()
                        val targetYear = call.request.queryParameters["year"]?.toIntOrNull() ?: java.time.LocalDate.now().year
                        
                        // Overlap Validation
                        val start = java.time.LocalDate.of(targetYear, event.startMonth, event.startDay)
                        val end = java.time.LocalDate.of(targetYear, event.endMonth, event.endDay)
                        
                        // Date Validation
                        if (start.isAfter(end)) {
                             return@post call.respond(
                                 HttpStatusCode.BadRequest, 
                                 com.bhanit.apps.echo.data.model.ApiErrorDTO(
                                     errorCode = "INVALID_DATE_RANGE",
                                     errorMessage = "Start date cannot be after end date.",
                                     errorDescription = "Start: $start, End: $end"
                                 )
                             )
                        }

                        if (seasonalEventRepository.hasOverlappingEvent(targetYear, start, end)) {
                             return@post call.respond(
                                 HttpStatusCode.Conflict, 
                                 com.bhanit.apps.echo.data.model.ApiErrorDTO(
                                     errorCode = "EVENT_OVERLAP",
                                     errorMessage = "Event overlaps with an existing season.",
                                     errorDescription = "Please check the dates."
                                 )
                             )
                        }

                        // Use a composite ID: NAME_YEAR if not provided
                        val id = if (event.id.isBlank()) "${event.name.uppercase().replace(" ", "_")}_$targetYear" else event.id
                        
                        val newEvent = event.copy(id = id)
                        
                        seasonalEventRepository.createEvent(newEvent, targetYear)
                        call.respond(HttpStatusCode.Created, newEvent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.BadRequest, 
                            com.bhanit.apps.echo.data.model.ApiErrorDTO(
                                errorCode = "INVALID_EVENT_DATA",
                                errorMessage = "Invalid event data format.",
                                errorDescription = e.message
                            )
                        )
                    }
                }

                delete("/{id}") {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN") return@delete call.respond(HttpStatusCode.Forbidden)
                    
                    val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    seasonalEventRepository.deleteEvent(id)
                    call.respond(HttpStatusCode.OK)
                }
            }

            // --- Notifications ---
            route("/notifications") {
                post("/broadcast") {
                    val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                    if (role != "ADMIN") return@post call.respond(HttpStatusCode.Forbidden, "Requires ADMIN role")
                    
                    try {
                        val req = call.receive<BroadcastRequestDTO>()
                        println("Admin Broadcast Request: Title='${req.title}', Filters=${req.filters}")
                        
                        val filters = TargetFilters(
                            platform = req.filters?.platform,
                            minVersion = req.filters?.minVersion,
                            daysInactive = req.filters?.daysInactive,
                            hasCompletedOnboarding = req.filters?.hasCompletedOnboarding
                        )
                        
                        val result = notificationService.broadcastNotification(req.title, req.body, filters, req.dryRun)
                        call.respond(result)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        call.respond(HttpStatusCode.InternalServerError, "Failed to broadcast: ${e.message}")
                    }
                }
            }


            // --- Debug Seeding ---
            route("/debug") {
                 post("/seed-users") {
                     val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                     if (role != "ADMIN") return@post call.respond(HttpStatusCode.Forbidden)

                     val count = call.request.queryParameters["count"]?.toIntOrNull() ?: 5
                     val created = mutableListOf<String>()
                     
                     repeat(count) { i ->
                         val phone = "987654321$i"
                         try {
                             val uid = userRepository.createUser(phone)
                             val name = "Test User ${i + 1}"
                             val username = "user_${System.currentTimeMillis()}_$i"
                             userRepository.updateProfile(uid, name, username, "binary", "test$i@example.com", null)
                             userRepository.completeOnboarding(uid, username, null, null)
                             // Grant some credits
                             val adminId = UUID.fromString(call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asString())
                             userRepository.grantCredits(adminId, uid, 100 * (i + 1), "Initial Seed", null)
                             created.add(uid.toString())
                         } catch (e: Exception) {
                             e.printStackTrace()
                             // Ignore duplicates
                         }
                     }
                     call.respond(mapOf("created" to created))
                 }


            }
        }
    }
}
