package com.bhanit.apps.echo.features.dashboard.data

import com.bhanit.apps.echo.data.model.AnalyticsDTO
import com.bhanit.apps.echo.data.model.TimeRange
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.toLocalDateTime

class DashboardApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun getAnalytics(range: TimeRange): AnalyticsDTO {
        return client.get("$baseUrl/dashboard/analytics") {
            parameter("range", range.name)
            // Send local date for accurate server-side processing
            val today = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
            parameter("localDate", today)
        }.body()
    }
}
