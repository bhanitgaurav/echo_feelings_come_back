package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

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
    val startDate: String? = null, // ISO Date string
    val endDate: String? = null,
    val instruction: String? = null
)

@Serializable
enum class MilestoneStatus {
    LOCKED, IN_PROGRESS, CLAIMED
}
