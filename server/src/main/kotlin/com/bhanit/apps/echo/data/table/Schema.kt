package com.bhanit.apps.echo.data.table

import com.bhanit.apps.echo.data.model.TransactionType
import com.bhanit.apps.echo.data.model.TicketStatus
import com.bhanit.apps.echo.data.model.TicketPriority
import com.bhanit.apps.echo.data.model.TicketCategory
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.dao.id.UUIDTable

// ... existing code ...

object Users : UUIDTable("users") {
    val phoneNumber = varchar("phone_number", 20).uniqueIndex() // Indexed & Unique
    val phoneHash = varchar("phone_hash", 64).uniqueIndex() // SHA-256 Hash for discovery
    val fullName = varchar("full_name", 50).nullable()
    val username = varchar("username", 20).uniqueIndex() // Changed to 20 chars
    val referralCode = varchar("referral_code", 8).uniqueIndex().nullable()
    val referredBy = reference("referred_by", Users).nullable()
    val createdAt = timestamp("created_at")
    val lastActiveAt = timestamp("last_active_at").nullable()
    val isActive = bool("is_active").default(true)
    val isShadowBanned = bool("is_shadow_banned").default(false)
    val credits = integer("credits").default(0)
    val gender = varchar("gender", 20).nullable()
    val email = varchar("email", 255).nullable()
    val profilePhoto = text("profile_photo").nullable()
    val fcmToken = varchar("fcm_token", 512).nullable().index("idx_users_fcm_token") // Indexed for fast unbinding
    val tokenVersion = integer("token_version").default(0) // For Single Session
    val onboardingCompleted = bool("onboarding_completed").default(false)
    val consentVersion = varchar("consent_version", 20).default("v1")
    val firstConsentAt = timestamp("first_consent_at").nullable()
    val lastConsentAt = timestamp("last_consent_at").default(java.time.Instant.now())
    
    // Privacy Settings
    val allowQrAutoConnect = bool("allow_qr_auto_connect").default(true)
    
    // Notification Settings
    val notifyFeelings = bool("notify_feelings").default(true)
    val notifyCheckins = bool("notify_checkins").default(true)
    val notifyReflections = bool("notify_reflections").default(true)
    val notifyRewards = bool("notify_rewards").default(true)
    val notifyInactiveReminders = bool("notify_inactive_reminders").default(true)
    
    // Device Metadata
    val platform = varchar("platform", 20).nullable().index() // ANDROID, IOS, WEB
    val appVersion = varchar("app_version", 20).nullable()
    val osVersion = varchar("os_version", 20).nullable()
    val deviceModel = varchar("device_model", 50).nullable()
    val locale = varchar("locale", 20).nullable()
    val timezone = varchar("timezone", 50).nullable()
    
    // Soft Delete
    val isDeleted = bool("is_deleted").default(false)
    val deletedAt = timestamp("deleted_at").nullable()

    init {
        index("idx_users_last_active", false, lastActiveAt)
    }
}

object UserContacts : UUIDTable("user_contacts") {
    val userId = reference("user_id", Users)
    val phoneHash = varchar("phone_hash", 64).index() // Indexed for fast join
    val localName = varchar("local_name", 100).nullable()
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(userId, phoneHash) // Prevent duplicates per user
    }
}

object Connections : UUIDTable("connections") {
    val userA = reference("user_a", Users)
    val userB = reference("user_b", Users)
    val status = enumerationByName("status", 20, ConnectionStatus::class)

    val initiatedBy = reference("initiated_by", Users)
    val createdAt = timestamp("created_at")

    // We will handle "LEAST/GREATEST" logic in Repository to ensure A < B uniqueness
    init {
        uniqueIndex(userA, userB)
    }
}
object Messages : UUIDTable("messages") {
    val senderId = reference("sender_id", Users)
    val receiverId = reference("receiver_id", Users)
    // Emotions is a standard Table (String PK), not UUIDTable. Use explicit definition.
    val feelingId = varchar("feeling_id", 50).references(Emotions.id, onDelete = ReferenceOption.RESTRICT)
    val content = text("content")
    val isAnonymous = bool("is_anonymous").default(true)
    val moderationStatus = enumerationByName("moderation_status", 20, ModerationStatus::class).default(ModerationStatus.CLEAN)
    val deliveryStatus = enumerationByName("delivery_status", 20, DeliveryStatus::class).default(DeliveryStatus.SENT)
    val createdAt = timestamp("created_at")
    val isRead = bool("is_read").default(false)

