package com.bhanit.apps.echo.features.settings.presentation.theme

import androidx.lifecycle.viewModelScope
import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.core.theme.ThemeConfig
import com.bhanit.apps.echo.core.theme.AppFont
import com.bhanit.apps.echo.core.theme.CustomThemeColors
import com.bhanit.apps.echo.features.auth.domain.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ThemeState(
    val currentTheme: ThemeConfig = ThemeConfig.SYSTEM
)

class ThemeViewModel(
    private val sessionRepository: SessionRepository
) : BaseViewModel<ThemeState>(ThemeState()) {

    val currentTheme = sessionRepository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeConfig.SYSTEM)

    fun setTheme(theme: ThemeConfig) {
        viewModelScope.launch {
            sessionRepository.setTheme(theme)
        }
    }

    val currentFont = sessionRepository.font
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppFont.INTER)

    fun setFont(font: AppFont) {
        viewModelScope.launch {
            sessionRepository.setFont(font)
        }
    }

    val customThemeColors = sessionRepository.customThemeColors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomThemeColors())

    fun updateCustomTheme(colors: CustomThemeColors) {
        viewModelScope.launch {
            sessionRepository.setCustomThemeColors(colors)
        }
    }
}
