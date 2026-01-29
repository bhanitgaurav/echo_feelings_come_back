package com.bhanit.apps.echo.features.credits.presentation

import androidx.lifecycle.viewModelScope
import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.data.model.CreditTransaction
import com.bhanit.apps.echo.data.model.PurchaseRequest
import com.bhanit.apps.echo.data.model.PurchaseType
import com.bhanit.apps.echo.features.user.domain.UserRepository
import kotlinx.coroutines.launch

data class CreditsState(
    val isLoading: Boolean = false,
    val isHistoryLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val credits: Int = 0,
    val milestones: List<com.bhanit.apps.echo.data.model.MilestoneStatusDto> = emptyList(),
    val phoneNumber: String? = null,
    val referralCode: String? = null,
    val referralUrl: String? = null,
    val history: List<CreditTransaction> = emptyList(),
    val emotions: List<com.bhanit.apps.echo.features.messaging.domain.Emotion> = emptyList(),
    val error: com.bhanit.apps.echo.core.network.AppException? = null,
    val purchaseSuccess: String? = null,
    val purchaseError: String? = null, // Separate error for dialogs
    val searchQuery: String = "",
    val activeFilter: com.bhanit.apps.echo.data.model.TransactionFilter = com.bhanit.apps.echo.data.model.TransactionFilter.ALL,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val page: Int = 1,
    val isLastPage: Boolean = false
)

