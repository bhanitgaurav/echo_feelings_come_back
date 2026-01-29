package com.bhanit.apps.echo.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

actual class ImagePickerLauncher(private val launcher: ActivityResultLauncher<PickVisualMediaRequest>) {
    actual fun launch() {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

@Composable
actual fun rememberImagePicker(onImagePicked: (ByteArray) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                // 1. Calculate sample size without loading image
                var inputStream = context.contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                val reqWidth = 1024
                val reqHeight = 1024
                
                // Calculate inSampleSize
                val (height: Int, width: Int) = options.run { outHeight to outWidth }
                var inSampleSize = 1
                if (height > reqHeight || width > reqWidth) {
                    val halfHeight: Int = height / 2
                    val halfWidth: Int = width / 2
                    while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                        inSampleSize *= 2
                    }
                }

                // 2. Decode with sample size
                inputStream = context.contentResolver.openInputStream(uri)
                val decodeOptions = BitmapFactory.Options().apply {
                     inSampleSize = inSampleSize
                     inJustDecodeBounds = false
                }
                val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                inputStream?.close()

                if (bitmap != null) {
                    // 3. Compress to ByteArray
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    onImagePicked(stream.toByteArray())
                    bitmap.recycle()
                    // Try to recycle stream explicitly? Not needed for ByteArrayOutputStream but good practice to close
                    stream.close()
                } else {
                     // Fallback check?
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    return remember(launcher) { ImagePickerLauncher(launcher) }
}
