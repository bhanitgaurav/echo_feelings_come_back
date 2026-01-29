package com.bhanit.apps.echo.features.messaging.presentation.components

import com.bhanit.apps.echo.features.messaging.domain.EchoShareModel
import platform.UIKit.*
import platform.Foundation.*
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake

actual class ShareManager {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun captureAndShare(model: EchoShareModel) {
        // Enforce Main Thread for UIKit operations
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            // Find the root view controller to present from
            val window = UIApplication.sharedApplication.keyWindow ?: UIApplication.sharedApplication.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
            val rootViewController = window?.rootViewController ?: return@withContext

            // Create a controller for the share card
            val shareController = ComposeUIViewController {
                EchoCardShareUI(model = model)
            }
            
            // Force Light Mode for the capture to ensure aesthetic consistency
            shareController.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleLight

            // Create the view
            val view = shareController.view
            val width = model.aspectRatio.w.toDouble()
            val height = model.aspectRatio.h.toDouble()
            val frame = CGRectMake(0.0, 0.0, width, height)
            view.setFrame(frame)
            
            // CRITICAL: For Compose (Metal) capture on iOS, the view must be in the hierarchy 
            // and we must use drawHierarchy, not renderInContext.
            // We attach it hiddenly to the window.
            window.addSubview(view)
            
            // Force Layout
            view.layoutIfNeeded()
            
            // Allow a brief moment for Compose to settle (fonts, async resources)
            // This is often needed for off-screen UIHostingControllers to render text correctly
            kotlinx.coroutines.delay(100) 
            
            // Render to image using drawHierarchy which supports Metal/OpenGL content
            val renderer = UIGraphicsImageRenderer(view.bounds)
            val image = renderer.imageWithActions { 
                 view.drawViewHierarchyInRect(view.bounds, afterScreenUpdates = true)
            }
            
            // Remove temp view
            view.removeFromSuperview()

            // Share using UIActivityViewController
            val activityViewController = UIActivityViewController(
                activityItems = listOf(image),
                applicationActivities = null
            )

            // For iPad: set source view to prevent crash
            activityViewController.popoverPresentationController?.sourceView = rootViewController.view
            activityViewController.popoverPresentationController?.sourceRect = CGRectMake(rootViewController.view.bounds.useContents { size.width } / 2.0, rootViewController.view.bounds.useContents { size.height } / 2.0, 0.0, 0.0)
            
            // Present
            rootViewController.presentViewController(activityViewController, animated = true, completion = null)
        }
    }
}
