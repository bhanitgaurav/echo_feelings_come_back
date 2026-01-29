package com.bhanit.apps.echo.features.habits.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

import io.ktor.http.ContentType
import io.ktor.http.contentType

class HabitApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun recordActivity(type: String) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        client.post("$baseUrl/habits/activity") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("type" to type, "date" to today))
        }
    }

    suspend fun getHabitStatus(): UserStreakData {
        return client.get("$baseUrl/habits/status").body()
    }

    suspend fun getInAppNotification(): NotificationDTO? {
        // Return nullable because 204 NoContent returns null body or throws?
        // Ktor body<T?>() handles generic nullable if status is 204/404? No, usually exception for 204.
        // Let's use safely call `body()` if status is OK.
        // Simplified: client.get().body<NotificationDTO?>() returns null if 204?
        // Actually, simple way: try/catch in ViewModel or check status. 
        // Best: client.get(...).body() works if server sends JSON. If 204, body() might fail or return Unit.
        // Let's use `body<NotificationDTO?>()`.
        try {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            return client.get("$baseUrl/notifications/in-app?date=$today").body()
        } catch (e: Exception) {
            return null
        }
    }
}

@Serializable
data class NotificationDTO(
    val id: String,
    val category: String,
    val type: String,
    val message: String,
    val sentiment: String? = null
)

@Serializable
data class UserStreakData(
    val userId: String,
    val presenceStreak: Int,
    val presenceLastActiveAt: String?, // Date string
    val kindnessStreak: Int,
    val kindnessLastActiveAt: String?,
    val responseStreak: Int,
    val responseLastActiveAt: String?,
    val gracePeriodUsedAt: String? = null,
    val updatedAt: String
)
