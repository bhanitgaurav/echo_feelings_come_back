package com.bhanit.apps.echo.features.messaging.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberShareManager(): ShareManager {
    return remember { ShareManager() }
}
