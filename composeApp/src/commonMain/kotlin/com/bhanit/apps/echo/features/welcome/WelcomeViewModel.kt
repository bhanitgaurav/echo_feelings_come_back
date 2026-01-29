package com.bhanit.apps.echo.features.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WelcomeViewModel : ViewModel() {
    // No complex state needed for now, just navigation handling if necessary.
    // In future, could track which slide is active or if user has seen detailed tutorial.
    
    private val _state = MutableStateFlow(WelcomeState())
    val state = _state.asStateFlow()

    fun onGetStarted() {
        // Logic if needed effectively handled by UI callback for now
    }
}

data class WelcomeState(
    val currentPage: Int = 0
)
