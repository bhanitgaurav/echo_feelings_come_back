package com.bhanit.apps.echo.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun EchoCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp), // Soft & Friendly
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant, // Glass or Surface
    elevation: Dp = 0.dp, // Flat by default for glass feel, or slight shadow
    border: BorderStroke? = BorderStroke(
        1.dp,
        Color.White.copy(alpha = 0.5f)
    ), // Subtle frost border
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = backgroundColor)
    val elevationObj = CardDefaults.cardElevation(defaultElevation = elevation)
    
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevationObj,
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevationObj,
            border = border,
            content = content
        )
    }
}
