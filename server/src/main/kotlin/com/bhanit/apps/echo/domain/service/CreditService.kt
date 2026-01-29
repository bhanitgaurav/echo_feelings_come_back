package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.data.model.MilestoneType
import com.bhanit.apps.echo.data.model.TransactionType
import java.util.UUID

interface CreditService {
    suspend fun awardCredits(
        userId: UUID, 
        amount: Int, 
        type: com.bhanit.apps.echo.data.model.TransactionType, 
        description: String, 
        relatedId: String? = null,
        visibility: com.bhanit.apps.echo.data.table.TransactionVisibility = com.bhanit.apps.echo.data.table.TransactionVisibility.VISIBLE,
        source: com.bhanit.apps.echo.data.table.RewardSource = com.bhanit.apps.echo.data.table.RewardSource.SYSTEM,
        intent: com.bhanit.apps.echo.data.table.TransactionIntent = com.bhanit.apps.echo.data.table.TransactionIntent.REWARD,
        metadata: String? = null
    )
    suspend fun checkAndAwardStreakMilestone(userId: UUID, streakType: MilestoneType, currentStreak: Int, cycle: Int)
    suspend fun checkAndAwardBalancedBonus(userId: UUID)
    suspend fun checkAndAwardWeeklyReflection(userId: UUID)
    suspend fun checkAndAwardFirstEcho(userId: UUID)
    suspend fun checkAndAwardFirstResponse(userId: UUID)
    
    suspend fun checkAndAwardConsistency(userId: UUID, date: java.time.LocalDate)
    suspend fun checkAndAwardSeasonal(userId: UUID, date: java.time.LocalDate, ruleType: com.bhanit.apps.echo.util.SeasonalRuleType, relatedSourceId: String? = null)
    
    suspend fun recordOneTimeReward(phoneHash: String, rewardType: com.bhanit.apps.echo.data.table.OneTimeRewardType)
}
