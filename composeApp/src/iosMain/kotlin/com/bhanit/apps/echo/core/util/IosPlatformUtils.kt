package com.bhanit.apps.echo.core.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIViewController
import platform.UIKit.UIPopoverPresentationController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import kotlin.experimental.ExperimentalNativeApi

class IosPlatformUtils : PlatformUtils {
    @OptIn(ExperimentalForeignApi::class)
    override fun shareText(text: String) {
        val controller = UIActivityViewController(listOf(text), null)
        val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootController != null) {
             rootController.presentViewController(controller, true, null)
             // For iPad support (popover) - Disabled due to compilation issues
             /*
             if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
                 val popover = (controller as UIViewController).popoverPresentationController
                 if (popover != null) {
                     popover.sourceView = rootController.view
                     rootController.view.bounds.useContents {
                        popover.sourceRect = CGRectMake(size.width / 2.0, size.height / 2.0, 0.0, 0.0)
                     }
                 }
             }
             */
        } else {
             println("Share failed: Root Layout not found")
        }
    }

    override fun openEmail(email: String, subject: String, body: String) {
        val urlString = "mailto:$email?subject=${subject}&body=${body}"
        val url = NSURL.URLWithString(urlString.replace(" ", "%20"))
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
        }
    }

    override fun openUrl(url: String) {
        val safeUrl = url.replace(" ", "%20")
        val nsUrl = NSURL.URLWithString(safeUrl)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl, mapOf<Any?, Any?>(), null)
        }
    }

    override suspend fun getClipboardText(): String? {
        // Accessing pasteboard should be quick, but adherence to suspend is good.
        // On iOS Main thread requirement depends on specific UI APIs. UIPasteboard.general is thread-safe generally but often accessed on main.
        return platform.UIKit.UIPasteboard.generalPasteboard.string
    }

    override fun setClipboardText(text: String) {
        platform.UIKit.UIPasteboard.generalPasteboard.string = text
    }

    override fun getAppVersionCode(): Long {
        val version = platform.Foundation.NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String
        return version?.toLongOrNull() ?: 1
    }
    
    override fun getPlatformName(): String = "ios"
    
    @OptIn(ExperimentalNativeApi::class)
    override fun isDebug(): Boolean {
        return Platform.isDebugBinary
    }
}
