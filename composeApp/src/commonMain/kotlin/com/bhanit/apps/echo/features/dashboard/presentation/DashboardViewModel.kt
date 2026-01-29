package com.bhanit.apps.echo.features.dashboard.presentation

import androidx.lifecycle.viewModelScope
import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.core.network.AppException
import com.bhanit.apps.echo.core.network.safeApiCall
import com.bhanit.apps.echo.data.model.AnalyticsDTO
import com.bhanit.apps.echo.data.model.TimeRange
import com.bhanit.apps.echo.features.auth.domain.SessionRepository
import com.bhanit.apps.echo.features.dashboard.data.DashboardApi
import kotlinx.coroutines.flow.MutableStateFlow
import com.bhanit.apps.echo.features.user.domain.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import com.bhanit.apps.echo.features.messaging.domain.EmotionRepository
import com.bhanit.apps.echo.features.messaging.domain.Emotion
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.Clock

data class DashboardState(
    val userName: String = "User",
    val isLoading: Boolean = true,
    val analytics: AnalyticsDTO? = null,
    val streakData: com.bhanit.apps.echo.features.habits.data.UserStreakData? = null,
    val showActivityOverview: Boolean = true,
    val error: AppException? = null,
    val selectedTimeRange: TimeRange = TimeRange.WEEK,
    val filterType: String = "ALL", // ALL, RECEIVED, SENT, EMOTION_KEY
    val showEmotions: Boolean = true,
    val emotions: List<Emotion> = emptyList(),
    val notification: com.bhanit.apps.echo.features.habits.data.NotificationDTO? = null,
    val profilePhoto: String? = null,
    val referralUrl: String = ""
)



class DashboardViewModel(
    private val sessionRepository: SessionRepository,
    private val dashboardApi: DashboardApi,
    private val userRepository: UserRepository,
    private val emotionRepository: EmotionRepository,
    private val habitApi: com.bhanit.apps.echo.features.habits.data.HabitApi,
    private val analytics: com.bhanit.apps.echo.shared.analytics.EchoAnalytics
) : BaseViewModel<DashboardState>(DashboardState()) {



    // BaseViewModel usually manages state, but if _uiState was used redundantly, we stick to BaseViewModel's pattern
    // Assuming BaseViewModel<S> exposes `val state: StateFlow<S>` and `fun updateState((S) -> S)`
    

    
    private var fetchJob: kotlinx.coroutines.Job? = null

    fun loadData() {
        fetchJob?.cancel()

        viewModelScope.launch {
            sessionRepository.showActivityOverview.collect { show ->
                updateState { it.copy(showActivityOverview = show) }
            }
        }
        
        fetchJob = viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            
            try {
                // 0. Record Activity FIRST (Fix Race Condition)
                try {
                    habitApi.recordActivity("DASHBOARD_OPEN")
                } catch (e: Exception) {
                    // Ignore recording failure, proceed to load data
                }

                // Log Analytics
                val hour = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).hour
                val timeBucket = when(hour) {
                    in 5..11 -> "MORNING"
                    in 12..16 -> "AFTERNOON"
                    in 17..20 -> "EVENING"
                    else -> "NIGHT"
                }
                
                analytics.logEvent(
                    com.bhanit.apps.echo.shared.analytics.EchoEvents.DASHBOARD_OPENED,
                    mapOf(
                        com.bhanit.apps.echo.shared.analytics.EchoParams.TIME_BUCKET to timeBucket,
                        com.bhanit.apps.echo.shared.analytics.EchoParams.TIMEZONE to kotlinx.datetime.TimeZone.currentSystemDefault().id
                    )
                )

                // 1. Load Session & User Profile (Same as before)
                val session = sessionRepository.getSession()
                var currentUsername = session?.username
                val phone = session?.phone ?: "User"

                // FCM Token Sync moved to App.kt to avoid redundant calls on filter change
                // Always fetch profile to ensure photo and username are up to date
                try {
                    userRepository.getProfile().onSuccess { profile ->
                        currentUsername = profile.username
                        if (session != null) {
                            sessionRepository.saveSession(
                                token = session.token,
                                userId = session.userId,
                                phone = session.phone,
                                username = profile.username,
                                isOnboardingCompleted = true
                            )
                        }
                        // Update photo in state
                        if (profile.photoUrl != null) {
                            updateState { it.copy(profilePhoto = profile.photoUrl, referralUrl = profile.referralUrl ?: "") }
                        } else {
                            updateState { it.copy(referralUrl = profile.referralUrl ?: "") }
                        }
                    }
                } catch (e: Exception) { /* Ignore */ }
                
                updateState { it.copy(userName = currentUsername ?: phone) }
                
                // 2. Load Analytics, Emotions, AND Streaks
                val analyticsDeferred = async { safeApiCall { dashboardApi.getAnalytics(state.value.selectedTimeRange) } }
                val emotionsDeferred = async { safeApiCall { emotionRepository.getEmotions() } }
                val streaksDeferred = async { safeApiCall { habitApi.getHabitStatus() } }
                val notificationDeferred = async { safeApiCall { habitApi.getInAppNotification() } }

                val analyticsResult = analyticsDeferred.await()
                val emotionsResult = emotionsDeferred.await()
                val streaksResult = streaksDeferred.await()
                val notificationResult = notificationDeferred.await()
                
                analyticsResult.onSuccess { analytics ->
                    updateState { it.copy(analytics = analytics) }
                }
                
                emotionsResult.onSuccess { list ->
                    updateState { it.copy(emotions = list) }
                }

                streaksResult.onSuccess { streaks ->
                    updateState { it.copy(streakData = streaks) }
                }

                notificationResult.onSuccess { notif ->
                    updateState { it.copy(notification = notif) }
                }

                updateState { it.copy(isLoading = false) }
            } catch (e: Exception) {
                 if (e !is kotlinx.coroutines.CancellationException) {
                     updateState { 
                         it.copy(isLoading = false, error = AppException("Unexpected Error", e.message ?: "Unknown")) 
                     }
                 }
            }
        }
    }
    
    fun onTimeRangeSelected(range: TimeRange) {
        updateState { it.copy(selectedTimeRange = range) }
        loadData()
    }
    
    fun onFilterTypeSelected(type: String) {
         updateState { it.copy(filterType = type) }
    }

    fun cycleFilter() {
        val nextFilter = when (state.value.filterType) {
            "ALL" -> "RECEIVED"
            "RECEIVED" -> "SENT"
            "SENT" -> "ALL"
            else -> "ALL"
        }
        updateState { it.copy(filterType = nextFilter) }
    }

    fun toggleEmotions() {
        updateState { it.copy(showEmotions = !it.showEmotions) }
    }
    
    fun logout() {
        viewModelScope.launch {
            sessionRepository.clearSession()
        }
    }
}
