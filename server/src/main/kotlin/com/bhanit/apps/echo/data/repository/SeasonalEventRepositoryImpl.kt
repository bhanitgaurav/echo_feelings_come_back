package com.bhanit.apps.echo.data.repository

import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import com.bhanit.apps.echo.data.table.SeasonalEvents
import com.bhanit.apps.echo.domain.repository.SeasonalEventRepository
import com.bhanit.apps.echo.util.SeasonalEvent
import com.bhanit.apps.echo.util.SeasonalRule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.time.Instant

class SeasonalEventRepositoryImpl : SeasonalEventRepository {
    
    // Explicit Json config to be safe
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun createEvent(event: SeasonalEvent, year: Int): String = dbQuery {
        SeasonalEvents.insert {
            it[id] = event.id
            it[SeasonalEvents.year] = year
            it[name] = event.name
            it[startDate] = LocalDate.of(year, event.startMonth, event.startDay)
            it[endDate] = LocalDate.of(year, event.endMonth, event.endDay)
            it[rulesJson] = json.encodeToString(event.rules)
            it[colorHex] = event.colorHex
            it[isActive] = true
            it[createdAt] = Instant.now()
        }
        event.id
    }

    override suspend fun updateEvent(id: String, event: SeasonalEvent, year: Int) = dbQuery {
        SeasonalEvents.update({ SeasonalEvents.id eq id }) {
            it[SeasonalEvents.year] = year
            it[name] = event.name
            it[startDate] = LocalDate.of(year, event.startMonth, event.startDay)
            it[endDate] = LocalDate.of(year, event.endMonth, event.endDay)
            it[rulesJson] = json.encodeToString(event.rules)
            it[colorHex] = event.colorHex
        }
        Unit
    }

    override suspend fun deleteEvent(id: String) = dbQuery {
        SeasonalEvents.deleteWhere { SeasonalEvents.id eq id }
        Unit
    }

    override suspend fun getEvent(id: String): SeasonalEvent? = dbQuery {
        SeasonalEvents.selectAll().where { SeasonalEvents.id eq id }
            .map { rowToSeasonalEvent(it) }
            .singleOrNull()
    }

    override suspend fun getEventsByYear(year: Int): List<SeasonalEvent> = dbQuery {
        SeasonalEvents.selectAll().where { SeasonalEvents.year eq year }
            .map { rowToSeasonalEvent(it) }
    }

    override suspend fun getActiveEvents(date: LocalDate): List<SeasonalEvent> = dbQuery {
        // Find events where (start <= date <= end) AND isActive = true
        // Note: Exposed doesn't support generic date comparison easily across DBs sometimes, 
        // but for basic LocalDate it should work if column is `date`.
        // However, year must also match or we assume the `startDate` and `endDate` columns hold the correct year.
        // Since we store explicit LocalDate in DB which HAS year, simple date comparison works.
        
        SeasonalEvents.selectAll()
            .where { (SeasonalEvents.startDate lessEq date) and (SeasonalEvents.endDate greaterEq date) and (SeasonalEvents.isActive eq true) }
            .map { rowToSeasonalEvent(it) }
    }

    override suspend fun hasOverlappingEvent(year: Int, start: LocalDate, end: LocalDate): Boolean = dbQuery {
         SeasonalEvents.selectAll().where {
             (SeasonalEvents.year eq year) and
             (SeasonalEvents.isActive eq true) and
             (SeasonalEvents.startDate lessEq end) and
             (SeasonalEvents.endDate greaterEq start)
         }.count() > 0
    }

    private fun rowToSeasonalEvent(row: ResultRow): SeasonalEvent {
        val rules = try {
            json.decodeFromString<List<SeasonalRule>>(row[SeasonalEvents.rulesJson])
        } catch (e: Exception) {
            emptyList()
        }
        
        val start = row[SeasonalEvents.startDate]
        val end = row[SeasonalEvents.endDate]
        
        return SeasonalEvent(
            id = row[SeasonalEvents.id],
            name = row[SeasonalEvents.name],
            colorHex = row[SeasonalEvents.colorHex],
            startMonth = start.month,
            startDay = start.dayOfMonth,
            endMonth = end.month,
            endDay = end.dayOfMonth,
            rules = rules,
            year = row[SeasonalEvents.year],
            isActive = row[SeasonalEvents.isActive]
        )
    }
}
