package com.bhanit.apps.echo.core.util

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.CoreCrypto.*

actual object HashUtils {
    @OptIn(ExperimentalForeignApi::class)
    actual fun sha256(input: String): String {
        val data = input.encodeToByteArray()
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
        
        data.usePinned { pinned ->
            CC_SHA256(pinned.addressOf(0), data.size.toUInt(), digest.refTo(0))
        }
        
        return digest.joinToString("") { 
            val i = it.toInt()
            val hexChars = "0123456789abcdef"
            "${hexChars[(i shr 4) and 0x0f]}${hexChars[i and 0x0f]}"
        }
    }
}
