package com.bhanit.apps.echo.features.auth.domain

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.bhanit.apps.echo.core.theme.CustomThemeColors

import com.bhanit.apps.echo.core.theme.AppFont
class SessionRepository(private val dataStore: DataStore<Preferences>) {
    
    init {
        println("DEBUG: SessionRepository Initialized: $this")
    }

    companion object {
        val KEY_TOKEN = stringPreferencesKey("auth_token")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_PHONE = stringPreferencesKey("phone")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_NOTIFICATIONS_FEELINGS = booleanPreferencesKey("notifications_feelings")
        val KEY_NOTIFICATIONS_CHECKINS = booleanPreferencesKey("notifications_checkins")
        val KEY_NOTIFICATIONS_REFLECTIONS = booleanPreferencesKey("notifications_reflections")
        val KEY_NOTIFICATIONS_REWARDS = booleanPreferencesKey("notifications_rewards")
        val KEY_NOTIFICATIONS_UPDATES = booleanPreferencesKey("notifications_updates")
        val KEY_NOTIFICATIONS_SOUND = booleanPreferencesKey("notifications_sound")
        val KEY_NOTIFICATIONS_VIBRATION = booleanPreferencesKey("notifications_vibration")
        val KEY_NOTIFICATIONS_INACTIVE_REMINDERS =
            booleanPreferencesKey("notifications_inactive_reminders")
        val KEY_REFERRAL_CODE = stringPreferencesKey("referral_code")
        val KEY_THEME = stringPreferencesKey("app_theme")
        val KEY_CUSTOM_THEME_COLORS = stringPreferencesKey("custom_theme_colors") // JSON string
        val KEY_FONT = stringPreferencesKey("app_font")
        val KEY_SHOW_ACTIVITY_OVERVIEW = booleanPreferencesKey("show_activity_overview")
        val KEY_LAST_RATED_VERSION = longPreferencesKey("last_rated_version")
        val KEY_SHARE_ASPECT_RATIO = stringPreferencesKey("share_aspect_ratio")
    }

    // In-memory cache to prevent race conditions during rapid login/API calls
    private var cachedToken: String? = null
    private var cachedUserId: String? = null
    private var cachedPhone: String? = null
    private var cachedUsername: String? = null
    
