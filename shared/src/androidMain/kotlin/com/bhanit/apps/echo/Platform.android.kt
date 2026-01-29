package com.bhanit.apps.echo

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val appVersion: String = "1.0.0" // TODO: Inject or BuildConfig
    override val osVersion: String = Build.VERSION.RELEASE
    override val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    override val locale: String = java.util.Locale.getDefault().toString()
    override val timezone: String = java.util.TimeZone.getDefault().id
}

actual fun getPlatform(): Platform = AndroidPlatform()