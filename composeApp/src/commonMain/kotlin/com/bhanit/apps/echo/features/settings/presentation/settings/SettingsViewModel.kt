package com.bhanit.apps.echo.features.settings.presentation.settings

import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.features.auth.domain.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.bhanit.apps.echo.features.messaging.domain.ShareAspectRatio

data class SettingsState(
    val isBiometricEnabled: Boolean = false,
    val allowQrAutoConnect: Boolean = true,
    val showActivityOverview: Boolean = true,
    val appVersion: String = "1.0.0" 
)

class SettingsViewModel(
    private val sessionRepository: SessionRepository,
    private val authRepository: com.bhanit.apps.echo.features.auth.domain.AuthRepository,
    private val platformUtils: com.bhanit.apps.echo.core.util.PlatformUtils,
    private val analytics: com.bhanit.apps.echo.shared.analytics.EchoAnalytics,
    private val reviewManager: com.bhanit.apps.echo.core.review.ReviewManager,
    private val userRepository: com.bhanit.apps.echo.features.user.domain.UserRepository,
    appConfig: com.bhanit.apps.echo.core.config.AppConfig
) : BaseViewModel<SettingsState>(
    SettingsState(appVersion = appConfig.version)
) {

    init {
        loadSettings()
    }

    // Ideally, collect flow from repository
    val isBiometricEnabled = sessionRepository.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showActivityOverview = sessionRepository.showActivityOverview
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val shareAspectRatio = sessionRepository.shareAspectRatio
        .stateIn(
            viewModelScope, 
            SharingStarted.WhileSubscribed(5000), 
            com.bhanit.apps.echo.features.messaging.domain.ShareAspectRatio.STORY
        )

    private fun loadSettings() {
        viewModelScope.launch {
            val result = userRepository.getProfile()
            result.onSuccess { profile ->
                updateState { it.copy(allowQrAutoConnect = profile.allowQrAutoConnect) }
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            sessionRepository.setBiometricEnabled(enabled)
            // State updates via Flow collection in UI or here if needed
        }
    }

    fun toggleQrAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            updateState { it.copy(allowQrAutoConnect = enabled) }
            // Optimistic update locally, check server
             val result = userRepository.updateProfile(
                com.bhanit.apps.echo.data.model.UpdateProfileRequest(
                    allowQrAutoConnect = enabled
                )
             )
             if (result.isFailure) {
                 // Revert on failure
                 updateState { it.copy(allowQrAutoConnect = !enabled) }
             }
        }
    }

    fun toggleShowActivityOverview(enabled: Boolean) {
        viewModelScope.launch {
            sessionRepository.setShowActivityOverview(enabled)
        }
    }

    fun setShareAspectRatio(ratio: com.bhanit.apps.echo.features.messaging.domain.ShareAspectRatio) {
        viewModelScope.launch {
            sessionRepository.setShareAspectRatio(ratio)
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (e: Exception) {
                // Ignore network errors, proceed to local cleanup
            }
            sessionRepository.clearSession()
            onLogoutSuccess()
        }
    }

    fun shareApp() {
        viewModelScope.launch {
            val referralCode = sessionRepository.referralCode.first()
            analytics.logEvent(com.bhanit.apps.echo.shared.analytics.EchoEvents.REFERRAL_SENT)
            val url = if (referralCode != null) "https://api-echo.bhanitgaurav.com/r/$referralCode?m=settings" 
                      else "https://api-echo.bhanitgaurav.com/r/WELCOME?m=settings"
            
            platformUtils.shareText("Hey, check out Echo! It's an awesome app for connecting with people. $url")
        }
    }

    fun contactSupport() {
        platformUtils.openEmail("support@echo.app", "Feedback for Echo App")
    }

    fun openPrivacyPolicy() {
        // platformUtils.openUrl("https://echo.app/privacy") // Or use internal screen
    }

    fun rateApp() {
        viewModelScope.launch {
            reviewManager.tryRequestReview()
        }
    }
    
    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = userRepository.deleteAccount()
            if (result.isSuccess) {
                 logout(onSuccess)
            } else {
                // Should show error via state/event, simple log for now or maybe expose error state
                // Ideally update state.error, but SettingsState doesn't have it yet.
                // Assuming success for critical flow or User will retry.
            }
        }
    }
}
