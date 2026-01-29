package com.bhanit.apps.echo.core.theme

import kotlinx.serialization.Serializable

enum class ThemeConfig {
    SYSTEM,
    LIGHT,
    DARK,
    GREEN_DARK,
    SOLAR,
    CUSTOM
}

@Serializable
data class CustomThemeColors(
    val primary: Long = 0xFF9B7CF5,   // Default Echo Purple
    val background: Long = 0xFFF8FAFF, // Default Light Background
    val surface: Long = 0xFFFFFFFF,    // Default Light Surface
    val isDark: Boolean = false         // Helper for text contrast
)
