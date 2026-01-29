package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Milestone(
    val id: String,
    val displayName: String,
    val description: String,
    val requiredCount: Int,
    val type: MilestoneType,
    val rewardCredits: Int
)

@Serializable
enum class MilestoneType {
    STREAK_PRESENCE,
    STREAK_KINDNESS,
    STREAK_RESPONSE,
    TOTAL_CREDITS,
    FIRST_TIME_ACTION
}

@Serializable
data class MilestoneStatusDto(
    val id: String,
    val displayName: String,
    val description: String,
    val progress: Int,
    val required: Int,
    val percentage: Int,
    val status: MilestoneStatus,
    val rewardCredits: Int,
    val colorHex: String? = null,
    val category: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val instruction: String? = null
)
