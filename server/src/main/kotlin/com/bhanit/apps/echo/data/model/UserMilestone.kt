package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserMilestone(
    val id: String,
    val status: MilestoneStatus,
    val progress: Int,
    val unlockedAt: Long?
)

@Serializable
enum class MilestoneStatus {
    LOCKED, IN_PROGRESS, CLAIMED
}
