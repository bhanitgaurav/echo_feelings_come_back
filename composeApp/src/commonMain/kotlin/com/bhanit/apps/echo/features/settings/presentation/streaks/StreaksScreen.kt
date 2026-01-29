package com.bhanit.apps.echo.features.settings.presentation.streaks

import androidx.compose.ui.unit.sp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.core.theme.AccentEcho
import com.bhanit.apps.echo.core.theme.EchoTheme // Removed
import com.bhanit.apps.echo.core.theme.EmotionLove
import com.bhanit.apps.echo.core.theme.PrimaryEcho
import com.bhanit.apps.echo.core.theme.SecondaryEcho
import com.bhanit.apps.echo.features.dashboard.presentation.DashboardViewModel
import com.bhanit.apps.echo.features.dashboard.presentation.components.StreakItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StreaksScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel = koinViewModel<DashboardViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

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
            )
            .padding(top = 48.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
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
                    text = "My Streaks",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                
                // 1. Current Stats Overview
                // Only show if we have data OR if we are loading.
                // If error occurs (data null && !loading), we hide the card completely.
                AnimatedContent(
                    targetState = state.streakData,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                    }
                ) { streaks ->
                    if (streaks != null) {
                        EchoCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StreakItem(
                                    label = "Presence",
                                    value = streaks.presenceStreak,
                                    icon = Icons.Default.Whatshot,
                                    color = PrimaryEcho
                                )
                                StreakItem(
                                    label = "Kindness",
                                    value = streaks.kindnessStreak,
                                    icon = Icons.Default.Favorite,
                                    color = EmotionLove
                                )
                                StreakItem(
                                    label = "Response",
                                    value = streaks.responseStreak,
                                    icon = androidx.compose.material.icons.Icons.Filled.Forum,
                                    color = SecondaryEcho
                                )
                            }
                        }
                    } else if (state.isLoading) {
                        // Loading Placeholder to prevent layout shift
                        EchoCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp) // Approximate height of the real card content
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    } else {
                        // Error or Idle case: Show nothing (Graceful degradation)
                        // Using a zero-size box to satisfy the lambda return type constraint if necessary,
                        // or simply nothing if consistent.
                        Spacer(Modifier.height(0.dp))
                    }
                    }

                Text(
                    text = "Understanding Streaks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 2. Explanations
                
                // Presence
                StreakInfoCard(
                    title = "Presence Streak",
                    icon = Icons.Default.Whatshot,
                    color = PrimaryEcho,
                    description = "Your Presence Streak tracks how often you simply show up for yourself. Just opening the Echo app each day counts.",
                    tip = "Tip: Consistency is key to building an emotional habit."
                )

                // Kindness
                StreakInfoCard(
                    title = "Kindness Streak",
                    icon = Icons.Default.Favorite,
                    color = EmotionLove,
                    description = "Your Kindness Streak grows when you actively send love. Sending an echo with a POSITIVE emotion (like Appreciation or Love) keeps this streak alive.",
                    tip = "Tip: Make someone's day a little brighter."
                )

                // Response
                StreakInfoCard(
                    title = "Response Streak",
                    icon = androidx.compose.material.icons.Icons.Filled.Forum,
                    color = SecondaryEcho,
                    description = "Your Response Streak grows when you reply to someone who shared a feeling with you.\n" +
                            "It reflects how often you acknowledge and engage with others’ emotions.",
                    tip = "Tip: A small response can mean a lot to someone."
                )

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

@Composable
fun StreakInfoCard(
    title: String,
    icon: ImageVector,
    color: Color,
    description: String,
    tip: String
) {
    EchoCard(backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
