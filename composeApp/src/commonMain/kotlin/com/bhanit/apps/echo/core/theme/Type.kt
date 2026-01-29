package com.bhanit.apps.echo.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.Font
import echo.composeapp.generated.resources.Res
import echo.composeapp.generated.resources.inter_bold
import echo.composeapp.generated.resources.inter_medium
import echo.composeapp.generated.resources.inter_regular
import echo.composeapp.generated.resources.jetbrains_mono_bold
import echo.composeapp.generated.resources.jetbrains_mono_medium
import echo.composeapp.generated.resources.jetbrains_mono_regular
import echo.composeapp.generated.resources.merriweather_bold
import echo.composeapp.generated.resources.merriweather_regular

// Custom Font Families
@Composable
fun getTypography(appFont: AppFont): Typography {
    val interFamily = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_bold, FontWeight.Bold)
    )

    val merriweatherFamily = FontFamily(
        Font(Res.font.merriweather_regular, FontWeight.Normal),
        Font(Res.font.merriweather_bold, FontWeight.Bold)
    )

    val jetbrainsMonoFamily = FontFamily(
        Font(Res.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(Res.font.jetbrains_mono_medium, FontWeight.Medium),
        Font(Res.font.jetbrains_mono_bold, FontWeight.Bold)
    )

    val defaultFontFamily = when (appFont) {
        AppFont.INTER -> interFamily
        AppFont.MERRIWEATHER -> merriweatherFamily
        AppFont.JETBRAINS_MONO -> jetbrainsMonoFamily
        AppFont.ROBOTO_SLAB -> FontFamily.Serif // Fallback until slab font resource is added
    }

    // Secondary font logic (optional pairings)
    val headlineFontFamily = when (appFont) {
        AppFont.INTER -> interFamily
        AppFont.MERRIWEATHER -> merriweatherFamily
        AppFont.JETBRAINS_MONO -> jetbrainsMonoFamily
        AppFont.ROBOTO_SLAB -> FontFamily.Serif
    }

    return Typography(
        // Headlines
        headlineLarge = TextStyle(
            fontFamily = headlineFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = headlineFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        // Body
        bodyLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        // Quotes/Feelings
        labelLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        )
    )
}

// Backwards compatibility until Theme.kt is fully wired up
// Backwards compatibility until Theme.kt is fully wired up - REMOVED because getTypography is now composable
// val EchoTypography = getTypography(AppFont.INTER) // This would error now
