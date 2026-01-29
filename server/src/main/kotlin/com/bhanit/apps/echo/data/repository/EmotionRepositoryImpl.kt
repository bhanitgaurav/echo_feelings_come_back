package com.bhanit.apps.echo.data.repository

import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import com.bhanit.apps.echo.data.model.Emotion
import com.bhanit.apps.echo.data.table.Emotions
import com.bhanit.apps.echo.data.table.UserUnlockedEmotions
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.util.UUID

interface EmotionRepository {
    suspend fun getAllEmotions(): List<Emotion>
    suspend fun getEmotionsForUser(userId: UUID): List<Emotion>
    suspend fun seedDefaultEmotions()
    suspend fun unlockEmotion(userId: UUID, emotionId: String)
    suspend fun recordEmotionUsage(userId: UUID, emotionId: String)
}

class EmotionRepositoryImpl : EmotionRepository {

    override suspend fun getAllEmotions(): List<Emotion> = dbQuery {
        Emotions.selectAll().map {
            Emotion(
                id = it[Emotions.id],
                displayName = it[Emotions.displayName],
                colorHex = it[Emotions.colorHex],
                exampleText = it[Emotions.exampleText],
                isDefault = it[Emotions.isDefault],
                isUnlocked = true, // All returned here exist
                price = it[Emotions.price],
                tier = it[Emotions.tier].name
            )
        }
    }

    override suspend fun getEmotionsForUser(userId: UUID): List<Emotion> = dbQuery {
        // 1. Fetch ALL default definitions
        val allEmotions = Emotions.selectAll().map {
            Emotion(
                id = it[Emotions.id],
                displayName = it[Emotions.displayName],
                colorHex = it[Emotions.colorHex],
                exampleText = it[Emotions.exampleText],
                isDefault = it[Emotions.isDefault],
                isUnlocked = it[Emotions.tier] == com.bhanit.apps.echo.data.table.EmotionTier.FREE, // Default unlocked if free
                price = it[Emotions.price],
                tier = it[Emotions.tier].name
            )
        }

        // 2. Fetch User's specific unlock records
        // Map: EmotionID -> UnlockRecord(usageLimit, usageCount, expiresAt)
        data class UnlockInfo(val limit: Int?, val count: Int, val expiresAt: java.time.Instant?)
        
        val userUnlocks = UserUnlockedEmotions
            .selectAll().where { UserUnlockedEmotions.userId eq userId }
            .associate { 
                it[UserUnlockedEmotions.emotionId] to 
                UnlockInfo(
                    it[UserUnlockedEmotions.usageLimit], 
                    it[UserUnlockedEmotions.usageCount],
                    it[UserUnlockedEmotions.expiresAt]
                )
            }

        // 3. Merge and Compute Status
        allEmotions.map { emotion ->
            if (emotion.tier == "FREE") {
                emotion.copy(isUnlocked = true)
            } else {
                val unlockInfo = userUnlocks[emotion.id]
                val isUnlocked = if (unlockInfo != null) {
                    val (limit, count, expiresAt) = unlockInfo
                    
                    // Check Expiry First
                    val active = if (expiresAt != null) {
                        expiresAt.isAfter(java.time.Instant.now())
                    } else {
                        true // No expiry means permanent (or count based)
                    }

                    if (!active) {
                        false
                    } else {
                         // Unlocked IF: No limit (Subscription) OR Count < Limit (Credits)
                        limit == null || count < limit
                    }
                } else {
                    false
                }
                    emotion.copy(
                        isUnlocked = isUnlocked,
                        expiresAt = if (isUnlocked && unlockInfo?.expiresAt != null) 
                                    unlockInfo.expiresAt.toEpochMilli() 
                                    else null
                    )
            }
        }
    }

