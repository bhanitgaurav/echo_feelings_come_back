package com.bhanit.apps.echo.core.base

import android.content.Context
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class BiometricManager(private val context: Context) {
    actual suspend fun canAuthenticate(): Boolean {
        val biometricManager = AndroidBiometricManager.from(context)
        return biometricManager.canAuthenticate(AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG) == AndroidBiometricManager.BIOMETRIC_SUCCESS
    }

    actual suspend fun authenticate(title: String, subtitle: String): Boolean = suspendCoroutine { continuation ->
        val activity = BiometricActivityProvider.getActivity()
        if (activity == null) {
            continuation.resume(false)
            return@suspendCoroutine
        }

        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                continuation.resume(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                continuation.resume(false)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Do not resume false here, let user retry
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}
