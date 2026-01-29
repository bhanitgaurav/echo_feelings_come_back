package com.bhanit.apps.echo.features.messaging.presentation.components

import com.bhanit.apps.echo.features.messaging.domain.EchoShareModel

expect class ShareManager {
    suspend fun captureAndShare(model: EchoShareModel)
}
