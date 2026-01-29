package com.bhanit.apps.echo.core.util

import androidx.compose.runtime.Composable

@Composable
expect fun ContactPermissionEffect(
    onPermissionResult: (Boolean) -> Unit
)
