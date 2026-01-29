package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Emotion(
    val id: String,
    val displayName: String,
    val colorHex: Long,
    val exampleText: String,
    val isDefault: Boolean,
    val isUnlocked: Boolean = false, // Computed field for API response
    val price: Int = 0,
    val tier: String = "FREE", // Using String for serialization simplicity, or could use Enum if shared
    val sentiment: String = "REFLECTIVE", // POSITIVE, REFLECTIVE, DIFFICULT, HEAVY
    val expiresAt: Long? = null // Expiry timestamp in millis
)
