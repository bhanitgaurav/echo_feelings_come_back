package com.bhanit.apps.echo.features.settings.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.core.presentation.components.DisableInputActions
import com.bhanit.apps.echo.core.theme.SurfaceGlass
import com.bhanit.apps.echo.core.theme.SurfaceGlassStrong
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<NotificationSettingsViewModel>()

    val feelingsEnabled by viewModel.feelingsEnabled.collectAsState()
    val checkinsEnabled by viewModel.checkinsEnabled.collectAsState()
    val reflectionsEnabled by viewModel.reflectionsEnabled.collectAsState()
    val rewardsEnabled by viewModel.rewardsEnabled.collectAsState()
    val updatesEnabled by viewModel.updatesEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val inactiveRemindersEnabled by viewModel.inactiveRemindersEnabled.collectAsState()

    val permissionHandler: com.bhanit.apps.echo.core.permission.PermissionHandler = org.koin.compose.koinInject()
    var permissionStatus by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.bhanit.apps.echo.core.permission.PermissionStatus.NOT_DETERMINED) }
    
    // Check permission on resume (using a simple LaunchedEffect for now, effectively checks on entry)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        permissionStatus = permissionHandler.getPermissionStatus()
    }
    
    // Lifecycle observer to re-check on resume would be ideal, but for now matching Dashboard logic.
    // Actually, let's add a simple resume check if possible, or just rely on manual navigation back/forth.
    // For consistency with Dashboard, just LaunchedEffect(Unit).

    // Use theme-aware glass color
    val glassColor = MaterialTheme.colorScheme.surfaceVariant

    val snackbarHostState = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is NotificationSettingsViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.surface
                )
            )
        )
    ) {
        DisableInputActions {
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. Custom Transparent Header
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Notification Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {


                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Permission Banner
                        if (permissionStatus == com.bhanit.apps.echo.core.permission.PermissionStatus.DENIED_FOREVER) {
                            item {
                                EchoCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { permissionHandler.openAppSettings() },
                                    backgroundColor = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "Notifications are disabled",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = "Tap to enable in Settings to receive echoes.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }

                            // CATEGORIES
                        item {
                            SettingsSectionTitle("Categories")
                            EchoCard(backgroundColor = glassColor) {
                                Column {
                                    val loadingStates by viewModel.loadingStates.collectAsState()
                                    
                                    SettingsSwitchItem(
                                        icon = Icons.Default.Favorite,
                                        title = "Feelings & Replies",
                                        checked = feelingsEnabled,
                                        isLoading = loadingStates[com.bhanit.apps.echo.features.auth.domain.SessionRepository.KEY_NOTIFICATIONS_FEELINGS.name] == true,
                                        onCheckedChange = { viewModel.toggleFeelings(it) })

                                    SettingsSwitchItem(
                                        icon = Icons.Default.NotificationsActive,
                                        title = "Gentle Check-ins",
                                        checked = checkinsEnabled,
                                        isLoading = loadingStates[com.bhanit.apps.echo.features.auth.domain.SessionRepository.KEY_NOTIFICATIONS_CHECKINS.name] == true,
                                        onCheckedChange = { viewModel.toggleCheckins(it) })

                                    if (checkinsEnabled) {
                                        SettingsSwitchItem(
                                            icon = Icons.Default.Assignment, // Placeholder icon
                                            title = "Allow reminders when inactive",
                                            checked = inactiveRemindersEnabled,
                                            isLoading = loadingStates[com.bhanit.apps.echo.features.auth.domain.SessionRepository.KEY_NOTIFICATIONS_INACTIVE_REMINDERS.name] == true,
                                            onCheckedChange = {
                                                viewModel.toggleInactiveReminders(
                                                    it
                                                )
                                            })
                                    }

                                    SettingsSwitchItem(
                                        icon = Icons.Default.SelfImprovement,
                                        title = "Reflections & Summaries",
                                        checked = reflectionsEnabled,
                                        isLoading = loadingStates[com.bhanit.apps.echo.features.auth.domain.SessionRepository.KEY_NOTIFICATIONS_REFLECTIONS.name] == true,
                                        onCheckedChange = { viewModel.toggleReflections(it) })

                                    SettingsSwitchItem(
                                        icon = Icons.Default.Redeem,
                                        title = "Rewards & Credits",
                                        checked = rewardsEnabled,
                                        isLoading = loadingStates[com.bhanit.apps.echo.features.auth.domain.SessionRepository.KEY_NOTIFICATIONS_REWARDS.name] == true,
                                        onCheckedChange = { viewModel.toggleRewards(it) })
                                }
                            }
                        }

                        // SYSTEM
                        item {
                            SettingsSectionTitle("System")
                            EchoCard(backgroundColor = glassColor) {
                                Column {
                                    SettingsSwitchItem(
                                        icon = Icons.Default.SystemUpdate,
                                        title = "App Updates & Important Notices",
                                        checked = updatesEnabled, // Always true
                                        onCheckedChange = { /* Disabled */ })
                                    Text(
                                        text = "Some notifications are required to keep Echo safe and functional.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            start = 72.dp, end = 16.dp, bottom = 16.dp
                                        )
                                    )
                                }
                            }
                        }

                        // ADVANCED
                        item {
                            SettingsSectionTitle("Advanced")
                            EchoCard(backgroundColor = glassColor) {
                                Column {
                                    SettingsSwitchItem(
                                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                                        title = "Sound",
                                        checked = soundEnabled,
                                        onCheckedChange = { viewModel.toggleSound(it) })
                                    SettingsSwitchItem(
                                        icon = Icons.Default.Vibration,
                                        title = "Vibration",
                                        checked = vibrationEnabled,
                                        onCheckedChange = { viewModel.toggleVibration(it) })
                                }
                            }
                        }
                    }
                }

            }
        }
        
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}