package com.bhanit.apps.echo.features.contact.presentation.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView
import kotlin.experimental.ExperimentalObjCName

// Bridge Object for predictable Swift Access
@OptIn(ExperimentalObjCName::class)
@ObjCName("IOSCameraBridge")
object IOSCameraBridge {
    // Factory now takes two callbacks: onScanned, onPermissionGranted
    var scannerFactory: ((onScanned: (String) -> Unit, onPermissionGranted: (Boolean) -> Unit) -> UIView)? = null
    
    // Swift will assign this closure when the view is created
    var torchController: ((Boolean) -> Unit)? = null
    
    // Check permission status on demand (e.g. on Resume)
    // 0 = NotDetermined, 1 = Restricted, 2 = Denied, 3 = Authorized
    var permissionStatus: (() -> Int)? = null

    // Request access
    var requestAccess: ((onResult: (Boolean) -> Unit) -> Unit)? = null
    
    // Open Settings on demand
    var openSettings: (() -> Unit)? = null
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    torchEnabled: Boolean,
    onQrScanned: (String) -> Unit,
    onPermissionGranted: (Boolean) -> Unit
) {
    // State to track if we should show the camera or the fallback UI
    var isAuthorized by remember { mutableStateOf(false) }
    var isDenied by remember { mutableStateOf(false) }
    
    // Check permission helper
    fun checkPermission() {
        val status = IOSCameraBridge.permissionStatus?.invoke() ?: 0 // Default to NotDetermined if missing
        
        when (status) {
            0 -> { // NotDetermined
                IOSCameraBridge.requestAccess?.invoke { granted ->
                    isAuthorized = granted
                    isDenied = !granted
                    onPermissionGranted(granted)
                }
            }
            3 -> { // Authorized
                isAuthorized = true
                isDenied = false
                onPermissionGranted(true)
            }
            else -> { // Denied (2) or Restricted (1)
                isAuthorized = false
                isDenied = true
                onPermissionGranted(false)
            }
        }
    }

    LaunchedEffect(Unit) {
        checkPermission()
    }
    
    // Observe Lifecycle for Resume (returning from Settings)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // React to torch changes
    LaunchedEffect(torchEnabled) {
        IOSCameraBridge.torchController?.invoke(torchEnabled)
    }

    if (isAuthorized) {
        val factory = IOSCameraBridge.scannerFactory
        if (factory != null) {
            UIKitView(
                factory = {
                    // We wrap the permission callback to also update local state
                    factory(onQrScanned) { granted ->
                        isAuthorized = granted
                        isDenied = !granted
                        onPermissionGranted(granted)
                    }
                },
                modifier = modifier
            )
        } else {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text("Camera bridge not initialized.")
            }
        }
    } else if (isDenied) {
        // Fallback UI for iOS - Only shown if explicitly denied/restricted
        Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
             androidx.compose.material3.Button(
                 onClick = {
                     IOSCameraBridge.openSettings?.invoke()
                 },
                 modifier = Modifier.padding(bottom = 150.dp)
             ) {
                 Text("Camera Permission Required. Tap to Grant.")
             }
        }
    }
    // Else: Waiting for permission (blank/loading)
}
