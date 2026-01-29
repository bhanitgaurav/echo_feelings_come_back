package com.bhanit.apps.echo.core.ui

import androidx.compose.runtime.Composable

interface PermissionLauncher {
    fun launch()
}

@Composable
expect fun rememberContactPermissionLauncher(onResult: (Boolean) -> Unit): PermissionLauncher
