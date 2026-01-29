package com.bhanit.apps.echo.domain.repository

import com.bhanit.apps.echo.data.table.ContentType

data class AppContent(
    val type: ContentType,
    val content: String,
    val updatedAt: java.time.Instant
)

interface ContentRepository {
    suspend fun getContent(type: ContentType): AppContent?
    suspend fun updateContent(type: ContentType, content: String)
}
