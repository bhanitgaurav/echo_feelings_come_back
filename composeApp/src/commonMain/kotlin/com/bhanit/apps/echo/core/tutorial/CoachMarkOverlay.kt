package com.bhanit.apps.echo.core.tutorial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun CoachMarkOverlay(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val controller = LocalCoachMarkController.current
    val target = controller.activeTarget

    Box(modifier = modifier) {
        content()

        AnimatedVisibility(
            visible = target != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            if (target != null) {
                androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    // Scrim with Cutout
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .pointerInput(target) {
                                detectTapGestures(
                                    onTap = { controller.dismissCurrent() }
                                )
                            }
                    ) {
                        drawRect(Color.Black.copy(alpha = 0.7f))
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = Offset(target.rect.left, target.rect.top),
                            size = Size(target.rect.width, target.rect.height),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            blendMode = BlendMode.Clear
                        )
                    }

                    // Tooltip
                    val density = LocalDensity.current
                    val screenWidthPx = with(density) { maxWidth.toPx() }
                    
                    val tooltipY = if (target.rect.center.y > (with(density) { 400.dp.toPx() })) { 
                        // Show above
                        (target.rect.top - 150).coerceAtLeast(50f) 
                    } else {
                        // Show below
                        (target.rect.bottom + 20)
                    }
                    
                    val tooltipX = (target.rect.center.x - with(density) { 140.dp.toPx() })
                        .coerceIn(
                            20f, 
                            screenWidthPx - with(density) { 280.dp.toPx() } - 20
                        )

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(tooltipX.roundToInt(), tooltipY.roundToInt()) }
                            .widthIn(max = 280.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = target.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = target.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { controller.dismissCurrent() },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Got it")
                            }
                        }
                    }
                }
            }
        }
    }
}
