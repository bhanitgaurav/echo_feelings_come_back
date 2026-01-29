package com.bhanit.apps.echo.features.settings.presentation.support

import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.data.model.TicketCategory
import com.bhanit.apps.echo.data.model.SupportRequest
import com.bhanit.apps.echo.features.settings.domain.SupportRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

import com.bhanit.apps.echo.data.model.SupportTicketDto

data class SupportState(
    val category: TicketCategory = TicketCategory.BUG,
    val subject: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val error: com.bhanit.apps.echo.core.network.AppException? = null,
    val isSuccess: Boolean = false,
    val tickets: List<SupportTicketDto> = emptyList()
)

class SupportViewModel(
    private val supportRepository: SupportRepository
) : BaseViewModel<SupportState>(SupportState()) {

    init {
        loadTickets()
    }

    fun loadTickets() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            supportRepository.getTickets(page = 1, limit = 50)
                .onSuccess { response ->
                    updateState { it.copy(tickets = response.tickets, isLoading = false) }
                }
                .onFailure {
                     val e = it as? com.bhanit.apps.echo.core.network.AppException
                    updateState { it.copy(isLoading = false, error = e) }
                }
        }
    }
    fun onCategoryChange(category: TicketCategory) {
        updateState { it.copy(category = category) }
    }

    fun onSubjectChange(subject: String) {
        if (subject.length <= 50) {
            updateState { it.copy(subject = subject, error = null) }
        }
    }

    fun onDescriptionChange(description: String) {
        if (description.length <= 500) {
            updateState { it.copy(description = description, error = null) }
        }
    }

    fun submitTicket() {
        val currentState = state.value
        
        if (currentState.subject.length < 3) {
            // Local validation error, maybe keep as String or create local AppException?
            // For now, let's just make a local AppException for consistency, or keep validation separate.
            // ProfileScreen separates validationError (String) and saveError (String).
            // Here `error` is unique. I'll create an auto-exception.
             updateState { it.copy(error = com.bhanit.apps.echo.core.network.AppException("Validation Error", "Subject must be at least 3 characters")) }
            return
        }
        
        if (currentState.description.length < 10) {
             updateState { it.copy(error = com.bhanit.apps.echo.core.network.AppException("Validation Error", "Description must be at least 10 characters")) }
             return
        }

        updateState { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val request = SupportRequest(
                category = currentState.category,
                subject = currentState.subject,
                description = currentState.description
            )
            
            supportRepository.submitTicket(request)
                .onSuccess {
                    updateState { it.copy(isLoading = false, isSuccess = true) }
                    loadTickets() // Refresh list
                }
                .onFailure { error ->
                    val e = error as? com.bhanit.apps.echo.core.network.AppException ?: com.bhanit.apps.echo.core.network.AppException("Error", error.message ?: "Failed")
                    updateState { it.copy(isLoading = false, error = e) }
                }
        }
    }
    
    fun resetState() {
        updateState { SupportState(tickets = it.tickets) } // Keep tickets
    }
}
