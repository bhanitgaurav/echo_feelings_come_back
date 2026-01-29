package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.util.SeasonalRuleType
import java.time.LocalDate
import java.util.UUID

interface RewardEvaluator {
    val priority: Int
    suspend fun evaluate(userId: UUID, eventType: SeasonalRuleType, date: LocalDate, relatedSourceId: String? = null)
}
