package com.bhanit.apps.echo.features.contact.presentation.list

import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.features.contact.domain.Connection
import com.bhanit.apps.echo.features.contact.domain.ContactRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

import com.bhanit.apps.echo.core.network.AppException
import com.bhanit.apps.echo.core.network.safeApiCall

data class PeopleState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val connections: List<Connection> = emptyList(),
    val error: AppException? = null,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val pendingRequests: List<Connection> = emptyList(), // Added
    val searchQuery: String = "",
    val searchMode: com.bhanit.apps.echo.core.presentation.components.SearchMode = com.bhanit.apps.echo.core.presentation.components.SearchMode.NAME_ID
)

class PeopleViewModel(
    private val contactRepository: ContactRepository
) : BaseViewModel<PeopleState>(PeopleState()) {



    private var searchJob: kotlinx.coroutines.Job? = null

    fun onSearchQueryChange(newQuery: String) {
        updateState { it.copy(searchQuery = newQuery) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500) // Debounce
            loadConnections(reset = true)
        }
    }

    fun onSearchModeChange(newMode: com.bhanit.apps.echo.core.presentation.components.SearchMode) {
        updateState { it.copy(searchMode = newMode) }
         // Optional: Trigger search immediately on mode switch?
         // Yes, if query exists.
         if (state.value.searchQuery.isNotEmpty()) {
             loadConnections(reset = true)
         }
    }

    fun loadConnections(reset: Boolean = false) {
        // if (state.value.isLoading && !reset) return // Allow cancel/restart for search
        
        val currentPage = if (reset) 1 else state.value.page
        val query = state.value.searchQuery

        viewModelScope.launch {
             updateState { 
                 if (reset) it.copy(isLoading = true, isRefreshing = true, error = null, connections = emptyList(), pendingRequests = emptyList()) 
                 else it.copy(isLoading = true, error = null)
             }
             
             safeApiCall {
                 val limit = 10
                 val newConnections = contactRepository.getConnections(currentPage, limit, query)
                 
                 // Fetch pending requests only on reset/first load or refresh
                 val requests = if (reset) contactRepository.getPendingRequests() else state.value.pendingRequests
                 
                 Pair(newConnections, requests)
             }.fold(
                 onSuccess = { resultPair ->
                     val newConnections: List<Connection> = resultPair.first
                     val requests: List<Connection> = resultPair.second
                     updateState {
                         it.copy(
                             isLoading = false,
                             isRefreshing = false,
                             connections = if (reset) newConnections else it.connections + newConnections,
                             pendingRequests = requests,
                             page = currentPage + 1,
                             hasMore = newConnections.size == 10
                         )
                     }
                 },
                 onFailure = { e ->
                     updateState {
                         it.copy(isLoading = false, isRefreshing = false, error = e as? AppException)
                     }
                 }
             )
        }
    }
    
    fun onAccept(connection: Connection) {
        viewModelScope.launch {
            try {
                contactRepository.acceptConnection(connection.userId)
                refresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun onDeny(connection: Connection) {
        viewModelScope.launch {
            try {
                contactRepository.denyConnection(connection.userId)
                refresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refresh() {
        loadConnections(reset = true)
    }

    fun loadNextPage() {
        if (state.value.hasMore && !state.value.isLoading) {
            loadConnections(reset = false)
        }
    }

    fun performAction(connection: Connection) {
        // Logic for click -> e.g. Open Chat
    }
}

private data class LoadResult(
    val connections: List<Connection>,
    val requests: List<Connection>
)
