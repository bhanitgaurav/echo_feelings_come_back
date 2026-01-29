package com.bhanit.apps.echo.data.repository

import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import com.bhanit.apps.echo.data.table.DailyStats
import com.bhanit.apps.echo.domain.repository.DailyStatsRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.Sum
import org.jetbrains.exposed.sql.Count
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.IntegerColumnType
import java.time.LocalDate
import java.util.UUID

class DailyStatsRepositoryImpl : DailyStatsRepository {
    override suspend fun incrementSentCount(userId: UUID, date: LocalDate) = dbQuery {
        try {
            val exists = DailyStats.selectAll()
                .where { (DailyStats.userId eq userId) and (DailyStats.date eq date) }.any()
            if (exists) {
                DailyStats.update({ (DailyStats.userId eq userId) and (DailyStats.date eq date) }) {
                    with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                        it.update(sentCount, sentCount + 1)
                    }
                }
            } else {
                DailyStats.insert {
                    it[this.userId] = userId
                    it[this.date] = date
                    it[sentCount] = 1
                    it[receivedCount] = 0
                }
            }
        } catch (e: Exception) {
            println("ERROR: DailyStatsRepository.incrementSentCount failed: ${e.message}")
            // Don't rethrow to avoid blocking message send
        }
        Unit
    }

    override suspend fun incrementReceivedCount(userId: UUID, date: LocalDate) = dbQuery {
        val exists = DailyStats.selectAll().where { (DailyStats.userId eq userId) and (DailyStats.date eq date) }.any()
        if (exists) {
            DailyStats.update({ (DailyStats.userId eq userId) and (DailyStats.date eq date) }) {
                 with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                    it.update(receivedCount, receivedCount + 1)
                }
            }
        } else {
            DailyStats.insert {
                it[this.userId] = userId
                it[this.date] = date
                it[sentCount] = 0
                it[receivedCount] = 1
            }
        }
        Unit
    }

    override suspend fun getStats(userId: UUID, date: LocalDate): ResultRow? = dbQuery {
        DailyStats.selectAll().where { (DailyStats.userId eq userId) and (DailyStats.date eq date) }.singleOrNull()
    }

    override suspend fun getDailyStats(userId: UUID, date: LocalDate): com.bhanit.apps.echo.domain.repository.DailyStatsSimple? = dbQuery {
        val statsRow = DailyStats.selectAll()
            .where { (DailyStats.userId eq userId) and (DailyStats.date eq date) }
            .singleOrNull() ?: return@dbQuery null

        // Fetch received sentiments
        val startOfDay = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()

        val sentiments = (com.bhanit.apps.echo.data.table.Messages innerJoin com.bhanit.apps.echo.data.table.Emotions)
            .select(com.bhanit.apps.echo.data.table.Emotions.sentiment)
            .where {
                (com.bhanit.apps.echo.data.table.Messages.receiverId eq userId) and
                (com.bhanit.apps.echo.data.table.Messages.createdAt greaterEq startOfDay) and
                (com.bhanit.apps.echo.data.table.Messages.createdAt less endOfDay)
            }
            .map { it[com.bhanit.apps.echo.data.table.Emotions.sentiment].name }
            .toSet()

        // Fetch sent sentiments
        val sentSentiments = (com.bhanit.apps.echo.data.table.Messages innerJoin com.bhanit.apps.echo.data.table.Emotions)
            .select(com.bhanit.apps.echo.data.table.Emotions.sentiment)
            .where {
                (com.bhanit.apps.echo.data.table.Messages.senderId eq userId) and
                (com.bhanit.apps.echo.data.table.Messages.createdAt greaterEq startOfDay) and
                (com.bhanit.apps.echo.data.table.Messages.createdAt less endOfDay)
            }
            .map { it[com.bhanit.apps.echo.data.table.Emotions.sentiment].name }
            .toSet()

        com.bhanit.apps.echo.domain.repository.DailyStatsSimple(
            sentCount = statsRow[DailyStats.sentCount],
            receivedCount = statsRow[DailyStats.receivedCount],
            receivedSentiments = sentiments,
            sentSentiments = sentSentiments
        )
    }



    override suspend fun getAnalytics(userId: UUID, range: com.bhanit.apps.echo.data.model.TimeRange, clientDate: LocalDate): com.bhanit.apps.echo.data.model.AnalyticsDTO = dbQuery {
        val now = clientDate
        val startDate = when (range) {
            com.bhanit.apps.echo.data.model.TimeRange.TODAY -> now.minusDays(1)
            com.bhanit.apps.echo.data.model.TimeRange.WEEK -> now.minusWeeks(1)
            com.bhanit.apps.echo.data.model.TimeRange.MONTH -> now.minusMonths(1)
            com.bhanit.apps.echo.data.model.TimeRange.THREE_MONTHS -> now.minusMonths(3)
            com.bhanit.apps.echo.data.model.TimeRange.SIX_MONTHS -> now.minusMonths(6)
            com.bhanit.apps.echo.data.model.TimeRange.YEAR -> now.minusYears(1)
            com.bhanit.apps.echo.data.model.TimeRange.FIVE_YEARS -> now.minusYears(5)
            com.bhanit.apps.echo.data.model.TimeRange.ALL -> LocalDate.MIN
        }
        
        val startInstant = if (range == com.bhanit.apps.echo.data.model.TimeRange.ALL) java.time.Instant.EPOCH else startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()

        // Fetch Blocked User IDs
        val blockedByMe = com.bhanit.apps.echo.data.table.UserBlocks
            .select(com.bhanit.apps.echo.data.table.UserBlocks.blockedId)
            .where { com.bhanit.apps.echo.data.table.UserBlocks.blockerId eq userId }
            .map { it[com.bhanit.apps.echo.data.table.UserBlocks.blockedId].value }
            
        val blockedMe = com.bhanit.apps.echo.data.table.UserBlocks
            .select(com.bhanit.apps.echo.data.table.UserBlocks.blockerId)
            .where { com.bhanit.apps.echo.data.table.UserBlocks.blockedId eq userId }
            .map { it[com.bhanit.apps.echo.data.table.UserBlocks.blockerId].value }

        val blockedIds = (blockedByMe + blockedMe).distinct()

        // Common Condition
        var condition: org.jetbrains.exposed.sql.Op<Boolean> = (
                (com.bhanit.apps.echo.data.table.Messages.senderId eq userId) or 
                (com.bhanit.apps.echo.data.table.Messages.receiverId eq userId)
            ) and (com.bhanit.apps.echo.data.table.Messages.createdAt greaterEq startInstant)

        if (blockedIds.isNotEmpty()) {
            condition = condition and (com.bhanit.apps.echo.data.table.Messages.senderId notInList blockedIds)
            condition = condition and (com.bhanit.apps.echo.data.table.Messages.receiverId notInList blockedIds)
        }

        // Result containers
        var totalSent = 0
        var totalReceived = 0
        val emotions = mutableMapOf<String, Int>()
        val sentEmotions = mutableMapOf<String, Int>()
        val receivedEmotions = mutableMapOf<String, Int>()
        val activityMap = mutableMapOf<String, Triple<Int, Int, MutableMap<String, Int>>>()

        // Optimize Select: Only fetch needed columns
        val messages = com.bhanit.apps.echo.data.table.Messages
            .select(com.bhanit.apps.echo.data.table.Messages.senderId, com.bhanit.apps.echo.data.table.Messages.feelingId, com.bhanit.apps.echo.data.table.Messages.createdAt)
            .where { condition }
            .toList()

        messages.forEach { row ->
            val isSender = row[com.bhanit.apps.echo.data.table.Messages.senderId].value == userId
            val zonedDateTime = row[com.bhanit.apps.echo.data.table.Messages.createdAt].atZone(java.time.ZoneId.systemDefault())
            val feeling = row[com.bhanit.apps.echo.data.table.Messages.feelingId]

            val key = if (range == com.bhanit.apps.echo.data.model.TimeRange.TODAY) {
                 zonedDateTime.toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.HOURS).toString()
            } else {
                 zonedDateTime.toLocalDate().toString()
            }
            
            val currentActivity = activityMap.getOrPut(key) { Triple(0, 0, mutableMapOf()) }

            if (isSender) {
                totalSent++
                activityMap[key] = Triple(currentActivity.first + 1, currentActivity.second, currentActivity.third)
                sentEmotions[feeling] = (sentEmotions[feeling] ?: 0) + 1
            } else {
                totalReceived++
                activityMap[key] = Triple(currentActivity.first, currentActivity.second + 1, currentActivity.third)
                receivedEmotions[feeling] = (receivedEmotions[feeling] ?: 0) + 1
            }
            
            emotions[feeling] = (emotions[feeling] ?: 0) + 1
            activityMap[key]!!.third[feeling] = (activityMap[key]!!.third[feeling] ?: 0) + 1
        }

        // Fill gaps logic
        val filledActivities = mutableListOf<com.bhanit.apps.echo.data.model.DailyActivity>()
        
        if (range == com.bhanit.apps.echo.data.model.TimeRange.TODAY) {
            val end = java.time.LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.HOURS)
            val start = end.minusHours(24)
            var current = start
            while (!current.isAfter(end)) {
                val key = current.toString()
                val data = activityMap[key] ?: Triple(0, 0, mutableMapOf())
                filledActivities.add(
                    com.bhanit.apps.echo.data.model.DailyActivity(
                        date = key,
                        sent = data.first,
                        received = data.second,
                        emotions = data.third
                    )
                )
                current = current.plusHours(1)
            }
        } else {
            val end = LocalDate.now()
            // Ensure reasonable start for ALL
            val effectiveStart = if (range == com.bhanit.apps.echo.data.model.TimeRange.ALL) {
                 // Try to pick first data point or default
                 activityMap.keys.minOrNull()?.let { LocalDate.parse(it) } ?: startDate
            } else startDate

            var current = effectiveStart
            while (!current.isAfter(end)) {
                val key = current.toString()
                val data = activityMap[key] ?: Triple(0, 0, mutableMapOf())
                filledActivities.add(
                    com.bhanit.apps.echo.data.model.DailyActivity(
                        date = key,
                        sent = data.first,
                        received = data.second,
                        emotions = data.third
                    )
                )
                current = current.plusDays(1)
            }
        }

        com.bhanit.apps.echo.data.model.AnalyticsDTO(
            totalSent = totalSent,
            totalReceived = totalReceived,
            currentStreak = 0, // Placeholder
            emotionBreakdown = emotions,
            sentEmotionBreakdown = sentEmotions,
            receivedEmotionBreakdown = receivedEmotions,
            dailyActivity = filledActivities
        )
    }

    override suspend fun getGlobalAnalytics(range: com.bhanit.apps.echo.data.model.TimeRange, clientDate: LocalDate): com.bhanit.apps.echo.data.model.AnalyticsDTO = dbQuery {
        val now = clientDate
        val startDate = when (range) {
            com.bhanit.apps.echo.data.model.TimeRange.TODAY -> now.minusDays(1)
            com.bhanit.apps.echo.data.model.TimeRange.WEEK -> now.minusWeeks(1)
            com.bhanit.apps.echo.data.model.TimeRange.MONTH -> now.minusMonths(1)
            com.bhanit.apps.echo.data.model.TimeRange.THREE_MONTHS -> now.minusMonths(3)
            com.bhanit.apps.echo.data.model.TimeRange.SIX_MONTHS -> now.minusMonths(6)
            com.bhanit.apps.echo.data.model.TimeRange.YEAR -> now.minusYears(1)
            com.bhanit.apps.echo.data.model.TimeRange.FIVE_YEARS -> now.minusYears(5)
            // Use logical default for SQL query optimization
            com.bhanit.apps.echo.data.model.TimeRange.ALL -> LocalDate.of(2023, 1, 1)
        }
        
        // 1. Activity Trends & Total Counts from DailyStats (Fast Aggregation)
        // Group by Date to get daily totals across all users
        val dailySum = Sum(DailyStats.sentCount, IntegerColumnType())
        val activityQuery = DailyStats
            .select(DailyStats.date, dailySum)
            .where { DailyStats.date greaterEq startDate }
            .groupBy(DailyStats.date)
            .orderBy(DailyStats.date to SortOrder.ASC)

        val dailyActivitiesMap = mutableMapOf<LocalDate, Int>()
        var totalSent = 0

        activityQuery.forEach { row ->
            val date = row[DailyStats.date]
            val count = row[dailySum] ?: 0
            dailyActivitiesMap[date] = count
            totalSent += count
        }

        // 2. Emotion Breakdown from Messages (Group By Aggregation)
        val startInstant = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
        val feelingIdCol = com.bhanit.apps.echo.data.table.Messages.feelingId
        val feelingCount = Count(feelingIdCol)
        
        val emotionCounts = com.bhanit.apps.echo.data.table.Messages
            .select(feelingIdCol, feelingCount)
            .where { com.bhanit.apps.echo.data.table.Messages.createdAt greaterEq startInstant }
            .groupBy(feelingIdCol)
            .associate { row ->
                row[feelingIdCol] to row[feelingCount].toInt()
            }

        // 3. Fill Gaps for Graph
        val filledActivities = mutableListOf<com.bhanit.apps.echo.data.model.DailyActivity>()
        val end = LocalDate.now()
        // Ensure we start from the actual data start if ALL, else requested start
        var current = if (range == com.bhanit.apps.echo.data.model.TimeRange.ALL) {
             dailyActivitiesMap.keys.minOrNull() ?: startDate
        } else {
             startDate
        }
        
        while (!current.isAfter(end)) {
            val count = dailyActivitiesMap[current] ?: 0
            filledActivities.add(
                com.bhanit.apps.echo.data.model.DailyActivity(
                    date = current.toString(),
                    sent = count,
                    received = 0, // Global received ~ sent usually
                    emotions = emptyMap() // Detailed emotion/day breakdown is heavy to compute globally, skipping for overview
                )
            )
            current = current.plusDays(1)
        }
        
        com.bhanit.apps.echo.data.model.AnalyticsDTO(
            totalSent = totalSent,
            totalReceived = totalSent,
            currentStreak = 0,
            emotionBreakdown = emotionCounts,
            sentEmotionBreakdown = emotionCounts,
            receivedEmotionBreakdown = emotionCounts,
            dailyActivity = filledActivities
        )
    }

}
