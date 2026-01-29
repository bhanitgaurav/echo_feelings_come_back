package com.bhanit.apps.echo.core.network

import io.ktor.client.HttpClient

import com.bhanit.apps.echo.features.auth.domain.SessionRepository

expect class HttpClientFactory(sessionRepository: SessionRepository, baseUrl: String) {
    fun create(): HttpClient
}
