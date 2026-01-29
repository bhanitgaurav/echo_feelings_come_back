package com.bhanit.apps.echo.domain.repository

import java.time.LocalDate
import java.util.UUID

interface DailyStatsRepository {
    suspend fun incrementSentCount(userId: UUID, date: LocalDate)
    suspend fun incrementReceivedCount(userId: UUID, date: LocalDate)
    suspend fun getStats(userId: UUID, date: LocalDate): org.jetbrains.exposed.sql.ResultRow?
    suspend fun getAnalytics(userId: UUID, range: com.bhanit.apps.echo.data.model.TimeRange, clientDate: LocalDate = LocalDate.now()): com.bhanit.apps.echo.data.model.AnalyticsDTO
    suspend fun getDailyStats(userId: UUID, date: LocalDate): DailyStatsSimple?

    // Admin Methods
    suspend fun getGlobalAnalytics(range: com.bhanit.apps.echo.data.model.TimeRange, clientDate: LocalDate = LocalDate.now()): com.bhanit.apps.echo.data.model.AnalyticsDTO
}

data class DailyStatsSimple(
    val sentCount: Int,
    val receivedCount: Int,
    val receivedSentiments: Set<String> = emptySet(),
    val sentSentiments: Set<String> = emptySet()
)
