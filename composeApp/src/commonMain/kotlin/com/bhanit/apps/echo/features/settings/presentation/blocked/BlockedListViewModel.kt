package com.bhanit.apps.echo.features.settings.presentation.blocked

import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.data.model.BlockedUserResponse
import com.bhanit.apps.echo.features.user.domain.UserRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

data class BlockedListState(
    val isLoading: Boolean = false,
    val blockedUsers: List<BlockedUserResponse> = emptyList(),
    val error: String? = null
)

class BlockedListViewModel(
    private val userRepository: UserRepository
) : BaseViewModel<BlockedListState>(BlockedListState()) {

    init {
        loadBlockedUsers()
    }

    fun loadBlockedUsers() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            userRepository.getBlockedUsers()
                .onSuccess { users ->
                     updateState { it.copy(isLoading = false, blockedUsers = users) }
                }
                .onFailure { error ->
                     updateState { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            userRepository.unblockUser(userId)
                .onSuccess {
                    loadBlockedUsers() // Refresh list
                }
                .onFailure { error ->
                    // Show error?
                }
        }
    }
}
