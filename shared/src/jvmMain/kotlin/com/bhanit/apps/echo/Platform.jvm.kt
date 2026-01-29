package com.bhanit.apps.echo

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val appVersion: String = "Server/1.0"
    override val osVersion: String = System.getProperty("os.name") + " " + System.getProperty("os.version")
    override val deviceModel: String = "Server"
    override val locale: String? = java.util.Locale.getDefault().toString()
    override val timezone: String? = java.util.TimeZone.getDefault().id
}

actual fun getPlatform(): Platform = JVMPlatform()