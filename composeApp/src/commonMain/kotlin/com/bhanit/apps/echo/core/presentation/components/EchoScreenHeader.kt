package com.bhanit.apps.echo.core.presentation.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import coil3.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawBehind

import androidx.compose.foundation.border
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush

@Composable
fun EchoScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    profileImageUrl: String? = null,
    onProfileClick: (() -> Unit)? = null,
    isLoading: Boolean = false,
    profileModifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = titleColor
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                actions()
                
                if (profileImageUrl != null || onProfileClick != null) {
                    // Animation for loading
                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing)
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp) // Larger touch target
                            .then(profileModifier)
                            .clip(CircleShape)
                            .clickable(enabled = onProfileClick != null) { onProfileClick?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        // Border / Loading Indicator
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val borderMod = if (isLoading) {
                            Modifier.drawBehind {
                                val brush = Brush.sweepGradient(
                                    colors = listOf(
                                        primaryColor,
                                        primaryColor.copy(alpha = 0.1f)
                                    )
                                )
                                rotate(angle) {
                                    drawCircle(
                                        brush = brush,
                                        radius = size.width / 2, // Full width
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                                    )
                                }
                            }
                        } else {
                            Modifier.border(2.dp, primaryColor, CircleShape)
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp) // Visual size
                                .then(borderMod)
                                .padding(3.dp) // Gap between border and image
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profileImageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = profileImageUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
