package com.bhanit.apps.echo.data.service

import com.bhanit.apps.echo.data.model.TransactionType
import com.bhanit.apps.echo.data.table.UserActivityMeta
import com.bhanit.apps.echo.data.table.CreditTransactions
import com.bhanit.apps.echo.domain.repository.SeasonalEventRepository
import com.bhanit.apps.echo.domain.service.CreditService
import com.bhanit.apps.echo.domain.service.RewardEvaluator
import com.bhanit.apps.echo.util.SeasonMeta
import com.bhanit.apps.echo.util.SeasonalRuleType
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.time.LocalDate
import java.util.UUID

class SeasonalRewardEvaluator(
    private val seasonalEventRepository: SeasonalEventRepository,
    private val creditService: CreditService
) : RewardEvaluator {

    override val priority: Int = 100
    
    // Using relaxed Json to handle schema changes gracefully
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun evaluate(userId: UUID, eventType: SeasonalRuleType, date: LocalDate, relatedSourceId: String?) {
        val activeEvents = seasonalEventRepository.getActiveEvents(date)
        if (activeEvents.isEmpty()) return

        newSuspendedTransaction {
            // 1. Get/Init Meta
            val metaRow = UserActivityMeta.selectAll().where { UserActivityMeta.userId eq userId }.singleOrNull()
            var jsonMeta = metaRow?.get(UserActivityMeta.seasonRewardMeta)
            val seasonData = if (jsonMeta != null) {
                try { json.decodeFromString<MutableMap<String, SeasonMeta>>(jsonMeta) } catch(e: Exception) { mutableMapOf() }
            } else {
                mutableMapOf()
            }

            var metaUpdated = false

            // Overlap Policy: STACKING (Check all active events)
            for (event in activeEvents) {
                val rule = event.rules.find { it.type == eventType } ?: continue

                // Clean up / Init Season Data
                val currentSeasonData = seasonData.getOrPut(event.id) { SeasonMeta() }
                val currentCount = currentSeasonData.counts.getOrPut(eventType.name) { 0 }
                val lastTime = currentSeasonData.lastAwardedAt.getOrPut(eventType.name) { 0L }
                
                // Daily Cap Key
                val dailyKey = "${eventType.name}_$date"
                val dailyCount = currentSeasonData.dailyCounts.getOrPut(dailyKey) { 0 }
                
                // 2. Check Global & Daily Caps
                if (currentCount >= rule.maxTotal) continue
                if (rule.oncePerSeason && currentCount >= 1) continue
                if (dailyCount >= rule.dailyCap) continue // Daily Cap Enforcement

                // 3. Check Cooldown
                val now = System.currentTimeMillis()
                if (rule.cooldownHours > 0) {
                     val cooldownMillis = rule.cooldownHours * 3600 * 1000L
                     if (now - lastTime < cooldownMillis) continue // Cooldown active
                }

                // 4. Generate Idempotency Key (Event-Based or Fallback)
                val idempotencyKey = if (relatedSourceId != null) {
                    "SEASON_${event.id}_${eventType.name}_$relatedSourceId" // Strong: Anchored to Event
                } else {
                    "SEASON_${event.id}_${eventType.name}_${currentCount + 1}" // Weak: Anchored to Count
                }

                // 5. Pre-Check: Does this reward already exist?
                val alreadyAwarded = CreditTransactions.selectAll().where { 
                    CreditTransactions.relatedId eq idempotencyKey 
                }.count() > 0

                if (alreadyAwarded) continue 

                // 6. Award Credits with Explainability
                try {
                    val metadataMap = mapOf(
                        "season" to event.id,
                        "rule" to eventType.name,
                        "progress" to "${currentCount + 1}/${rule.maxTotal}",
                        "daily_progress" to "${dailyCount + 1}/${rule.dailyCap}"
                    )
                    
                    creditService.awardCredits(
                        userId = userId,
                        amount = rule.bonusCredits,
                        type = TransactionType.SEASON_REWARD, 
                        description = "${event.name} Appreciation",
                        relatedId = idempotencyKey,
                        visibility = com.bhanit.apps.echo.data.table.TransactionVisibility.VISIBLE,
                        source = com.bhanit.apps.echo.data.table.RewardSource.SEASONAL,
                        intent = com.bhanit.apps.echo.data.table.TransactionIntent.REWARD,
                        metadata = json.encodeToString(metadataMap)
                    )
                    
                    // 7. Update Meta
                    currentSeasonData.counts[eventType.name] = currentCount + 1
                    currentSeasonData.lastAwardedAt[eventType.name] = now
                    currentSeasonData.dailyCounts[dailyKey] = dailyCount + 1
                    metaUpdated = true
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 8. Flush Meta to DB
            if (metaUpdated) {
                UserActivityMeta.update({ UserActivityMeta.userId eq userId }) {
                    it[seasonRewardMeta] = json.encodeToString(seasonData)
                    it[lastSeasonRewardAt] = date
                }
            }
        }
    }
}
