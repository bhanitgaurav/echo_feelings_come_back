package com.bhanit.apps.echo.core.config

import com.bhanit.apps.echo.core.util.PlatformUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UpdateStatus {
    NONE,
    OPTIONAL,
    MANDATORY
}

data class UpdateInfo(
    val status: UpdateStatus,
    val updateUrl: String = "",
    val latestVersion: Long = 0
)

class UpdateManager(
    private val configService: ConfigService,
    private val platformUtils: PlatformUtils // Need to add getAppVersionCode to PlatformUtils
) {
    private val _updateState = MutableStateFlow(UpdateInfo(UpdateStatus.NONE))
    val updateState = _updateState.asStateFlow()

    suspend fun checkUpdate() {
        // Attempt fetch, but proceed regardless of result (result is true only if *new* config was applied)
        try {
            configService.fetchAndActivate()
        } catch (e: Exception) {
            // Log error, but proceed with cached values
            e.printStackTrace()
        }

        val currentVersion = platformUtils.getAppVersionCode()
        val platform = platformUtils.getPlatformName()
        
        // Try explicit platform keys first, fallback to generic (which handled by Console conditions)
        // But user requested explicit segregation support in code.
        
        val minVersionKey = "${platform}_min_version_code"
        val latestVersionKey = "${platform}_latest_version_code"
        
        // If the specific key returns 0 (missing), we could fallback to "min_version_code" 
        // but relying on explicit keys is safer for segregation.
        // Let's check specific key first.
        
        var minVersion = configService.getLong(minVersionKey)
        if (minVersion == 0L) minVersion = configService.getLong("min_version_code")
        
        var latestVersion = configService.getLong(latestVersionKey)
        if (latestVersion == 0L) latestVersion = configService.getLong("latest_version_code")

        val urlKey = "${platform}_update_url"
        var updateUrl = configService.getString(urlKey)
        if (updateUrl.isBlank()) updateUrl = configService.getString("update_url")

        val status = when {
            // Bypass mandatory updates in Debug builds to prevent blocking development
            platformUtils.isDebug() -> UpdateStatus.NONE 
            currentVersion < minVersion -> UpdateStatus.MANDATORY
            currentVersion < latestVersion -> UpdateStatus.OPTIONAL
            else -> UpdateStatus.NONE
        }

        _updateState.value = UpdateInfo(status, updateUrl, latestVersion)
    }

    fun dismissUpdate() {
        // Only dismiss if optional
        if (_updateState.value.status == UpdateStatus.OPTIONAL) {
            _updateState.value = UpdateInfo(UpdateStatus.NONE)
        }
    }
}
