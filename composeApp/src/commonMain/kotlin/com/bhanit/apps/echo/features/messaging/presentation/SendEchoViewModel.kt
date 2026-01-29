package com.bhanit.apps.echo.features.messaging.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhanit.apps.echo.features.contact.domain.ContactRepository
import com.bhanit.apps.echo.features.contact.domain.Connection
import com.bhanit.apps.echo.features.messaging.domain.Emotion
import com.bhanit.apps.echo.features.messaging.domain.Message
import com.bhanit.apps.echo.features.messaging.domain.MessagingRepository
import com.bhanit.apps.echo.core.network.AppException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

data class SendEchoState(
    val isSending: Boolean = false,
    val isHistoryLoading: Boolean = false,
    val connections: List<Connection> = emptyList(),
    val history: List<Message> = emptyList(),
    val sendSuccess: Boolean = false,
    val sendError: AppException? = null,
    val historyError: AppException? = null,
    val emotions: List<Emotion> = emptyList(),
    val remainingDailyLimit: Int? = null,
    val dailyLimitTotal: Int? = null,

    val blockSuccess: Boolean = false,
    val removeConnectionSuccess: Boolean = false,
    val isConnected: Boolean = true // Default to true, updated by check
)

class SendEchoViewModel(
    private val messagingRepository: MessagingRepository, 
    private val contactRepository: ContactRepository,
    private val emotionRepository: com.bhanit.apps.echo.features.messaging.domain.EmotionRepository,
    private val userRepository: com.bhanit.apps.echo.features.user.domain.UserRepository,
    private val analytics: com.bhanit.apps.echo.shared.analytics.EchoAnalytics,
    private val reviewManager: com.bhanit.apps.echo.core.review.ReviewManager,
    private val sessionRepository: com.bhanit.apps.echo.features.auth.domain.SessionRepository,
    private val platformUtils: com.bhanit.apps.echo.core.util.PlatformUtils
) : ViewModel() {

    private val _state = MutableStateFlow(SendEchoState())
    val state: StateFlow<SendEchoState> = _state.asStateFlow()

    init {
        loadEmotions()
    }

    private fun loadEmotions() {
        viewModelScope.launch {
            val emotions = emotionRepository.getEmotions()
            val sortedEmotions = emotions.sortedWith(
                compareBy<Emotion> { 
                    when {
                        it.tier == "FREE" -> 0
                        it.isUnlocked -> 1
                        else -> 2
                    }
                }.thenBy { it.price }
            )
            _state.update { it.copy(emotions = sortedEmotions) }
        }
    }

    fun loadHistory(receiverId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isHistoryLoading = true, historyError = null) }
            val result = messagingRepository.getSentMessages(receiverId, 1, 10)
            result.fold(
                onSuccess = { messages ->
                    _state.update { it.copy(history = messages, isHistoryLoading = false) }
                },
                onFailure = { e ->
                    val appError = e as? AppException ?: AppException("Error", e.message ?: "Unknown error")
                    _state.update { it.copy(historyError = appError, isHistoryLoading = false) }
                }
            )
        }
    }
    
    fun loadUsage(receiverId: String) {
        viewModelScope.launch {
            messagingRepository.getDailyUsage(receiverId).fold(
                onSuccess = { map ->
                    _state.update { 
                        it.copy(
                            remainingDailyLimit = map["remaining"],
                            dailyLimitTotal = map["limit"]
                        ) 
                    }
                },
                onFailure = { 
                     println("Error fetching usage: ${it.message}")
                }
            )
        }
    }

    fun checkConnectionStatus(receiverId: String) {
        viewModelScope.launch {
             contactRepository.getConnections().collect { connections ->
                 val isConnected = connections.any { it.userId == receiverId }
                 _state.update { it.copy(isConnected = isConnected) }
             }
        }
    }

    fun sendEcho(receiverId: String, emotion: Emotion, content: String, isAnonymous: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, sendSuccess = false, sendError = null) }
            val result = messagingRepository.sendMessage(receiverId, emotion, content, isAnonymous)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isSending = false, sendSuccess = true) }
                    
                    // Analytics
                    analytics.logEvent(
                        com.bhanit.apps.echo.shared.analytics.EchoEvents.FEELING_SENT,
                        mapOf(
                            com.bhanit.apps.echo.shared.analytics.EchoParams.SENTIMENT to emotion.displayName,
                            "emotion_id" to emotion.id
                        )
                    )
                    
                    
                    loadHistory(receiverId) 
                    loadUsage(receiverId)
                    
                    // Smart Trigger: Ask for review if new version or not rated
                    viewModelScope.launch {
                        val currentVersion = platformUtils.getAppVersionCode()
                        val lastRated = sessionRepository.lastRatedVersion.first()
                        
                        if (currentVersion > lastRated) {
                            reviewManager.tryRequestReview()
                            // Mark as rated for this version immediately after launch attempt
                            // (We assume success or at least that we tried)
                            sessionRepository.setLastRatedVersion(currentVersion)
                        }
                    }
                },
                onFailure = { e ->
                    val appError = e as? AppException ?: AppException("Error", e.message ?: "Unknown error")
                    _state.update { it.copy(isSending = false, sendError = appError) }
                }
            )
        }
    }

    fun blockUser(receiverId: String) {
         viewModelScope.launch {
            userRepository.blockUser(receiverId).fold(
                onSuccess = {
                     _state.update { it.copy(blockSuccess = true) } 
                },
                onFailure = { e ->
                     // Handle error if needed
                     println("Error blocking user: $e")
                }
            )
        }
    }

    fun removeConnection(receiverId: String) {
        viewModelScope.launch {
            try {
                contactRepository.removeConnection(receiverId)
                _state.update { it.copy(removeConnectionSuccess = true) }
            } catch (e: Exception) {
                println("Error removing connection: $e")
                 // Optionally set an error state
            }
        }
    }

    fun boostLimit(receiverId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, sendSuccess = false, sendError = null) }
            // Using generic PurchaseRequest
            val request = com.bhanit.apps.echo.data.model.PurchaseRequest(
                type = com.bhanit.apps.echo.data.model.PurchaseType.LIMIT_BOOST,
                itemId = "",
                targetUserId = receiverId
            )
            // Cost is 1. Should be dynamic but hardcoded for now or we rely on server returning error if low balance.
            val result = userRepository.purchaseItem(request)
            
            if (result.isSuccess) {
                 // Refresh usage
                 loadUsage(receiverId)
                 _state.update { it.copy(isSending = false) } // Just stop loading
            } else {
                 val error = result.exceptionOrNull()
                 val appError = error as? AppException ?: AppException("Error", error?.message ?: "Boost Failed")
                 _state.update { it.copy(isSending = false, sendError = appError) }
            }
        }
    }

    fun resetState() {
        _state.update { it.copy(sendSuccess = false, sendError = null, blockSuccess = false, removeConnectionSuccess = false) }
    }
}
