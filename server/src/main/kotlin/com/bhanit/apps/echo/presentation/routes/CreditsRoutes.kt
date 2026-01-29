package com.bhanit.apps.echo.presentation.routes

import com.bhanit.apps.echo.data.model.PurchaseRequest
import com.bhanit.apps.echo.data.model.PurchaseType
import com.bhanit.apps.echo.data.table.Users
import com.bhanit.apps.echo.domain.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

import com.bhanit.apps.echo.data.repository.EmotionRepository


import com.bhanit.apps.echo.domain.service.CreditService
import com.bhanit.apps.echo.util.CreditConstants
import com.bhanit.apps.echo.data.model.MilestoneStatusDto
import com.bhanit.apps.echo.data.model.MilestoneStatus
import com.bhanit.apps.echo.data.table.CreditTransactions
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import com.bhanit.apps.echo.util.SeasonalConfig
import com.bhanit.apps.echo.util.SeasonMeta // Start using Shared one
import com.bhanit.apps.echo.util.SeasonalRuleType
import com.bhanit.apps.echo.data.model.TransactionType
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

fun Route.creditsRoutes(
    userRepository: UserRepository, 
    emotionRepository: EmotionRepository,
    creditService: CreditService,
    habitRepository: com.bhanit.apps.echo.domain.repository.HabitRepository, // Needed for streak data
    seasonalEventRepository: com.bhanit.apps.echo.domain.repository.SeasonalEventRepository
) {
    route("/user") {
        
        get("/credits") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val userId = if (userIdString != null) UUID.fromString(userIdString) else return@get call.respond(HttpStatusCode.Unauthorized)

            val user = userRepository.getUser(userId)
            if (user != null) {
                // Return simple map or DTO
                call.respond(mapOf("credits" to user[Users.credits]))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/credits/milestones") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val userId = if (userIdString != null) UUID.fromString(userIdString) else return@get call.respond(HttpStatusCode.Unauthorized)

            // 1. Get current streaks
            val streaks = habitRepository.getStreaks(userId)
            
            // Fetch User for Timezone
            val user = userRepository.getUser(userId)
            val zoneId = try {
                 if (user?.get(Users.timezone) != null) java.time.ZoneId.of(user[Users.timezone]) else java.time.ZoneId.systemDefault()
            } catch (e: Exception) { java.time.ZoneId.systemDefault() }
            val today = java.time.LocalDate.now(zoneId)

            // 2. Map milestones to status
            val milestoneDtos = transaction {
                CreditConstants.MILESTONES.map { milestone ->
                    val currentProgress = when (milestone.type) {
                        com.bhanit.apps.echo.data.model.MilestoneType.STREAK_PRESENCE -> streaks?.presenceStreak ?: 0
                        com.bhanit.apps.echo.data.model.MilestoneType.STREAK_KINDNESS -> streaks?.kindnessStreak ?: 0
                        com.bhanit.apps.echo.data.model.MilestoneType.STREAK_RESPONSE -> streaks?.responseStreak ?: 0
                        else -> 0
                    }
                    
                    // Check if claimed
                    // A milestone is claimed if a transaction exists with relatedId = "STREAK_REWARD_{TYPE}_{COUNT}"
                    val relatedIdToCheck = "STREAK_REWARD_${milestone.type.name}_${milestone.requiredCount}"
                    val isClaimed = CreditTransactions.selectAll().where {
                        (CreditTransactions.userId eq userId) and (CreditTransactions.relatedId eq relatedIdToCheck)
                    }.count() > 0

                    val status = when {
                        isClaimed -> MilestoneStatus.CLAIMED
                        currentProgress >= milestone.requiredCount -> MilestoneStatus.CLAIMED // Visually treat as claimed/completed
                        else -> MilestoneStatus.IN_PROGRESS
                    }
                    
                    // If not claimed but met requirement, it means auto-claim hasn't run or just happened. 
                    // Our logic is auto-claim on event. 
                    // "Locked" logic: actually strictly speaking STREAK is always "In Progress" until hit.
                    // But effectively: 
                    // If current < required -> IN_PROGRESS (Visual: Locked/Greyed out but bar filling)
                    // If current >= required & !claimed -> UNCLAIMED (Should not happen with auto-claim)
                    // If claimed -> CLAIMED
                    
                    // For UI simplicity: 
                    // If claimed -> CLAIMED
                    // Else -> IN_PROGRESS (show bar)
                    
                    MilestoneStatusDto(
                        id = milestone.id,
                        displayName = milestone.displayName,
                        description = milestone.description,
                        progress = currentProgress,
                        required = milestone.requiredCount,
                        percentage = (currentProgress.toFloat() / milestone.requiredCount.toFloat() * 100).coerceIn(0f, 100f).toInt(),
                        status = if (isClaimed) MilestoneStatus.CLAIMED else MilestoneStatus.IN_PROGRESS, // Use IN_PROGRESS for both locked/active logic to keep simple
                        rewardCredits = milestone.rewardCredits,
                        instruction = when (milestone.type) {
                            com.bhanit.apps.echo.data.model.MilestoneType.STREAK_PRESENCE -> "Open Echo every day to build this streak."
                            com.bhanit.apps.echo.data.model.MilestoneType.STREAK_KINDNESS -> "Send one positive echo each day."
                            com.bhanit.apps.echo.data.model.MilestoneType.STREAK_RESPONSE -> "Respond to an echo shared with you each day."
                            else -> "Complete the action to earn this reward."
                        }
                    )
                }
            }
            
            
            // 3. Add Consistency & Seasonal (Server-side calculation)
            val currentYear = today.year
            val allSeasonalEvents = seasonalEventRepository.getEventsByYear(currentYear)
            
            // Optimization (Elite Improvement 2): Use Cached Meta instead of Ledger Scan
            val seasonalMetaMap = transaction {
                 val row = com.bhanit.apps.echo.data.table.UserActivityMeta.selectAll()
                    .where { com.bhanit.apps.echo.data.table.UserActivityMeta.userId eq userId }
                    .singleOrNull()
                 val json = row?.get(com.bhanit.apps.echo.data.table.UserActivityMeta.seasonRewardMeta)
                 if (json != null) {
                     try { Json.decodeFromString<MutableMap<String, SeasonMeta>>(json) } catch(e: Exception) { mutableMapOf() }
                 } else {
                     mutableMapOf()
                 }
            }
            
            // A. Consistency (Needs DB Meta) -> Reuse meta fetch? 
            // We fetched meta above inside transaction. We can merge these blocks.
            // Let's keep logic simple. We need to reuse the `totalActiveDays` too.
            val totalActiveDays = transaction {
                 com.bhanit.apps.echo.data.table.UserActivityMeta.select(com.bhanit.apps.echo.data.table.UserActivityMeta.totalActiveDays)
                    .where { com.bhanit.apps.echo.data.table.UserActivityMeta.userId eq userId }
                    .singleOrNull()?.get(com.bhanit.apps.echo.data.table.UserActivityMeta.totalActiveDays) ?: 0
            }
            val nextConsistency_target = ((totalActiveDays / 10) + 1) * 10
            val consistencyMilestone = MilestoneStatusDto(
                id = "CONSISTENCY_TRACKER",
                displayName = "Total Active Days",
                description = "Consistency builds identity.",
                progress = totalActiveDays,
                required = nextConsistency_target,
                percentage = (totalActiveDays.toFloat() / nextConsistency_target.toFloat() * 100).toInt(),
                status = MilestoneStatus.IN_PROGRESS,
                rewardCredits = 5,
                instruction = "Stay active consistently to earn this appreciation."
            )
            
             // B. Seasonal Milestones
            val seasonalMilestones = mutableListOf<MilestoneStatusDto>()
            allSeasonalEvents.forEach { event ->
                val start = java.time.LocalDate.of(currentYear, event.startMonth, event.startDay)
                val end = java.time.LocalDate.of(currentYear, event.endMonth, event.endDay)
                
                val isActive = !today.isBefore(start) && !today.isAfter(end)
                val isUpcoming = today.isBefore(start)
                
                if (isActive) {
                    // Active: Read Cache
                    val seasonCache = seasonalMetaMap[event.id]
                    
                    event.rules.forEach { rule ->
                        val current = seasonCache?.counts?.get(rule.type.name) ?: 0
                        
                        // Check if claimed (Reward Transaction Exists)
                        // Or calculate from progress if we trust the loop logic "If >= maxTotal, we awarded it".
                        // Better to check actual transaction for "CLAIMED" status to be safe.
                        // Or assume if current >= maxTotal, it's claimed (since we auto-claim).
                        val isClaimed = current >= rule.maxTotal
                        
                        seasonalMilestones.add(
                            MilestoneStatusDto(
                                id = "SEASON_${event.id}_${rule.type}",
                                displayName = event.name + " (" + rule.type.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() } + ")",
                                description = "Earn credits during ${event.name}",
                                progress = current,
                                required = rule.maxTotal,
                                percentage = (current.toFloat() / rule.maxTotal.toFloat() * 100).coerceIn(0f, 100f).toInt(),
                                status = if (isClaimed) MilestoneStatus.CLAIMED else MilestoneStatus.IN_PROGRESS,
                                rewardCredits = rule.bonusCredits,
                                colorHex = event.colorHex,
                                category = event.category.name,
                                startDate = start.toString(),
                                endDate = end.toString(),
                                instruction = when (rule.type) {
                                    SeasonalRuleType.SEND_POSITIVE -> "Send echoes during ${event.name}."
                                    SeasonalRuleType.RESPOND -> "Reply to echoes during ${event.name}."
                                    SeasonalRuleType.COMEBACK -> "Return to Echo during ${event.name}."
                                    SeasonalRuleType.BALANCED_DAY_MULTIPLIER -> "Achieve a Balanced Day during ${event.name}."
                                }
                            )
                        )
                    }
                } else if (isUpcoming) {
                    // Upcoming: Locked
                    event.rules.take(1).forEach { rule ->
                         seasonalMilestones.add(
                            MilestoneStatusDto(
                                id = "SEASON_${event.id}_UPCOMING",
                                displayName = event.name,
                                description = "Starts on ${start.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${start.dayOfMonth}",
                                progress = 0,
                                required = rule.maxTotal,
                                percentage = 0,
                                status = MilestoneStatus.LOCKED, // Locked
                                rewardCredits = rule.bonusCredits,
                                colorHex = event.colorHex,
                                category = event.category.name,
                                startDate = start.toString(),
                                endDate = end.toString(),
                                instruction = "Get ready! Event starts soon."
                            )
                        )
                    }
                }
            }
            
            call.respond(milestoneDtos + consistencyMilestone + seasonalMilestones)
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
            // Filter out internal tracking events from the wallet view.
            // We show "Financial" transactions (Rewards, Purchases) but hide "Progress" events (Sent Echo, etc).
            val hiddenTypes = listOf(TransactionType.SENT_ECHO, TransactionType.RECEIVED_ECHO)
            val filteredHistory = history.filter { it.type !in hiddenTypes }
            call.respond(filteredHistory)
        }

        post("/purchase") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userIdString = principal?.payload?.getClaim("id")?.asString()
            val userId = if (userIdString != null) UUID.fromString(userIdString) else return@post call.respond(HttpStatusCode.Unauthorized)

            try {
                val request = call.receive<PurchaseRequest>()
                
                // Determine Cost
                val cost = when (request.type) {
                    PurchaseType.EMOTION_UNLOCK -> {
                        if (request.itemId.isBlank()) throw IllegalArgumentException("Emotion ID required.")
                        val emotions = emotionRepository.getAllEmotions()
                        val emotion = emotions.find { it.id == request.itemId } ?: throw IllegalArgumentException("Invalid Emotion ID")
                        emotion.price
                    }
                    PurchaseType.LIMIT_BOOST -> 1
                    PurchaseType.EMOTION_TIME -> 100 // Legacy
                    PurchaseType.EMOTION_COUNT -> 50 * (request.count ?: 1) // Legacy
                    PurchaseType.RELATION_QUOTA -> 20 * (request.count ?: 1) // Legacy
                }

                val success = userRepository.purchaseItem(userId, request, cost)
                if (success) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.BadRequest, com.bhanit.apps.echo.data.model.ApiErrorDTO(errorMessage = "Purchase failed"))
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, com.bhanit.apps.echo.data.model.ApiErrorDTO(errorMessage = e.message ?: "Invalid Request"))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, com.bhanit.apps.echo.data.model.ApiErrorDTO(errorMessage = "Failed to process purchase"))
            }
        }
    }
}
