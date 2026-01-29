package com.bhanit.apps.echo.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bhanit.apps.echo.core.config.UpdateStatus
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun UpdateOverlayPreview() {
    UpdateOverlay(
        status = UpdateStatus.MANDATORY,
        updateUrl = "https://play.google.com/store/apps/details?id=com.bhanit.apps.echo",
        onUpdateClick = {},
        onDismissOptional = {}
    )
}

@Composable
fun UpdateOverlay(
    status: UpdateStatus,
    updateUrl: String,
    onUpdateClick: (String) -> Unit,
    onDismissOptional: () -> Unit
) {
    if (status == UpdateStatus.MANDATORY) {
        // Blocking Full Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Update Required",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A new version of Echo is available. Please update to continue using the app.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { onUpdateClick(updateUrl) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Update Now")
                }
            }
        }
    } else if (status == UpdateStatus.OPTIONAL) {
        // Dialog
        AlertDialog(
            onDismissRequest = onDismissOptional,
            icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
            title = { Text("Update Available") },
            text = { Text("A new version of Echo is available. Would you like to update now?") },
            confirmButton = {
                TextButton(onClick = { 
                    onUpdateClick(updateUrl)
                    onDismissOptional() 
                }) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissOptional) {
                    Text("Later")
                }
            }
        )
    }
}
