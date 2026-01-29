package com.bhanit.apps.echo.features.settings.presentation.about

import androidx.lifecycle.viewModelScope
import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.features.settings.domain.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class AboutState(
    val content: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val envName: String = "",
    val version: String = ""
)

class AboutViewModel(
    private val contentRepository: ContentRepository,
    private val appConfig: com.bhanit.apps.echo.core.config.AppConfig
) : BaseViewModel<AboutState>(AboutState(envName = appConfig.envName, version = appConfig.version)) {

    init {
        fetchContent()
    }

    private fun fetchContent() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            contentRepository.getContent("ABOUT")
                .catch { e ->
                    updateState { it.copy(isLoading = false, error = e.message) }
                }
                .collect { content ->
                    updateState { it.copy(isLoading = false, content = content) }
                }
        }
    }
}
