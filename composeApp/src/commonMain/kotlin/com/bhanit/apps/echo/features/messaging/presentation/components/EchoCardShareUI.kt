package com.bhanit.apps.echo.features.messaging.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhanit.apps.echo.core.theme.AppFont
import com.bhanit.apps.echo.core.theme.getTypography
import com.bhanit.apps.echo.features.messaging.domain.EchoShareModel
import com.bhanit.apps.echo.features.messaging.domain.ShareAspectRatio
import echo.composeapp.generated.resources.Res
import echo.composeapp.generated.resources.echo_logo
import org.jetbrains.compose.resources.painterResource

/**
 * Static UI for sharing an Echo Card.
 *
 * Visual Hierarchy (Strict):
 * 1. Received message (Primary emotional weight)
 * 2. Optional reply (Secondary, softer)
 * 3. Context + footer (Whisper-level)
 */
@Composable
fun EchoCardShareUI(
    model: EchoShareModel, modifier: Modifier = Modifier
) {
    // Force a warm neutral background and specific typography for the image export
    // We are NOT using the system theme here to ensure consistent "paper-like" export.
    // Enhanced Background: Warm Human Gradient
    val paperStart = Color(0xFFFDFCFB) // Very light warm
    val paperEnd = Color(0xFFF5F0E6)   // Soft beige
    val gradient = Brush.verticalGradient(listOf(paperStart, paperEnd))
    val inkColor = Color(0xFF2C2C2C) // Soft Black
    val whisperColor = Color(0xFF8E8E93) // IOS-style gray
    val decorationColor = Color(model.moodColor).copy(alpha = 0.08f) // Very faint mood color

    Box(
        modifier = modifier.fillMaxSize() // Fill the container (which determines ratio)
            .background(gradient), contentAlignment = Alignment.Center
    ) {
        // --- BACKGROUND DECORATION ---

        // 1. Large Watermark Logo (Centered & Subtle Texture)
        val logoPainter = painterResource(Res.drawable.echo_logo)
        androidx.compose.foundation.Image(
            painter = logoPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.8f) // Large texture
                .align(Alignment.Center).alpha(0.02f), // Extremely faint texture
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(inkColor)
        )

        // 2. Subtle Mood Overlay (instead of curves)
        Box(
            modifier = Modifier.fillMaxSize().background(Color(model.moodColor).copy(alpha = 0.03f))
        )

        // Dynamic Padding Calculation - Increased as per Keepsake feel
        val topPadding = if (model.aspectRatio == ShareAspectRatio.SQUARE) 40.dp else 120.dp
        val bottomPadding = 40.dp

        // Main Content Structure
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(start = 32.dp, end = 32.dp, top = topPadding, bottom = bottomPadding)
        ) {
            // --- TOP WHISPER ---
            TopHeader(whisperColor)

            Spacer(modifier = Modifier.weight(1f))

            // --- MAIN CONTENT (Container of Care) ---
            EchoContainer(model, inkColor)

            if (model.repliedText != null) {
                val contentGap = if (model.aspectRatio == ShareAspectRatio.SQUARE) 8.dp else 16.dp
                Spacer(modifier = Modifier.height(contentGap))
            }

            EchoBackContainer(model, inkColor, whisperColor)

            Spacer(modifier = Modifier.weight(1f))

            // --- FOOTER (Signature) ---
            BottomFooter(model, inkColor, whisperColor)
        } // End Main Column
    } // End Root Box
}

@Composable
fun EchoBackContainer(model: EchoShareModel, inkColor: Color, whisperColor: Color) {
    if (model.repliedText != null) {
        // Reflection Group: Column inside Row
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "I echoed back", style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.sp,
                        color = whisperColor.copy(alpha = 0.8f)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = model.repliedText, style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
                        color = inkColor.copy(alpha = 0.8f),
                        lineHeight = 22.sp
                    ), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
fun BottomFooter(
    model: EchoShareModel, inkColor: Color, whisperColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {


        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Branding Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Mood Dot (The only strong color)
                Box(
                    modifier = Modifier.size(6.dp).clip(CircleShape)
                        .background(Color(model.moodColor))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Logo Icon
                androidx.compose.foundation.Image(
                    painter = painterResource(Res.drawable.echo_logo),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    alpha = 0.9f,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        inkColor.copy(
                            alpha = 0.7f
                        )
                    )
                )
                Text(
                    text = "echo", // Lowercase signature
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = getTypography(AppFont.MERRIWEATHER).titleMedium.fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ), color = inkColor.copy(alpha = 0.8f)
                )
            }

            // Tagline
            Text(
                text = "- feelings come back",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = whisperColor.copy(alpha = 0.5f)
            )
        }

    }
}

@Composable
fun EchoContainer(
    model: EchoShareModel,
    inkColor: Color,
) {
    // Soft Message Container Row
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.55f)).padding(
                    horizontal = 24.dp, vertical = 24.dp
                )
        ) {
            Text(
                text = model.receivedText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = getTypography(AppFont.MERRIWEATHER).headlineMedium.fontFamily,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    letterSpacing = 0.3.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = inkColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun TopHeader(whisperColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Someone sent me this", style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Normal
            ), color = whisperColor.copy(alpha = 0.6f), textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HumanPause(whisperColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(
            text = "·", style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 20.sp, color = whisperColor.copy(alpha = 0.4f)
            )
        )
    }
}