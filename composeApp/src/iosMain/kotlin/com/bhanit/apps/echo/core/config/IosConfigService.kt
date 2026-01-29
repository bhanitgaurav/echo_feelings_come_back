package com.bhanit.apps.echo.core.config

class IosConfigService : ConfigService {
    // Stub implementation for now to prevent crash
    // In future, integrate Firebase Remote Config for iOS
    
    override suspend fun fetchAndActivate(): Boolean {
        return true // Simulate success
    }

    override fun getLong(key: String): Long {
        return when (key) {
            "ios_min_version_code" -> 1L // Example: Force update if app version < 1
            "ios_latest_version_code" -> 1L
            // Legacy keys fallback
            "min_version_code" -> 1L
            "latest_version_code" -> 1L
            else -> 0L
        }
    }

    override fun getString(key: String): String {
        return when (key) {
            "update_url" -> "https://apps.apple.com/in/app/echo-feelings-come-back/id6757484280?ct=echo_in_app_update"
            else -> ""
        }
    }

    override fun getBoolean(key: String): Boolean {
        return false
    }
}
