package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.data.table.EmotionSentiment
import com.bhanit.apps.echo.domain.repository.HabitRepository
import com.bhanit.apps.echo.domain.repository.UserStreakData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID


class HabitService(
    private val habitRepository: HabitRepository,
    private val creditService: com.bhanit.apps.echo.domain.service.CreditService
) {

    enum class ActivityType {
        ECHOS_OPEN,     // Updates Presence
        DASHBOARD_OPEN, // Updates Presence
        MESSAGE_SENT,   // Updates Presence, Kindness (if positive)
        ECHO_BACK_SENT  // Updates Response (NOT Presence/Kindness)
    }

    suspend fun recordActivity(userId: UUID, type: ActivityType, emotionId: String? = null, date: LocalDate = LocalDate.now()) {
        var streakData = habitRepository.getStreaks(userId)
        if (streakData == null) {
            habitRepository.createStreaks(userId)
            streakData = habitRepository.getStreaks(userId)!!
        }

        val today = date

        // 1. Presence Streak
        if (type != ActivityType.ECHO_BACK_SENT) {
            updateStreak(
                userId, 
                streakData.presenceStreak,
                streakData.presenceCycle,
                streakData.presenceLastActiveAt, 
                today,
                streakData.gracePeriodUsedAt
            ) { newStreak, newCycle, newLastActive ->
                habitRepository.updatePresenceStreak(userId, newStreak, newCycle, newLastActive)
                // Trigger Credits (pass cycle)
                creditService.checkAndAwardStreakMilestone(userId, com.bhanit.apps.echo.data.model.MilestoneType.STREAK_PRESENCE, newStreak, newCycle)
                
                // Consistency & Seasonal
                creditService.checkAndAwardConsistency(userId, today)
                creditService.checkAndAwardSeasonal(userId, newLastActive, com.bhanit.apps.echo.util.SeasonalRuleType.BALANCED_DAY_MULTIPLIER, relatedSourceId = newLastActive.toString()) // Check for balanced
            }
        }

        // 2. Response Streak
        if (type == ActivityType.ECHO_BACK_SENT) {
            updateSimpleStreak(
                userId,
                streakData.responseStreak,
                streakData.responseCycle,
                streakData.responseLastActiveAt,
                today
            ) { newStreak, newCycle, newLastActive ->
                habitRepository.updateResponseStreak(userId, newStreak, newCycle, newLastActive)
                // Trigger Credits
                creditService.checkAndAwardStreakMilestone(userId, com.bhanit.apps.echo.data.model.MilestoneType.STREAK_RESPONSE, newStreak, newCycle)
                
                creditService.checkAndAwardFirstResponse(userId)
            }
        }

        // 3. Kindness Streak
        if (type == ActivityType.MESSAGE_SENT && emotionId != null) {
            val sentiment = habitRepository.getEmotionSentiment(emotionId)
            
            // Allow First Echo Bonus regardless of sentiment? 
            if (sentiment == EmotionSentiment.POSITIVE) {
                updateSimpleStreak(
                    userId,
                    streakData.kindnessStreak,
                    streakData.kindnessCycle,
                    streakData.kindnessLastActiveAt,
                    today
                ) { newStreak, newCycle, newLastActive ->
                    habitRepository.updateKindnessStreak(userId, newStreak, newCycle, newLastActive)
                    // Trigger Credits
                    creditService.checkAndAwardStreakMilestone(userId, com.bhanit.apps.echo.data.model.MilestoneType.STREAK_KINDNESS, newStreak, newCycle)
                }
            }
        }
        
        if (type == ActivityType.MESSAGE_SENT) {
             creditService.checkAndAwardFirstEcho(userId)
        }
    }

    private suspend fun updateStreak(
        userId: UUID,
        currentStreak: Int,
        currentCycle: Int,
        lastActive: LocalDate?,
        today: LocalDate,
        graceUsedAt: LocalDateTime?,
        updateCallback: suspend (Int, Int, LocalDate) -> Unit
    ) {
        if (lastActive == today) return 

        if (lastActive == null) {
            // First time
            updateCallback(1, 1, today)
            return
        }

        val daysBetween = ChronoUnit.DAYS.between(lastActive, today)

        when (daysBetween) {
            1L -> {
                // Consecutive day
                updateCallback(currentStreak + 1, currentCycle, today)
            }
            else -> {
                // Check Grace
                if (daysBetween == 2L && isGraceAvailable(graceUsedAt)) {
                    // Use Grace
                    habitRepository.useGracePeriod(userId, LocalDateTime.now())
                    updateCallback(currentStreak + 1, currentCycle, today)
                } else {
                    // Break streak -> Increment Cycle! (Recyclable Logic)
                    // If currentStreak was significant (>1?), we consider it a reset of a cycle.
                    // If streak was 0 or 1, does cycle increment? Usually yes, every "try" is a cycle.
                    val newCycle = if (currentStreak > 1) currentCycle + 1 else currentCycle 
                    updateCallback(1, newCycle, today)
                    
                    // Comeback Logic trigger?
                    // Ideally we flag this event, and next time we check for comeback.
                    // Actually, if we reset to 1 today, we are "active" today.
                    // If we reach Day 2 next time, we award comeback.
                    // This is handled by CreditService.checkAndAwardComeback(streak=2)
                }
            }
        }
    }
    
    private suspend fun updateSimpleStreak(
        userId: UUID,
        currentStreak: Int,
        currentCycle: Int,
        lastActive: LocalDate?,
        today: LocalDate,
        updateCallback: suspend (Int, Int, LocalDate) -> Unit
    ) {
        if (lastActive == today) return

        if (lastActive == null) {
            updateCallback(1, 1, today)
            return
        }

        val daysBetween = ChronoUnit.DAYS.between(lastActive, today)
        if (daysBetween == 1L) {
             updateCallback(currentStreak + 1, currentCycle, today)
        } else {
             // Reset
             val newCycle = if (currentStreak > 1) currentCycle + 1 else currentCycle
             updateCallback(1, newCycle, today)
        }
    }

    private fun isGraceAvailable(lastUsedAt: LocalDateTime?): Boolean {
        if (lastUsedAt == null) return true
        val daysSinceUse = ChronoUnit.DAYS.between(lastUsedAt, LocalDateTime.now())
        return daysSinceUse >= 7
    }
    
    suspend fun getHabitStatus(userId: UUID): UserStreakData? {
        return habitRepository.getStreaks(userId)
    }
}
