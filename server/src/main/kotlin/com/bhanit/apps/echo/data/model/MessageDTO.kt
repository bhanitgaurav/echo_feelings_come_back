package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageDTO(
    val id: String,
    val senderId: String,
    val senderUsername: String? = null,
    val receiverId: String,
    val receiverUsername: String? = null,
    val feeling: String, // Changed from FeelingType to String (Emotion ID)
    val content: String,
    val isAnonymous: Boolean,
    val timestamp: Long,
    val replyContent: String? = null,
    val replyTimestamp: Long? = null,
    val isRead: Boolean
)
