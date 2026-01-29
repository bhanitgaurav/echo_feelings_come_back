package com.bhanit.apps.echo.features.settings.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhanit.apps.echo.features.auth.domain.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.ktor.client.call.body

class NotificationSettingsViewModel(
    private val sessionRepository: SessionRepository,
    private val userRepository: com.bhanit.apps.echo.features.user.domain.UserRepository
) : ViewModel() {

    private val _loadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val loadingStates = _loadingStates.asStateFlow()

    val feelingsEnabled = sessionRepository.notificationsFeelings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val checkinsEnabled = sessionRepository.notificationsCheckins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reflectionsEnabled = sessionRepository.notificationsReflections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val rewardsEnabled = sessionRepository.notificationsRewards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val updatesEnabled = sessionRepository.notificationsUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val soundEnabled = sessionRepository.notificationsSound
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrationEnabled = sessionRepository.notificationsVibration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val inactiveRemindersEnabled = sessionRepository.notificationsInactiveReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    private val _uiEvent = kotlinx.coroutines.channels.Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private fun toggleSetting(
        name: String, 
        enabled: Boolean, 
        preferenceKey: androidx.datastore.preferences.core.Preferences.Key<Boolean>,
        updateRemote: suspend () -> Result<Unit>
    ) {
        viewModelScope.launch {
            // Optimistic Update
            sessionRepository.setNotificationPreference(preferenceKey, enabled)
            _loadingStates.update { it + (preferenceKey.name to true) }
            
            try {
                updateRemote().getOrThrow()
                val status = if (enabled) "enabled" else "disabled"
                _uiEvent.send(UiEvent.ShowSnackbar("$name notifications $status"))
            } catch (e: CancellationException) {
                // Job cancelled (e.g. user left screen).
                // Ensure rollback happens even if scope is cancelled.
                withContext(NonCancellable) {
                    sessionRepository.setNotificationPreference(preferenceKey, !enabled)
                }
                throw e // Rethrow to ensure structured concurrency
            } catch (e: io.ktor.client.plugins.ClientRequestException) {
                // Rollback
                withContext(NonCancellable) {
                    sessionRepository.setNotificationPreference(preferenceKey, !enabled)
                }
                
                val errorMsg = try {
                    e.response.body<com.bhanit.apps.echo.data.model.ApiErrorDTO>().errorMessage ?: "Failed to update $name settings"
                } catch(parseErr: Exception) {
                    "Failed to update $name settings"
                }
                _uiEvent.send(UiEvent.ShowSnackbar(errorMsg))
            } catch (e: io.ktor.client.plugins.ServerResponseException) {
                // Rollback
                withContext(NonCancellable) {
                    sessionRepository.setNotificationPreference(preferenceKey, !enabled)
                }
                
                 val errorMsg = try {
                    e.response.body<com.bhanit.apps.echo.data.model.ApiErrorDTO>().errorMessage ?: "Server error while updating $name"
                } catch(parseErr: Exception) {
                    "Server error while updating $name"
                }
                _uiEvent.send(UiEvent.ShowSnackbar(errorMsg))
            } catch (e: Exception) {
                // Rollback
                withContext(NonCancellable) {
                    sessionRepository.setNotificationPreference(preferenceKey, !enabled)
                }
                
                // Check specifically for UnknownHostException or other IO errors which might be wrapped
                val message = if (e.message?.contains("UnknownHostException") == true || e.message?.contains("ConnectException") == true) {
                    "No internet connection"
                } else {
                    "Failed to update $name settings"
                }
                _uiEvent.send(UiEvent.ShowSnackbar(message))
            } finally {
                _loadingStates.update { it - preferenceKey.name }
            }
        }
    }

    fun toggleFeelings(enabled: Boolean) {
        toggleSetting(
            name = "Feelings",
            enabled = enabled,
            preferenceKey = SessionRepository.KEY_NOTIFICATIONS_FEELINGS,
            updateRemote = { userRepository.updateNotificationSettings(feelings = enabled) }
        )
    }

    fun toggleCheckins(enabled: Boolean) {
        toggleSetting(
            name = "Check-ins",
            enabled = enabled,
            preferenceKey = SessionRepository.KEY_NOTIFICATIONS_CHECKINS,
            updateRemote = { userRepository.updateNotificationSettings(checkins = enabled) }
        )
    }

    fun toggleReflections(enabled: Boolean) {
        toggleSetting(
            name = "Reflections",
            enabled = enabled,
            preferenceKey = SessionRepository.KEY_NOTIFICATIONS_REFLECTIONS,
            updateRemote = { userRepository.updateNotificationSettings(reflections = enabled) }
        )
    }

    fun toggleRewards(enabled: Boolean) {
        toggleSetting(
            name = "Rewards",
            enabled = enabled,
            preferenceKey = SessionRepository.KEY_NOTIFICATIONS_REWARDS,
            updateRemote = { userRepository.updateNotificationSettings(rewards = enabled) }
        )
    }

    // Updates are read-only but we might want to support toggling internally or for future use
    fun toggleUpdates(enabled: Boolean) {
        viewModelScope.launch {
            sessionRepository.setNotificationPreference(SessionRepository.KEY_NOTIFICATIONS_UPDATES, enabled)
        }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            sessionRepository.setNotificationPreference(SessionRepository.KEY_NOTIFICATIONS_SOUND, enabled)
        }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            sessionRepository.setNotificationPreference(SessionRepository.KEY_NOTIFICATIONS_VIBRATION, enabled)
        }
    }

    fun toggleInactiveReminders(enabled: Boolean) {
        toggleSetting(
            name = "Inactive Reminders",
            enabled = enabled,
            preferenceKey = SessionRepository.KEY_NOTIFICATIONS_INACTIVE_REMINDERS,
            updateRemote = { userRepository.updateNotificationSettings(inactiveReminders = enabled) }
        )
    }
}
