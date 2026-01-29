package com.bhanit.apps.echo.core.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberContactPermissionLauncher(onResult: (Boolean) -> Unit): PermissionLauncher {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(isGranted)
    }

    return remember(launcher) {
        object : PermissionLauncher {
            override fun launch() {
                launcher.launch(android.Manifest.permission.READ_CONTACTS)
            }
        }
    }
}
