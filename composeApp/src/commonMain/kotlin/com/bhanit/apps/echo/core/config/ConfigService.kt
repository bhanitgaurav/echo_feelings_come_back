package com.bhanit.apps.echo.core.config

interface ConfigService {
    suspend fun fetchAndActivate(): Boolean
    fun getLong(key: String): Long
    fun getString(key: String): String
    fun getBoolean(key: String): Boolean
}