    init {
        index("idx_messages_receiver_created", false, receiverId, createdAt)
        index("idx_messages_sender_created", false, senderId, createdAt)
        // High-performance index for history filtering between two specific users
        index("idx_messages_sender_receiver_created", false, senderId, receiverId, createdAt)
    }
}

object Replies : UUIDTable("replies") {
    val messageId = reference("message_id", Messages).uniqueIndex()
    val senderId = reference("sender_id", Users) // The one who replies (original receiver)
    val content = text("content")
    val createdAt = timestamp("created_at")
}

object DailyStats : Table("daily_stats") {
    val userId = reference("user_id", Users)
    val date = date("date")
    val sentCount = integer("sent_count").default(0)
    val receivedCount = integer("received_count").default(0)
    
    override val primaryKey = PrimaryKey(userId, date)
}

object Referrals : Table("referrals") {
    val referrerId = reference("referrer_id", Users)
    val referredUserId = reference("referred_user_id", Users)
    val status = enumerationByName("status", 20, ReferralStatus::class)
    val rewardCredited = bool("reward_credited").default(false)
}

object CreditTransactions : UUIDTable("credit_transactions") {
    val userId = reference("user_id", Users)
    val amount = integer("amount")
    val type = enumerationByName("type", 30, TransactionType::class)
    val description = text("description")
    val relatedId = varchar("related_id", 100).nullable() // e.g., referrerUserId or emotionId
    val createdAt = timestamp("created_at")

    // Minor Improvements: Explicit Visibility & Analytics Source
    val visibility = enumerationByName("visibility", 20, TransactionVisibility::class).default(TransactionVisibility.VISIBLE)
    val rewardSource = enumerationByName("reward_source", 20, RewardSource::class).default(RewardSource.SYSTEM)
    
    // Refinements: Intent & Explainability
    val intent = enumerationByName("intent", 20, TransactionIntent::class).default(TransactionIntent.REWARD)
    val metadataJson = text("metadata_json").nullable() // For explainability

    init {
        index("idx_credits_user_type_created", false, userId, type, createdAt)
        index("idx_credits_user_visible", false, userId, visibility, createdAt)
        // Optimization: Fast Idempotency Checks
        index("idx_credits_user_related", false, userId, relatedId)
    }
}

object UserBlocks : Table("user_blocks") {
    val blockerId = reference("blocker_id", Users).index("idx_blocks_blocker")
    val blockedId = reference("blocked_id", Users).index("idx_blocks_blocked")
    val createdAt = timestamp("created_at")
}

object Reports : Table("reports") {
    val reporterId = reference("reporter_id", Users)
    val messageId = reference("message_id", Messages)
    val reason = text("reason")
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(reporterId, messageId)
    }
}

object Otps : Table("otps") {
    val phoneNumber = varchar("phone_number", 20)
    val otpCode = varchar("otp_code", 10)
    val expiresAt = timestamp("expires_at")
    
    override val primaryKey = PrimaryKey(phoneNumber)
}

object Emotions : Table("emotions") {
    val id = varchar("id", 50) // e.g. "APPRECIATION"
    val displayName = varchar("display_name", 100)
    val colorHex = long("color_hex") // Store as Long (0xFF...)
    val exampleText = text("example_text")
    val isDefault = bool("is_default").default(false)
    val createdAt = timestamp("created_at")
    val price = integer("price").default(0) // Cost in credits (0 = Free)
    val sentiment = enumerationByName("sentiment", 20, EmotionSentiment::class).default(EmotionSentiment.REFLECTIVE)
    val tier = enumerationByName("tier", 20, EmotionTier::class).default(EmotionTier.FREE)

    override val primaryKey = PrimaryKey(id)
}

object UserUnlockedEmotions : Table("user_unlocked_emotions") {
    val userId = reference("user_id", Users)
    val emotionId = varchar("emotion_id", 50).references(Emotions.id, onDelete = ReferenceOption.CASCADE)
    val unlockedAt = timestamp("unlocked_at")
    val usageLimit = integer("usage_limit").nullable() // NULL = Infinite (Subscription), Value = Credits
    val usageCount = integer("usage_count").default(0)
    val expiresAt = timestamp("expires_at").nullable() // New: For time-based unlocks

