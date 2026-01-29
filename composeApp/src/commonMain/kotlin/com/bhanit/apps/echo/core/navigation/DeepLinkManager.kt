package com.bhanit.apps.echo.core.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DeepLinkManager {
    private val _deepLinkEvent = MutableSharedFlow<DeepLinkData>(replay = 1)
    val deepLinkEvent = _deepLinkEvent.asSharedFlow()

    suspend fun handleDeepLink(data: DeepLinkData) {
        _deepLinkEvent.emit(data)
    }
}
