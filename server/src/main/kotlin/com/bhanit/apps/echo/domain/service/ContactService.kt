package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.data.model.SyncContactsRequest
import com.bhanit.apps.echo.data.model.SyncContactsResponse
import com.bhanit.apps.echo.data.model.UserSummary
import com.bhanit.apps.echo.data.table.Users
import com.bhanit.apps.echo.data.table.ConnectionStatus
import com.bhanit.apps.echo.domain.repository.UserRepository
import com.bhanit.apps.echo.domain.repository.ConnectionRepository
import com.bhanit.apps.echo.domain.repository.ContactRepository
import java.util.UUID

class ContactService(
    private val userRepository: UserRepository,
    private val connectionRepository: ConnectionRepository,
    private val contactRepository: com.bhanit.apps.echo.domain.repository.ContactRepository
) {
    suspend fun syncContacts(request: SyncContactsRequest, userId: UUID? = null): SyncContactsResponse {
        // 1. Find registered users matching hashes
        val matches = userRepository.findUsersByHashes(request.hashedNumbers)
        
        // 2. Persist these contacts if userId is provided (Authenticated Sync)
        if (userId != null) {
            // Map hashes to names from request? 
            // The request.hashedNumbers is just a list of strings if I recall correctly.
            // Wait, SyncContactsRequest typically is list of hashes?
            // If it's just a list<String>, we don't have local names.
            // Ideally SyncContactsRequest should have name + hash.
            // Let's assume for now we just store the hash, and name if available (which it isn't in current DTO).
            // We will update DTO later if needed. For now, just store hash. Local name will be null.
            val contactPairs = request.hashedNumbers.map { it to null as String? }
            contactRepository.upsertContacts(userId, contactPairs)
        }
    
        // PRIVACY FILTER:
        val privacyExcludeIds = if (userId != null) {
            val blockedByMe = userRepository.getBlockedUsers(userId).map { it[Users.id].value }
            val blockedMe = userRepository.getBlockedBy(userId).map { it[Users.id].value }
            (blockedByMe + blockedMe).distinct()
        } else {
            emptyList()
        }

        val summaries = matches.mapNotNull { 
            val id = it[Users.id].value
            if (id in privacyExcludeIds) return@mapNotNull null

            UserSummary(
                id = id.toString(),
                username = it[Users.username],
                phoneHash = it[Users.phoneHash]
            )
        }
        return SyncContactsResponse(summaries, emptyList())
    }
    
    suspend fun getAvailableContacts(currentUserId: String, limit: Int, offset: Int): List<com.bhanit.apps.echo.data.model.ConnectionResponse> {
        val userUUID = UUID.fromString(currentUserId)

        // PRIVACY FILTER:
        val blockedByMe = userRepository.getBlockedUsers(userUUID).map { it[Users.id].value }
        val blockedMe = userRepository.getBlockedBy(userUUID).map { it[Users.id].value }
        val privacyExcludeIds = (blockedByMe + blockedMe).distinct()

        val available = contactRepository.getAvailableContacts(userUUID, limit, offset)
            .filter { it.id !in privacyExcludeIds }
        
        val mapped = available.map { row ->
             val status = row.status ?: "NOT_CONNECTED"
             var finalStatus = status
             
             if (status == "PENDING") {
                 val initiatedBy = row.initiatedBy
                 finalStatus = if (initiatedBy != null && initiatedBy == userUUID) {
                     "PENDING_OUTGOING"
                 } else {
                     "PENDING_INCOMING"
                 }
             } else if (status == "NONE" || status == "null") {
                 finalStatus = "NOT_CONNECTED"
             }
             
             com.bhanit.apps.echo.data.model.ConnectionResponse(
                 userId = row.id.toString(),
                 username = row.username ?: "Unknown",
                 phoneNumber = row.phoneNumber,
                 status = finalStatus,
                 avatarUrl = row.avatarUrl
             )
        }
        
        val filtered = mapped.filter { 
            it.userId != currentUserId // Include CONNECTED users so client knows their status
        }
        return filtered
    }
    
    suspend fun requestConnection(fromUserId: String, targetUserId: String) {
        val fromUUID = UUID.fromString(fromUserId)
        val targetUUID = UUID.fromString(targetUserId)

        if (fromUUID == targetUUID) {
            throw IllegalArgumentException("Cannot create connection with self")
        }

        // Check if there is already an incoming request from target
        val existingRequest = connectionRepository.getPendingRequests(fromUUID) // Returns requests WHERE I am the target
            .find { it[com.bhanit.apps.echo.data.table.Connections.initiatedBy].value == targetUUID }
            
        if (existingRequest != null) {
            // Auto-accept
            acceptConnection(targetUserId, fromUserId)
        } else {
            // Create new request
            connectionRepository.createConnectionRequest(fromUUID, targetUUID)
            
            // Notify Target
            try {
                val (token, platform) = userRepository.getFcmTokenAndPlatform(targetUUID)
                if (!token.isNullOrBlank()) {
                    val senderRow = userRepository.getUser(fromUUID)
                    val senderName = senderRow?.get(Users.username) ?: "Someone"
                    val data = mapOf("type" to "CONNECTION")
                    val result = FCMService.sendNotification(token, "New Connection Request", "$senderName wants to connect with you.", data, platform = platform)
                    if (result is FCMService.FCMResult.InvalidToken) {
                        userRepository.clearFcmToken(targetUUID)
                    }
                }
            } catch (e: Exception) {
                println("ERROR: Failed to send connection request notification: ${e.message}")
            }
        }
    }

    suspend fun connectByReferral(requestorId: String, referralCode: String): com.bhanit.apps.echo.data.model.ReferralResponse {
        val requestorUUID = UUID.fromString(requestorId)
        val targetUUID = userRepository.findUserByReferralCode(referralCode)
            ?: throw IllegalArgumentException("Invalid referral code")
            
        // Fetch Target User Details
        val targetUser = userRepository.getUser(targetUUID) ?: throw IllegalArgumentException("User not found")
        val targetName = targetUser[Users.username]
        val targetPhoto = targetUser[Users.profilePhoto]

        if (requestorUUID == targetUUID) {
             return com.bhanit.apps.echo.data.model.ReferralResponse(
                 status = "SELF",
                 userId = targetUUID.toString(),
                 username = targetName,
                 avatarUrl = targetPhoto
             )
        }
        
        // Priority 1: Check Blocks (Safety First)
        // 1. Did Target block Requestor?
        if (connectionRepository.isBlocked(targetUUID, requestorUUID)) {
             return com.bhanit.apps.echo.data.model.ReferralResponse(
                 status = "UNAVAILABLE",
                 userId = targetUUID.toString(),
                 username = "User",
                 avatarUrl = null
             )
        }
        
        // 2. Did Requestor block Target?
        if (connectionRepository.isBlocked(requestorUUID, targetUUID)) {
             return com.bhanit.apps.echo.data.model.ReferralResponse(
                 status = "BLOCKED",
                 userId = targetUUID.toString(),
                 username = targetName,
                 avatarUrl = targetPhoto
             )
        }
        
        // Check existing connection
        val status = connectionRepository.getConnectionStatus(requestorUUID, targetUUID)
        if (status == ConnectionStatus.CONNECTED) {
            return com.bhanit.apps.echo.data.model.ReferralResponse(
                 status = "ALREADY_CONNECTED",
                 userId = targetUUID.toString(),
                 username = targetName,
                 avatarUrl = targetPhoto
            )
        }
        // No need to check status == BLOCKED here as generic status, because we checked generic UserBlocks above.
        // But if connection table says BLOCKED but UserBlocks is empty (sync issue), we might want to respect it?
        // Let's assume UserBlocks is truth. If Connection.status is BLOCKED but no UserBlock row, it's weird.
        // We can leave the generic check or just rely on the above.
        // If we trust UserBlocks as the source of truth for blocking, the above is sufficient.
        
        // Check Pending Request (Incoming or Outgoing)
        val pending = connectionRepository.getPendingRequests(requestorUUID).find { it[com.bhanit.apps.echo.data.table.Connections.initiatedBy].value == targetUUID }
        if (pending != null) {
            acceptConnection(targetUUID.toString(), requestorId)
            return com.bhanit.apps.echo.data.model.ReferralResponse(
                 status = "CONNECTED",
                 userId = targetUUID.toString(),
                 username = targetName,
                 avatarUrl = targetPhoto
            )
        }
        
        // Privacy Check: Does target allow auto-connect via QR?
        val allowAutoConnect = targetUser[Users.allowQrAutoConnect]
        if (allowAutoConnect) {
             // AUTO CONNECT
             connectionRepository.createConnection(requestorUUID, targetUUID, ConnectionStatus.CONNECTED)
             // Notify Target
             try {
                val (token, platform) = userRepository.getFcmTokenAndPlatform(targetUUID)
                if (!token.isNullOrBlank()) {
                    val requestorRow = userRepository.getUser(requestorUUID)
                    val requestorName = requestorRow?.get(Users.username) ?: "Someone"
                    val data = mapOf("type" to "CONNECTION")
                    FCMService.sendNotification(token, "New Connection", "$requestorName scanned your QR and connected!", data, platform = platform)
                }
             } catch (e: Exception) { println("Error sending notif: ${e.message}") }

             return com.bhanit.apps.echo.data.model.ReferralResponse(
                 status = "CONNECTED",
                 userId = targetUUID.toString(),
                 username = targetName,
                 avatarUrl = targetPhoto
             )
        }

        // Otherwise -> Send Request (default behavior below)

        
        // Check Sent Request
        val sent = connectionRepository.getSentRequests(requestorUUID).find { 
             val userA = it[com.bhanit.apps.echo.data.table.Connections.userA].value
             val userB = it[com.bhanit.apps.echo.data.table.Connections.userB].value
             (userA == targetUUID || userB == targetUUID)
        }
        if (sent != null) {
            return com.bhanit.apps.echo.data.model.ReferralResponse(
                 status = "PENDING",
                 userId = targetUUID.toString(),
                 username = targetName,
                 avatarUrl = targetPhoto
             )
        }

        // Create Request
        requestConnection(requestorId, targetUUID.toString())
        return com.bhanit.apps.echo.data.model.ReferralResponse(
             status = "REQUEST_SENT",
             userId = targetUUID.toString(),
             username = targetName,
             avatarUrl = targetPhoto
         )
    }

    suspend fun connectOnboardingReferral(referrerId: UUID, refereeId: UUID) {
        // Safety Checks
        if (referrerId == refereeId) return

        // 1. Check Blocks
        if (connectionRepository.isBlocked(referrerId, refereeId) || 
            connectionRepository.isBlocked(refereeId, referrerId)) {
            return
        }
        
        // 2. Check Existing
        val status = connectionRepository.getConnectionStatus(referrerId, refereeId)
        if (status == ConnectionStatus.CONNECTED) return
        
        // 3. Create Connection (No Notifications needed here as UserRoutes sends Welcome/Referral Joined notifs which imply connection)
        // Or we can rely on the fact they are connected.
        connectionRepository.createConnection(referrerId, refereeId, ConnectionStatus.CONNECTED)
    }
    
    suspend fun acceptConnection(requestorId: String, acceptorId: String) {
        connectionRepository.updateConnectionStatus(
            UUID.fromString(requestorId), 
            UUID.fromString(acceptorId), 
            ConnectionStatus.CONNECTED
        )
        
        // Notify Requestor
        try {
            val requestorUUID = UUID.fromString(requestorId)
            val (token, platform) = userRepository.getFcmTokenAndPlatform(requestorUUID)
            if (!token.isNullOrBlank()) {
                val acceptorUUID = UUID.fromString(acceptorId)
                val acceptorRow = userRepository.getUser(acceptorUUID)
                val acceptorName = acceptorRow?.get(Users.username) ?: "Someone"
                val data = mapOf("type" to "CONNECTION")
                val result = FCMService.sendNotification(token, "Connection Accepted", "$acceptorName accepted your connection request!", data, platform = platform)
                if (result is FCMService.FCMResult.InvalidToken) {
                    userRepository.clearFcmToken(requestorUUID)
                }
            }
        } catch (e: Exception) {
             println("ERROR: Failed to send connection accept notification: ${e.message}")
        }
    }

    suspend fun denyConnection(requestorId: String, denierId: String) {
        connectionRepository.deleteConnection(
            UUID.fromString(requestorId), 
            UUID.fromString(denierId)
        )
    }

    suspend fun removeConnection(currentUserId: String, targetId: String) {
         connectionRepository.deleteConnection(
            UUID.fromString(currentUserId),
            UUID.fromString(targetId)
        )
    }

    suspend fun getPendingRequests(userId: String): List<com.bhanit.apps.echo.data.model.ConnectionResponse> {
        val userUUID = UUID.fromString(userId)
        val requests = connectionRepository.getPendingRequests(userUUID)
        
        // Fetch User Details for requestors
        val requestorIds = requests.map { it[com.bhanit.apps.echo.data.table.Connections.initiatedBy].value }
        val users = userRepository.findUsersByIds(requestorIds)
        val userMap = users.associateBy { row -> row[Users.id].value }
        
        return requests.mapNotNull { req ->
             val senderId = req[com.bhanit.apps.echo.data.table.Connections.initiatedBy].value
             val sender = userMap[senderId] ?: return@mapNotNull null
             
             com.bhanit.apps.echo.data.model.ConnectionResponse(
                 userId = senderId.toString(),
                 username = sender[Users.username] ?: "Unknown",
                 phoneNumber = sender[Users.phoneNumber],
                 status = "PENDING_INCOMING",
                 avatarUrl = sender[Users.profilePhoto]
             )
        }
    }

    suspend fun getSentRequests(userId: String): List<com.bhanit.apps.echo.data.model.ConnectionResponse> {
        val userUUID = UUID.fromString(userId)
        val requests = connectionRepository.getSentRequests(userUUID)
        
        // Fetch User Details for recipients (Identify the OTHER user)
        val recipientIds = requests.map { req ->
             val userA = req[com.bhanit.apps.echo.data.table.Connections.userA].value
             val userB = req[com.bhanit.apps.echo.data.table.Connections.userB].value
             if (userA == userUUID) userB else userA
        }
        
        val users = userRepository.findUsersByIds(recipientIds)
        val userMap = users.associateBy { row -> row[Users.id].value }
        
        return requests.mapNotNull { req ->
             // Identify the OTHER user.
             val userA = req[com.bhanit.apps.echo.data.table.Connections.userA].value
             val userB = req[com.bhanit.apps.echo.data.table.Connections.userB].value
             val targetId = if (userA == userUUID) userB else userA
             
             val target = userMap[targetId] ?: return@mapNotNull null
             
             com.bhanit.apps.echo.data.model.ConnectionResponse(
                 userId = targetId.toString(),
                 username = target[Users.username] ?: "Unknown",
                 phoneNumber = target[Users.phoneNumber],
                 status = "PENDING_OUTGOING",
                 avatarUrl = target[Users.profilePhoto]
             )
        }
    }

    suspend fun cancelConnectionRequest(initiatorId: String, targetId: String) {
        val initiatorUUID = UUID.fromString(initiatorId)
        val targetUUID = UUID.fromString(targetId)
        // Delete pending request
        connectionRepository.deleteConnection(initiatorUUID, targetUUID)
    }

    suspend fun blockUser(blockerId: String, blockedId: String) {
        val blockerUUID = UUID.fromString(blockerId)
        val blockedUUID = UUID.fromString(blockedId)
        
         // 1. Update Connection Status (Soft Block - for UI/Connection State)
         connectionRepository.updateConnectionStatus(
             blockerUUID, 
             blockedUUID, 
             ConnectionStatus.BLOCKED
         )
         
         // 2. Insert into UserBlocks (Hard Block - for System Privacy)
         userRepository.blockUser(blockerUUID, blockedUUID)
    }
    suspend fun getConnections(userId: String, limit: Int, offset: Int, query: String? = null): List<com.bhanit.apps.echo.data.model.ConnectionResponse> {
        try {
            val userUUID = UUID.fromString(userId)
            val connections = connectionRepository.getConnections(userUUID, limit, offset)
            
            val friendIds = connections.map {
                val userA = it[com.bhanit.apps.echo.data.table.Connections.userA].value
                val userB = it[com.bhanit.apps.echo.data.table.Connections.userB].value
                if (userA == userUUID) userB else userA
            }
            
            val users = userRepository.findUsersByIds(friendIds)
            val userMap = users.associateBy { row -> row[Users.id].value }
            
            val mapped = connections.mapNotNull { conn ->
                 val userA = conn[com.bhanit.apps.echo.data.table.Connections.userA].value
                 val userB = conn[com.bhanit.apps.echo.data.table.Connections.userB].value
                 val friendId = if (userA == userUUID) userB else userA
                 val friend = userMap[friendId] ?: return@mapNotNull null
                 
                 com.bhanit.apps.echo.data.model.ConnectionResponse(
                     userId = friendId.toString(),
                     username = friend[Users.username] ?: "Unknown",
                     phoneNumber = friend[Users.phoneNumber],
                     status = conn[com.bhanit.apps.echo.data.table.Connections.status].name, // CONNECTED
                     avatarUrl = friend[Users.profilePhoto]
                 )
            }
            
            return if (query.isNullOrEmpty()) {
                mapped
            } else {
                mapped.filter { 
                    (it.username?.contains(query, ignoreCase = true) == true) || 
                    it.phoneNumber.contains(query, ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    suspend fun searchUsers(currentUserId: String, query: String, limit: Int, offset: Long): List<com.bhanit.apps.echo.data.model.ConnectionResponse> {
        val userUUID = UUID.fromString(currentUserId)
        
        val qDigits = query.filter { it.isDigit() }
        val isPotentiallyPhone = qDigits.length >= 10
        val searchPhone = isPotentiallyPhone
        
        // Exact match for username (non-phone), Partial allowed for Phone.
        val exactMatch = !isPotentiallyPhone
        
        // OPTIMIZATION: Passed currentUserId to Repository for SQL-side filtering
        val results = userRepository.searchUsers(query, limit, offset, includePhone = searchPhone, exactMatch = exactMatch, currentUserId = userUUID)
        
        return results.mapNotNull { row ->
            val id = row[Users.id].value
            if (id == userUUID) return@mapNotNull null // Exclude self
            
            val phone = row[Users.phoneNumber]
            val username = row[Users.username]
            val avatar = row[Users.profilePhoto]
            
            // Check matches
            val matchesUsername = username.contains(query, ignoreCase = true)
            
            // Strict Phone Logic:
            // If it didn't match by username, it must have matched by phone (or other fields if any).
            // We only allow "Phone Match" if the user searched a COMPLETE number.
            // Heuristic: Query has 10+ digits AND matches the end of the user's phone digits.
            val pDigits = phone.filter { it.isDigit() }
            val matchesPhoneStrict = isPotentiallyPhone && pDigits.endsWith(qDigits)

            // If it's not a username match AND not a strict phone match, hide it.
            // (This filters out partial phone matches like "987" matching "98765...")
            if (!matchesUsername && !matchesPhoneStrict) {
                return@mapNotNull null
            }
            
            // Privacy Logic: Show phone ONLY if it was a strict phone match.
            // If matched by username, hide phone.
            val finalPhone = if (matchesPhoneStrict) phone else "" 
            
            com.bhanit.apps.echo.data.model.ConnectionResponse(
                userId = id.toString(),
                username = username,
                phoneNumber = finalPhone,
                status = "NONE", // Status will be resolved by client against local/sent lists
                avatarUrl = avatar
            )
        }
    }
}
