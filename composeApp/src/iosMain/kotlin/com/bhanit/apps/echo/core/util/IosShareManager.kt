package com.bhanit.apps.echo.core.util

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

class IosShareManager : ShareManager {
    override fun shareText(text: String) {
        val activityViewController = UIActivityViewController(listOf(text), null)
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController
        
        rootViewController?.presentViewController(
            viewControllerToPresent = activityViewController,
            animated = true,
            completion = null
        )
    }
}