class CreditsViewModel(
    private val userRepository: UserRepository,
    private val emotionRepository: com.bhanit.apps.echo.features.messaging.domain.EmotionRepository,
    private val analytics: com.bhanit.apps.echo.shared.analytics.EchoAnalytics
) : BaseViewModel<CreditsState>(CreditsState()) {

    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            

            val creditsResult = userRepository.getCredits()
            val milestonesResult = userRepository.getMilestones()
            val profileResult = userRepository.getProfile()
            val emotionsResult = try { Result.success(emotionRepository.getEmotions(true)) } catch(e: Exception) { Result.failure(e) }

            // Initial History Load
            val filterStartDate = state.value.startDate?.let { com.bhanit.apps.echo.core.util.DateTimeUtils.getStartOfDayInUserTimeZone(it) }
            val filterEndDate = state.value.endDate?.let { com.bhanit.apps.echo.core.util.DateTimeUtils.getEndOfDayInUserTimeZone(it) }
            
            val historyResult = userRepository.getCreditHistory(
                page = 1, 
                pageSize = 20, 
                query = state.value.searchQuery.takeIf { it.isNotBlank() }, 
                filter = state.value.activeFilter,
                startDate = filterStartDate,
                endDate = filterEndDate
            )

            if (creditsResult.isSuccess && historyResult.isSuccess) {
                val profile = profileResult.getOrNull()
                val history = historyResult.getOrDefault(emptyList())
                updateState { 
                    it.copy(
                        isLoading = false,
                        credits = creditsResult.getOrDefault(0),
                        milestones = milestonesResult.getOrDefault(emptyList()),
                        history = history,
                        emotions = emotionsResult.getOrDefault(emptyList()),
                        referralCode = profile?.referralCode,
                        referralUrl = profile?.referralUrl,
                        phoneNumber = profile?.phoneNumber, // Fix: Populate phone number for UI logic
                        page = 1,
                        isLastPage = history.size < 20
                    )
                }
            } else {
                 val exception = creditsResult.exceptionOrNull() ?: historyResult.exceptionOrNull()
                 updateState { 
                    it.copy(
                        isLoading = false,
                        error = exception as? com.bhanit.apps.echo.core.network.AppException
                    )
                }
            }
        }
    }
    fun onSearch(query: String) {
        updateState { it.copy(searchQuery = query) }
        searchJob?.cancel()
        
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            searchJob = viewModelScope.launch {
                refreshHistory(reset = true)
            }
        } else if (trimmed.length >= 3) {
            searchJob = viewModelScope.launch {
                kotlinx.coroutines.delay(500) // Debounce
                refreshHistory(reset = true)
            }
        }
    }

    fun onFilter(filter: com.bhanit.apps.echo.data.model.TransactionFilter) {
        updateState { it.copy(activeFilter = filter) }
        viewModelScope.launch {
            refreshHistory(reset = true)
        }
    }

    fun onDateRangeSelected(start: Long?, end: Long?) {
        updateState { it.copy(startDate = start, endDate = end) }
        viewModelScope.launch {
            refreshHistory(reset = true)
        }
    }

    fun loadNextPage() {
        val currentState = state.value
        if (currentState.isLoadingMore || currentState.isLastPage || currentState.isHistoryLoading) return

        viewModelScope.launch {
            updateState { it.copy(isLoadingMore = true) }
            val nextPage = currentState.page + 1
            
            val filterStartDate = currentState.startDate?.let { com.bhanit.apps.echo.core.util.DateTimeUtils.getStartOfDayInUserTimeZone(it) }
            val filterEndDate = currentState.endDate?.let { com.bhanit.apps.echo.core.util.DateTimeUtils.getEndOfDayInUserTimeZone(it) }
            
            val result = userRepository.getCreditHistory(
                page = nextPage,
                pageSize = 20,
                query = currentState.searchQuery.takeIf { it.isNotBlank() },
                filter = currentState.activeFilter,
                startDate = filterStartDate,
                endDate = filterEndDate
            )

            if (result.isSuccess) {
                val newItems = result.getOrDefault(emptyList())
                updateState { 
                    it.copy(
                        isLoadingMore = false,
                        history = it.history + newItems,
                        page = nextPage,
                        isLastPage = newItems.size < 20
                    )
                }
            } else {
                updateState { it.copy(isLoadingMore = false) } 
            }
        }
    }

    private suspend fun refreshHistory(reset: Boolean) {
        updateState { it.copy(isHistoryLoading = true) }
        
        val filterStartDate = state.value.startDate?.let { com.bhanit.apps.echo.core.util.DateTimeUtils.getStartOfDayInUserTimeZone(it) }
        val filterEndDate = state.value.endDate?.let { com.bhanit.apps.echo.core.util.DateTimeUtils.getEndOfDayInUserTimeZone(it) }
        
        val result = userRepository.getCreditHistory(
            page = 1,
            pageSize = 20,
            query = state.value.searchQuery.takeIf { it.isNotBlank() },
            filter = state.value.activeFilter,
            startDate = filterStartDate,
            endDate = filterEndDate
        )
        
        if (result.isSuccess) {
            val history = result.getOrDefault(emptyList())
            updateState { 
                it.copy(
                    isHistoryLoading = false,
                    history = history,
                    page = 1,
                    isLastPage = history.size < 20
                )
            }
        } else {
            updateState { it.copy(isHistoryLoading = false, error = result.exceptionOrNull() as? com.bhanit.apps.echo.core.network.AppException) }
        }
    }

    fun purchaseItem(type: PurchaseType, itemId: String, count: Int? = null, targetUserId: String? = null) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null, purchaseSuccess = null, purchaseError = null) }
            val request = PurchaseRequest(type, itemId, targetUserId, count = count)
            val result = userRepository.purchaseItem(request)
            
            if (result.isSuccess) {
                updateState { it.copy(purchaseSuccess = "Purchase Successful!", isLoading = false) }
                analytics.logEvent(
                    com.bhanit.apps.echo.shared.analytics.EchoEvents.CREDITS_USED,
                    mapOf(
                        com.bhanit.apps.echo.shared.analytics.EchoParams.CREDIT_REASON to type.name,
                        "item_id" to itemId
                    )
                )
                loadData() // Refresh everything
            } else {
                // Use purchaseError instead of global error to avoid hiding UI
                val exception = result.exceptionOrNull()
                val message = exception?.message ?: "Purchase failed"
                updateState { it.copy(isLoading = false, purchaseError = message) }
            }
        }
    }
    
    fun clearMessage() {
        updateState { it.copy(error = null, purchaseSuccess = null, purchaseError = null) }
    }
}
