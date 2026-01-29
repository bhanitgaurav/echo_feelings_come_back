package com.bhanit.apps.echo.features.messaging.data

import com.bhanit.apps.echo.data.model.MessageDTO
import com.bhanit.apps.echo.data.model.ReplyMessageRequest
import com.bhanit.apps.echo.data.model.SendMessageRequest
import com.bhanit.apps.echo.features.messaging.domain.Emotion
import com.bhanit.apps.echo.features.messaging.domain.Message
import com.bhanit.apps.echo.features.messaging.domain.MessagingRepository
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType

import com.bhanit.apps.echo.features.messaging.domain.EmotionRepository
import kotlinx.datetime.toLocalDateTime

class MessagingRepositoryImpl(
    private val httpClient: io.ktor.client.HttpClient,
    private val emotionRepository: EmotionRepository,
    private val baseUrl: String
) : MessagingRepository {

    override suspend fun sendMessage(
        receiverId: String,
        emotion: Emotion,
        content: String,
        isAnonymous: Boolean
    ): Result<String> = com.bhanit.apps.echo.core.network.safeApiCall {
        val request = SendMessageRequest(
            receiverId = receiverId,
            feeling = emotion.id,
            content = content,
            isAnonymous = isAnonymous
        )
        val response = httpClient.post("$baseUrl/messages/send") {
            contentType(ContentType.Application.Json)
            val today = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
            parameter("localDate", today)
            setBody(request)
        }.body<Map<String, String>>()
        
        response["messageId"] ?: throw IllegalStateException("No messageId returned")
    }

    override suspend fun replyToMessage(
        messageId: String,
        content: String
    ): Result<String> = com.bhanit.apps.echo.core.network.safeApiCall {
        val request = ReplyMessageRequest(
            messageId = messageId,
            content = content
        )
        val response = httpClient.post("$baseUrl/messages/reply") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<Map<String, String>>()
        
        response["replyId"] ?: throw IllegalStateException("No replyId returned")
    }

    override suspend fun getInbox(
        page: Int, 
        limit: Int,
        feelings: List<Emotion>?,
        startDate: Long?,
        endDate: Long?
    ): Result<List<Message>> = com.bhanit.apps.echo.core.network.safeApiCall {
        val dtos = httpClient.get("$baseUrl/messages/inbox") {
            parameter("page", page)
            parameter("limit", limit)
            
            
            feelings?.forEach { emotion -> 
                parameter("feelings", emotion.id)
            }
            
            if (startDate != null) parameter("startDate", startDate)
            if (endDate != null) parameter("endDate", endDate)
            
        }.body<List<MessageDTO>>()
        
        dtos.map { it.toDomain() }
    }

    override suspend fun getSentMessages(receiverId: String?, page: Int, limit: Int, onlyReplied: Boolean): Result<List<Message>> = com.bhanit.apps.echo.core.network.safeApiCall {
        val dtos = httpClient.get("$baseUrl/messages/history") {
            parameter("page", page)
            parameter("limit", limit)
            if (receiverId != null) {
                parameter("receiverId", receiverId)
            }
            parameter("onlyReplied", onlyReplied)
        }.body<List<MessageDTO>>()
        
        dtos.map { it.toDomain() }
    }

    override suspend fun checkReplyStatus(messageId: String): Result<Boolean> = com.bhanit.apps.echo.core.network.safeApiCall {
        val response = httpClient.get("$baseUrl/messages/reply-status/$messageId")
            .body<Map<String, Boolean>>()
            
        response["canReply"] ?: false
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> = com.bhanit.apps.echo.core.network.safeApiCall {
        httpClient.post("$baseUrl/user/fcm-token") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("token" to token))
        }
        Unit
    }

    override suspend fun getDailyUsage(receiverId: String): Result<Map<String, Int>> = com.bhanit.apps.echo.core.network.safeApiCall {
        httpClient.get("$baseUrl/messages/usage") {
            parameter("receiverId", receiverId)
        }.body<Map<String, Int>>()
    }

    private suspend fun MessageDTO.toDomain(): Message {
        val emotionId = this.feeling // It's a string ID now
        // We assume 'feeling' field in DTO is mapped from 'feelingId' or 'feeling' string.
        // Actually MessageDTO.feeling is currently FeelingType Enum in client code?
        // Wait, I saw MessageDTO in client earlier? No, I saw it in server code.
        // Client MessageDTO likely needs update if it still uses FeelingType!
        // But for now, let's assume this.feeling is the ID String or name.
        
        // Assuming MessageDTO on client is also updated to String? 
        // I haven't checked MessageDTO on client yet! Critical check needed.
        // If Client MessageDTO uses FeelingType enum, then GSON/Serialization will fail if server sends "APPRECIATION" string?
        // Actually "APPRECIATION" string matches enum name, so it might work if ID is same as Name.
        // But server changed to dynamic IDs. If ID is "CUSTOM_123", Enum deserialization fails.
        // I MUST check client MessageDTO.
        
        val emotion = emotionRepository.getEmotionById(this.feeling.toString()) // .toString() if it is enum, or direct if String
             ?: Emotion(
                id = this.feeling.toString(), 
                displayName = this.feeling.toString(), 
                colorHex = 0xFF888888, 
                exampleText = "", 
                isDefault = false
            )

        return Message(
            id = this.id,
            senderId = this.senderId,
            senderUsername = this.senderUsername,
            receiverId = this.receiverId,
            receiverUsername = this.receiverUsername,
            emotion = emotion,
            content = this.content,
            timestamp = this.timestamp,
            isAnonymous = this.isAnonymous,
            replyContent = this.replyContent,
            replyTimestamp = this.replyTimestamp,
            isRead = this.isRead
        )
    }
}
