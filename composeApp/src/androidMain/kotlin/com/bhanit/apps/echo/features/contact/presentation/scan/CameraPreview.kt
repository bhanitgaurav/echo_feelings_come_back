package com.bhanit.apps.echo.features.contact.presentation.scan

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    torchEnabled: Boolean,
    onQrScanned: (String) -> Unit,
    onPermissionGranted: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(hasCameraPermission) {
        onPermissionGranted(hasCameraPermission)
    }

    // Check permission on resume (e.g. returning from Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Keep track of the camera control to toggle torch
    var cameraControl: androidx.camera.core.CameraControl? by remember { mutableStateOf(null) }

    LaunchedEffect(torchEnabled, cameraControl) {
        try {
            cameraControl?.enableTorch(torchEnabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                val activity = context as? android.app.Activity
                // If rationale is false AND it was just denied, it usually means "Don't ask again" was checked
                // or the permission was permanently denied before.
                if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                    showSettingsDialog = true
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        // Create PreviewView with MATCH_PARENT
        val previewView = remember { 
            PreviewView(context).apply {
                id = android.view.View.generateViewId()
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
        
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                android.widget.FrameLayout(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    addView(previewView)
                }
            }
        )

        LaunchedEffect(lifecycleOwner) {
            val executor = ContextCompat.getMainExecutor(context)
            
            try {
                // Await CameraProvider
                val cameraProvider = kotlin.coroutines.suspendCoroutine<ProcessCameraProvider> { continuation ->
                    cameraProviderFuture.addListener({
                        continuation.resumeWith(Result.success(cameraProviderFuture.get()))
                    }, executor)
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                
                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        val scanner = BarcodeScanning.getClient()
                        
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let { value ->
                                        onQrScanned(value)
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                cameraControl = camera.cameraControl
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    } else {
        // Fallback UI or Dialog
        Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
             Button(
                 onClick = { launcher.launch(Manifest.permission.CAMERA) },
                 modifier = Modifier.padding(bottom = 150.dp)
             ) {
                 Text("Camera Permission Required. Tap to Grant.")
             }
        }

        if (showSettingsDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Camera Permission Required") },
                text = { Text("To scan QR codes, please enable Camera permission in App Settings.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                        showSettingsDialog = false
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