    override val primaryKey = PrimaryKey(userId, emotionId)
}

object UserRelationLimits : Table("user_relation_limits") {
    val senderId = uuid("sender_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val receiverId = uuid("receiver_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val dailyLimit = integer("daily_limit").default(3)
    val boostExpiresAt = timestamp("boost_expires_at").nullable() // For 24h limit boost
    val boostCount = integer("boost_count").default(0) // Track stacked boosts
    val updatedAt = timestamp("updated_at").default(java.time.Instant.now())

    override val primaryKey = PrimaryKey(senderId, receiverId)
}

// Enums
enum class ConnectionStatus { PENDING, CONNECTED, BLOCKED }
enum class ModerationStatus { CLEAN, FLAGGED, BLOCKED }
enum class DeliveryStatus { SENT, DELIVERED }
enum class ReferralStatus { PENDING, COMPLETED }
enum class EmotionSentiment { POSITIVE, REFLECTIVE, DIFFICULT, HEAVY }
enum class EmotionTier { FREE, PREMIUM, PREMIUM_PLUS }
enum class TransactionVisibility { VISIBLE, INTERNAL }
enum class RewardSource { SEASONAL, STREAK, REFERRAL, PURCHASE, SYSTEM }
enum class TransactionIntent { REWARD, SPEND, LOG }

object SupportTickets : UUIDTable("support_tickets") {
    val userId = reference("user_id", Users).nullable()
    val contactEmail = varchar("contact_email", 255).nullable() // For guest tickets
    val category = enumerationByName("category", 30, TicketCategory::class)
    val subject = varchar("subject", 255)
    val description = text("description")
    val status = enumerationByName("status", 20, TicketStatus::class).default(TicketStatus.OPEN)
    val priority = enumerationByName("priority", 20, TicketPriority::class).default(TicketPriority.MEDIUM)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at").default(java.time.Instant.now())

    init {
        index("idx_tickets_user", false, userId)
        index("idx_tickets_status", false, status)
        index("idx_tickets_created", false, createdAt)
    }
}

object SupportTicketHistory : UUIDTable("support_ticket_history") {
    val ticketId = reference("ticket_id", SupportTickets)
    val oldStatus = enumerationByName("old_status", 20, TicketStatus::class).nullable()
    val newStatus = enumerationByName("new_status", 20, TicketStatus::class)
    val changedAt = timestamp("changed_at").default(java.time.Instant.now())
    // val changedBy = reference("changed_by", Users).nullable() // Optional: for Admin ID later
}


object UserStreaks : Table("user_streaks") {
    val userId = reference("user_id", Users)
    
    // Streak 1: Emotional Presence (Any activity)
    val presenceStreak = integer("presence_streak").default(0)
    val presenceCycle = integer("presence_cycle").default(0) // Increments on reset
    val presenceLastActiveAt = date("presence_last_active_at").nullable()
    
    // Streak 2: Kindness (Positive emotions)
    val kindnessStreak = integer("kindness_streak").default(0)
    val kindnessCycle = integer("kindness_cycle").default(0)
    val kindnessLastActiveAt = date("kindness_last_active_at").nullable()
    
    // Streak 3: Response (Echo Back Sent)
    val responseStreak = integer("response_streak").default(0)
    val responseCycle = integer("response_cycle").default(0)
    val responseLastActiveAt = date("response_last_active_at").nullable()
    
    val gracePeriodUsedAt = timestamp("grace_period_used_at").nullable()
    val updatedAt = timestamp("updated_at").default(java.time.Instant.now())
    
    override val primaryKey = PrimaryKey(userId)
}

object NotificationMessages : UUIDTable("notification_messages") {
    val category = varchar("category", 50) // STREAK, REFLECTION, SOCIAL, SYSTEM
    val type = varchar("type", 50) // STREAK_REMINDER, CONNECTION_REQUEST, etc.
    val messageTemplate = text("message_template")
    val isRotational = bool("is_rotational").default(false)
    val isActive = bool("is_active").default(true)
}

object NotificationHistory : UUIDTable("notification_history") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val messageId = reference("message_id", NotificationMessages, onDelete = ReferenceOption.CASCADE)
    val sentAt = timestamp("sent_at").clientDefault { java.time.Instant.now() }

