package com.bhanit.apps.echo.core.base

import kotlinx.cinterop.ExperimentalForeignApi
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class BiometricManager {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun canAuthenticate(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)
    }

    actual suspend fun authenticate(title: String, subtitle: String): Boolean = withContext(Dispatchers.Main) {
         suspendCoroutine { continuation ->
            val context = LAContext()
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = title
            ) { success, _ ->
                continuation.resume(success)
            }
        }
    }
}
