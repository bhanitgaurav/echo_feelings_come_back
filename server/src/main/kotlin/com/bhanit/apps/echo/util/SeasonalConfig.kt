package com.bhanit.apps.echo.util

import java.time.LocalDate
import java.time.Month
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class SeasonMeta(
    val counts: MutableMap<String, Int> = mutableMapOf(),
    val lastAwardedAt: MutableMap<String, Long> = mutableMapOf(), // Epoch Millis
    val dailyCounts: MutableMap<String, Int> = mutableMapOf() // Key: "RuleName_YYYY-MM-DD"
)

enum class SeasonalRuleType {
    SEND_POSITIVE,
    RESPOND,
    COMEBACK,
    BALANCED_DAY_MULTIPLIER
}

@Serializable
data class SeasonalRule(
    val type: SeasonalRuleType,
    val bonusCredits: Int,
    val dailyCap: Int = Int.MAX_VALUE,
    val weeklyCap: Int = Int.MAX_VALUE,
    val maxTotal: Int = Int.MAX_VALUE, // Season cap
    val oncePerSeason: Boolean = false,
    val cooldownHours: Int = 0
)

enum class SeasonalCategory {
    GLOBAL, RELIGIOUS, CULTURAL, SOCIAL, SEASONAL
}

@Serializable
data class SeasonalEvent(
    val id: String,
    val templateId: String = "CUSTOM",
    val name: String,
    val category: SeasonalCategory = SeasonalCategory.SEASONAL,
    val colorHex: String, // Client visual theme
    val startMonth: Month, 
    val startDay: Int,
    val endMonth: Month,
    val endDay: Int,
    val rules: List<SeasonalRule>,
    val year: Int? = null,
    val isActive: Boolean = true
) {
    fun isActive(date: LocalDate): Boolean {
        val start = LocalDate.of(date.year, startMonth, startDay)
        var end = LocalDate.of(date.year, endMonth, endDay)
        
        // Handle year wrap (e.g., Dec 25 - Jan 5) - rare but possible
        if (end.isBefore(start)) {
            // If today is in Jan, check vs start of prev year? Or just simple check?
            // Simple approach: Assume seasons don't wrap years for now, or handle strictly.
            // Let's assume non-wrapping windows for simplicity (most festivals are short).
            // For New Year (Jan 1), it's just Jan 1 - Jan 7.
        }
        
        return !date.isBefore(start) && !date.isAfter(end)
    }
}

object SeasonalConfig {
    val EVENTS = listOf(
        // Valentine's Week (Feb 10 - Feb 16)
        SeasonalEvent(
            id = "VALENTINE",
            templateId = "VALENTINE",
            name = "Valentine's Week",
            category = SeasonalCategory.GLOBAL,
            colorHex = "0xFFE91E63", // Pink
            startMonth = Month.FEBRUARY,
            startDay = 10,
            endMonth = Month.FEBRUARY,
            endDay = 16,
            rules = listOf(
                SeasonalRule(SeasonalRuleType.SEND_POSITIVE, bonusCredits = 2, dailyCap = 1, maxTotal = 10), // Appreciation Bonus
                SeasonalRule(SeasonalRuleType.RESPOND, bonusCredits = 1, maxTotal = 5), // Response Bonus
                SeasonalRule(SeasonalRuleType.COMEBACK, bonusCredits = 3, oncePerSeason = true) // Love Comeback
            )
        ),
        
        // World Kindness Day (Nov 13)
        SeasonalEvent(
            id = "KINDNESS_DAY",
            templateId = "KINDNESS_DAY",
            name = "World Kindness Day",
            category = SeasonalCategory.GLOBAL,
            colorHex = "0xFF2196F3", // Blue
            startMonth = Month.NOVEMBER,
            startDay = 13,
            endMonth = Month.NOVEMBER,
            endDay = 13,
            rules = listOf(
                SeasonalRule(SeasonalRuleType.SEND_POSITIVE, bonusCredits = 3, maxTotal = 6)
            )
        ),

        // Christmas (Dec 24 - Dec 26)
        SeasonalEvent(
            id = "CHRISTMAS",
            templateId = "CHRISTMAS",
            name = "Holiday Seasons",
            category = SeasonalCategory.GLOBAL,
            colorHex = "0xFF4CAF50", // Green
            startMonth = Month.DECEMBER,
            startDay = 24,
            endMonth = Month.DECEMBER,
            endDay = 26,
            rules = listOf(
                SeasonalRule(SeasonalRuleType.SEND_POSITIVE, bonusCredits = 2, maxTotal = 6),
                SeasonalRule(SeasonalRuleType.COMEBACK, bonusCredits = 3, oncePerSeason = true)
            )
        )
        // Add Diwali/Holi/Ramadan dynamically if dates are fixed or need lunar calc.
        // For static MVP, we stick to fixed solar dates.
        // Diwali/Holi change dates. We'd need a dynamic lookup service or yearly manual config update?
        // OR a "LunarEvent" class.
        // For now, let's stick to the user's specific Valentine request + fixed ones.
    )

    fun getActiveEvent(date: LocalDate): SeasonalEvent? {
        return EVENTS.find { it.isActive(date) }
    }
}
