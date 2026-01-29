package com.bhanit.apps.echo.features.splash

import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.core.base.BiometricManager
import com.bhanit.apps.echo.features.auth.domain.SessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

data class SplashState(
    val isCheckingSession: Boolean = true,
    val needsBiometric: Boolean = false,
    val navigateTo: SplashDestination? = null
)

enum class SplashDestination {
    LOGIN, DASHBOARD, ONBOARDING, WELCOME
}

class SplashViewModel(
    private val sessionRepository: SessionRepository,
    private val biometricManager: BiometricManager
) : BaseViewModel<SplashState>(SplashState()) {

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            // Add a small delay for branding visibility if needed
            delay(1000)
            
            // calling getSession() populates the in-memory cache in SessionRepository
            val session = sessionRepository.getSession()
            if (session != null) {
                // We are logged in and cache is hot
                val biometricEnabled = sessionRepository.isBiometricEnabled.first()
                if (biometricEnabled && biometricManager.canAuthenticate()) {
                    updateState { it.copy(isCheckingSession = false, needsBiometric = true) }
                    doBiometricAuth()
                } else {
                    checkOnboarding()
                }
            } else {
                updateState { it.copy(isCheckingSession = false, navigateTo = SplashDestination.WELCOME) }
            }
        }
    }

    private suspend fun checkOnboarding() {
        val onboardingCompleted = sessionRepository.isOnboardingCompleted.first()
        println("DEBUG: SplashViewModel - checkOnboarding: $onboardingCompleted")
        if (onboardingCompleted) {
             updateState { it.copy(isCheckingSession = false, needsBiometric = false, navigateTo = SplashDestination.DASHBOARD) }
        } else {
             updateState { it.copy(isCheckingSession = false, needsBiometric = false, navigateTo = SplashDestination.ONBOARDING) }
        }
    }

    fun doBiometricAuth() {
        viewModelScope.launch {
            val success = biometricManager.authenticate(
                title = "Unlock Echo",
                subtitle = "Confirm your identity"
            )
            if (success) {
                checkOnboarding()
            } else {
                // Determine what to do on failure? Retry? Logout?
                // For now, let user click Retry again.
            }
        }
    }
}
