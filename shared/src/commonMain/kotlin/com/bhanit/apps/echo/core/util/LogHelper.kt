package com.bhanit.apps.echo.core.util

expect object LogHelper {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
