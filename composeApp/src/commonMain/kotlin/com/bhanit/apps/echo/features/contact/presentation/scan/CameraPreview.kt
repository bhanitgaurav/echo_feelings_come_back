package com.bhanit.apps.echo.features.contact.presentation.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CameraPreview(
    modifier: Modifier = Modifier,
    torchEnabled: Boolean = false,
    onQrScanned: (String) -> Unit,
    onPermissionGranted: (Boolean) -> Unit = {}
)
