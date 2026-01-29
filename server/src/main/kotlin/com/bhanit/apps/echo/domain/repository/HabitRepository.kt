package com.bhanit.apps.echo.domain.repository

import com.bhanit.apps.echo.data.table.EmotionSentiment
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

interface HabitRepository {
    suspend fun getStreaks(userId: UUID): UserStreakData?
    suspend fun createStreaks(userId: UUID)
    suspend fun updatePresenceStreak(userId: UUID, streak: Int, cycle: Int, lastActive: LocalDate)
    suspend fun updateKindnessStreak(userId: UUID, streak: Int, cycle: Int, lastActive: LocalDate)
    suspend fun updateResponseStreak(userId: UUID, streak: Int, cycle: Int, lastActive: LocalDate)
    suspend fun useGracePeriod(userId: UUID, usedAt: LocalDateTime)
    
    suspend fun getEmotionSentiment(emotionId: String): EmotionSentiment?
    suspend fun getUsersForStreakReminder(): List<StreakReminderTarget>
}

data class StreakReminderTarget(
    val userId: UUID,
    val fcmToken: String
)

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString())
}

object InstantSerializer : KSerializer<java.time.Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: java.time.Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): java.time.Instant = java.time.Instant.parse(decoder.decodeString())
}

@Serializable
data class UserStreakData(
    @Serializable(with = UUIDSerializer::class)
    val userId: UUID,
    val presenceStreak: Int,
    val presenceCycle: Int = 1,
    @Serializable(with = LocalDateSerializer::class)
    val presenceLastActiveAt: LocalDate?,
    val kindnessStreak: Int,
    val kindnessCycle: Int = 1,
    @Serializable(with = LocalDateSerializer::class)
    val kindnessLastActiveAt: LocalDate?,
    val responseStreak: Int,
    val responseCycle: Int = 1,
    @Serializable(with = LocalDateSerializer::class)
    val responseLastActiveAt: LocalDate?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val gracePeriodUsedAt: LocalDateTime?,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: java.time.Instant
)
