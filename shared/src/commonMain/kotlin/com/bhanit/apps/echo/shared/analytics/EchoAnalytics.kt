package com.bhanit.apps.echo.shared.analytics

interface EchoAnalytics {
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())
    fun setUserId(id: String)
    fun setUserProperty(name: String, value: String)
    fun logCrash(message: String)
}

object EchoEvents {
    const val APP_OPEN = "app_open"
    const val APP_BACKGROUND = "app_background"
    const val ONBOARDING_STARTED = "onboarding_started"
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val FIRST_CONNECTION_ADDED = "first_connection_added"
    const val FIRST_FEELING_SENT = "first_feeling_sent"
    const val DASHBOARD_OPENED = "dashboard_opened"
    
    // Streaks
    const val PRESENCE_STREAK_INCREMENTED = "presence_streak_incremented"
    const val KINDNESS_STREAK_INCREMENTED = "kindness_streak_incremented"
    const val REFLECTION_STREAK_INCREMENTED = "reflection_streak_incremented"
    const val STREAK_BROKEN = "streak_broken"
    
    // Feelings
    const val FEELING_SENT = "feeling_sent"
    const val FEELING_RECEIVED = "feeling_received"
    
    // Notifications
    const val NOTIFICATION_RECEIVED = "notification_received"
    const val NOTIFICATION_OPENED = "notification_opened"
    const val NOTIFICATION_DISMISSED = "notification_dismissed"
    const val NOTIFICATION_DISABLED = "notification_disabled"
    
    // Credits
    const val CREDITS_EARNED = "credits_earned"
    const val CREDITS_USED = "credits_used"
    
    // Referrals
    const val REFERRAL_SENT = "referral_sent"
    const val REFERRAL_JOINED = "referral_joined"
    const val REFERRAL_REWARD_GRANTED = "referral_reward_granted"
    
    // Screen View
    const val SCREEN_VIEW = "screen_view"
}

object EchoParams {
    const val STREAK_TYPE = "streak_type"
    const val STREAK_LENGTH_BUCKET = "streak_length_bucket"
    const val SENTIMENT = "sentiment"
    const val IS_REPLY = "is_reply"
    const val TIME_BUCKET = "time_bucket"
    const val DAY_TYPE = "day_type"
    const val TIMEZONE = "timezone"
    const val NOTIFICATION_TYPE = "notification_type"
    const val TIME_SINCE_LAST_OPEN_BUCKET = "time_since_last_open_bucket"
    const val CREDIT_REASON = "credit_reason"
    const val CREDITS_BALANCE_BUCKET = "credits_balance_bucket"
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
}
