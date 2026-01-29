package com.bhanit.apps.echo.features.settings.data

import com.bhanit.apps.echo.core.network.safeApiCall
import com.bhanit.apps.echo.data.model.SupportRequest
import com.bhanit.apps.echo.data.model.SupportResponse
import com.bhanit.apps.echo.features.settings.domain.SupportRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

import io.ktor.client.call.body

import io.ktor.client.request.get
import io.ktor.client.request.parameter

class SupportRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : SupportRepository {
    override suspend fun submitTicket(request: SupportRequest): Result<SupportResponse> {
        return safeApiCall {
            httpClient.post("$baseUrl/support") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

        }
    }

    override suspend fun getTickets(page: Int, limit: Int): Result<com.bhanit.apps.echo.data.model.SupportHistoryResponse> {
        return safeApiCall {
            httpClient.get("$baseUrl/support") {
                parameter("page", page)
                parameter("limit", limit)
            }.body()
        }
    }
}
