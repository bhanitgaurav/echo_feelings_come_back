package com.bhanit.apps.echo.util

import com.bhanit.apps.echo.data.model.Milestone
import com.bhanit.apps.echo.data.model.MilestoneType

object CreditConstants {
    const val PRESENCE_STREAK_3_DAYS = 2
    const val PRESENCE_STREAK_7_DAYS = 5
    const val PRESENCE_STREAK_14_DAYS = 10
    
    const val KINDNESS_STREAK_3_DAYS = 5
    const val KINDNESS_STREAK_7_DAYS = 12
    
    const val RESPONSE_STREAK_3_DAYS = 4
    const val RESPONSE_STREAK_7_DAYS = 10
    
    const val BALANCED_ACTIVITY_BONUS = 2
    const val WEEKLY_REFLECTION_REWARD = 3
    
    const val FIRST_ECHO_BONUS = 3
    const val FIRST_RESPONSE_BONUS = 3
    
    val MILESTONES = listOf(
        // Presence
        Milestone("presence_3", "3-Day Presence", "Show up 3 days in a row", 3, MilestoneType.STREAK_PRESENCE, PRESENCE_STREAK_3_DAYS),
        Milestone("presence_7", "7-Day Presence", "Show up 7 days in a row", 7, MilestoneType.STREAK_PRESENCE, PRESENCE_STREAK_7_DAYS),
        Milestone("presence_14", "14-Day Presence", "Show up 14 days in a row", 14, MilestoneType.STREAK_PRESENCE, PRESENCE_STREAK_14_DAYS),
        
        // Kindness
        Milestone("kindness_3", "3-Day Kindness", "Send positive echoes 3 days in a row", 3, MilestoneType.STREAK_KINDNESS, KINDNESS_STREAK_3_DAYS),
        Milestone("kindness_7", "7-Day Kindness", "Send positive echoes 7 days in a row", 7, MilestoneType.STREAK_KINDNESS, KINDNESS_STREAK_7_DAYS),
        
        // Response
        Milestone("response_3", "3-Day Response", "Respond to echoes 3 days in a row", 3, MilestoneType.STREAK_RESPONSE, RESPONSE_STREAK_3_DAYS),
        Milestone("response_7", "7-Day Response", "Respond to echoes 7 days in a row", 7, MilestoneType.STREAK_RESPONSE, RESPONSE_STREAK_7_DAYS)
    )
}
