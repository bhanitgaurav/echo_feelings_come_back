package com.bhanit.apps.echo.data.service

import com.bhanit.apps.echo.util.SeasonalConfig
import com.bhanit.apps.echo.util.SeasonMeta
import com.bhanit.apps.echo.util.SeasonalRuleType
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import com.bhanit.apps.echo.data.model.MilestoneType
import com.bhanit.apps.echo.data.model.TransactionType
import com.bhanit.apps.echo.data.table.CreditTransactions
import com.bhanit.apps.echo.data.table.UserActivityMeta
import com.bhanit.apps.echo.data.table.Users
import com.bhanit.apps.echo.data.table.UserStreaks
import com.bhanit.apps.echo.data.table.RewardHistory
import com.bhanit.apps.echo.data.table.OneTimeRewardType
import com.bhanit.apps.echo.domain.service.CreditService
import com.bhanit.apps.echo.util.CreditConstants
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


import com.bhanit.apps.echo.domain.repository.SeasonalEventRepository

class CreditServiceImpl(
    private val seasonalEventRepository: SeasonalEventRepository,
    private val notificationService: com.bhanit.apps.echo.domain.service.NotificationService
) : CreditService {

    override suspend fun awardCredits(
        userId: UUID,
        amount: Int,
        type: TransactionType,
        description: String,
        relatedId: String?,
        visibility: com.bhanit.apps.echo.data.table.TransactionVisibility,
        source: com.bhanit.apps.echo.data.table.RewardSource,
        intent: com.bhanit.apps.echo.data.table.TransactionIntent,
        metadata: String?
    ) = dbQuery {
        // 1. Update Balance
        Users.update({ Users.id eq userId }) {
            with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                it[Users.credits] = Users.credits + amount
            }
        }

        // 2. Transact
        com.bhanit.apps.echo.data.table.CreditTransactions.insert {
            it[com.bhanit.apps.echo.data.table.CreditTransactions.userId] = userId
            it[com.bhanit.apps.echo.data.table.CreditTransactions.amount] = amount
            it[com.bhanit.apps.echo.data.table.CreditTransactions.type] = type
            it[com.bhanit.apps.echo.data.table.CreditTransactions.description] = description
            it[com.bhanit.apps.echo.data.table.CreditTransactions.relatedId] = relatedId
            it[com.bhanit.apps.echo.data.table.CreditTransactions.createdAt] = java.time.Instant.now()
            it[com.bhanit.apps.echo.data.table.CreditTransactions.visibility] = visibility
            it[com.bhanit.apps.echo.data.table.CreditTransactions.rewardSource] = source
            it[com.bhanit.apps.echo.data.table.CreditTransactions.intent] = intent
            it[com.bhanit.apps.echo.data.table.CreditTransactions.metadataJson] = metadata
        }
        
        // 3. Notify (Fire & Forget logic via safe try-catch inside runBlocking or just suspend match)
        // Since we are in dbQuery (transaction block), we should be careful about side effects.
        // Ideally we should send notification AFTER transaction commit. 
        // But exposed 'newSuspendedTransaction' or 'transaction' blocks don't easily support post-commit hooks without boilerplate.
        // However, FCM sending is a network call. Doing it inside DB transaction holds the connection.
        // Better to return a flag or do it outside.
        // 'awardCredits' returns Unit.
        // We can launch a coroutine scope using GlobalScope or similar if we want to decouple from DB transaction.
        // Or better: notificationService.sendRewardNotification is a suspend function.
        // If we call it here, it blocks the DB transaction. BAD practice.
        // BUT dbQuery in DatabaseFactory usually wraps `newSuspendedTransaction`.
        
        // Let's use GlobalScope just to be safe and non-blocking for the DB, 
        // OR better yet, since we are inside `dbQuery` which is likely `newSuspendedTransaction(Dispatchers.IO)`,
        // we should try to move side effects outside.
        // But `awardCredits` body IS the `dbQuery` block.
        
        // Practical compromise: Launch a coroutine for notification.
        kotlinx.coroutines.GlobalScope.launch {
            try {
                if (visibility == com.bhanit.apps.echo.data.table.TransactionVisibility.VISIBLE &&
                    intent == com.bhanit.apps.echo.data.table.TransactionIntent.REWARD &&
                    amount > 0) {
                    notificationService.sendRewardNotification(userId, type, amount, relatedId, description)
                }
            } catch (e: Exception) {
                println("WARN: Failed to trigger reward notification: ${e.message}")
            }
        }
        Unit
    }

    override suspend fun checkAndAwardStreakMilestone(userId: UUID, streakType: MilestoneType, currentStreak: Int, cycle: Int) = dbQuery {
        val milestone = CreditConstants.MILESTONES.find { 
            it.type == streakType && it.requiredCount == currentStreak 
        } ?: return@dbQuery

        // Infinite Cycle Logic: Unique ID includes Cycle
        val relatedId = "STREAK_REWARD_${streakType.name}_${currentStreak}_CYCLE_${cycle}"

        // Idempotency Check
        val exists = com.bhanit.apps.echo.data.table.CreditTransactions
            .select(com.bhanit.apps.echo.data.table.CreditTransactions.id)
            .where {
                (com.bhanit.apps.echo.data.table.CreditTransactions.userId eq userId) and
                (com.bhanit.apps.echo.data.table.CreditTransactions.relatedId eq relatedId)
            }
            .count() > 0

        if (!exists) {
            awardCredits(
                userId = userId,
                amount = milestone.rewardCredits,
                type = TransactionType.STREAK_REWARD,
                description = "${milestone.displayName} Streak",
                relatedId = relatedId,
                source = com.bhanit.apps.echo.data.table.RewardSource.STREAK,
                intent = com.bhanit.apps.echo.data.table.TransactionIntent.REWARD,
                metadata = null
            )
        }
    }
    
    override suspend fun checkAndAwardConsistency(userId: UUID, date: LocalDate) = dbQuery {
        val today = date
        
        // 1. Get/Update total active days
        var meta = UserActivityMeta.selectAll().where { UserActivityMeta.userId eq userId }.singleOrNull()
        
        if (meta == null) {
            UserActivityMeta.insert {
                it[this.userId] = userId
                it[totalActiveDays] = 1
                it[lastBalancedBonusAt] = null // Init
            }
            meta = UserActivityMeta.selectAll().where { UserActivityMeta.userId eq userId }.single()
        } else {
             // Only increment if not already updated today?
             // Actually HabitService logic calls this every activity. We should only increment once per day.
             // We can use lastBalancedBonusAt as a "last active check" proxy or add a new field.
             // But simpler: We assume `HabitService` calls us.
             // We need to know if we already incremented today.
             // Let's rely on `presenceLastActiveAt` from `UserStreaks` or `lastActiveAt` from `Users`? 
             // `UserStreaks` tracks specific streaks.
             // Let's use `UserActivityMeta.totalActiveDays`. 
             // Ideally we shouldn't increment if already active.
             // BUT `HabitService.updateStreak` logic checks `if (lastActive == today) return`. 
             // So this method is ONLY called if it's a NEW active day.
             // So we can safely increment.
             
             UserActivityMeta.update({ UserActivityMeta.userId eq userId }) {
                 with(SqlExpressionBuilder) {
                     it.update(totalActiveDays, totalActiveDays + 1)
                 }
             }
        }
        
        // Refresh meta
        val newCount = UserActivityMeta.select(UserActivityMeta.totalActiveDays)
            .where { UserActivityMeta.userId eq userId }
            .single()[UserActivityMeta.totalActiveDays] + 1 // +1 because update happened inside dbQuery transaction scope?
            // Actually Exposed update returns count, not value.
            // We need to re-fetch or trust logic.
            // Let's refetch.
        val currentDays = UserActivityMeta.selectAll().where { UserActivityMeta.userId eq userId }.single()[UserActivityMeta.totalActiveDays]

        // 2. Check for Consistency Reward (Every 10 days)
        if (currentDays > 0 && currentDays % 10 == 0) {
            val scalingAmount = when {
                currentDays <= 100 -> 5
                currentDays <= 500 -> 4
                else -> 3
            }
            
            val relatedId = "CONSISTENCY_CYCLE_${currentDays}"
            
            // Idempotency Check
            val exists = com.bhanit.apps.echo.data.table.CreditTransactions
                 .select(com.bhanit.apps.echo.data.table.CreditTransactions.id)
                 .where {
                     (com.bhanit.apps.echo.data.table.CreditTransactions.userId eq userId) and
                     (com.bhanit.apps.echo.data.table.CreditTransactions.relatedId eq relatedId)
                 }
                 .count() > 0

            if (!exists) {
                awardCredits(
                    userId = userId,
                    amount = scalingAmount,
                    type = TransactionType.BALANCED_ACTIVITY_BONUS, // Or new CONSISTENCY_BONUS type? Use BALANCED for now or MANUAL
                    description = "Consistency Appreciation",
                    relatedId = relatedId,
                    source = com.bhanit.apps.echo.data.table.RewardSource.SYSTEM,
                    intent = com.bhanit.apps.echo.data.table.TransactionIntent.REWARD,
                    metadata = null
                )
            }
        }
    }

    private val evaluator = SeasonalRewardEvaluator(seasonalEventRepository, this)

    override suspend fun checkAndAwardSeasonal(userId: UUID, date: LocalDate, ruleType: SeasonalRuleType, relatedSourceId: String?) {
        evaluator.evaluate(userId, ruleType, date, relatedSourceId)
    }

    override suspend fun recordOneTimeReward(phoneHash: String, rewardType: com.bhanit.apps.echo.data.table.OneTimeRewardType) = dbQuery {
        // Idempotent insert
        val exists = RewardHistory.select(RewardHistory.phoneHash)
            .where { (RewardHistory.phoneHash eq phoneHash) and (RewardHistory.rewardType eq rewardType) }
            .count() > 0
            
        if (!exists) {
            RewardHistory.insert {
                it[this.phoneHash] = phoneHash
                it[this.rewardType] = rewardType
                it[this.claimedAt] = java.time.Instant.now()
            }
        }
    }


    override suspend fun checkAndAwardBalancedBonus(userId: UUID) = dbQuery {
        val today = LocalDate.now()
        
        // 1. Check if already awarded today
        val meta = UserActivityMeta.selectAll().where { UserActivityMeta.userId eq userId }.singleOrNull()
        val lastAwarded = meta?.get(UserActivityMeta.lastBalancedBonusAt)
        
        if (lastAwarded == today) return@dbQuery

        // Double Check Transaction Log (System Hardening)
        val relatedId = "BALANCED_BONUS_$today"
        val exists = com.bhanit.apps.echo.data.table.CreditTransactions
            .select(com.bhanit.apps.echo.data.table.CreditTransactions.id)
            .where {
                (com.bhanit.apps.echo.data.table.CreditTransactions.userId eq userId) and
                (com.bhanit.apps.echo.data.table.CreditTransactions.relatedId eq relatedId)
            }
            .count() > 0

        if (exists) return@dbQuery

        // 2. Check if all 3 streaks were active TODAY
        val streaks = UserStreaks.selectAll().where { UserStreaks.userId eq userId }.singleOrNull() ?: return@dbQuery
        
        val presenceActive = streaks[UserStreaks.presenceLastActiveAt] == today
        val kindnessActive = streaks[UserStreaks.kindnessLastActiveAt] == today
        val responseActive = streaks[UserStreaks.responseLastActiveAt] == today // Assuming we track this now

        if (presenceActive && kindnessActive && responseActive) {
            // Award
            awardCredits(
                userId = userId,
                amount = CreditConstants.BALANCED_ACTIVITY_BONUS,
                type = TransactionType.BALANCED_ACTIVITY_BONUS,
                description = "A Balanced Day",
                relatedId = "BALANCED_BONUS_$today",
                source = com.bhanit.apps.echo.data.table.RewardSource.STREAK,
                intent = com.bhanit.apps.echo.data.table.TransactionIntent.REWARD,
                metadata = null
            )
            
            // Update Meta
            if (meta == null) {
                UserActivityMeta.insert {
                    it[this.userId] = userId
                    it[lastBalancedBonusAt] = today
                }
            } else {
                UserActivityMeta.update({ UserActivityMeta.userId eq userId }) {
                    it[lastBalancedBonusAt] = today
                }
            }
        }
        Unit
    }
    
    override suspend fun checkAndAwardWeeklyReflection(userId: UUID) = dbQuery {
        val today = LocalDate.now()
        val weekFields = WeekFields.of(Locale.getDefault())
        val weekNumber = today.get(weekFields.weekOfWeekBasedYear())
        val year = today.get(weekFields.weekBasedYear())
        val currentWeekIso = "$year-$weekNumber"
        
        val meta = UserActivityMeta.selectAll().where { UserActivityMeta.userId eq userId }.singleOrNull()
        val lastReflectionWeek = meta?.get(UserActivityMeta.lastReflectionRewardWeek)

        if (lastReflectionWeek == currentWeekIso) return@dbQuery

        // Award
        awardCredits(
            userId = userId,
            amount = CreditConstants.WEEKLY_REFLECTION_REWARD,
            type = TransactionType.WEEKLY_REFLECTION_REWARD,
            description = "Weekly Reflection",
            relatedId = "REFLECTION_$currentWeekIso",
            source = com.bhanit.apps.echo.data.table.RewardSource.SYSTEM,
            intent = com.bhanit.apps.echo.data.table.TransactionIntent.REWARD,
            metadata = null
        )
        
        // Update Meta
        if (meta == null) {
            UserActivityMeta.insert {
                it[this.userId] = userId
                it[lastReflectionRewardWeek] = currentWeekIso
            }
        } else {
            UserActivityMeta.update({ UserActivityMeta.userId eq userId }) {
                it[lastReflectionRewardWeek] = currentWeekIso
            }
        }
        Unit
    }


    override suspend fun checkAndAwardFirstEcho(userId: UUID) = dbQuery {
        // 1. Check local transaction history
        val exists = com.bhanit.apps.echo.data.table.CreditTransactions
            .select(com.bhanit.apps.echo.data.table.CreditTransactions.id)
            .where { 
                (com.bhanit.apps.echo.data.table.CreditTransactions.userId eq userId) and 
                (com.bhanit.apps.echo.data.table.CreditTransactions.relatedId eq "FIRST_TIME_ECHO") 
            }
            .count() > 0

        if (exists) return@dbQuery

        // 2. Check Global One-Time Reward History (Double-Dip Prevention)
        val user = Users.selectAll().where { Users.id eq userId }.singleOrNull() ?: return@dbQuery
        val phoneHash = user[Users.phoneHash]

        val alreadyClaimed = RewardHistory.select(RewardHistory.phoneHash)
            .where { (RewardHistory.phoneHash eq phoneHash) and (RewardHistory.rewardType eq OneTimeRewardType.FIRST_ECHO) }
            .count() > 0
            
        if (alreadyClaimed) return@dbQuery

        // 3. Award
        awardCredits(
            userId = userId,
            amount = CreditConstants.FIRST_ECHO_BONUS,
            type = TransactionType.FIRST_TIME_BONUS,
            description = "Your First Echo",
            relatedId = "FIRST_TIME_ECHO",
            source = com.bhanit.apps.echo.data.table.RewardSource.SYSTEM,
            intent = com.bhanit.apps.echo.data.table.TransactionIntent.REWARD,
            metadata = null
        )
        
        // 4. Record Global History
        recordOneTimeReward(phoneHash, OneTimeRewardType.FIRST_ECHO)
    }

    override suspend fun checkAndAwardFirstResponse(userId: UUID) = dbQuery {
        // 1. Check local transaction history
        val exists = com.bhanit.apps.echo.data.table.CreditTransactions
            .select(com.bhanit.apps.echo.data.table.CreditTransactions.id)
            .where { 
                (com.bhanit.apps.echo.data.table.CreditTransactions.userId eq userId) and 
                (com.bhanit.apps.echo.data.table.CreditTransactions.relatedId eq "FIRST_TIME_RESPONSE") 
            }
            .count() > 0

        if (exists) return@dbQuery

        // 2. Check Global One-Time Reward History
        val user = Users.selectAll().where { Users.id eq userId }.singleOrNull() ?: return@dbQuery
        val phoneHash = user[Users.phoneHash]

        val alreadyClaimed = RewardHistory.select(RewardHistory.phoneHash)
            .where { (RewardHistory.phoneHash eq phoneHash) and (RewardHistory.rewardType eq OneTimeRewardType.FIRST_RESPONSE) }
            .count() > 0
            
        if (alreadyClaimed) return@dbQuery

        // 3. Award
        awardCredits(
            userId = userId,
            amount = CreditConstants.FIRST_RESPONSE_BONUS,
            type = TransactionType.FIRST_TIME_BONUS,
            description = "Your First Response",
            relatedId = "FIRST_TIME_RESPONSE",
            source = com.bhanit.apps.echo.data.table.RewardSource.SYSTEM,
            intent = com.bhanit.apps.echo.data.table.TransactionIntent.REWARD,
            metadata = null
        )
        
        // 4. Record Global History
        recordOneTimeReward(phoneHash, OneTimeRewardType.FIRST_RESPONSE)
    }
}
