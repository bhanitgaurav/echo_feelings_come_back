package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    REFERRAL_REWARD,
    PURCHASE,
    ADMIN_ADJUSTMENT,
    MANUAL_ADJUSTMENT,
    SIGNUP_BONUS,
    EMOTION_UNLOCK,
    MANUAL_GRANT,
    STREAK_REWARD,
    BALANCED_ACTIVITY_BONUS,
    WEEKLY_REFLECTION_REWARD,
    FIRST_TIME_BONUS,
    SENT_ECHO,
    RECEIVED_ECHO,
    SEASON_REWARD
}

@Serializable
enum class PurchaseType {
    EMOTION_TIME,
    EMOTION_COUNT,
    RELATION_QUOTA,
    EMOTION_UNLOCK,
    LIMIT_BOOST
}

@Serializable
data class CreditTransaction(
    val id: String,
    val amount: Int,
    val type: TransactionType,
    val description: String,
    val relatedId: String?,
    val timestamp: Long // Epoch millis
)

@Serializable
data class PurchaseRequest(
    val type: PurchaseType,
    val itemId: String, // Emotion ID or blank for generic quota
    val targetUserId: String? = null,
    val durationHours: Int? = null,
    val count: Int? = null
)

@Serializable
enum class TransactionFilter {
    ALL,
    EARNED,
    SPENT
}