    override suspend fun seedDefaultEmotions() = dbQuery {
        data class EmotionDef(
            val id: String, 
            val name: String, 
            val color: Long, 
            val example: String, 
            val price: Int, 
            val tier: com.bhanit.apps.echo.data.table.EmotionTier,
            val sentiment: com.bhanit.apps.echo.data.table.EmotionSentiment
        )

        val definitions = listOf(
            // FREE
            EmotionDef("APPRECIATION", "Appreciation", 0xFF9B7CF5, "I really appreciated something you did today.", 0, com.bhanit.apps.echo.data.table.EmotionTier.FREE, com.bhanit.apps.echo.data.table.EmotionSentiment.POSITIVE),
            EmotionDef("GRATITUDE", "Gratitude", 0xFF7F8FEF, "I felt grateful for you today.", 0, com.bhanit.apps.echo.data.table.EmotionTier.FREE, com.bhanit.apps.echo.data.table.EmotionSentiment.POSITIVE),
            EmotionDef("LOVE", "Love", 0xFFFF9ECF, "I felt love toward you today.", 0, com.bhanit.apps.echo.data.table.EmotionTier.FREE, com.bhanit.apps.echo.data.table.EmotionSentiment.POSITIVE),
            EmotionDef("PROUD", "Proud", 0xFF6EE7B7, "I felt proud of you today.", 0, com.bhanit.apps.echo.data.table.EmotionTier.FREE, com.bhanit.apps.echo.data.table.EmotionSentiment.POSITIVE),
            EmotionDef("COMFORT", "Comfort", 0xFF3ECACB, "You made me feel calm and comfortable.", 0, com.bhanit.apps.echo.data.table.EmotionTier.FREE, com.bhanit.apps.echo.data.table.EmotionSentiment.POSITIVE),
            
            // REFLECTIVE (formerly Neutral)
            EmotionDef("MISSING", "Missing", 0xFFA5B4FC, "I missed you today.", 0, com.bhanit.apps.echo.data.table.EmotionTier.FREE, com.bhanit.apps.echo.data.table.EmotionSentiment.REFLECTIVE),
            EmotionDef("DISTANT", "Distant", 0xFFCBD5E1, "I felt a bit distant recently.", 10, com.bhanit.apps.echo.data.table.EmotionTier.PREMIUM, com.bhanit.apps.echo.data.table.EmotionSentiment.REFLECTIVE),
            EmotionDef("UNNOTICED", "Unnoticed", 0xFFFDE68A, "I felt a little unnoticed.", 10, com.bhanit.apps.echo.data.table.EmotionTier.PREMIUM, com.bhanit.apps.echo.data.table.EmotionSentiment.REFLECTIVE),
            EmotionDef("CONFUSED", "Confused", 0xFF93C5FD, "I felt confused about how things were.", 10, com.bhanit.apps.echo.data.table.EmotionTier.PREMIUM, com.bhanit.apps.echo.data.table.EmotionSentiment.REFLECTIVE),

            // DIFFICULT
            EmotionDef("DISAPPOINTED", "Disappointed", 0xFFFDBA74, "I felt a bit disappointed.", 10, com.bhanit.apps.echo.data.table.EmotionTier.PREMIUM, com.bhanit.apps.echo.data.table.EmotionSentiment.DIFFICULT),
            EmotionDef("HURT", "Hurt", 0xFFFCA5A5, "I felt hurt today.", 15, com.bhanit.apps.echo.data.table.EmotionTier.PREMIUM_PLUS, com.bhanit.apps.echo.data.table.EmotionSentiment.DIFFICULT),
            EmotionDef("FRUSTRATED", "Frustrated", 0xFFFB7185, "I felt frustrated earlier.", 15, com.bhanit.apps.echo.data.table.EmotionTier.PREMIUM_PLUS, com.bhanit.apps.echo.data.table.EmotionSentiment.DIFFICULT),
            EmotionDef("JEALOUS", "Insecure", 0xFFFDE047, "I felt insecure today.", 15, com.bhanit.apps.echo.data.table.EmotionTier.PREMIUM_PLUS, com.bhanit.apps.echo.data.table.EmotionSentiment.DIFFICULT), // Renamed Jealous -> Insecure
            EmotionDef("ANGRY", "Angry", 0xFFEF4444, "I felt angry about something today.", 15, com.bhanit.apps.echo.data.table.EmotionTier.PREMIUM_PLUS, com.bhanit.apps.echo.data.table.EmotionSentiment.DIFFICULT),

            // HEAVY
            EmotionDef("RESENTFUL", "Resentful", 0xFF9A6B4F, "Something has been sitting with me for a while.", 15, com.bhanit.apps.echo.data.table.EmotionTier.PREMIUM_PLUS, com.bhanit.apps.echo.data.table.EmotionSentiment.HEAVY)
        )

        definitions.forEach { def ->
            val exists = Emotions.selectAll().where { Emotions.id eq def.id }.count() > 0
            
            if (exists) {
                Emotions.update({ Emotions.id eq def.id }) {
                    it[displayName] = def.name
                    it[colorHex] = def.color
                    it[exampleText] = def.example
                    it[price] = def.price
                    it[tier] = def.tier
                    it[sentiment] = def.sentiment
                }
            } else {
                Emotions.insert {
                    it[id] = def.id
                    it[displayName] = def.name
                    it[colorHex] = def.color
                    it[exampleText] = def.example
                    it[isDefault] = true
                    it[createdAt] = Instant.now()
                    it[price] = def.price
                    it[tier] = def.tier
                    it[sentiment] = def.sentiment
                }
            }
        }
    }

    override suspend fun unlockEmotion(userId: UUID, emotionId: String) = dbQuery {
         UserUnlockedEmotions.insertIgnore {
            it[UserUnlockedEmotions.userId] = userId
            it[UserUnlockedEmotions.emotionId] = emotionId
            it[UserUnlockedEmotions.unlockedAt] = Instant.now()
        }
        Unit
    }
    override suspend fun recordEmotionUsage(userId: UUID, emotionId: String): Unit = dbQuery {
        UserUnlockedEmotions.update({ (UserUnlockedEmotions.userId eq userId) and (UserUnlockedEmotions.emotionId eq emotionId) }) {
            with(SqlExpressionBuilder) {
                it.update(usageCount, usageCount + 1)
            }
        }
        Unit
    }
}
