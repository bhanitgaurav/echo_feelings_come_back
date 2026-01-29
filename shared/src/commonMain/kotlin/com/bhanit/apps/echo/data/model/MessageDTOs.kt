package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    val receiverId: String,
    val feeling: String, // Dynamic Emotion ID
    val content: String,
    val isAnonymous: Boolean = true
)

@Serializable
data class ReplyMessageRequest(
    val messageId: String,
    val content: String
)

@Serializable
data class MessageDTO(
    val id: String,
    val senderId: String,
    val senderUsername: String? = null,
    val receiverId: String,
    val receiverUsername: String? = null,
    val feeling: String, // Dynamic Emotion ID
    val content: String,
    val isAnonymous: Boolean,
    val timestamp: Long,
    val replyContent: String? = null,
    val replyTimestamp: Long? = null,
    val isRead: Boolean = false
)
