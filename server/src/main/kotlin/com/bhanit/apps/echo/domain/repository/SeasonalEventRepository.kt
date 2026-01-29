package com.bhanit.apps.echo.domain.repository

import com.bhanit.apps.echo.util.SeasonalEvent
import java.time.LocalDate

interface SeasonalEventRepository {
    suspend fun createEvent(event: SeasonalEvent, year: Int): String
    suspend fun updateEvent(id: String, event: SeasonalEvent, year: Int)
    suspend fun deleteEvent(id: String)
    suspend fun getEvent(id: String): SeasonalEvent?
    suspend fun getEventsByYear(year: Int): List<SeasonalEvent>
    suspend fun getActiveEvents(date: LocalDate): List<SeasonalEvent>
    suspend fun hasOverlappingEvent(year: Int, start: LocalDate, end: LocalDate): Boolean
}
