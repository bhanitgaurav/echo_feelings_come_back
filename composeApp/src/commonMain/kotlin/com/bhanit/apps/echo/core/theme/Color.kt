package com.bhanit.apps.echo.core.theme

import androidx.compose.ui.graphics.Color

/* ---------------------------------------------------
 * Brand Colors — Echo (Purple → Blue → Teal)
 * --------------------------------------------------- */

// Primary Brand
val PrimaryEcho = Color(0xFF9B7CF5)        // Soft Purple (Emotion)
val PrimaryEchoLight = Color(0xFFC4B5FD)   // Lavender Light
val SecondaryEcho = Color(0xFF7F8FEF)      // Blue-Violet (Trust)
val AccentEcho = Color(0xFF3ECACB)         // Teal (Healing / Return)

/* ---------------------------------------------------
 * Emotion Colors (Soft, Non-aggressive)
 * --------------------------------------------------- */

val EmotionLove = Color(0xFFFF9ECF)        // Soft Pink (Affection)
val EmotionHappy = Color(0xFF6EE7B7)       // Mint Green (Joy)
val EmotionSad = Color(0xFF93C5FD)         // Calm Blue (Sadness)
val EmotionAnger = Color(0xFFFCA5A5)       // Soft Coral (Anger)
val EmotionJealousy = Color(0xFFFDE68A)    // Muted Yellow (Jealousy)

/* ---------------------------------------------------
 * Backgrounds & Surfaces — Light Mode
 * --------------------------------------------------- */

val BackgroundLight = Color(0xFFF8FAFF)    // Very Light Lavender
val BackgroundPaper = Color(0xFFFFFFFF)   // Pure White

val SurfaceGlass = Color(0xCCFFFFFF)       // Glassmorphism (80%)
val SurfaceGlassStrong = Color(0xFF0B1020) // Glassmorphism (90%)

/* ---------------------------------------------------
 * Backgrounds & Surfaces — Dark Mode
 * --------------------------------------------------- */

val BackgroundDark = Color(0xFF0B1020)     // Deep Indigo-Navy
val SurfaceDark = Color(0xFF161B33)        // Elevated Dark Surface

// Green Dark Theme (Nature/Healing)
val PrimaryGreen = Color(0xFF10B981)       // Emerald
val SecondaryGreen = Color(0xFF34D399)     // Soft Teal
val TertiaryGreen = Color(0xFF059669)      // Forest

val BackgroundGreenDark = Color(0xFF022C22) // Deep Pine/Forest
val SurfaceGreenDark = Color(0xFF064E3B)    // Dark Emerald Surface

// Solar Theme (Warm/Happy)
val PrimarySolar = Color(0xFFF59E0B)       // Warm Amber
val SecondarySolar = Color(0xFFFBBF24)     // Bright Yellow-Orange
val TertiarySolar = Color(0xFFD97706)      // Deep Amber

val BackgroundSolar = Color(0xFFFFFBEB)    // Very Pale Cream (Amber 50)
val SurfaceSolar = Color(0xFFFEF3C7)       // Pale Yellow Surface (Amber 100)
val SurfaceGlassSolar = Color(0xCCFEF3C7)  // 80% Solar Glass

/* ---------------------------------------------------
 * Text Colors
 * --------------------------------------------------- */

val TextPrimaryLight = Color(0xFF1F2937)   // Almost Black
val TextSecondaryLight = Color(0xFF6B7280) // Neutral Gray

val TextPrimaryDark = Color(0xFFF9FAFB)    // Near White
val TextSecondaryDark = Color(0xFFCBD5E1)  // Soft Gray

val TextDisabled = Color(0xFF9CA3AF)

/* ---------------------------------------------------
 * Status / Feedback
 * --------------------------------------------------- */

val ErrorRed = Color(0xFFF87171)            // Soft Red
val SuccessGreen = Color(0xFF34D399)        // Teal-Green Success

/* ---------------------------------------------------
 * Glass Surfaces — Dark Theme (Tinted, Not White)
 * --------------------------------------------------- */

val SurfaceGlassDark = Color(0xCC161B33)       // 80% dark-tinted glass
val SurfaceGlassDarkStrong = Color(0xE6161B33) // 90% dark-tinted glass

val SurfaceGreenGlass = Color(0xCC064E3B)      // 80% Green-tinted glass
val SurfaceGreenGlassStrong = Color(0xE6064E3B)

/* ---------------------------------------------------
 * Scrims — Contrast without killing translucency
 * --------------------------------------------------- */

val ScrimLight = Color.Black.copy(alpha = 0.04f)
val ScrimDark = Color.White.copy(alpha = 0.06f)

/* ---------------------------------------------------
 * Emotion Colors — Dark Theme Variants
 * --------------------------------------------------- */

val EmotionLoveDark = Color(0xFFE88BB8)
val EmotionHappyDark = Color(0xFF5FD3A1)
val EmotionSadDark = Color(0xFF7FB0F5)
val EmotionAngerDark = Color(0xFFE98A8A)
val EmotionJealousyDark = Color(0xFFEAD47A)
