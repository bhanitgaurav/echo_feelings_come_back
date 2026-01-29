package com.bhanit.apps.echo.features.contact.domain

import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getConnections(): Flow<List<Connection>>
    suspend fun syncContacts(contacts: List<Contact>): List<Contact>
    suspend fun getConnections(page: Int, limit: Int, query: String? = null): List<Connection> // Pagination
    suspend fun getUnifiedContacts(): List<Contact>
    suspend fun getAvailableContacts(page: Int, limit: Int): List<Connection>
    suspend fun searchAvailableContacts(query: String, page: Int, limit: Int): List<Connection>
    suspend fun getFriendRequests(): List<Connection>
    suspend fun inviteUser(phoneNumber: String)
    suspend fun requestConnection(userId: String)
    suspend fun acceptConnection(userId: String)
    suspend fun denyConnection(userId: String)
    suspend fun getPendingRequests(): List<Connection>
    suspend fun getSentRequests(): List<Connection>
    suspend fun cancelRequest(userId: String)
    suspend fun removeConnection(userId: String)
    suspend fun connectByReferral(code: String): Result<com.bhanit.apps.echo.data.model.ReferralResponse>
    fun openSettings()
}
