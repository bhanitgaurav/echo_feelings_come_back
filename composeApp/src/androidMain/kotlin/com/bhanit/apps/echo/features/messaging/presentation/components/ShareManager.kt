package com.bhanit.apps.echo.features.messaging.presentation.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.bhanit.apps.echo.features.messaging.domain.EchoShareModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class ShareManager(private val context: Context) {
    actual suspend fun captureAndShare(model: EchoShareModel) {
        withContext(Dispatchers.Main) {
            val bitmap = captureView(context, model)
            if (bitmap != null) {
                shareBitmap(context, bitmap)
            }
        }
    }

    private fun captureView(context: Context, model: EchoShareModel): Bitmap? {
        // We must attach to a window to access the WindowRecomposer, otherwise Compose crashes.
        // We'll attach it invisibly, draw, and remove it immediately.
        val activity = context.findActivity() ?: return null
        val root = activity.window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content) ?: return null
        
        val view = ComposeView(context).apply {
            // Set the logic to dispose composition when we detach
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                EchoCardShareUI(model = model)
            }
        }

        try {
            // 1. Attach to window (invisible)
            view.visibility = View.INVISIBLE
            root.addView(view)

            // 2. Measure and Layout manually
            // We force a specific size for the share card based on user preference
            val width = model.aspectRatio.w
            val height = model.aspectRatio.h
            
            val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, width, height)

            // 3. Draw to Bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            // Fill background manually in case transparent (though CardUI has background)
            canvas.drawColor(android.graphics.Color.WHITE) 
            view.draw(canvas)

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            // 4. Cleanup
            try {
                root.removeView(view)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    private fun Context.findActivity(): android.app.Activity? {
        var ctx = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }

    private suspend fun shareBitmap(context: Context, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            try {
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()
                val stream = FileOutputStream("$cachePath/echo_share.png") // Overwrite same file to avoid clutter
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                val imagePath = File(context.cacheDir, "images")
                val newFile = File(imagePath, "echo_share.png")
                val contentUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    newFile
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/png"
                }
                
                val chooser = Intent.createChooser(shareIntent, "Share Echo via")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
