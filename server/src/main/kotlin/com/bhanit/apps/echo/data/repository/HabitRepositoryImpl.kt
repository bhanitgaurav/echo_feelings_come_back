package com.bhanit.apps.echo.data.repository

import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import com.bhanit.apps.echo.data.table.EmotionSentiment
import com.bhanit.apps.echo.data.table.Emotions
import com.bhanit.apps.echo.data.table.UserStreaks
import com.bhanit.apps.echo.domain.repository.HabitRepository
import com.bhanit.apps.echo.domain.repository.UserStreakData
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.and
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.time.ZoneId

class HabitRepositoryImpl : HabitRepository {
    override suspend fun getStreaks(userId: UUID): UserStreakData? = dbQuery {
        UserStreaks.selectAll().where { UserStreaks.userId eq userId }
            .map {
                val gracePeriodInstant = it[UserStreaks.gracePeriodUsedAt]
                val gracePeriodLocalDateTime = gracePeriodInstant?.let { instant ->
                   LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))
                }
                
                UserStreakData(
                    userId = it[UserStreaks.userId].value,
                    presenceStreak = it[UserStreaks.presenceStreak],
                    presenceCycle = it[UserStreaks.presenceCycle],
                    presenceLastActiveAt = it[UserStreaks.presenceLastActiveAt],
                    kindnessStreak = it[UserStreaks.kindnessStreak],
                    kindnessCycle = it[UserStreaks.kindnessCycle],
                    kindnessLastActiveAt = it[UserStreaks.kindnessLastActiveAt],
                    responseStreak = it[UserStreaks.responseStreak],
                    responseCycle = it[UserStreaks.responseCycle],
                    responseLastActiveAt = it[UserStreaks.responseLastActiveAt],
                    gracePeriodUsedAt = gracePeriodLocalDateTime,
                    updatedAt = it[UserStreaks.updatedAt]
                )
            }.singleOrNull()
    }
    


    override suspend fun createStreaks(userId: UUID) = dbQuery {
        UserStreaks.insert {
            it[this.userId] = userId
            it[presenceStreak] = 0
            it[kindnessStreak] = 0
            it[responseStreak] = 0
            
            it[presenceCycle] = 1
            it[kindnessCycle] = 1
            it[responseCycle] = 1
            
            it[updatedAt] = java.time.Instant.now()
        }
        Unit
    }

    override suspend fun updatePresenceStreak(userId: UUID, streak: Int, cycle: Int, lastActive: LocalDate) = dbQuery {
        UserStreaks.update({ UserStreaks.userId eq userId }) {
            it[presenceStreak] = streak
            it[presenceCycle] = cycle
            it[presenceLastActiveAt] = lastActive
            it[updatedAt] = java.time.Instant.now()
        }
        Unit
    }

    override suspend fun updateKindnessStreak(userId: UUID, streak: Int, cycle: Int, lastActive: LocalDate) = dbQuery {
        UserStreaks.update({ UserStreaks.userId eq userId }) {
            it[kindnessStreak] = streak
            it[kindnessCycle] = cycle
            it[kindnessLastActiveAt] = lastActive
            it[updatedAt] = java.time.Instant.now()
        }
        Unit
    }

    override suspend fun updateResponseStreak(userId: UUID, streak: Int, cycle: Int, lastActive: LocalDate) = dbQuery {
        UserStreaks.update({ UserStreaks.userId eq userId }) {
            it[responseStreak] = streak
            it[responseCycle] = cycle
            it[responseLastActiveAt] = lastActive
            it[updatedAt] = java.time.Instant.now()
        }
        Unit
    }

    override suspend fun useGracePeriod(userId: UUID, usedAt: LocalDateTime) = dbQuery {
        val instant = usedAt.toInstant(java.time.ZoneOffset.UTC)
        UserStreaks.update({ UserStreaks.userId eq userId }) {
            it[gracePeriodUsedAt] = instant
            it[updatedAt] = java.time.Instant.now()
        }
        Unit
    }

    override suspend fun getEmotionSentiment(emotionId: String): EmotionSentiment? = dbQuery {
        Emotions.selectAll().where { Emotions.id eq emotionId }
            .map { it[Emotions.sentiment] }
            .singleOrNull()
    }

    override suspend fun getUsersForStreakReminder(): List<com.bhanit.apps.echo.domain.repository.StreakReminderTarget> = dbQuery {
        val today = LocalDate.now()
        // Streak >= 2 AND LastActive < Today (Server Time)
        // Join with Users to get FCM Token
        (UserStreaks innerJoin com.bhanit.apps.echo.data.table.Users)
            .select(UserStreaks.userId, com.bhanit.apps.echo.data.table.Users.fcmToken)
            .where {
                (UserStreaks.presenceStreak greaterEq 2) and
                (UserStreaks.presenceLastActiveAt less today) and
                (com.bhanit.apps.echo.data.table.Users.fcmToken.isNotNull())
            }
            .map {
                com.bhanit.apps.echo.domain.repository.StreakReminderTarget(
                    userId = it[UserStreaks.userId].value,
                    fcmToken = it[com.bhanit.apps.echo.data.table.Users.fcmToken]!!
                )
            }
    }
}
