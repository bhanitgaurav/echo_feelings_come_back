package com.bhanit.apps.echo.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/* ---------------------------------------------------
 * Glass Accessors
 * --------------------------------------------------- */

val ColorSchemeGlass: Color
    @Composable get() =
        if (isSystemInDarkTheme()) SurfaceGlassDark else SurfaceGlass

val ColorSchemeGlassStrong: Color
    @Composable get() =
        if (isSystemInDarkTheme()) SurfaceGlassDarkStrong else SurfaceGlassStrong

val ColorSchemeScrim: Color
    @Composable get() =
        if (isSystemInDarkTheme()) ScrimDark else ScrimLight


