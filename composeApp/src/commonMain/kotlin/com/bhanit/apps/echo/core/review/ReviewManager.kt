package com.bhanit.apps.echo.core.review

interface ReviewManager {
    suspend fun tryRequestReview()
}
