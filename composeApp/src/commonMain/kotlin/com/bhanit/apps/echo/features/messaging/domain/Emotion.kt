package com.bhanit.apps.echo.features.messaging.domain


import kotlinx.serialization.Serializable

@Serializable
data class Emotion(
    val id: String,
    val displayName: String,
    val colorHex: Long,
    val exampleText: String,
    val isDefault: Boolean,
    val isUnlocked: Boolean = true,
    val price: Int = 0,
    val tier: String = "FREE",
    val expiresAt: Long? = null
) {
    // Helper to get compose Color (if we were in a file that imports compose, but this is domain)
    // We will do conversion in UI
}
