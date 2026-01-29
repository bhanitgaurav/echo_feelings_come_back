package com.bhanit.apps.echo.core.util

interface PlatformUtils {
    fun shareText(text: String)
    fun openEmail(email: String, subject: String, body: String = "")
    fun openUrl(url: String)
    suspend fun getClipboardText(): String?
    fun setClipboardText(text: String)
    fun getAppVersionCode(): Long
    fun getPlatformName(): String
    fun isDebug(): Boolean
}
