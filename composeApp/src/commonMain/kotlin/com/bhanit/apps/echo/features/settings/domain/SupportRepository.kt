package com.bhanit.apps.echo.features.settings.domain

import com.bhanit.apps.echo.data.model.SupportRequest
import com.bhanit.apps.echo.data.model.SupportResponse

import com.bhanit.apps.echo.data.model.SupportHistoryResponse

interface SupportRepository {
    suspend fun submitTicket(request: SupportRequest): Result<SupportResponse>
    suspend fun getTickets(page: Int, limit: Int): Result<SupportHistoryResponse>
}
