package com.bhanit.apps.echo.data.repository

import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import com.bhanit.apps.echo.data.table.Users
import com.bhanit.apps.echo.data.table.SystemConfig
import com.bhanit.apps.echo.domain.repository.UserRepository
import com.bhanit.apps.echo.data.model.TransactionType
import com.bhanit.apps.echo.data.model.PurchaseType
import com.bhanit.apps.echo.data.model.PurchaseRequest
import com.bhanit.apps.echo.data.model.CreditTransaction
import com.bhanit.apps.echo.data.table.RewardHistory
import com.bhanit.apps.echo.data.table.OneTimeRewardType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.LowerCase
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.or
import java.time.Instant
import java.util.UUID

class UserRepositoryImpl : UserRepository {
    override suspend fun findUserByPhone(phone: String): UUID? = dbQuery {
        Users.selectAll().where { Users.phoneNumber eq phone }
            .map { it[Users.id].value }
            .singleOrNull()
    }

    override suspend fun findUsersByHashes(hashes: List<String>): List<ResultRow> = dbQuery {
        Users.selectAll().where { Users.phoneHash inList hashes }
            .toList()
    }

    override suspend fun createUser(phone: String): UUID = dbQuery {
        val hash = com.bhanit.apps.echo.data.util.CryptoUtils.sha256(phone)
        var generatedUsername = generateUsername()
        while (isUsernameTakenInternal(generatedUsername)) {
            generatedUsername = generateUsername()
        }

        var myReferralCode = generateReferralCode()
        while (!Users.selectAll().where { Users.referralCode eq myReferralCode }.empty()) {
            myReferralCode = generateReferralCode()
        }

        Users.insertAndGetId {
            it[Users.phoneNumber] = phone
            it[Users.phoneHash] = hash
            it[Users.createdAt] = Instant.now()
            it[Users.isActive] = true
            it[Users.username] = generatedUsername
            it[Users.referralCode] = myReferralCode
        }.value
    }

    override suspend fun getUser(id: UUID): ResultRow? = dbQuery {
        Users.selectAll().where { Users.id eq id }.singleOrNull()
    }

    override suspend fun updateProfile(id: UUID, fullName: String?, username: String?, gender: String?, email: String?, photoUrl: String?, allowQrAutoConnect: Boolean?) {
        // Validation Logic
        if (fullName != null) {
            val trimmedName = fullName.trim()
            if (trimmedName.length > 50) {
                 throw IllegalArgumentException("Full name cannot exceed 50 characters.")
            }
            if (trimmedName.isNotEmpty() && !trimmedName.matches(Regex("^[a-zA-Z ]+$"))) {
                throw IllegalArgumentException("Full name can only contain letters and spaces.")
            }
        }
        
        if (username != null) {
             val trimmedUsername = username.trim()
             if (trimmedUsername.length < 8 || trimmedUsername.length > 20) {
                  throw IllegalArgumentException("Username must be between 8 and 20 characters.")
             }
             if (!trimmedUsername.matches(Regex("^[a-zA-Z0-9_]+$"))) {
                  throw IllegalArgumentException("Username can only contain letters, numbers, and underscores.")
             }
             // Check uniqueness if changing
             val existing = getUser(id)
             val currentUsername = existing?.get(Users.username)
             if (trimmedUsername != currentUsername && isUsernameTaken(trimmedUsername)) {
                  throw IllegalArgumentException("Username '$trimmedUsername' is already taken.")
             }
        }
        
        if (email != null) {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isNotEmpty() && !trimmedEmail.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {
                 throw IllegalArgumentException("Invalid email format.")
            }
        }

        dbQuery {
            Users.update({ Users.id eq id }) {
                fullName?.let { name -> it[Users.fullName] = name.trim() }
                username?.let { name -> it[Users.username] = name.trim() }
                gender?.let { g -> it[Users.gender] = g }
                email?.let { e -> it[Users.email] = e.trim() }
                photoUrl?.let { p -> it[Users.profilePhoto] = p }
                allowQrAutoConnect?.let { a -> it[Users.allowQrAutoConnect] = a }
            }
        }
    }

    override suspend fun isUsernameTaken(username: String): Boolean = dbQuery {
        isUsernameTakenInternal(username)
    }

    private fun isUsernameTakenInternal(username: String): Boolean {
        return !Users.selectAll().where { Users.username eq username }.empty()
    }
    
    private fun generateUsername(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }

    override suspend fun completeOnboarding(userId: UUID, username: String, referralCode: String?, photoUrl: String?): Pair<Boolean, UUID?> = dbQuery {
        // 1. Validate Username
        val trimmedUsername = username.trim()
        if (trimmedUsername.length < 8 || trimmedUsername.length > 20) {
            throw IllegalArgumentException("Username must be between 8 and 20 characters.")
        }
        if (!trimmedUsername.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            throw IllegalArgumentException("Username can only contain letters, numbers, and underscores.")
        }
        if (isUsernameTakenInternal(trimmedUsername)) {
             // Check if it's the SAME user (updating their own username during onboarding retry)
             val currentUser = Users.selectAll().where { Users.id eq userId }.singleOrNull()
             if (currentUser != null && currentUser[Users.username] != trimmedUsername) {
                 throw IllegalArgumentException("Username '$trimmedUsername' is already taken.")
             }
        }

        // 2. Get or Generate own Referral Code
        val currentUserRow = Users.selectAll().where { Users.id eq userId }.singleOrNull()
        var myReferralCode = currentUserRow?.get(Users.referralCode)
        
        if (myReferralCode == null) {
            myReferralCode = generateReferralCode()
            while (!Users.selectAll().where { Users.referralCode eq myReferralCode }.empty()) {
                myReferralCode = generateReferralCode()
            }
        }

        // 3. Process Referral Code (if provided)
        var referrerId: UUID? = null
        var referrerReward = 0
        var myReward = 0

        if (!referralCode.isNullOrBlank()) {
            val referrerRow = Users.selectAll().where { Users.referralCode eq referralCode.uppercase() }.singleOrNull()
            if (referrerRow != null) {
                // Ignore deleted users
                if (referrerRow[Users.isDeleted]) {
                    referrerId = null
                } else {
                    val rId = referrerRow[Users.id].value
                    if (rId != userId) {
                        referrerId = rId
                        referrerReward = REFERRAL_REWARD_REFERRER
                        
                        // Check One-Time Reward Eligibility for Referee (Me)
                        val myPhoneHash = currentUserRow?.get(Users.phoneHash)
                        var alreadyClaimed = false
                        if (myPhoneHash != null) {
                            alreadyClaimed = RewardHistory.select(RewardHistory.phoneHash)
                                .where { (RewardHistory.phoneHash eq myPhoneHash) and (RewardHistory.rewardType eq OneTimeRewardType.SIGNUP_BONUS) }
                                .count() > 0
                        }
                        
                        if (!alreadyClaimed) {
                            myReward = REFERRAL_REWARD_REFEREE
                        }
                    }
                }
            } else {
                throw IllegalArgumentException("Invalid referral code.")
            }
        }

        // 4. Update User & Insert Transactions
        Users.update({ Users.id eq userId }) {
            it[Users.username] = trimmedUsername
            it[Users.referralCode] = myReferralCode
            it[Users.referredBy] = referrerId
            it[Users.onboardingCompleted] = true
             if (photoUrl != null) it[Users.profilePhoto] = photoUrl
            if (myReward > 0) {
                with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                    it[Users.credits] = Users.credits + myReward
                }
            }
        }

