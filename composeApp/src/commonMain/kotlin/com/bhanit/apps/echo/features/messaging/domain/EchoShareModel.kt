package com.bhanit.apps.echo.features.messaging.domain

/**
 * Data model for the shared Echo Card.
 * Designed to separate the shareable "Image" content from the interactive "App" content.
 */

enum class ShareAspectRatio(val ratio: Float, val w: Int, val h: Int, val displayName: String) {
    SQUARE(1f, 1080, 1080, "Square (1:1)"),
    STORY(9f / 16f, 1080, 1920, "Story (9:16)")
}

data class EchoShareModel(
    val receivedText: String,
    val repliedText: String? = null,
    val moodColor: Long,
    // Optional overrides for A/B testing or future variants
    val contextLine: String = "A message I received", 
    val footerText: String = "Echo",
    val aspectRatio: ShareAspectRatio = ShareAspectRatio.SQUARE
)
