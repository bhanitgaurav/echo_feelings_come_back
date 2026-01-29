package com.bhanit.apps.echo.features.settings.presentation.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.shadow
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import echo.composeapp.generated.resources.Res
import echo.composeapp.generated.resources.echo_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun ProfileQrDialog(
    username: String,
    photoUrl: String,
    referralUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        EchoCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // QR Code
                    if (referralUrl.isNotBlank()) {
                        val qrUrl = if (referralUrl.contains("?")) "$referralUrl&m=qr_scan" else "$referralUrl?m=qr_scan"
                        com.bhanit.apps.echo.core.presentation.components.QrCodeImage(
                            data = qrUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(16.dp)
                        )
                    }
                    
                    // Center Logo
                    Box(modifier = Modifier
                         .size(60.dp)
                         .shadow(4.dp, CircleShape)
                         .clip(CircleShape)
                         .background(Color.White)
                         .padding(4.dp)
                    ) {
                        if (!photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = username,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(52.dp)
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            androidx.compose.foundation.Image(
                                painter = painterResource(Res.drawable.echo_logo),
                                contentDescription = "Echo Logo",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(42.dp) // Adjusted size for logo
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Scan to connect instantly",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
