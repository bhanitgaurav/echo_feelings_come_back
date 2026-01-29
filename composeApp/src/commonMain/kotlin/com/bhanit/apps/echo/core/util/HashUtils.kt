package com.bhanit.apps.echo.core.util

expect object HashUtils {
    fun sha256(input: String): String
}
