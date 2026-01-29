package com.bhanit.apps.echo.features.settings.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import org.koin.compose.viewmodel.koinViewModel
import com.bhanit.apps.echo.features.messaging.domain.ShareAspectRatio

@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    onNavigateToTerms: () -> Unit, // Changed
    onNavigateToPrivacy: () -> Unit, // Changed
    onNavigateToAbout: () -> Unit,
    onNavigateToRefer: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToStreaks: () -> Unit
) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ).padding(top = 48.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Custom Header "Setting"
            com.bhanit.apps.echo.core.presentation.components.EchoScreenHeader(
                title = "Settings"
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ACCOUNT SECTION
                item {
                    SettingsSectionTitle("Account")
                    EchoCard {
                        Column {
                            SettingsItem(
                                icon = Icons.Default.Person,
                                title = "Profile",
                                onClick = onNavigateToProfile
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = Icons.Default.Block,
                                title = "Blocked Users",
                                onClick = onNavigateToBlocked
                            )
                        }
                    }
                }

                // SECURITY SECTION
                item {
                    SettingsSectionTitle("Security")
                    EchoCard {
                        Column {
                            SettingsSwitchItem(
                                icon = Icons.Default.Fingerprint,
                                title = "App Lock (Biometric)",
                                checked = isBiometricEnabled,
                                onCheckedChange = { viewModel.toggleBiometric(it) }
                            )
                        }
                    }
                }

                // PRIVACY SECTION
                item {
                    SettingsSectionTitle("Privacy")
                    EchoCard {
                        Column {
                             SettingsSwitchItem(
                                icon = Icons.Default.Person, // Or QrCode icon if available
                                title = "Auto-Connect via QR",
                                checked = viewModel.state.collectAsState().value.allowQrAutoConnect,
                                onCheckedChange = { viewModel.toggleQrAutoConnect(it) }
                            )
                        }
                    }
                }

                // PREFERENCES SECTION
                item {
                    SettingsSectionTitle("Preferences")
                    EchoCard {
                        Column {
                            SettingsItem(
                                icon = Icons.Outlined.Palette,
                                title = "App Theme",
                                onClick = onNavigateToTheme
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = Icons.Outlined.Notifications,
                                title = "Notifications",
                                onClick = onNavigateToNotifications
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            // Share Ratio Selector
                            /*
                            var showRatioDropdown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                            val currentRatio by viewModel.shareAspectRatio.collectAsState()
                            
                            Box {
                                SettingsItem(
                                    icon = Icons.Default.Share, // Using Share icon
                                    title = "Share Card Size",
                                    subtitle = currentRatio.displayName,
                                    onClick = { showRatioDropdown = true }
                                )
                                
                                androidx.compose.material3.DropdownMenu(
                                    expanded = showRatioDropdown,
                                    onDismissRequest = { showRatioDropdown = false }
                                ) {
                                    com.bhanit.apps.echo.features.messaging.domain.ShareAspectRatio.entries.forEach { ratio ->
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(ratio.displayName) },
                                            onClick = {
                                                viewModel.setShareAspectRatio(ratio)
                                                showRatioDropdown = false
                                            },
                                            trailingIcon = if (currentRatio == ratio) {
                                                { Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                            } else null
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            */
                            SettingsSwitchItem(
                                icon = Icons.Default.GraphicEq,
                                title = "Show Activity Overview",
                                checked = viewModel.showActivityOverview.collectAsState().value,
                                onCheckedChange = { viewModel.toggleShowActivityOverview(it) }
                            )
                        }
                    }
                }

                // REWARDS SECTION
                item {
                    SettingsSectionTitle("Rewards")
                    EchoCard {
                        Column {
                            SettingsItem(
                                icon = Icons.Default.Redeem,
                                title = "Refer & Earn",
                                onClick = onNavigateToRefer
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = Icons.Default.Star, // Using Star as placeholder for Premium/Credits
                                title = "Premium / Credits",
                                onClick = onNavigateToPremium,
                                iconTint = Color(0xFFFFD700)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = Icons.Default.Whatshot,
                                title = "My Streaks",
                                onClick = onNavigateToStreaks
                            )
                        }
                    }
                }

                // SUPPORT SECTION
                item {
                    SettingsSectionTitle("Support")
                    EchoCard {
                        Column {
                            SettingsItem(
                                icon = Icons.Default.Email,
                                title = "Support",
                                onClick = onNavigateToSupport
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = Icons.Default.Star,
                                title = "Rate Us",
                                onClick = { viewModel.rateApp() }
                            )
                        }
                    }
                }

                // LEGAL SECTION
                item {
                    SettingsSectionTitle("Legal")
                    EchoCard {
                        Column {
                            SettingsItem(
                                icon = Icons.Default.Info,
                                title = "About Echo",
                                onClick = onNavigateToAbout
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = Icons.Default.Policy,
                                title = "Terms & Conditions",
                                onClick = onNavigateToTerms
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = Icons.Default.Policy,
                                title = "Privacy Policy",
                                onClick = onNavigateToPrivacy
                            )
                        }
                    }
                }

                // LOGOUT
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    EchoCard(
                        modifier = Modifier.clickable { viewModel.logout(onLogout) },
                        backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Log out",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // DELETE ACCOUNT
                item {
                    var showDeleteDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                    if (showDeleteDialog) {
                       androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Account") },
                            text = { Text("Are you sure you want to delete your account? This action cannot be undone. You will lose all credits, history, and connections.") },
                            confirmButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        showDeleteDialog = false
                                        viewModel.deleteAccount(onLogout)
                                    },
                                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .clickable { showDeleteDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                         Text(
                            "Delete Account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = showChevron, onClick = onClick) // Disable click effect if no action
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    iconTint.copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showChevron) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    isLoading: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.defaultMinSize(minWidth = 52.dp, minHeight = 48.dp)
        ) {
             if (isLoading) {
                 androidx.compose.material3.CircularProgressIndicator(
                     modifier = Modifier.size(20.dp),
                     strokeWidth = 2.dp,
                     color = MaterialTheme.colorScheme.primary
                 )
             } else {
                 Switch(
                     checked = checked,
                     onCheckedChange = onCheckedChange,
                     colors = SwitchDefaults.colors(
                         checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                         checkedTrackColor = MaterialTheme.colorScheme.primary,
                         uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                         uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                     )
                 )
             }
        }
    }
}
