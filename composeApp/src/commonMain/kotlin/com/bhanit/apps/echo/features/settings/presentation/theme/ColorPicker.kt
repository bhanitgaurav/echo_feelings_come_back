package com.bhanit.apps.echo.features.settings.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    var currentColor by remember { mutableStateOf(initialColor) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Pick a Color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                HsvColorPicker(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    initialColor = initialColor,
                    onColorChanged = { currentColor = it }
                )

                // Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(initialColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Change to",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onColorSelected(currentColor); onDismissRequest() }) {
                        Text("Select")
                    }
                }
            }
        }
    }
}

@Composable
fun HsvColorPicker(
    modifier: Modifier = Modifier,
    initialColor: Color,
    onColorChanged: (Color) -> Unit
) {
    val hsvState = remember(initialColor) {
        val (h, s, v) = rgbToHsv(initialColor)
        mutableStateOf(Triple(h, s, v))
    }
    
    var hue by remember { mutableFloatStateOf(hsvState.value.first) }
    var saturation by remember { mutableFloatStateOf(hsvState.value.second) }
    var value by remember { mutableFloatStateOf(hsvState.value.third) }

    // Update color when HSV changes
    LaunchedEffect(hue, saturation, value) {
        val color = Color.hsv(hue, saturation, value)
        onColorChanged(color)
    }

    Column(modifier = modifier) {
        // Saturation/Value Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                        value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        saturation = (offset.x / size.width).coerceIn(0f, 1f)
                        value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Hue base
                drawRect(Color.hsv(hue, 1f, 1f))
                
                // Saturation gradient (Left to Right: White to Transparent)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, Color.Transparent)
                    )
                )
                
                // Value gradient (Top to Bottom: Transparent to Black)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )

                // Selection Indicator
                val x = saturation * size.width
                val y = (1f - value) * size.height
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.Black,
                    radius = 8.dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Hue Slider
        Box(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        hue = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        hue = (offset.x / size.width).coerceIn(0f, 1f) * 360f
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradient = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    )
                )
                drawRect(brush = gradient)

                // Slider Indicator
                val x = (hue / 360f) * size.width
                drawLine(
                    color = Color.White,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 4.dp.toPx()
                )
                drawCircle(
                     color = Color.White,
                     radius = 12.dp.toPx(),
                     center = Offset(x, size.height / 2),
                     style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

// Fallback for Color.hsv if not available in common (it is available in Compose UI Graphics)
// Since we are in commonMain, we might not have android.graphics.Color.colorToHSV.
// We need a pure Kotlin implementation for RGB to HSV conversion if we want to support non-Android targets initially.
// However, since the user is on Mac and this is likely KMP, let's use a simple helper.

// Pure Kotlin RGB to HSV
fun rgbToHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val d = max - min

    val h = when {
        d == 0f -> 0f
        max == r -> ((g - b) / d % 6)
        max == g -> ((b - r) / d + 2)
        else -> ((r - g) / d + 4)
    } * 60f

    val s = if (max == 0f) 0f else d / max
    val v = max

    return Triple(if (h < 0) h + 360f else h, s, v)
}
