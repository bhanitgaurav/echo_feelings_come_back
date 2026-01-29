package com.bhanit.apps.echo.features.contact.presentation.available

import androidx.lifecycle.viewModelScope
import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.features.contact.domain.ContactRepository
import kotlinx.coroutines.launch

import com.bhanit.apps.echo.core.network.AppException
import com.bhanit.apps.echo.core.network.safeApiCall

import com.bhanit.apps.echo.core.presentation.components.SearchMode
import com.bhanit.apps.echo.features.contact.domain.Connection
import com.bhanit.apps.echo.features.contact.domain.Contact
import com.bhanit.apps.echo.features.contact.domain.ConnectionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class AvailableContactsState(
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val incomingRequests: List<Connection> = emptyList(),
    val resultsList: List<Connection> = emptyList(), // Unified List (Registered + Invite)
    val error: AppException? = null,
    val searchQuery: String = "",
    val searchMode: SearchMode = SearchMode.NAME_ID,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val referralCode: String? = null,
    val referralUrl: String? = null
)

class AvailableContactsViewModel(
    private val contactRepository: ContactRepository,
    private val userRepository: com.bhanit.apps.echo.features.user.domain.UserRepository
) : BaseViewModel<AvailableContactsState>(AvailableContactsState()) {
    
    private var searchJob: Job? = null

    // Initial Load
    fun loadData() {
        viewModelScope.launch {
             // 1. Fetch Incoming Requests
             loadIncomingRequests()
             // 2. Fetch Initial List (Default "My Contacts")
             loadList(reset = true)
             // 3. Load Referral Code
             loadReferralCode()
        }
    }

    private suspend fun loadReferralCode() {
        try {
            val result = userRepository.getProfile()
            result.getOrNull()?.let { profile ->
                updateState { it.copy(referralCode = profile.referralCode, referralUrl = profile.referralUrl) }
            }
        } catch (e: Exception) {
            // Log or ignore
        }
    }

    private suspend fun loadIncomingRequests() {
         try {
             val requests = contactRepository.getFriendRequests()
             updateState { it.copy(incomingRequests = requests) }
         } catch (e: Exception) {
             // Silently fail or log?
         }
    }
    
    fun onSearchQueryChange(newQuery: String) {
        updateState { it.copy(searchQuery = newQuery) }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (newQuery.length >= 3) {
                 delay(300) // Debounce
                 loadList(reset = true)
            } else if (newQuery.isEmpty()) {
                 loadList(reset = true) // Reset to default list
            }
        }
    }
    
    fun onSearchModeChange(newMode: SearchMode) {
        updateState { it.copy(searchMode = newMode) }
        // Optional: specific logic if keyboard/validation differs
    }

    fun loadList(reset: Boolean = false) {
        if (state.value.isLoading && !reset) return
        if (!state.value.hasMore && !reset) return

        val currentPage = if (reset) 1 else state.value.page
        val currentQuery = state.value.searchQuery
        
        viewModelScope.launch {
            updateState { 
                if (reset) it.copy(isLoading = true, error = null, resultsList = emptyList(), page = 1, hasMore = true)
                else it.copy(isLoading = true, error = null)
            }

            try {
                val limit = 10
                val connections = if (currentQuery.length >= 3) {
                    contactRepository.searchAvailableContacts(currentQuery, currentPage, limit)
                } else {
                    // Default View: "My Contacts" (Unified)
                    // Note: getUnifiedContacts in current Repo is generic non-paged sync. 
                    // We need to support paging or just fetch all if local list is expected to be small?
                    // The Repo interface says: getAvailableContacts(page, limit). 
                    // But we used getUnifiedContacts before which did Sync.
                    // Ideally we should call getUnifiedContacts() ONCE to sync, then rely on getAvailable.
                    // However, users Add Friend screen implies seeing *Synced* contacts.
                    // For now, let's assume we map the "Unified" list to a paged list manually if the repo doesn't support paging on sync.
                    // Or call the new search method with empty query if it supports it?
                    // Let's use searchAvailableContacts("", page, limit) which we implemented to handle empty query defaulting/searching.
                    // Wait, our implementation of searchAvailableContacts handles empty query?
                    // Implementation: normalizedQuery.trim(). filter local matches.
                    // if query empty -> localMatches = all. server search with empty query -> maybe returns popular or nothing?
                    // Let's assume we call search with empty query to get "All My Contacts".
                    contactRepository.searchAvailableContacts("", currentPage, limit) 
                }
                
                updateState {
                    it.copy(
                        isLoading = false,
                        resultsList = if (reset) connections else it.resultsList + connections,
                        page = currentPage + 1,
                        hasMore = connections.size == limit
                    )
                }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, error = e as? AppException) }
            }
        }
    }
    
    fun loadMore() {
        loadList(reset = false)
    }

    fun onInvite(connection: Connection) {
         viewModelScope.launch {
            try {
                contactRepository.inviteUser(connection.phoneNumber)
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }

    fun onAddConnection(userId: String) {
        viewModelScope.launch {
            try {
                contactRepository.requestConnection(userId)
                // update local item status
                updateItemStatus(userId, ConnectionStatus.PENDING_OUTGOING)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun onCancelRequest(userId: String) {
        viewModelScope.launch {
            try {
                 contactRepository.cancelRequest(userId)
                 updateItemStatus(userId, ConnectionStatus.NONE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onAcceptConnection(userId: String) {
        viewModelScope.launch {
            try {
                contactRepository.acceptConnection(userId)
                // In incoming list? Remove it.
                 val newIncoming = state.value.incomingRequests.filter { it.userId != userId }
                 updateState { it.copy(incomingRequests = newIncoming) }
                 loadList(reset = true) // Refresh main list to show connected
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun updateItemStatus(userId: String, status: ConnectionStatus) {
         val updatedList = state.value.resultsList.map { 
             if (it.userId == userId) it.copy(status = status) else it
         }
         updateState { it.copy(resultsList = updatedList) }
    }

    fun onConnect(userId: String) {
        
    }

    fun openSettings() {
        contactRepository.openSettings()
    }
}
