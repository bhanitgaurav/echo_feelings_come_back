package com.bhanit.apps.echo.features.messaging.domain

data class Message(
    val id: String,
    val senderId: String,
    val senderUsername: String? = null,
    val receiverId: String,
    val receiverUsername: String? = null,
    val emotion: Emotion,
    val content: String,
    val timestamp: Long,
    val isAnonymous: Boolean = true,
    val replyContent: String? = null,
    val replyTimestamp: Long? = null,
    val isRead: Boolean = false
)
