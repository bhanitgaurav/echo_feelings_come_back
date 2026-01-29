package com.bhanit.apps.echo.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreImage.CIFilter
import platform.CoreImage.filterWithName

import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setValue
import platform.UIKit.UIImageView
import platform.UIKit.UIImage
import platform.UIKit.UIColor
import platform.UIKit.UIViewContentMode

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrCodeImage(
    data: String,
    modifier: Modifier
) {
    val qrImage = remember(data) {
        generateQrImage(data)
    }

    if (qrImage != null) {
        UIKitView(
            factory = {
                UIImageView().apply {
                    contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
                    backgroundColor = UIColor.whiteColor
                }
            },
            update = { view ->
                (view as? UIImageView)?.image = qrImage
            },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun generateQrImage(content: String): UIImage? {
    val data = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding)
    
    val filter = CIFilter.filterWithName("CIQRCodeGenerator") ?: return null
    filter.setValue(data, forKey = "inputMessage")
    filter.setValue("H", forKey = "inputCorrectionLevel") // High error correction
    
    val outputImage = filter.outputImage ?: return null
    
    // Scale up the image (CGAffineTransform) to ensure it's not blurry
    // CIQRCodeGenerator returns a 1-pixel-per-module image.
    val scaleX = 10.0
    val scaleY = 10.0
    val transformed = outputImage.imageByApplyingTransform(
        platform.CoreGraphics.CGAffineTransformMakeScale(scaleX, scaleY)
    )
    
    return UIImage.imageWithCIImage(transformed)
}
