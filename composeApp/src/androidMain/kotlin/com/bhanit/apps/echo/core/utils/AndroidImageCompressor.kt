package com.bhanit.apps.echo.core.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidImageCompressor : ImageCompressor {
    override suspend fun compress(bytes: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        try {
            // 1. Decode bitmap
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            // 2. Calculate scale factor to reduce memory usage during decode
            // Target max dimension, e.g., 1024x1024
            val reqWidth = 1024
            val reqHeight = 1024
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: return@withContext bytes // Fallback if decode fails

            // 3. Compress
            val outputStream = ByteArrayOutputStream()
            // Compress to JPEG with 80% quality
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            
            val compressedBytes = outputStream.toByteArray()
            
            // Clean up
            bitmap.recycle()
            outputStream.close()
            
            compressedBytes
        } catch (e: Exception) {
            e.printStackTrace()
            bytes // Fallback to original
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