        if (referrerId != null && referrerReward > 0) {
            Users.update({ Users.id eq referrerId }) {
                with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                    it[Users.credits] = Users.credits + referrerReward
                }
            }

            // Record Transactions
            com.bhanit.apps.echo.data.table.CreditTransactions.insert {
                it[com.bhanit.apps.echo.data.table.CreditTransactions.userId] = referrerId
                it[com.bhanit.apps.echo.data.table.CreditTransactions.amount] = referrerReward
                it[com.bhanit.apps.echo.data.table.CreditTransactions.type] = TransactionType.REFERRAL_REWARD
                it[com.bhanit.apps.echo.data.table.CreditTransactions.description] = "You invited a friend"
                it[com.bhanit.apps.echo.data.table.CreditTransactions.relatedId] = userId.toString()
                it[com.bhanit.apps.echo.data.table.CreditTransactions.createdAt] = Instant.now()
                it[com.bhanit.apps.echo.data.table.CreditTransactions.visibility] = com.bhanit.apps.echo.data.table.TransactionVisibility.VISIBLE
                it[com.bhanit.apps.echo.data.table.CreditTransactions.rewardSource] = com.bhanit.apps.echo.data.table.RewardSource.REFERRAL
                it[com.bhanit.apps.echo.data.table.CreditTransactions.intent] = com.bhanit.apps.echo.data.table.TransactionIntent.REWARD
            }
        }

        if (myReward > 0) {
             com.bhanit.apps.echo.data.table.CreditTransactions.insert {
                it[com.bhanit.apps.echo.data.table.CreditTransactions.userId] = userId
                it[com.bhanit.apps.echo.data.table.CreditTransactions.amount] = myReward
                it[com.bhanit.apps.echo.data.table.CreditTransactions.type] = TransactionType.SIGNUP_BONUS
                it[com.bhanit.apps.echo.data.table.CreditTransactions.description] = "Welcome Gift"
                it[com.bhanit.apps.echo.data.table.CreditTransactions.relatedId] = referrerId.toString()
                it[com.bhanit.apps.echo.data.table.CreditTransactions.createdAt] = Instant.now()
                it[com.bhanit.apps.echo.data.table.CreditTransactions.visibility] = com.bhanit.apps.echo.data.table.TransactionVisibility.VISIBLE
                it[com.bhanit.apps.echo.data.table.CreditTransactions.rewardSource] = com.bhanit.apps.echo.data.table.RewardSource.REFERRAL
                it[com.bhanit.apps.echo.data.table.CreditTransactions.intent] = com.bhanit.apps.echo.data.table.TransactionIntent.REWARD
            }

            // Record in History
            val myPhoneHash = currentUserRow?.get(Users.phoneHash)
            if (myPhoneHash != null) {
                val exists = RewardHistory.select(RewardHistory.phoneHash)
                    .where { (RewardHistory.phoneHash eq myPhoneHash) and (RewardHistory.rewardType eq OneTimeRewardType.SIGNUP_BONUS) }
                    .count() > 0
                
                if (!exists) {
                     RewardHistory.insert {
                        it[this.phoneHash] = myPhoneHash
                        it[this.rewardType] = OneTimeRewardType.SIGNUP_BONUS
                        it[this.claimedAt] = Instant.now()
                    }
                }
            }
        }

        true to referrerId
    }

    override suspend fun isOnboardingCompleted(userId: UUID): Boolean = dbQuery {
        Users.selectAll().where { Users.id eq userId }
            .map { it[Users.onboardingCompleted] }

            .singleOrNull() ?: false
    }

    override suspend fun updateConsent(userId: UUID) {
        dbQuery {
            // Check if first consent is already recorded
            val user = Users.selectAll().where { Users.id eq userId }.singleOrNull()
            val firstConsent = user?.get(Users.firstConsentAt)

            Users.update({ Users.id eq userId }) {
                if (firstConsent == null) {
                    it[Users.firstConsentAt] = Instant.now()
                }
                it[Users.lastConsentAt] = Instant.now()
                it[Users.consentVersion] = "v1"
            }
        }
    }

    override suspend fun resetUserData(userId: UUID): Boolean = dbQuery {
        try {
            // Reset to a state where they can onboard again
            // Rename username to free it up (optional, or just random)
            // Clear referral code
            // Clear onboarding flag
            
            val randomSuffix = (System.currentTimeMillis() % 100000).toString()
            val tempUsername = "reset_$randomSuffix"
            
            Users.update({ Users.id eq userId }) {
                it[Users.username] = tempUsername
                it[Users.referralCode] = null
                it[Users.onboardingCompleted] = false
                it[Users.credits] = 0
                it[Users.referredBy] = null
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun generateReferralCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }

    companion object {
        private const val REFERRAL_REWARD_REFERRER = 10
        private const val REFERRAL_REWARD_REFEREE = 5
    }

    override suspend fun getBlockedUsers(userId: UUID): List<ResultRow> = dbQuery {
        val blocks = com.bhanit.apps.echo.data.table.UserBlocks.selectAll()
            .where { com.bhanit.apps.echo.data.table.UserBlocks.blockerId eq userId }
            .toList()
            
        val blockedIds = blocks.map { it[com.bhanit.apps.echo.data.table.UserBlocks.blockedId] }
        
        Users.selectAll().where { Users.id inList blockedIds }.toList()
    }

    override suspend fun getBlockedBy(userId: UUID): List<ResultRow> = dbQuery {
        val blocks = com.bhanit.apps.echo.data.table.UserBlocks.selectAll()
            .where { com.bhanit.apps.echo.data.table.UserBlocks.blockedId eq userId }
            .toList()
            
        val blockerIds = blocks.map { it[com.bhanit.apps.echo.data.table.UserBlocks.blockerId] }
        
        Users.selectAll().where { Users.id inList blockerIds }.toList()
    }

    override suspend fun blockUser(blockerId: UUID, blockedId: UUID) {
        dbQuery {
            com.bhanit.apps.echo.data.table.UserBlocks.insert {
                it[com.bhanit.apps.echo.data.table.UserBlocks.blockerId] = blockerId
                it[com.bhanit.apps.echo.data.table.UserBlocks.blockedId] = blockedId
                it[com.bhanit.apps.echo.data.table.UserBlocks.createdAt] = Instant.now()
            }
        }
    }

    override suspend fun unblockUser(blockerId: UUID, blockedId: UUID) {
        dbQuery {
            // 1. Remove from UserBlocks
            com.bhanit.apps.echo.data.table.UserBlocks.deleteWhere {
                (com.bhanit.apps.echo.data.table.UserBlocks.blockerId eq blockerId) and 
                (com.bhanit.apps.echo.data.table.UserBlocks.blockedId eq blockedId)
            }
            
            // 2. Remove from Connections (to reset status and prevent unique constraint errors on re-connect)
            // Handle bi-directional check for Connections table (userA < userB typically, but deleteWhere handles criteria)
            val idA = if (blockerId < blockedId) blockerId else blockedId
            val idB = if (blockerId < blockedId) blockedId else blockerId

            com.bhanit.apps.echo.data.table.Connections.deleteWhere {
                (com.bhanit.apps.echo.data.table.Connections.userA eq idA) and 
                (com.bhanit.apps.echo.data.table.Connections.userB eq idB)
            }
        }
    }

    override suspend fun incrementTokenVersion(userId: UUID): Int = dbQuery {
        // Increment and return new version.
        // Exposed update returns count of updated rows, so we need to fetch after update.
        Users.update({ Users.id eq userId }) {
            with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                it[Users.tokenVersion] = Users.tokenVersion + 1
            }
        }
        
        Users.selectAll().where { Users.id eq userId }
            .map { it[Users.tokenVersion] }
            .single()
    }

    override suspend fun getUserTokenVersion(userId: UUID): Int = dbQuery {
        Users.selectAll().where { Users.id eq userId }
            .map { it[Users.tokenVersion] }
            .single()
    }

    override suspend fun findUsersByIds(ids: List<UUID>): List<ResultRow> = dbQuery {
        Users.selectAll().where { Users.id inList ids }
            .toList()
    }
    
    override suspend fun saveFcmToken(userId: UUID, token: String) {
        dbQuery {
            // Unbind this token from any other users to prevent cross-account notifications on same device
            Users.update({ (Users.fcmToken eq token) and (Users.id neq userId) }) {
                it[Users.fcmToken] = null
            }
            
            Users.update({ Users.id eq userId }) {
                it[Users.fcmToken] = token
            }
        }
    }

    override suspend fun getFcmToken(userId: UUID): String? = dbQuery {
        Users.selectAll().where { Users.id eq userId }
            .map { it[Users.fcmToken] }
            .singleOrNull()
    }

    override suspend fun getFcmTokenAndPlatform(userId: UUID): Pair<String?, String?> = dbQuery {
         Users.selectAll().where { Users.id eq userId }
            .map { it[Users.fcmToken] to it[Users.platform] }
            .singleOrNull() ?: (null to null)
    }

    override suspend fun clearFcmToken(userId: UUID): Unit = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[fcmToken] = null
        }
    }

    override suspend fun setUserInactive(userId: UUID): Unit = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[isActive] = false
        }
    }

    override suspend fun deleteUser(userId: UUID) {
        dbQuery {
            val user = Users.selectAll().where { Users.id eq userId }.singleOrNull() ?: return@dbQuery
            val originalPhone = user[Users.phoneNumber]
            val originalUsername = user[Users.username]
            val timestamp = System.currentTimeMillis()
            val randomSuffix = (1000..9999).random() // 4 digits

            // 0. Sync Legacy Rewards to RewardHistory (Before Anonymization)
            // We scan for rewards they HAVE, and ensure they are in the history table.
            val myPhoneHash = user[Users.phoneHash]
            
            // A. Signup Bonus
            val hasSignupParams = com.bhanit.apps.echo.data.table.CreditTransactions
                .select(com.bhanit.apps.echo.data.table.CreditTransactions.id)
                .where { 
                    (com.bhanit.apps.echo.data.table.CreditTransactions.userId eq userId) and 
                    (com.bhanit.apps.echo.data.table.CreditTransactions.type eq TransactionType.SIGNUP_BONUS) 
                }
                .count() > 0
                
            if (hasSignupParams) {
                 val exists = RewardHistory.select(RewardHistory.phoneHash)
                    .where { (RewardHistory.phoneHash eq myPhoneHash) and (RewardHistory.rewardType eq OneTimeRewardType.SIGNUP_BONUS) }
                    .count() > 0
                 if (!exists) {
                     RewardHistory.insert {
                         it[this.phoneHash] = myPhoneHash
                         it[this.rewardType] = OneTimeRewardType.SIGNUP_BONUS
                         it[this.claimedAt] = Instant.now()
                     }
                 }
            }
            
            // B. First Echo
            val hasFirstEcho = com.bhanit.apps.echo.data.table.CreditTransactions
                .select(com.bhanit.apps.echo.data.table.CreditTransactions.id)
                .where { 
                    (com.bhanit.apps.echo.data.table.CreditTransactions.userId eq userId) and 
                    (com.bhanit.apps.echo.data.table.CreditTransactions.relatedId eq "FIRST_TIME_ECHO") 
                }
                .count() > 0
                
            if (hasFirstEcho) {
                 val exists = RewardHistory.select(RewardHistory.phoneHash)
                    .where { (RewardHistory.phoneHash eq myPhoneHash) and (RewardHistory.rewardType eq OneTimeRewardType.FIRST_ECHO) }
                    .count() > 0
                 if (!exists) {
                     RewardHistory.insert {
                         it[this.phoneHash] = myPhoneHash
                         it[this.rewardType] = OneTimeRewardType.FIRST_ECHO
                         it[this.claimedAt] = Instant.now()
                     }
                 }
            }
            
            // C. First Response
            val hasFirstResponse = com.bhanit.apps.echo.data.table.CreditTransactions
                .select(com.bhanit.apps.echo.data.table.CreditTransactions.id)
                .where { 
                    (com.bhanit.apps.echo.data.table.CreditTransactions.userId eq userId) and 
                    (com.bhanit.apps.echo.data.table.CreditTransactions.relatedId eq "FIRST_TIME_RESPONSE") 
                }
                .count() > 0
                
            if (hasFirstResponse) {
                 val exists = RewardHistory.select(RewardHistory.phoneHash)
                    .where { (RewardHistory.phoneHash eq myPhoneHash) and (RewardHistory.rewardType eq OneTimeRewardType.FIRST_RESPONSE) }
                    .count() > 0
                 if (!exists) {
                     RewardHistory.insert {
                         it[this.phoneHash] = myPhoneHash
                         it[this.rewardType] = OneTimeRewardType.FIRST_RESPONSE
                         it[this.claimedAt] = Instant.now()
                     }
                 }
            }


            // 1. Anonymize User Data (Free up unique constraints)
            // Constraints: phoneNumber varchar(20), username varchar(20)
            // Format: d{timestamp}{random} = 1 + 13 + 4 = 18 chars. Fits.
            
            Users.update({ Users.id eq userId }) {
                it[isDeleted] = true
                it[isActive] = false
                it[deletedAt] = Instant.now()
                it[phoneNumber] = "d${timestamp}${randomSuffix}" 
                it[phoneHash] = "del_${timestamp}_${UUID.randomUUID()}" // varchar(64) - Fits
                it[username] = "d${timestamp}${randomSuffix}"  
                it[email] = null // Clear email if exists
                it[fcmToken] = null // Stop notifications
                it[profilePhoto] = null
                // Invalidate session
                it[tokenVersion] = (user[tokenVersion] + 1)
            }

            // 2. Cleanup Relations
            // Remove connections to prevent showing up in others' lists
            com.bhanit.apps.echo.data.table.Connections.deleteWhere {
                (com.bhanit.apps.echo.data.table.Connections.userA eq userId) or 
                (com.bhanit.apps.echo.data.table.Connections.userB eq userId)
            }
            
            // Remove synced contacts
            com.bhanit.apps.echo.data.table.UserContacts.deleteWhere {
                com.bhanit.apps.echo.data.table.UserContacts.userId eq userId
            }
            
            // Note: We keep Messages, Credits, etc. for history/legal reasons, 
            // but since the user is "deleted" and relations are gone, they are effectively inaccessible to others.
        }
    }


    override suspend fun getCreditHistory(
        userId: UUID,
        page: Int,
        pageSize: Int,
        query: String?,
        filter: com.bhanit.apps.echo.data.model.TransactionFilter?,
        startDate: Long?,
        endDate: Long?
    ): List<com.bhanit.apps.echo.data.model.CreditTransaction> = dbQuery {
        
        val transactionsQuery = com.bhanit.apps.echo.data.table.CreditTransactions.selectAll()
            .where { com.bhanit.apps.echo.data.table.CreditTransactions.userId eq userId }
            .andWhere { com.bhanit.apps.echo.data.table.CreditTransactions.visibility eq com.bhanit.apps.echo.data.table.TransactionVisibility.VISIBLE }

        // Filter
        if (filter != null) {
             transactionsQuery.adjustWhere {
                val old = this
                val newOp = when (filter) {
                    com.bhanit.apps.echo.data.model.TransactionFilter.EARNED -> {
                        com.bhanit.apps.echo.data.table.CreditTransactions.amount greater 0
                    }
                    com.bhanit.apps.echo.data.model.TransactionFilter.SPENT -> {
                        com.bhanit.apps.echo.data.table.CreditTransactions.amount less 0
                    }
                    else -> Op.TRUE
                }
                old?.and(newOp) ?: newOp
            }
        }

        // Search
        if (!query.isNullOrBlank()) {
             transactionsQuery.adjustWhere {
                 val old = this
                 val searchOp = LowerCase(com.bhanit.apps.echo.data.table.CreditTransactions.description) like "%${query.lowercase()}%"
                 old?.and(searchOp) ?: searchOp
             }
        }

        if (startDate != null || endDate != null) {
            val start = startDate?.let { java.time.Instant.ofEpochMilli(it) }
            val end = endDate?.let { java.time.Instant.ofEpochMilli(it) }
            
            transactionsQuery.adjustWhere {
                val old = this
                var dateOp: Op<Boolean>? = null
                
                if (start != null) {
                    dateOp = com.bhanit.apps.echo.data.table.CreditTransactions.createdAt greaterEq start
                }
                
                if (end != null) {
                    val endOp = com.bhanit.apps.echo.data.table.CreditTransactions.createdAt lessEq end
                    dateOp = dateOp?.and(endOp) ?: endOp
                }
                
                if (dateOp != null) {
                    old?.and(dateOp) ?: dateOp
                } else {
                    old ?: Op.TRUE
                }
            }
        }

        val transactions = transactionsQuery
            .orderBy(com.bhanit.apps.echo.data.table.CreditTransactions.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            .toList()

        // Gather related user IDs for dynamic username fetching
        val relatedUserIds = transactions.mapNotNull {
            val type = it[com.bhanit.apps.echo.data.table.CreditTransactions.type]
            if (type == TransactionType.REFERRAL_REWARD || type == TransactionType.SIGNUP_BONUS || type == TransactionType.PURCHASE) {
                it[com.bhanit.apps.echo.data.table.CreditTransactions.relatedId]?.let { idStr ->
                    try {
                        UUID.fromString(idStr)
                    } catch (e: Exception) {
                        null 
                    }
                }
            } else {
                null
            }
        }.distinct()

        val usernames = if (relatedUserIds.isNotEmpty()) {
            Users.selectAll().where { Users.id inList relatedUserIds }
                .associate { it[Users.id].value.toString() to it[Users.username] }
        } else {
            emptyMap()
        }

        transactions.map { row ->
            val type = row[com.bhanit.apps.echo.data.table.CreditTransactions.type]
            val relatedId = row[com.bhanit.apps.echo.data.table.CreditTransactions.relatedId]
            var description = row[com.bhanit.apps.echo.data.table.CreditTransactions.description]

            if (relatedId != null) {
                 val currentUsername = usernames[relatedId]
                 if (currentUsername != null) {
                     when (type) {
                         TransactionType.REFERRAL_REWARD -> description = "Referral Bonus: Joined by @$currentUsername"
                         TransactionType.SIGNUP_BONUS -> description = "Signup Bonus: Referred by @$currentUsername"
                         TransactionType.PURCHASE -> {
                             // Detect if it was a Limit Boost purchase
                             if (description.startsWith("Limit Boost")) {
                                 description = "Limit Boost for @$currentUsername"
                             }
                         }
                         else -> {} 
                     }
                 }
            }

            com.bhanit.apps.echo.data.model.CreditTransaction(
                id = row[com.bhanit.apps.echo.data.table.CreditTransactions.id].value.toString(),
                amount = row[com.bhanit.apps.echo.data.table.CreditTransactions.amount],
                type = type,
                description = description,
                relatedId = relatedId,
                timestamp = row[com.bhanit.apps.echo.data.table.CreditTransactions.createdAt].toEpochMilli()
            )
        }
    }

    override suspend fun purchaseItem(userId: UUID, request: com.bhanit.apps.echo.data.model.PurchaseRequest, cost: Int): Boolean = dbQuery {
        // 1. Check Balance
        val userRow = Users.selectAll().where { Users.id eq userId }.singleOrNull() ?: return@dbQuery false
        val currentCredits = userRow[Users.credits]
        
        if (currentCredits < cost) {
            throw IllegalArgumentException("Insufficient credits.")
        }

        // 2. Apply Purchase Logic
        when (request.type) {
            com.bhanit.apps.echo.data.model.PurchaseType.EMOTION_UNLOCK -> {
                if (request.itemId.isBlank()) throw IllegalArgumentException("Emotion ID required.")
                val durationDays = 7
                val expiry = Instant.now().plusSeconds(durationDays * 24 * 3600L)
                
                val existing = com.bhanit.apps.echo.data.table.UserUnlockedEmotions.selectAll()
                    .where { (com.bhanit.apps.echo.data.table.UserUnlockedEmotions.userId eq userId) and (com.bhanit.apps.echo.data.table.UserUnlockedEmotions.emotionId eq request.itemId) }
                    .singleOrNull()

                if (existing != null) {
                    com.bhanit.apps.echo.data.table.UserUnlockedEmotions.update({ (com.bhanit.apps.echo.data.table.UserUnlockedEmotions.userId eq userId) and (com.bhanit.apps.echo.data.table.UserUnlockedEmotions.emotionId eq request.itemId) }) {
                        it[expiresAt] = expiry
                        it[usageLimit] = null // Clear any limits, purely time based now
                    }
                } else {
                    com.bhanit.apps.echo.data.table.UserUnlockedEmotions.insert {
                        it[com.bhanit.apps.echo.data.table.UserUnlockedEmotions.userId] = userId
                        it[emotionId] = request.itemId
                        it[unlockedAt] = Instant.now()
                        it[expiresAt] = expiry
                        it[usageLimit] = null
                    }
                }
            }
            com.bhanit.apps.echo.data.model.PurchaseType.LIMIT_BOOST -> {
                if (request.targetUserId.isNullOrBlank()) throw IllegalArgumentException("Target User ID required.")
                val targetUuid = UUID.fromString(request.targetUserId)
                
                val existing = com.bhanit.apps.echo.data.table.UserRelationLimits.selectAll()
                    .where { (com.bhanit.apps.echo.data.table.UserRelationLimits.senderId eq userId) and (com.bhanit.apps.echo.data.table.UserRelationLimits.receiverId eq targetUuid) }
                    .singleOrNull()
                
                // Expiry Logic: Max(Now, CurrentExpiry) + 24 Hours
                val currentExpiry = existing?.get(com.bhanit.apps.echo.data.table.UserRelationLimits.boostExpiresAt)
                val isBoostActive = currentExpiry != null && currentExpiry.isAfter(Instant.now())
                
                val baseTime = if (isBoostActive) currentExpiry!! else Instant.now()
                val newExpiry = baseTime.plusSeconds(24 * 3600L)
                
                // Count Logic: If active, increment. Else 1.
                val currentCount = if (isBoostActive) existing?.get(com.bhanit.apps.echo.data.table.UserRelationLimits.boostCount) ?: 0 else 0
                val newCount = currentCount + 1

                if (existing != null) {
                    com.bhanit.apps.echo.data.table.UserRelationLimits.update({ (com.bhanit.apps.echo.data.table.UserRelationLimits.senderId eq userId) and (com.bhanit.apps.echo.data.table.UserRelationLimits.receiverId eq targetUuid) }) {
                         it[boostExpiresAt] = newExpiry
                         it[boostCount] = newCount
                         it[updatedAt] = Instant.now()
                    }
                } else {
                    com.bhanit.apps.echo.data.table.UserRelationLimits.insert {
                        it[senderId] = userId
                        it[receiverId] = targetUuid
                        it[dailyLimit] = 3 // Default
                        it[boostExpiresAt] = newExpiry
                        it[boostCount] = newCount
                        it[updatedAt] = Instant.now()
                    }
                }
            }
            com.bhanit.apps.echo.data.model.PurchaseType.EMOTION_TIME -> {
                if (request.itemId.isBlank()) throw IllegalArgumentException("Emotion ID required.")
                val durationHours = request.durationHours ?: 24
                
                // Upsert logic for UserUnlockedEmotions
                val existing = com.bhanit.apps.echo.data.table.UserUnlockedEmotions.selectAll()
                    .where { (com.bhanit.apps.echo.data.table.UserUnlockedEmotions.userId eq userId) and (com.bhanit.apps.echo.data.table.UserUnlockedEmotions.emotionId eq request.itemId) }
                    .singleOrNull()

                if (existing != null) {
                    com.bhanit.apps.echo.data.table.UserUnlockedEmotions.update({ (com.bhanit.apps.echo.data.table.UserUnlockedEmotions.userId eq userId) and (com.bhanit.apps.echo.data.table.UserUnlockedEmotions.emotionId eq request.itemId) }) {
                        it[expiresAt] = Instant.now().plusSeconds(durationHours * 3600L)
                        // If switching from limit to time, maybe clear usage limit? Or keep it?
                        it[usageLimit] = null // Override count limit with time-based access
                    }
                } else {
                    com.bhanit.apps.echo.data.table.UserUnlockedEmotions.insert {
                        it[com.bhanit.apps.echo.data.table.UserUnlockedEmotions.userId] = userId
                        it[emotionId] = request.itemId
                        it[unlockedAt] = Instant.now()
                        it[expiresAt] = Instant.now().plusSeconds(durationHours * 3600L)
                        it[usageLimit] = null // Infinite during time
                    }
                }
            }
            com.bhanit.apps.echo.data.model.PurchaseType.EMOTION_COUNT -> {
                 if (request.itemId.isBlank()) throw IllegalArgumentException("Emotion ID required.")
                 val countToAdd = request.count ?: 1
                 
                 val existing = com.bhanit.apps.echo.data.table.UserUnlockedEmotions.selectAll()
                    .where { (com.bhanit.apps.echo.data.table.UserUnlockedEmotions.userId eq userId) and (com.bhanit.apps.echo.data.table.UserUnlockedEmotions.emotionId eq request.itemId) }
                    .singleOrNull()

                 if (existing != null) {
                    com.bhanit.apps.echo.data.table.UserUnlockedEmotions.update({ (com.bhanit.apps.echo.data.table.UserUnlockedEmotions.userId eq userId) and (com.bhanit.apps.echo.data.table.UserUnlockedEmotions.emotionId eq request.itemId) }) {
                        with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                            val currentLimit = existing[usageLimit] ?: 0
                            it[usageLimit] = currentLimit + countToAdd
                        }
                    }
                 } else {
                     com.bhanit.apps.echo.data.table.UserUnlockedEmotions.insert {
                        it[com.bhanit.apps.echo.data.table.UserUnlockedEmotions.userId] = userId
                        it[emotionId] = request.itemId
                        it[unlockedAt] = Instant.now()
                        it[usageLimit] = countToAdd
                    }
                 }
            }
            com.bhanit.apps.echo.data.model.PurchaseType.RELATION_QUOTA -> {
                if (request.targetUserId.isNullOrBlank()) throw IllegalArgumentException("Target User ID required.")
                val targetUuid = UUID.fromString(request.targetUserId)
                val countToAdd = request.count ?: 1
                
                val existing = com.bhanit.apps.echo.data.table.UserRelationLimits.selectAll()
                    .where { (com.bhanit.apps.echo.data.table.UserRelationLimits.senderId eq userId) and (com.bhanit.apps.echo.data.table.UserRelationLimits.receiverId eq targetUuid) }
                    .singleOrNull()
                
                if (existing != null) {
                    com.bhanit.apps.echo.data.table.UserRelationLimits.update({ (com.bhanit.apps.echo.data.table.UserRelationLimits.senderId eq userId) and (com.bhanit.apps.echo.data.table.UserRelationLimits.receiverId eq targetUuid) }) {
                         with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                            it[dailyLimit] = dailyLimit + countToAdd
                            it[updatedAt] = Instant.now()
                         }
                    }
                } else {
                    com.bhanit.apps.echo.data.table.UserRelationLimits.insert {
                        it[senderId] = userId
                        it[receiverId] = targetUuid
                        it[dailyLimit] = 3 + countToAdd // Default 3 + boost
                        it[updatedAt] = Instant.now()
                    }
                }
            }
        }

        // 3. Deduct Credits
        Users.update({ Users.id eq userId }) {
            with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                it[Users.credits] = Users.credits - cost
            }
        }

        // 4. Record Transaction
        // 4. Record Transaction
        var description = "Purchase: ${request.type} ${request.itemId.ifBlank { request.targetUserId ?: "" }}"
        
        if (request.type == com.bhanit.apps.echo.data.model.PurchaseType.LIMIT_BOOST && !request.targetUserId.isNullOrBlank()) {
             val targetUser = Users.selectAll().where { Users.id eq UUID.fromString(request.targetUserId) }.singleOrNull()
             val targetUsername = targetUser?.get(Users.username) ?: "Unknown"
             description = "Limit Boost for @$targetUsername"
        }

        com.bhanit.apps.echo.data.table.CreditTransactions.insert {
            it[com.bhanit.apps.echo.data.table.CreditTransactions.userId] = userId
            it[amount] = -cost
            it[type] = com.bhanit.apps.echo.data.model.TransactionType.PURCHASE
            it[this.description] = description
            it[relatedId] = request.itemId.ifBlank { request.targetUserId }
            it[createdAt] = Instant.now()
            it[visibility] = com.bhanit.apps.echo.data.table.TransactionVisibility.VISIBLE
            it[rewardSource] = com.bhanit.apps.echo.data.table.RewardSource.PURCHASE
            it[intent] = com.bhanit.apps.echo.data.table.TransactionIntent.SPEND
        }
        
        true
    }

    override suspend fun searchUsers(query: String?, limit: Int, offset: Long, includePhone: Boolean, exactMatch: Boolean, currentUserId: UUID?): List<ResultRow> = dbQuery {
        val usersQuery = Users.selectAll()

        if (currentUserId != null) {
            // OPTIMIZATION: Use Subquery to exclude blocks instead of passing large lists
            val blockedByMe = com.bhanit.apps.echo.data.table.UserBlocks.select(com.bhanit.apps.echo.data.table.UserBlocks.blockedId)
                .where { com.bhanit.apps.echo.data.table.UserBlocks.blockerId eq currentUserId }
            
            val blockedMe = com.bhanit.apps.echo.data.table.UserBlocks.select(com.bhanit.apps.echo.data.table.UserBlocks.blockerId) // Select blockerId because THAT user blocked ME
                .where { com.bhanit.apps.echo.data.table.UserBlocks.blockedId eq currentUserId }

            usersQuery.andWhere { Users.id notInSubQuery blockedByMe }
            usersQuery.andWhere { Users.id notInSubQuery blockedMe }
            
            // Also exclude self
            usersQuery.andWhere { Users.id neq currentUserId }
        }
        
        if (!query.isNullOrBlank()) {
             usersQuery.adjustWhere {
                 val old = this
                 var searchOp: Op<Boolean> = if (exactMatch) {
                     LowerCase(Users.username) eq query.lowercase()
                 } else {
                     LowerCase(Users.username) like "%${query.lowercase()}%"
                 }
                 
                 if (includePhone) {
                     searchOp = searchOp or (Users.phoneNumber like "%${query}%")
                 }
                 
                 old?.and(searchOp) ?: searchOp
             }
        }
        
        usersQuery
            .orderBy(Users.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(limit, offset = offset)
            .toList()
    }

    override suspend fun grantCredits(adminId: UUID, targetUserId: UUID, amount: Int, reason: String, notes: String?): UUID = dbQuery {
        // 1. Update User Credits
        Users.update({ Users.id eq targetUserId }) {
             with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                 it[Users.credits] = Users.credits + amount
             }
        }
        
        // 2. Insert Transaction
        com.bhanit.apps.echo.data.table.CreditTransactions.insertAndGetId {
            it[com.bhanit.apps.echo.data.table.CreditTransactions.userId] = targetUserId
            it[com.bhanit.apps.echo.data.table.CreditTransactions.amount] = amount
            it[com.bhanit.apps.echo.data.table.CreditTransactions.type] = TransactionType.MANUAL_ADJUSTMENT
            it[com.bhanit.apps.echo.data.table.CreditTransactions.description] = reason
            it[com.bhanit.apps.echo.data.table.CreditTransactions.relatedId] = adminId.toString()
            it[com.bhanit.apps.echo.data.table.CreditTransactions.createdAt] = Instant.now()
            it[com.bhanit.apps.echo.data.table.CreditTransactions.visibility] = com.bhanit.apps.echo.data.table.TransactionVisibility.VISIBLE
            it[com.bhanit.apps.echo.data.table.CreditTransactions.intent] = com.bhanit.apps.echo.data.table.TransactionIntent.REWARD
        }.value
    }

    override suspend fun getUserDetails(userId: UUID): ResultRow? = dbQuery {
        Users.selectAll().where { Users.id eq userId }.singleOrNull()
    }
    
    override suspend fun findUserIdsByFilter(daysInactive: Int?, hasCompletedOnboarding: Boolean?, minVersion: String?, platform: String?): List<UUID> = dbQuery {
        // If we have version/platform filters, we need those columns.
        // Simplest approach: Select ID and relevant columns, filter, then map to ID.
        
        val query = Users.selectAll()
        
        if (daysInactive != null) {
            val cutoff = java.time.Instant.now().minusSeconds(daysInactive * 24 * 3600L)
            query.andWhere { Users.lastActiveAt less cutoff }
        }
        
        if (hasCompletedOnboarding != null) {
            query.andWhere { Users.onboardingCompleted eq hasCompletedOnboarding }
        }
        
        if (platform != null) {
             query.andWhere { LowerCase(Users.platform) like "%${platform.lowercase()}%" }
        }
        
        query.andWhere { Users.isActive eq true }
        
        val results = query.map { 
            val id = it[Users.id].value
            val v = it[Users.appVersion]
            id to v
        }
        
        if (minVersion != null) {
            results.filter { (_, version) ->
                compareVersions(version, minVersion) >= 0
            }.map { it.first }
        } else {
            results.map { it.first }
        }
    }

    override suspend fun updateDeviceMetadata(userId: UUID, platform: String, appVersion: String, osVersion: String, deviceModel: String, locale: String, timezone: String) {
        dbQuery {
            Users.update({ Users.id eq userId }) {
                it[Users.platform] = platform
                it[Users.appVersion] = appVersion
                it[Users.osVersion] = osVersion
                it[Users.deviceModel] = deviceModel
                it[Users.locale] = locale
                it[Users.timezone] = timezone
                it[Users.isActive] = true // Implicitly active if syncing
                it[Users.lastActiveAt] = java.time.Instant.now()
            }
        }
    }



    override suspend fun findUserByReferralCode(code: String): UUID? = dbQuery {
        Users.selectAll().where { Users.referralCode eq code }
            .map { it[Users.id].value }
            .singleOrNull()
    }

    override suspend fun getReferralBaseUrl(): String = dbQuery {
        val row = SystemConfig.selectAll().where { SystemConfig.key eq "referral_base_url" }.singleOrNull()
        row?.get(SystemConfig.value) ?: "https://echo.app" // Default Fallback, remove /r from default if we append it later
    }

    private fun compareVersions(v1: String?, v2: String): Int {
        if (v1 == null) return -1 // Null version < Min Version
        val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        val length = maxOf(parts1.size, parts2.size)
        
        for (i in 0 until length) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) {
                return num1 - num2
            }
        }
        return 0
    }

    override suspend fun recordTransaction(
        userId: UUID, 
        type: TransactionType, 
        amount: Int, 
        relatedId: String?, 
        notes: String?,
        visibility: com.bhanit.apps.echo.data.table.TransactionVisibility,
        source: com.bhanit.apps.echo.data.table.RewardSource,
        intent: com.bhanit.apps.echo.data.table.TransactionIntent,
        metadata: String?
    ): UUID = dbQuery {
        com.bhanit.apps.echo.data.table.CreditTransactions.insertAndGetId {
            it[com.bhanit.apps.echo.data.table.CreditTransactions.userId] = userId
            it[com.bhanit.apps.echo.data.table.CreditTransactions.amount] = amount
            it[com.bhanit.apps.echo.data.table.CreditTransactions.type] = type
            it[com.bhanit.apps.echo.data.table.CreditTransactions.description] = notes ?: type.name
            it[com.bhanit.apps.echo.data.table.CreditTransactions.relatedId] = relatedId
            it[com.bhanit.apps.echo.data.table.CreditTransactions.createdAt] = Instant.now()
            it[com.bhanit.apps.echo.data.table.CreditTransactions.visibility] = visibility
            it[com.bhanit.apps.echo.data.table.CreditTransactions.rewardSource] = source
            it[com.bhanit.apps.echo.data.table.CreditTransactions.intent] = intent
            it[com.bhanit.apps.echo.data.table.CreditTransactions.metadataJson] = metadata
        }.value
    }

    override suspend fun getActiveUsersWithMetadata(): List<Triple<java.util.UUID, String, String?>> = dbQuery {
        com.bhanit.apps.echo.data.table.Users.join(com.bhanit.apps.echo.data.table.UserActivityMeta, org.jetbrains.exposed.sql.JoinType.LEFT, com.bhanit.apps.echo.data.table.Users.id, com.bhanit.apps.echo.data.table.UserActivityMeta.userId)
            .selectAll()
            .where { (com.bhanit.apps.echo.data.table.Users.isActive eq true) and (com.bhanit.apps.echo.data.table.Users.timezone.isNotNull()) }
            .map { 
                Triple(
                    it[com.bhanit.apps.echo.data.table.Users.id].value, 
                    it[com.bhanit.apps.echo.data.table.Users.timezone]!!, 
                    it.getOrNull(com.bhanit.apps.echo.data.table.UserActivityMeta.seasonRewardMeta)
                ) 
            }
    }

    override suspend fun markSeasonAnnounced(userId: UUID, seasonId: String) = dbQuery {
        val meta = com.bhanit.apps.echo.data.table.UserActivityMeta.selectAll().where { com.bhanit.apps.echo.data.table.UserActivityMeta.userId eq userId }.singleOrNull()

        
        if (meta != null) {
            val currentJson = meta[com.bhanit.apps.echo.data.table.UserActivityMeta.seasonRewardMeta]
            val newJson = if (currentJson == null) {
                Json.encodeToString(listOf(seasonId))
            } else {
                try {
                    val list = Json.decodeFromString<MutableList<String>>(currentJson)
                    if (!list.contains(seasonId)) list.add(seasonId)
                    Json.encodeToString(list)
                } catch(e: Exception) {
                    Json.encodeToString(listOf(seasonId))
                }
            }
            com.bhanit.apps.echo.data.table.UserActivityMeta.update({ com.bhanit.apps.echo.data.table.UserActivityMeta.userId eq userId }) {
                it[seasonRewardMeta] = newJson
            }
            Unit
        } else {
            // Should exist due to getActiveUsersWithMetadata join, but insert safe fallback
            com.bhanit.apps.echo.data.table.UserActivityMeta.insert {
                 it[com.bhanit.apps.echo.data.table.UserActivityMeta.userId] = userId
                 it[seasonRewardMeta] = Json.encodeToString(listOf(seasonId))
            }
            Unit
        }
    }

    override suspend fun updateNotificationSettings(
        userId: UUID, 
        notifyFeelings: Boolean?, 
        notifyCheckins: Boolean?, 
        notifyReflections: Boolean?, 
        notifyRewards: Boolean?, 
        notifyInactiveReminders: Boolean?
    ) = dbQuery {
        Users.update({ Users.id eq userId }) {
            if (notifyFeelings != null) it[Users.notifyFeelings] = notifyFeelings
            if (notifyCheckins != null) it[Users.notifyCheckins] = notifyCheckins
            if (notifyReflections != null) it[Users.notifyReflections] = notifyReflections
            if (notifyRewards != null) it[Users.notifyRewards] = notifyRewards
            if (notifyInactiveReminders != null) it[Users.notifyInactiveReminders] = notifyInactiveReminders
        }
        Unit
    }

    override suspend fun resetNotificationSettings(userId: UUID) = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[notifyFeelings] = true
            it[notifyCheckins] = true
            it[notifyReflections] = true
            it[notifyRewards] = true
            it[notifyInactiveReminders] = true
        }
        Unit
    }
}
