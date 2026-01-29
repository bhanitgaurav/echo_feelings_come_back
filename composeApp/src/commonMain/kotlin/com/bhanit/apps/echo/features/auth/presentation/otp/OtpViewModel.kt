package com.bhanit.apps.echo.features.auth.presentation.otp

import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.features.auth.domain.AuthRepository
import com.bhanit.apps.echo.features.auth.domain.SessionRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

data class OtpState(
    val isLoading: Boolean = false,
    val otp: String = "",
    val error: String? = null,
    val isVerified: Boolean = false,
    val isNewUser: Boolean? = null,
    val isOnboardingCompleted: Boolean = false
)

class OtpViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val messagingRepository: com.bhanit.apps.echo.features.messaging.domain.MessagingRepository,
    private val fcmTokenProvider: com.bhanit.apps.echo.core.fcm.FcmTokenProvider,
    private val analytics: com.bhanit.apps.echo.shared.analytics.EchoAnalytics
) : BaseViewModel<OtpState>(OtpState()) {

    fun onOtpChange(otp: String) {
        val numericOtp = otp.filter { it.isDigit() }
        updateState { it.copy(otp = numericOtp, error = null) }
    }

    fun verifyOtp(phone: String) {
        val otp = state.value.otp
        if (otp.length != 6) {
            updateState { it.copy(error = "Enter 6 digit OTP") }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            
            authRepository.verifyOtp(phone, otp)
                .onSuccess { response ->
                    if (response.isNewUser) {
                        analytics.logEvent(com.bhanit.apps.echo.shared.analytics.EchoEvents.ONBOARDING_STARTED)
                    } else {
                         // Login successful
                    }
                    
                    sessionRepository.saveSession(response.token, response.userId, phone, isOnboardingCompleted = response.isOnboardingCompleted)
                    sessionRepository.setBiometricEnabled(true)
                    updateState { it.copy(isLoading = false, isVerified = true, isNewUser = response.isNewUser, isOnboardingCompleted = response.isOnboardingCompleted) }
                    
                    // Trigger FCM Token Sync
                    launch {
                        try {
                             val token = fcmTokenProvider.getToken()
                             if (token != null) {
                                 messagingRepository.updateFcmToken(token)
                             }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, error = error.message ?: "Invalid OTP") }
                }
        }
    }
}