    val accessToken: String? 
        get() = cachedToken

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_TOKEN] != null
    }.distinctUntilChanged()

    val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_ENABLED] ?: false
    }.distinctUntilChanged()

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }.distinctUntilChanged()

    val notificationsFeelings: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_FEELINGS] ?: true
    }.distinctUntilChanged()

    val notificationsCheckins: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_CHECKINS] ?: true
    }.distinctUntilChanged()

    val notificationsReflections: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_REFLECTIONS] ?: true
    }.distinctUntilChanged()

    val notificationsRewards: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_REWARDS] ?: true
    }.distinctUntilChanged()

    val notificationsUpdates: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_UPDATES] ?: true
    }.distinctUntilChanged()

    val notificationsSound: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_SOUND] ?: true
    }.distinctUntilChanged()

    val notificationsVibration: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_VIBRATION] ?: true
    }.distinctUntilChanged()

    val notificationsInactiveReminders: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_INACTIVE_REMINDERS] ?: true
    }.distinctUntilChanged()

    val referralCode: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_REFERRAL_CODE]
    }.distinctUntilChanged()

    val theme: Flow<com.bhanit.apps.echo.core.theme.ThemeConfig> = dataStore.data.map { preferences ->
        val themeName = preferences[KEY_THEME]
        if (themeName != null) {
            try {
                com.bhanit.apps.echo.core.theme.ThemeConfig.valueOf(themeName)
            } catch (e: Exception) {
                com.bhanit.apps.echo.core.theme.ThemeConfig.SYSTEM
            }
        } else {
            com.bhanit.apps.echo.core.theme.ThemeConfig.SYSTEM
        }
    }.distinctUntilChanged()

    val customThemeColors: Flow<CustomThemeColors> = dataStore.data.map { preferences ->
        val json = preferences[KEY_CUSTOM_THEME_COLORS]
        if (json != null) {
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                CustomThemeColors() // Fallback to default
            }
        } else {
            CustomThemeColors()
        }
    }.distinctUntilChanged()

    val font: Flow<AppFont> = dataStore.data.map { preferences ->
        val fontName = preferences[KEY_FONT]
        if (fontName != null) {
            try {
                AppFont.valueOf(fontName)
            } catch (e: Exception) {
                AppFont.INTER
            }
        } else {
            AppFont.INTER
        }
    }.distinctUntilChanged()

    val showActivityOverview: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SHOW_ACTIVITY_OVERVIEW] ?: true
    }.distinctUntilChanged()


    val lastRatedVersion: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_RATED_VERSION] ?: 0L
    }.distinctUntilChanged()

    suspend fun setTheme(config: com.bhanit.apps.echo.core.theme.ThemeConfig) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME] = config.name
        }
    }

    suspend fun setCustomThemeColors(colors: CustomThemeColors) {
        dataStore.edit { preferences ->
            preferences[KEY_CUSTOM_THEME_COLORS] = Json.encodeToString(colors)
        }
    }

    suspend fun setFont(font: AppFont) {
        dataStore.edit { preferences ->
            preferences[KEY_FONT] = font.name
        }
    }

    suspend fun setShowActivityOverview(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_ACTIVITY_OVERVIEW] = enabled
        }
    }

    val shareAspectRatio: Flow<com.bhanit.apps.echo.features.messaging.domain.ShareAspectRatio> = dataStore.data.map { preferences ->
        val name = preferences[KEY_SHARE_ASPECT_RATIO]
        if (name != null) {
            try {
                com.bhanit.apps.echo.features.messaging.domain.ShareAspectRatio.valueOf(name)
            } catch (e: Exception) {
                com.bhanit.apps.echo.features.messaging.domain.ShareAspectRatio.STORY
            }
        } else {
            com.bhanit.apps.echo.features.messaging.domain.ShareAspectRatio.STORY
        }
    }.distinctUntilChanged()

    suspend fun setShareAspectRatio(aspectRatio: com.bhanit.apps.echo.features.messaging.domain.ShareAspectRatio) {
        dataStore.edit { preferences ->
            preferences[KEY_SHARE_ASPECT_RATIO] = aspectRatio.name
        }
    }


    suspend fun saveReferralCode(code: String) {
        dataStore.edit { preferences ->
            if (preferences[KEY_REFERRAL_CODE] == null) { // Only save if not already set (first install/open)
                preferences[KEY_REFERRAL_CODE] = code
            }
        }
    }

    suspend fun saveSession(token: String, userId: String, phone: String, username: String? = null, isOnboardingCompleted: Boolean = false) {
        println("DEBUG: SessionRepo - Saving Session. Token: ...${token.takeLast(6)}")

        // Update Cache Immediately
        cachedToken = token
        cachedUserId = userId
        cachedPhone = phone
        cachedUsername = username
        
        dataStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
            preferences[KEY_USER_ID] = userId
            preferences[KEY_PHONE] = phone
            preferences[KEY_ONBOARDING_COMPLETED] = isOnboardingCompleted
            if (username != null) {
                preferences[KEY_USERNAME] = username
            } else {
                preferences.remove(KEY_USERNAME)
            }
        }
        println("DEBUG: SessionRepo - Saved.")
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setNotificationPreference(key: Preferences.Key<Boolean>, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[key] = enabled
        }
    }

    suspend fun clearSession() {
        println("DEBUG: SessionRepo - Clearing Session")

        // Clear Cache Immediately
        cachedToken = null
        cachedUserId = null
        cachedPhone = null
        cachedUsername = null
        
        dataStore.edit { preferences ->
            preferences.remove(KEY_TOKEN)
            preferences.remove(KEY_USER_ID)
            preferences.remove(KEY_USERNAME)
            // We might keep phone or biometric preference depending on UX
            preferences.remove(KEY_NOTIFICATIONS_FEELINGS)
            preferences.remove(KEY_NOTIFICATIONS_CHECKINS)
            preferences.remove(KEY_NOTIFICATIONS_REFLECTIONS)
            preferences.remove(KEY_NOTIFICATIONS_REWARDS)
            preferences.remove(KEY_NOTIFICATIONS_UPDATES)
            preferences.remove(KEY_NOTIFICATIONS_SOUND)
            preferences.remove(KEY_NOTIFICATIONS_VIBRATION)
            preferences.remove(KEY_NOTIFICATIONS_INACTIVE_REMINDERS)
            preferences.remove(KEY_REFERRAL_CODE)
        }
    }

    suspend fun getSession(): UserSession? {
        // Return cache if available
        if (cachedToken != null && cachedUserId != null && cachedPhone != null) {
            println("DEBUG: SessionRepo - GetSession (Cache). Token: ...${cachedToken?.takeLast(6)}")
            return UserSession(cachedToken!!, cachedUserId!!, cachedPhone!!, cachedUsername)
        }
        
        val preferences = dataStore.data.first()
        val token = preferences[KEY_TOKEN]
        val userId = preferences[KEY_USER_ID]
        val phone = preferences[KEY_PHONE]
        val username = preferences[KEY_USERNAME]

        println("DEBUG: SessionRepo - GetSession (DataStore). Token: ...${token?.takeLast(6)}")

        // Update cache
        if (token != null) {
            cachedToken = token
            cachedUserId = userId
            cachedPhone = phone
            cachedUsername = username
        }

        return if (token != null && userId != null && phone != null) {
            UserSession(token, userId, phone, username)
        } else {
            null
        }
    }

    suspend fun updateSessionUserInfo(username: String? = null, photoUrl: String? = null) {
        // We only update if we have an active session
        val currentToken = cachedToken ?: return
        val currentUserId = cachedUserId ?: return
        val currentPhone = cachedPhone ?: return

        println("DEBUG: SessionRepo - updateSessionUserInfo. Username: $username")

        // Update Cache
        if (username != null) cachedUsername = username
        // Note: we don't cache photoUrl in session currently, but if we did, we'd update it here.
        // The current UserSession object and SessionReposity implementation seems to only track username.
        // If the UserSession struct has photoUrl, we should update it.
        // Let's check UserSession class definition if possible, or just ignore photoUrl if it's not there.
        // Based on getSession returning UserSession(cachedToken!!, cachedUserId!!, cachedPhone!!, cachedUsername),
        // it seems photoUrl is NOT in UserSession. So we only sync username.

        dataStore.edit { preferences ->
             if (username != null) {
                preferences[KEY_USERNAME] = username
            }
        }
    }

    suspend fun setLastRatedVersion(version: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_RATED_VERSION] = version
        }
    }
}
