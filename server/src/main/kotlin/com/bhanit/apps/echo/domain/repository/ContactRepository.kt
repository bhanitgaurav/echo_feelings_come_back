package com.bhanit.apps.echo.domain.repository

import java.util.UUID

interface ContactRepository {
    suspend fun upsertContacts(userId: UUID, contacts: List<Pair<String, String?>>)
    suspend fun getAvailableContacts(userId: UUID, limit: Int, offset: Int): List<AvailableContactRow>
}

data class AvailableContactRow(
    val id: java.util.UUID,
    val username: String?,
    val phoneNumber: String,
    val avatarUrl: String?,
    val status: String?, // "PENDING", "CONNECTED", "BLOCKED" or null
    val initiatedBy: java.util.UUID? = null
)
