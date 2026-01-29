package com.bhanit.apps.echo.features.contact.presentation.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.designsystem.components.EchoButton
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit
) {
    val viewModel = koinViewModel<ScanQrViewModel>()
    val state by viewModel.state.collectAsState()
    val snackbarHostState =
        remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isTorchEnabled by remember { mutableStateOf(false) }
    var isCameraPermissionGranted by remember { mutableStateOf(false) }

    // Handle Navigation Side Effect
    LaunchedEffect(state.result) {
        state.result?.let { res ->
            if (res.status == "CONNECTED" || res.status == "ALREADY_CONNECTED") {
                // Show Snackbar in a separate coroutine so it doesn't block the delay
                launch {
                    snackbarHostState.showSnackbar(
                        message = if (res.status == "CONNECTED") "Connected!" else "Already Connected",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }

                // Wait for a moment to let the user see the success message
                kotlinx.coroutines.delay(1000)

                onNavigateToChat(res.userId, res.username)
            }
        }
    }

    // Blocked User Alert Dialog
    if (state.result?.status == "BLOCKED") {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.onDismissResult() },
            title = { Text("Blocked User") },
            text = { Text("You have blocked this user. Do you want to unblock them to connect?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        state.result?.userId?.let { userId ->
                            viewModel.unblockUser(userId)
                        }
                    }
                ) {
                    Text("Unblock")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.onDismissResult() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isScanning) {
                // 1. Camera Preview (Full Screen)
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    torchEnabled = isTorchEnabled,
                    onQrScanned = viewModel::onCodeScanned,
                    onPermissionGranted = { isCameraPermissionGranted = it }
                )

                if (isCameraPermissionGranted) {
                    // 2. Translucent Overlay with Cutout
                    val overlayColor = Color.Black.copy(alpha = 0.6f)
                val cutoutSize = 280.dp 
                val primaryColor = MaterialTheme.colorScheme.primary
                val verticalBiasOffset = (-60).dp // Move everything up
                val cornerRadius = 16.dp

                // Helper to draw rounded corners
                fun Path.addCorner(
                    startX: Float, startY: Float, 
                    cornerX: Float, cornerY: Float, 
                    endX: Float, endY: Float, 
                    radius: Float
                ) {
                    moveTo(startX, startY)
                    lineTo(
                        if (startX == cornerX) cornerX else if (cornerX < startX) cornerX + radius else cornerX - radius,
                        if (startY == cornerY) cornerY else if (cornerY < startY) cornerY + radius else cornerY - radius
                    )
                    quadraticBezierTo(cornerX, cornerY, endX, endY)
                }

                Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.99f)) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val cutoutWidth = cutoutSize.toPx()
                    val cutoutHeight = cutoutSize.toPx()
                    
                    // Center with vertical offset
                    val cutoutLeft = (canvasWidth - cutoutWidth) / 2
                    val cutoutTop = (canvasHeight - cutoutHeight) / 2 + verticalBiasOffset.toPx()

                    // Draw dark overlay
                    drawRect(color = overlayColor)

                    // Cut out the center
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(cutoutLeft, cutoutTop),
                        size = Size(cutoutWidth, cutoutHeight),
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )

                    // Draw corner borders around the cutout
                    val cornerLength = 40.dp.toPx()
                    val strokeWidth = 5.dp.toPx()
                    val cornerColor = primaryColor
                    val radiusPx = cornerRadius.toPx()
                    
                    // Top Left
                    drawPath(
                        path = Path().apply {
                            moveTo(cutoutLeft, cutoutTop + cornerLength)
                            lineTo(cutoutLeft, cutoutTop + radiusPx)
                            quadraticBezierTo(cutoutLeft, cutoutTop, cutoutLeft + radiusPx, cutoutTop)
                            lineTo(cutoutLeft + cornerLength, cutoutTop)
                        },
                        color = cornerColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Top Right
                    drawPath(
                        path = Path().apply {
                            moveTo(cutoutLeft + cutoutWidth - cornerLength, cutoutTop)
                            lineTo(cutoutLeft + cutoutWidth - radiusPx, cutoutTop)
                            quadraticBezierTo(cutoutLeft + cutoutWidth, cutoutTop, cutoutLeft + cutoutWidth, cutoutTop + radiusPx)
                            lineTo(cutoutLeft + cutoutWidth, cutoutTop + cornerLength)
                        },
                        color = cornerColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Bottom Left
                    drawPath(
                        path = Path().apply {
                            moveTo(cutoutLeft, cutoutTop + cutoutHeight - cornerLength)
                            lineTo(cutoutLeft, cutoutTop + cutoutHeight - radiusPx)
                            quadraticBezierTo(cutoutLeft, cutoutTop + cutoutHeight, cutoutLeft + radiusPx, cutoutTop + cutoutHeight)
                            lineTo(cutoutLeft + cornerLength, cutoutTop + cutoutHeight)
                        },
                        color = cornerColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Bottom Right
                    drawPath(
                        path = Path().apply {
                            moveTo(cutoutLeft + cutoutWidth - cornerLength, cutoutTop + cutoutHeight)
                            lineTo(cutoutLeft + cutoutWidth - radiusPx, cutoutTop + cutoutHeight)
                            quadraticBezierTo(cutoutLeft + cutoutWidth, cutoutTop + cutoutHeight, cutoutLeft + cutoutWidth, cutoutTop + cutoutHeight - radiusPx)
                            lineTo(cutoutLeft + cutoutWidth, cutoutTop + cutoutHeight - cornerLength)
                        },
                        color = cornerColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 3. UI Controls Layer (Aligned with the cutout)
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val centerY = maxHeight / 2
                    val cutoutBottom = centerY + (cutoutSize / 2) + verticalBiasOffset
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = cutoutBottom + 24.dp), // Start 24dp below the cutout
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Align QR code within the frame",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(10.dp))
                        
                        // Torch Button
                        IconButton(
                            onClick = { isTorchEnabled = !isTorchEnabled },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(56.dp)
                        ) {
                            Icon(
                                if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, 
                                contentDescription = "Toggle Flashlight", 
                                tint = if (isTorchEnabled) Color.Yellow else Color.White
                            )
                        }
                    }
                    }
                }
            }

            // Top Buttons (Close - Top Right)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp) // Reduced to moves it up further
            ) {
                // Close Button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }


            // Result / Loading / Success Overlay (Copied from previous logic, mostly unchanged)
            if (state.isLoading || state.error != null || state.result != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    EchoCard(
                        modifier = Modifier.padding(32.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text("Verifying Code...")
                            } else if (state.error != null) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Error",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(state.error ?: "", textAlign = TextAlign.Center)
                                Spacer(Modifier.height(24.dp))
                                EchoButton(
                                    text = "Try Again",
                                    onClick = { viewModel.onDismissResult() })
                            } else {
                                state.result?.let { res ->
                                    if (res.status == "BLOCKED") return@let
                                    val isConnectionSuccess =
                                        res.status == "CONNECTED" || res.status == "ALREADY_CONNECTED"

                                    if (isConnectionSuccess) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.Green,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = if (res.status == "CONNECTED") "Connected!" else "Already Connected",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Redirecting to chat...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        val title = when (res.status) {
                                            "REQUEST_SENT" -> "Request Sent!"
                                            "PENDING" -> "Request Pending"
                                            "SELF" -> "That's You!"
                                            "UNAVAILABLE" -> "User Unavailable"
                                            else -> "Scanned"
                                        }

                                        val icon = when (res.status) {
                                            "REQUEST_SENT" -> Icons.AutoMirrored.Filled.Send
                                            "PENDING" -> Icons.Default.Schedule
                                            "UNAVAILABLE" -> Icons.Default.Close
                                            else -> Icons.Default.Info
                                        }

                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(Modifier.height(16.dp))

                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(24.dp))
                                        EchoButton(text = "OK", onClick = {
                                            viewModel.onDismissResult()
                                            if (res.status == "REQUEST_SENT" || res.status == "PENDING") {
                                                onNavigateBack()
                                            }
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