    init {
        index("idx_notif_hist_user_sent", false, userId, sentAt)
    }
}

object AppContents : Table("app_contents") {
    val id = enumerationByName("id", 20, ContentType::class) // PK
    val content = text("content")
    val updatedAt = timestamp("updated_at").default(java.time.Instant.now())

    override val primaryKey = PrimaryKey(id)
}


// Admin Portal Tables

enum class AdminRole { ADMIN, SUPPORT, ANALYST }
enum class SenderType { USER, ADMIN }

enum class ContentType { ABOUT, PRIVACY, TERMS }

object AdminUsers : UUIDTable("admin_users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = enumerationByName("role", 20, AdminRole::class).default(AdminRole.SUPPORT)
    val isActive = bool("is_active").default(true)
    val lastLoginAt = timestamp("last_login_at").nullable()
    val lastLoginIp = varchar("last_login_ip", 45).nullable()
    val createdAt = timestamp("created_at").default(java.time.Instant.now())
}

object AuditLogs : UUIDTable("audit_logs") {
    val adminId = reference("admin_id", AdminUsers)
    val action = varchar("action", 100) // e.g. "GRANT_CREDITS", "BROADCAST_NOTIFICATION"
    val targetId = varchar("target_id", 100).nullable() // ID of the User, Ticket, etc.
    val details = text("details").nullable() // JSON payload
    val ipAddress = varchar("ip_address", 45).nullable() // IPv6 can be 45 chars
    val createdAt = timestamp("created_at").default(java.time.Instant.now())
}

object TicketReplies : UUIDTable("ticket_replies") {
    val ticketId = reference("ticket_id", SupportTickets, onDelete = ReferenceOption.CASCADE)
    val senderId = uuid("sender_id") // Can be AdminID or UserID
    val senderType = enumerationByName("sender_type", 20, SenderType::class)
    val content = text("content")
    val createdAt = timestamp("created_at").default(java.time.Instant.now())
    
    init {
        index("idx_replies_ticket_created", false, ticketId, createdAt)
    }
}

object UserActivityMeta : UUIDTable("user_activity_meta") {
    val userId = reference("user_id", Users).uniqueIndex() // One per user
    val lastDashboardOpenAt = timestamp("last_dashboard_open_at").nullable()
    val lastBalancedBonusAt = date("last_balanced_bonus_at").nullable()
    val lastReflectionRewardWeek = varchar("last_reflection_reward_week", 10).nullable() // YYYY-WW
    
    // New Fields
    val totalActiveDays = integer("total_active_days").default(0)
    val lastSeasonRewardAt = date("last_season_reward_at").nullable()
    val seasonRewardMeta = text("season_reward_meta").nullable() // JSON

    init {
        index("idx_meta_bonus", false, lastBalancedBonusAt)
    }
}
// --- Seasonal Events (Dynamic) ---

enum class SeasonalCategory { GLOBAL, RELIGIOUS, CULTURAL, SOCIAL, SEASONAL }

object SeasonalEvents : Table("seasonal_events") {
    val id = varchar("id", 50) // e.g., "VALENTINE_2026" or UUID
    val templateId = varchar("template_id", 50).default("CUSTOM") // e.g. "VALENTINE"
    val year = integer("year") // 2026
    val name = varchar("name", 100) // "Valentine's Week"
    val category = enumerationByName("category", 20, SeasonalCategory::class).default(SeasonalCategory.SEASONAL)
    val startDate = date("start_date")
    val endDate = date("end_date")
    val description = varchar("description", 255).nullable()
    val rulesJson = text("rules_json") // Serialized list of SeasonalRule
    val colorHex = varchar("color_hex", 10).default("0xFFE91E63")
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").default(java.time.Instant.now())
    
    override val primaryKey = PrimaryKey(id)
}

enum class OneTimeRewardType { SIGNUP_BONUS, FIRST_ECHO, FIRST_RESPONSE }

object RewardHistory : Table("reward_history") {
    val phoneHash = varchar("phone_hash", 64) // SHA-256 Hash
    val rewardType = enumerationByName("reward_type", 50, OneTimeRewardType::class)
    val claimedAt = timestamp("claimed_at").default(java.time.Instant.now())
    
    override val primaryKey = PrimaryKey(phoneHash, rewardType)
}
