package com.bhanit.apps.echo.core.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

class IosImageCompressor : ImageCompressor {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun compress(bytes: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        try {
            val data = bytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
            }
            val image = UIImage.imageWithData(data) ?: return@withContext bytes

            // Compress to JPEG with 0.8 quality
            // UIImageJPEGRepresentation returns NSData?
            val compressedData = UIImageJPEGRepresentation(image, 0.8) ?: return@withContext bytes

            val byteArray = ByteArray(compressedData.length.toInt())
            if (compressedData.length > 0u) {
                byteArray.usePinned { pinned ->
                    memcpy(pinned.addressOf(0), compressedData.bytes, compressedData.length)
                }
            }
            byteArray
        } catch (e: Exception) {
            e.printStackTrace()
            bytes
        }
    }
}
