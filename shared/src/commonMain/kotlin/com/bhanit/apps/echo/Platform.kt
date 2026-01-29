package com.bhanit.apps.echo

interface Platform {
    val name: String
    val appVersion: String
    val osVersion: String
    val deviceModel: String
    val locale: String?
    val timezone: String?
}

expect fun getPlatform(): Platform