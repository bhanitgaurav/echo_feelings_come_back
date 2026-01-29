package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TimeRange(val label: String) {
    TODAY("1D"),
    WEEK("1W"),
    MONTH("1M"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    YEAR("1Y"),
    FIVE_YEARS("5Y"),
    ALL("All")
}

@Serializable
data class AnalyticsDTO(
    val totalSent: Int,
    val totalReceived: Int,
    val currentStreak: Int,
    val emotionBreakdown: Map<String, Int>, // Global
    val sentEmotionBreakdown: Map<String, Int>,
    val receivedEmotionBreakdown: Map<String, Int>,
    val dailyActivity: List<DailyActivity>,
)

@Serializable
data class DailyActivity(
    val date: String, // ISO Date string YYYY-MM-DD
    val sent: Int,
    val received: Int,
    val emotions: Map<String, Int> = emptyMap(),
)
