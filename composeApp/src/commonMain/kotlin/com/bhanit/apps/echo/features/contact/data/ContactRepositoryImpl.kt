package com.bhanit.apps.echo.features.contact.data

import com.bhanit.apps.echo.features.contact.domain.Connection
import com.bhanit.apps.echo.features.contact.domain.ConnectionStatus
import com.bhanit.apps.echo.features.contact.domain.ContactRepository
import com.bhanit.apps.echo.features.contact.domain.Contact
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.parameter
import io.ktor.http.contentType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class ContactRepositoryImpl(
    private val httpClient: io.ktor.client.HttpClient,
    private val contactManager: com.bhanit.apps.echo.features.contact.manager.ContactManager,
    private val sessionRepository: com.bhanit.apps.echo.features.auth.domain.SessionRepository,
    private val shareManager: com.bhanit.apps.echo.core.util.ShareManager,
    private val baseUrl: String
) : ContactRepository {

    override fun getConnections(): Flow<List<Connection>> = flow {
         // Deprecated or can emit initial load
         emit(getConnections(1, 20, null))
    }

    override suspend fun syncContacts(contacts: List<Contact>): List<Contact> {
        // Generate multiple hashes for each contact to improve matching
        val contactHashesMap = contacts.associate { contact: Contact ->
            val normalized = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(contact.phoneNumber)
            val raw = if (normalized.startsWith("+91")) normalized.removePrefix("+91") else normalized
            val last10 = if (normalized.length > 10) normalized.takeLast(10) else normalized
            
            val hashes = mutableSetOf<String>()
            hashes.add(com.bhanit.apps.echo.core.util.HashUtils.sha256(normalized))
            hashes.add(com.bhanit.apps.echo.core.util.HashUtils.sha256(raw))
            hashes.add(com.bhanit.apps.echo.core.util.HashUtils.sha256(last10))
            
            contact.phoneNumber to hashes
        }

        val allHashes = contactHashesMap.values.flatten().distinct()

        val response: com.bhanit.apps.echo.data.model.SyncContactsResponse = com.bhanit.apps.echo.core.network.safeApiCall {
            httpClient.post("$baseUrl/contacts/sync") {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(com.bhanit.apps.echo.data.model.SyncContactsRequest(allHashes))
            }.body<com.bhanit.apps.echo.data.model.SyncContactsResponse>()
        }.getOrThrow()
        
        val registeredMap = response.registeredUsers.associateBy { it.phoneHash }
        
        return contacts.map { contact ->
            // Check if ANY of the contact's hashes match a registered user
            val contactHashes = contactHashesMap[contact.phoneNumber] ?: emptySet()
            val match = contactHashes.firstNotNullOfOrNull { hash -> registeredMap[hash] }
            
            if (match != null) {
                contact.copy(isRegistered = true, userId = match.id)
            } else {
                contact
            }
        }
    }

    override suspend fun getConnections(page: Int, limit: Int, query: String?): List<Connection> {
        val response: List<com.bhanit.apps.echo.data.model.ConnectionResponse> = com.bhanit.apps.echo.core.network.safeApiCall {
            httpClient.get("$baseUrl/contacts/connections") {
                parameter("page", page)
                parameter("limit", limit)
                if (!query.isNullOrEmpty()) parameter("query", query)
            }.body<List<com.bhanit.apps.echo.data.model.ConnectionResponse>>()
        }.getOrThrow()

        val connections = response.map { dto ->
             Connection(
                 userId = dto.userId,
                 username = dto.username ?: "Unknown",
                 phoneNumber = dto.phoneNumber, // Mapped
                 status = try { ConnectionStatus.valueOf(dto.status ?: "CONNECTED") } catch(e: Exception) { ConnectionStatus.CONNECTED },
                 avatarUrl = dto.avatarUrl?.takeIf { it.isNotBlank() }
             )
        }
        
        return maskConnections(connections)
    }


    override suspend fun getAvailableContacts(page: Int, limit: Int): List<Connection> {
        val response: List<com.bhanit.apps.echo.data.model.ConnectionResponse> = com.bhanit.apps.echo.core.network.safeApiCall {
            httpClient.get("$baseUrl/contacts/available") {
                parameter("page", page)
                parameter("limit", limit)
            }.body<List<com.bhanit.apps.echo.data.model.ConnectionResponse>>()
        }.getOrThrow()

        return response.map { dto ->
             Connection(
                 userId = dto.userId,
                 username = dto.username ?: "Unknown",
                 phoneNumber = dto.phoneNumber, 
                 status = try { ConnectionStatus.valueOf(dto.status ?: "NONE") } catch(e: Exception) { ConnectionStatus.NONE },
                 avatarUrl = dto.avatarUrl?.takeIf { it.isNotBlank() }
             )
        }
    }
    
    override suspend fun getUnifiedContacts(): List<Contact> {
        // 1. Fetch Local Contacts
        // 1. Fetch Local Contacts
        var deviceContacts = contactManager.fetchContacts()
        // Removed empty check to allow network error propagation


        // Filter out Self
        val session = sessionRepository.getSession()
        if (session != null) {
            val myPhone = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(session.phone)
            deviceContacts = deviceContacts.filter { 
                val contactPhone = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(it.phoneNumber)
                contactPhone != myPhone && !contactPhone.endsWith(myPhone.takeLast(10)) 
            }
        }

        // 2. Sync Contacts (Upserts to Server)
        // Generate multiple hashes for each contact to handle format mismatches (e.g. +91 vs raw)
        val contactHashesMap = deviceContacts.associate { contact ->
            val normalized = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(contact.phoneNumber)
            val raw = if (normalized.startsWith("+91")) normalized.removePrefix("+91") else normalized
            // Also consider last 10 digits if different
            val last10 = if (normalized.length > 10) normalized.takeLast(10) else normalized
            
            val hashes = mutableSetOf<String>()
            hashes.add(com.bhanit.apps.echo.core.util.HashUtils.sha256(normalized))
            hashes.add(com.bhanit.apps.echo.core.util.HashUtils.sha256(raw))
            hashes.add(com.bhanit.apps.echo.core.util.HashUtils.sha256(last10))
            
            contact.id to hashes // Map Contact ID (or phone) to its set of hashes
        }

        val allHashes = contactHashesMap.values.flatten().distinct()
        val syncRequest = com.bhanit.apps.echo.data.model.SyncContactsRequest(allHashes)
        
        // We need to fetch registered users
        var serverMap: Map<String, com.bhanit.apps.echo.data.model.ConnectionResponse> = emptyMap()
        
        try {
           val syncResponse: com.bhanit.apps.echo.data.model.SyncContactsResponse = com.bhanit.apps.echo.core.network.safeApiCall {
                httpClient.post("$baseUrl/contacts/sync") {
                    contentType(io.ktor.http.ContentType.Application.Json)
                    setBody(syncRequest)
                }.body<com.bhanit.apps.echo.data.model.SyncContactsResponse>()
            }.getOrThrow()
            
            // SyncResponse contains UserSummary with phoneHash.
            // We need to map these back to our contacts.
            // But wait, the current impl below calls `getAvailableContacts` which fetches ALL available contacts.
            // Does `sync` just register them? Yes.
            // The `getAvailableContacts` call below retrieves the detailed status. 
            // BUT `getAvailableContacts` on server relies on the "Connection/Contact" table? 
            // No, `getAvailableContacts` usually implies "Find registered users from my contacts".
            // If the server `getAvailableContacts` uses the previously uploaded contacts, then we rely on that.
            // Let's check `ContactService` on server.
            // Server `syncContacts` finds users by hashes and returns them. It also UPSERTS to ContactRepository.
            // Server `getAvailableContacts` queries `ContactRepository` for this user and JOINs with Users.
            
            // So, since we just upserted with MULTIPLE hashes (wait, we sent hashes to FIND, but did we upsert valid contacts?)
            // Server `syncContacts` logic:
            // val contactPairs = request.hashedNumbers.map { it to null } -> upsertContacts.
            // It upserts ALL hashes we sent as "My Contacts". 
            // So if I send hash(+91...) AND hash(700...), server saves both as YOUR contacts.
            // Then `getAvailableContacts` will find any registered user matching ANY of those hashes.
            // So fetching `getAvailableContacts` afterwards SHOULD work and return the "Correct" single user.
            
            // However, `getAvailableContacts` returns ConnectionResponse which has `phoneNumber`.
            // Which phone number? The one from the Registered User table.
            
            // So we don't need to do complex matching locally IF we trust `getAvailableContacts` returns everything.
            // Let's rely on the strategy: 
            // 1. Send ALL variants to Sync.
            // 2. Fetch Available.
            // 3. Match Available (which has Server Phone) to Local (which has Local Phone).
            // Problem: `getAvailableContacts` returns registered users. 
            // We need to match them back to `deviceContacts` to show the correct local name.
            // `ConnectionResponse` has `phoneNumber` (User's reg phone).
            // `deviceContacts` has `+91...` (Local phone).
            // Mismatch again?
            // User Reg: 700...
            // Local: +91700...
            // `getAvailableContacts` returns user with `700...`.
            // We iterate `deviceContacts` (+91700...). match with `700...`. 
            // We need to fuzzy match here too.
            
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Fetch Available Contacts
        val serverContacts = getAvailableContacts(1, 10000)
        
        // Map server contacts by their phone number (Raw string)
        val serverMapByNormalized = serverContacts.associateBy { 
             com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(it.phoneNumber)
        }
        
        // 4. Merge
        return deviceContacts.mapNotNull { local ->
            val normalizedLocal = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(local.phoneNumber)
            val serverData = serverMapByNormalized[normalizedLocal]
            
            if (serverData != null) {
                // If already connected or has incoming request, hide from "Add Friends" list
                if (serverData.status == ConnectionStatus.CONNECTED || serverData.status == ConnectionStatus.PENDING_INCOMING) {
                    return@mapNotNull null
                }
                
                Contact(
                    name = local.name, 
                    phoneNumber = local.phoneNumber,
                    isRegistered = true,
                    userId = serverData.userId,
                    photoUrl = serverData.avatarUrl?.takeIf { it.isNotBlank() },
                    status = serverData.status.name
                )
            } else {
                 Contact(
                    name = local.name,
                    phoneNumber = local.phoneNumber,
                    isRegistered = false,
                    status = "NOT_CONNECTED"
                )
            }
        }.sortedBy { it.name }
    }

    override suspend fun requestConnection(userId: String) {
        com.bhanit.apps.echo.core.network.safeApiCall {
            httpClient.post("$baseUrl/contacts/connect/$userId")
        }.getOrThrow()
    }

    override suspend fun acceptConnection(userId: String) {
        com.bhanit.apps.echo.core.network.safeApiCall {
            httpClient.post("$baseUrl/contacts/accept/$userId")
        }.getOrThrow()
    }

    override suspend fun denyConnection(userId: String) {
        com.bhanit.apps.echo.core.network.safeApiCall {
            httpClient.post("$baseUrl/contacts/deny/$userId")
        }.getOrThrow()
    }

    override suspend fun inviteUser(phoneNumber: String) {
        val referralCode = sessionRepository.referralCode.first()
        val url = if (referralCode != null) "https://api-echo.bhanitgaurav.com/r/$referralCode?m=invite" 
                  else "https://api-echo.bhanitgaurav.com/r?m=invite"
        
        shareManager.shareText("Join me on Echo! Connect with me here: $url")
    }
    override suspend fun getFriendRequests(): List<Connection> {
        val response: List<com.bhanit.apps.echo.data.model.ConnectionResponse> = com.bhanit.apps.echo.core.network.safeApiCall {
            httpClient.get("$baseUrl/contacts/requests").body<List<com.bhanit.apps.echo.data.model.ConnectionResponse>>()
        }.getOrThrow()

        val requests = response.map { dto ->
             Connection(
                 userId = dto.userId,
                 username = dto.username ?: "Unknown",
                 phoneNumber = dto.phoneNumber, 
                 status = ConnectionStatus.PENDING_INCOMING,
                 avatarUrl = dto.avatarUrl?.takeIf { it.isNotBlank() }
             )
        }
        return maskConnections(requests)
    }
    
    override suspend fun getPendingRequests(): List<Connection> {
        return getFriendRequests()
    }

    override suspend fun getSentRequests(): List<Connection> {
        val response: List<com.bhanit.apps.echo.data.model.ConnectionResponse> = com.bhanit.apps.echo.core.network.safeApiCall {
            httpClient.get("$baseUrl/contacts/requests/sent").body<List<com.bhanit.apps.echo.data.model.ConnectionResponse>>()
        }.getOrThrow()

        val requests = response.map { dto ->
             Connection(
                 userId = dto.userId,
                 username = dto.username ?: "Unknown",
                 phoneNumber = dto.phoneNumber, 
                 status = ConnectionStatus.PENDING_OUTGOING,
                 avatarUrl = dto.avatarUrl?.takeIf { it.isNotBlank() }
             )
        }
        return maskConnections(requests)
    }

    override suspend fun cancelRequest(userId: String) {
        com.bhanit.apps.echo.core.network.safeApiCall {
            httpClient.post("$baseUrl/contacts/cancel/$userId")
        }.getOrThrow()
    }

    override suspend fun removeConnection(userId: String) {
        com.bhanit.apps.echo.core.network.safeApiCall {
            httpClient.post("$baseUrl/contacts/connect/$userId/remove")
        }.getOrThrow()
    }

    override suspend fun connectByReferral(code: String): Result<com.bhanit.apps.echo.data.model.ReferralResponse> {
        return try {
            val response: com.bhanit.apps.echo.data.model.ReferralResponse = com.bhanit.apps.echo.core.network.safeApiCall {
                httpClient.post("$baseUrl/contacts/referral") {
                    contentType(io.ktor.http.ContentType.Application.Json)
                    setBody(mapOf("code" to code))
                }.body<com.bhanit.apps.echo.data.model.ReferralResponse>()
            }.getOrThrow()
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun searchAvailableContacts(query: String, page: Int, limit: Int): List<Connection> {
        // 1. Fetch Exclusion Lists (Connected, Incoming Requests)
        val incomingRequests = try { getFriendRequests() } catch (e: Exception) { emptyList() }
        val sentRequests = try { getSentRequests() } catch (e: Exception) { emptyList() }
        val connections = try { getConnections(1, 1000) } catch(e: Exception) { emptyList() }
        
        val exclusionPhones = (incomingRequests + connections).map { 
            com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(it.phoneNumber) 
        }.toSet()
        val exclusionIds = (incomingRequests + connections).map { it.userId }.toSet()
        
        val sentRequestIds = sentRequests.map { it.userId }.toSet()

        // 2. Fetch & Filter Local Contacts
        val deviceContacts = contactRepositoryHelperGetLocalContacts()
        val normalizedQuery = query.trim()
        val localMatches = deviceContacts.filter { contact ->
            val normPhone = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(contact.phoneNumber)
            val matchesQuery = contact.name.contains(normalizedQuery, ignoreCase = true) || contact.phoneNumber.contains(normalizedQuery)
            val isExcluded = exclusionPhones.contains(normPhone)
            matchesQuery && !isExcluded
        }
        
        // 3. Check Registration of Local Matches (Sync)
        // This is critical to distinguish "Invite" vs "Add" for local contacts
        val resolvedLocalMatches = if (localMatches.isNotEmpty()) {
             syncContacts(localMatches) 
        } else emptyList()
        
        // 4. Map Local Matches to Connections
        val localConnectionResults = resolvedLocalMatches.mapNotNull { contact ->
             if (contact.isRegistered && contact.userId != null) {
                 if (exclusionIds.contains(contact.userId)) null // Should be filtered
                 else {
                     // Check if Pending Outgoing
                     val status = if (sentRequestIds.contains(contact.userId)) ConnectionStatus.PENDING_OUTGOING else ConnectionStatus.NONE
                     
                     Connection(
                         userId = contact.userId,
                         username = contact.name, // Use local name
                         phoneNumber = contact.phoneNumber,
                         status = status, 
                         avatarUrl = contact.photoUrl
                     )
                 }
             } else {
                 // Invite
                 Connection(
                     userId = "",
                     username = contact.name,
                     phoneNumber = contact.phoneNumber,
                     status = ConnectionStatus.NONE,
                     avatarUrl = null
                 )
             }
        }

        // 5. Search Server (Fuzzy Search) - Only if query exists
        val serverResponse: List<com.bhanit.apps.echo.data.model.ConnectionResponse> = if (normalizedQuery.length >= 3) {
            try {
                // Main Query Search
                val textSearchParams = com.bhanit.apps.echo.core.network.safeApiCall {
                    httpClient.get("$baseUrl/contacts/search") {
                        parameter("query", normalizedQuery)
                        parameter("page", 1)
                        parameter("limit", 20)
                    }.body<List<com.bhanit.apps.echo.data.model.ConnectionResponse>>()
                }.getOrThrow()

                // FALLBACK SEARCH (Optimized):
                // Run these checks in PARALLEL to avoid latency
                // FALLBACK SEARCH (Optimized):
                // Run these checks in PARALLEL to avoid latency
                // FALLBACK SEARCH (Disabled by user request)
                val fallbackResults = emptyList<com.bhanit.apps.echo.data.model.ConnectionResponse>()
                /*
                // FALLBACK SEARCH (Optimized):
                // Run these checks in PARALLEL to avoid latency
                val fallbackResults = coroutineScope {
                    resolvedLocalMatches
                        .filter { !it.isRegistered } 
                        .map { contact: Contact ->
                            async {
                                val phoneQuery = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(contact.phoneNumber)
                                try {
                                     httpClient.get("$baseUrl/contacts/search") {
                                        parameter("query", phoneQuery)
                                        parameter("page", 1)
                                        parameter("limit", 1)
                                    }.body<List<com.bhanit.apps.echo.data.model.ConnectionResponse>>().firstOrNull()
                                } catch (e: Exception) { null }
                            }
                        }.awaitAll().filterNotNull()
                }
                */
                
                (textSearchParams + fallbackResults).distinctBy { it.userId }
                
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        // 6. Map Server Results (Include Local Matches by Username)
        val localPhoneToNameMap = deviceContacts.associate { 
             com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(it.phoneNumber) to it.name 
        }

        val serverResults = serverResponse.mapNotNull { dto ->
             val p = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(dto.phoneNumber)
             
             if (exclusionIds.contains(dto.userId) || exclusionPhones.contains(p)) return@mapNotNull null

             val localName = localPhoneToNameMap[p]
             
             // STRICT MATCH RULE:
             // If NOT a local contact, we only show if Query matches Username (Exact) OR Phone (Exact)
             // If it IS a local contact, we found it via server usage check, so we keep it (it merges with local fuzzy rules)
             if (localName == null) {
                 val matchesUsername = dto.username?.equals(normalizedQuery, ignoreCase = true) == true
                 val matchesPhone = p == com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(normalizedQuery)
                 if (!matchesUsername && !matchesPhone) return@mapNotNull null
             }

             // Check if Pending Outgoing
             val status = if (sentRequestIds.contains(dto.userId)) ConnectionStatus.PENDING_OUTGOING else try { ConnectionStatus.valueOf(dto.status ?: "NONE") } catch(e: Exception) { ConnectionStatus.NONE }
        
             Connection(
                 userId = dto.userId,
                 username = localName ?: dto.username ?: "Unknown", // Prefer Local Name
                 phoneNumber = dto.phoneNumber,
                 status = status,
                 avatarUrl = dto.avatarUrl?.takeIf { it.isNotBlank() }
             )
        }
        
        // 7. Merge, Sort, and Paginate
        val allResults = (localConnectionResults + serverResults)
            .groupBy { com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(it.phoneNumber) }
            .map { (_, duplicates) ->
                // Prioritize the one with a userId (Registered) over one without
                duplicates.find { it.userId.isNotEmpty() } ?: duplicates.first()
            }
            .sortedWith(compareByDescending<Connection> { it.userId.isNotEmpty() } // Registered first
                .thenBy { it.username }) // Then Alphabetical
        
        // Manual Pagination
        val fromIndex = (page - 1) * limit
        if (fromIndex >= allResults.size) return emptyList()
        return allResults.drop(fromIndex).take(limit)
    }

    private suspend fun contactRepositoryHelperGetLocalContacts(): List<Contact> {
         val deviceContacts = contactManager.fetchContacts().map {
             Contact(
                 name = it.name,
                 phoneNumber = it.phoneNumber,
                 isRegistered = false,
                 status = "NOT_CONNECTED"
             )
         }
         
         val session = sessionRepository.getSession()
         if (session != null) {
            val myPhone = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(session.phone)
            return deviceContacts.filter { 
                val contactPhone = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(it.phoneNumber)
                contactPhone != myPhone && !contactPhone.endsWith(myPhone.takeLast(10)) 
            }
        }
        return deviceContacts
    }

    override fun openSettings() {
        contactManager.openSettings()
    }

    private suspend fun maskConnections(connections: List<Connection>): List<Connection> {
        val deviceContacts = contactManager.fetchContacts()
        
        // Optimization: Pre-compute a cleanup map for O(1) lookup.
        // We map specific phone number variations to the Contact object.
        val contactMap = mutableMapOf<String, com.bhanit.apps.echo.features.contact.manager.DeviceContact>()
        deviceContacts.forEach { contact ->
            val normalized = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(contact.phoneNumber)
            
            // Prioritize exact normalized match
            if (!contactMap.containsKey(normalized)) {
                contactMap[normalized] = contact
            }
            
            // Also map the last 10 digits for loose matching (if applicable)
            if (normalized.length > 10) {
                val last10 = normalized.takeLast(10)
                if (!contactMap.containsKey(last10)) {
                    contactMap[last10] = contact
                }
            }
        }

        return connections.map { connection ->
            val serverNumber = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(connection.phoneNumber)
            
            // Fast lookup:
            // 1. Try exact normalized match
            // 2. If valid length, try matching last 10 digits
            var matchingContact = contactMap[serverNumber]
            
            if (matchingContact == null && serverNumber.length > 10) {
                matchingContact = contactMap[serverNumber.takeLast(10)]
            }

            if (matchingContact != null) {
                // If the user is saved in local contacts, display the name saved in the device (e.g. "Mom")
                // instead of the username set by the user on the server.
                connection.copy(username = matchingContact.name)
            } else {
                // If not in contacts, hide the phone number for privacy.
                connection.copy(phoneNumber = "")
            }
        }
    }
}
