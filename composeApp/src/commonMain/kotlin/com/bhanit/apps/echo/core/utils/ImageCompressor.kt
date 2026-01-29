package com.bhanit.apps.echo.core.utils

interface ImageCompressor {
    suspend fun compress(bytes: ByteArray): ByteArray
}
