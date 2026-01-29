package com.bhanit.apps.echo.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.BlendMode.Companion.Color
import androidx.compose.ui.graphics.Color

/* ---------------------------------------------------
 * Light Theme
 * --------------------------------------------------- */

private val LightColorScheme = lightColorScheme(
    primary = PrimaryEcho,
    onPrimary = TextPrimaryDark,

    primaryContainer = PrimaryEchoLight,
    onPrimaryContainer = TextPrimaryLight,

    secondary = SecondaryEcho,
    onSecondary = TextPrimaryDark,

    tertiary = AccentEcho,
    onTertiary = TextPrimaryDark,

    background = BackgroundLight,
    onBackground = TextPrimaryLight,

    surface = BackgroundPaper,
    onSurface = TextPrimaryLight,

    surfaceVariant = SurfaceGlass,
    onSurfaceVariant = TextSecondaryLight,

    error = ErrorRed,
    onError = TextPrimaryDark
)

/* ---------------------------------------------------
 * Dark Theme
 * --------------------------------------------------- */

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryEcho,
    onPrimary = TextPrimaryDark,

    primaryContainer = SecondaryEcho, // Slight contrast shift in dark
    onPrimaryContainer = TextPrimaryDark,

    secondary = SecondaryEcho,
    onSecondary = TextPrimaryDark,

    tertiary = AccentEcho,
    onTertiary = TextPrimaryDark,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,

    surfaceVariant = SurfaceGlassDark, // 👈 changed
    onSurfaceVariant = TextSecondaryDark,

    error = ErrorRed,
    onError = TextPrimaryDark
)

/* ---------------------------------------------------
 * App Theme
 * --------------------------------------------------- */

/* ---------------------------------------------------
 * Green Dark Theme
 * --------------------------------------------------- */

private val GreenDarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = TextPrimaryDark,

    primaryContainer = TertiaryGreen,
    onPrimaryContainer = TextPrimaryDark,

    secondary = SecondaryGreen,
    onSecondary = TextPrimaryDark,

    tertiary = AccentEcho, // Keep the brand teal/blue as accent
    onTertiary = TextPrimaryDark,

    background = BackgroundGreenDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceGreenDark,
    onSurface = TextPrimaryDark,

    surfaceVariant = SurfaceGreenGlass, 
    onSurfaceVariant = TextSecondaryDark,

    error = ErrorRed,
    onError = TextPrimaryDark
)

/* ---------------------------------------------------
 * Solar Theme
 * --------------------------------------------------- */

private val SolarColorScheme = lightColorScheme(
    primary = PrimarySolar,
    onPrimary = TextPrimaryLight, 

    primaryContainer = SecondarySolar,
    onPrimaryContainer = TextPrimaryLight,

    secondary = SecondarySolar,
    onSecondary = TextPrimaryLight,

    tertiary = TertiarySolar,
    onTertiary = TextPrimaryLight,

    background = BackgroundSolar,
    onBackground = TextPrimaryLight,

    surface = SurfaceSolar,
    onSurface = TextPrimaryLight,

    surfaceVariant = SurfaceGlassSolar, 
    onSurfaceVariant = TextSecondaryLight,

    error = ErrorRed,
    onError = TextPrimaryLight
)

/* ---------------------------------------------------
 * Custom Theme Generator
 * --------------------------------------------------- */

private fun createCustomColorScheme(colors: CustomThemeColors): androidx.compose.material3.ColorScheme {
    val textPrimary = if (colors.isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (colors.isDark) TextSecondaryDark else TextSecondaryLight
    
    // Glass surface opacity based on dark/light
    val glassColor = colors.surface.let { 
        Color(it).copy(alpha = 0.8f) // Simple 80% opacity
    }

    return if (colors.isDark) {
        darkColorScheme(
            primary = Color(colors.primary),
            onPrimary = TextPrimaryDark,
            
            primaryContainer = Color(colors.primary).copy(alpha = 0.7f),
            onPrimaryContainer = TextPrimaryDark,

            secondary = Color(colors.primary), // Reuse primary for now or add secondary input later
            onSecondary = TextPrimaryDark,

            tertiary = AccentEcho,
            onTertiary = TextPrimaryDark,

            background = Color(colors.background),
            onBackground = TextPrimaryDark,

            surface = Color(colors.surface),
            onSurface = TextPrimaryDark,

            surfaceVariant = glassColor,
            onSurfaceVariant = TextSecondaryDark,

            error = ErrorRed,
            onError = TextPrimaryDark
        )
    } else {
        lightColorScheme(
            primary = Color(colors.primary),
            onPrimary = TextPrimaryLight,
            
            primaryContainer = Color(colors.primary).copy(alpha = 0.3f), // Lighter container
            onPrimaryContainer = TextPrimaryLight,

            secondary = Color(colors.primary),
            onSecondary = TextPrimaryLight,

            tertiary = AccentEcho,
            onTertiary = TextPrimaryLight,

            background = Color(colors.background),
            onBackground = TextPrimaryLight,

            surface = Color(colors.surface),
            onSurface = TextPrimaryLight,

            surfaceVariant = glassColor,
            onSurfaceVariant = TextSecondaryLight,

            error = ErrorRed,
            onError = TextPrimaryLight
        )
    }
}

/* ---------------------------------------------------
 * App Theme
 * --------------------------------------------------- */

enum class AppTheme {
    Light,
    Dark,
    GreenDark,
    Solar,
    Custom
}

@Composable
fun EchoTheme(
    appTheme: AppTheme = if (isSystemInDarkTheme()) AppTheme.Dark else AppTheme.Light,
    appFont: AppFont = AppFont.INTER,
    customThemeColors: CustomThemeColors? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (appTheme) {
        AppTheme.Light -> LightColorScheme
        AppTheme.Dark -> DarkColorScheme
        AppTheme.GreenDark -> GreenDarkColorScheme
        AppTheme.Solar -> SolarColorScheme
        AppTheme.Custom -> if (customThemeColors != null) createCustomColorScheme(customThemeColors) else DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(appFont),
        content = content
    )
}
