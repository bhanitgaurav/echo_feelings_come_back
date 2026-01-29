package com.bhanit.apps.echo.features.dashboard.presentation

import com.bhanit.apps.echo.features.messaging.domain.Emotion
import com.bhanit.apps.echo.core.tutorial.coachMark
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bhanit.apps.echo.core.designsystem.components.EchoButton
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.core.presentation.components.LogicWrapper
import com.bhanit.apps.echo.core.theme.AccentEcho
import com.bhanit.apps.echo.core.theme.ColorSchemeScrim
import com.bhanit.apps.echo.core.theme.EmotionAnger
import com.bhanit.apps.echo.core.theme.EmotionHappy
import com.bhanit.apps.echo.core.theme.EmotionJealousy
import com.bhanit.apps.echo.core.theme.EmotionLove
import com.bhanit.apps.echo.core.theme.EmotionSad
import com.bhanit.apps.echo.core.theme.PrimaryEcho
import com.bhanit.apps.echo.core.theme.SecondaryEcho
import com.bhanit.apps.echo.data.model.DailyActivity
import com.bhanit.apps.echo.data.model.TimeRange
import com.bhanit.apps.echo.features.dashboard.presentation.components.DashboardShimmer
import com.bhanit.apps.echo.features.dashboard.presentation.components.StreakItem
import com.bhanit.apps.echo.features.messaging.presentation.fadingEdge
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI


//@Preview
//@Composable
//fun DashboardScreenPreview() {
//    // Mock State for Preview
//    val mockState = DashboardState(
//        userName = "Bhanit",
//        analytics = AnalyticsDTO(
//            totalSent = 10,
//            totalReceived = 5,
//            currentStreak = 12,
//            emotionBreakdown = mapOf("LOVE" to 5, "ANGER" to 2, "JEALOUSY" to 8),
//            sentEmotionBreakdown = mapOf("LOVE" to 2, "ANGER" to 1),
//            receivedEmotionBreakdown = mapOf("LOVE" to 3, "ANGER" to 1, "JEALOUSY" to 8),
//            dailyActivity = listOf(
//                DailyActivity("2023-10-01", 5, 2, mapOf("LOVE" to 4, "ANGER" to 3)),
//                DailyActivity("2023-10-02", 3, 5, mapOf("LOVE" to 2, "JEALOUSY" to 6)),
//                DailyActivity("2023-10-03", 8, 1, mapOf("LOVE" to 6, "ANGER" to 3))
//            )
//        )
//    )
//    MaterialTheme {
//        DashboardScreenContent(
//            state = mockState,
//            onNavigateToSend = {},
//            onNavigateToInbox = {},
//            onNavigateToSettings = {},
//            onFilterTypeSelected = {},
//            onTimeRangeSelected = {},
//            onCycleFilter = {},
//            onToggleEmotions = {  }
//        )
//    }
//}


@OptIn(KoinExperimentalAPI::class)
@Composable
fun DashboardScreen(
    onNavigateToSend: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToScanQr: () -> Unit = {},
    onNavigateToStreaks: () -> Unit = {},
    onNavigateToJourney: () -> Unit = {}
) {
    val viewModel = koinViewModel<DashboardViewModel>()
    val permissionHandler: com.bhanit.apps.echo.core.permission.PermissionHandler =
        org.koin.compose.koinInject()
    val state by viewModel.state.collectAsState()

    var permissionStatus by remember { mutableStateOf(com.bhanit.apps.echo.core.permission.PermissionStatus.GRANTED) }
    var showQrDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        permissionStatus = permissionHandler.getPermissionStatus()

        if (permissionStatus == com.bhanit.apps.echo.core.permission.PermissionStatus.NOT_DETERMINED) {
            permissionHandler.requestNotificationPermission()
            // Re-check after request (small delay or just rely on next launch,
            // but simpler to just update status if we could,
            // though request returns immediately on some platforms)
        }
    }

    DashboardScreenContent(
        state = state,
        permissionStatus = permissionStatus,
        onOpenSettings = { permissionHandler.openAppSettings() },
        onNavigateToSend = onNavigateToSend,
        onNavigateToInbox = onNavigateToInbox,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToProfile = { showQrDialog = true },
        onFilterTypeSelected = { viewModel.onFilterTypeSelected(it) },
        onTimeRangeSelected = { viewModel.onTimeRangeSelected(it) },
        onCycleFilter = { viewModel.cycleFilter() },
        onToggleEmotions = { viewModel.toggleEmotions() },
        onRetry = viewModel::loadData,
        onNavigateToScanQr = onNavigateToScanQr,
        onNavigateToStreaks = onNavigateToStreaks,
        onNavigateToJourney = onNavigateToJourney
    )

    if (showQrDialog) {
        com.bhanit.apps.echo.features.settings.presentation.profile.ProfileQrDialog(
            username = state.userName,
            photoUrl = state.profilePhoto ?: "",
            referralUrl = state.referralUrl,
            onDismiss = { showQrDialog = false }
        )
    }
}

@Composable
fun DashboardScreenContent(
    state: DashboardState,
    permissionStatus: com.bhanit.apps.echo.core.permission.PermissionStatus,
    onOpenSettings: () -> Unit,
    onNavigateToSend: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onFilterTypeSelected: (String) -> Unit,
    onTimeRangeSelected: (TimeRange) -> Unit,
    onCycleFilter: () -> Unit,
    onToggleEmotions: () -> Unit,
    onRetry: () -> Unit,
    onNavigateToScanQr: () -> Unit,
    onNavigateToStreaks: () -> Unit,
    onNavigateToJourney: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Animation States
    var startAnimation by remember { mutableStateOf(false) }
    val animatableAlpha = remember { Animatable(0f) }
    val animatableSlide = remember { Animatable(50f) }

    LaunchedEffect(Unit) {
        startAnimation = true
        launch {
            animatableAlpha.animateTo(1f, animationSpec = tween(1000))
        }
        launch {
            animatableSlide.animateTo(
                0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToScanQr,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 80.dp) // Adjust for bottom nav if needed
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        // Sky & Trust Background
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
                ).windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)
        ) {
            LogicWrapper(
                isLoading = state.isLoading && state.analytics == null,
                error = state.error,
                onRetry = onRetry,
                loadingContent = { DashboardShimmer() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Header Row
                    var isProfileLoading by remember { mutableStateOf(false) }

                    com.bhanit.apps.echo.core.presentation.components.EchoScreenHeader(
                        title = state.userName,
                        profileImageUrl = state.profilePhoto,
                        isLoading = isProfileLoading,
                        onProfileClick = {
                            if (isProfileLoading) return@EchoScreenHeader
                            isProfileLoading = true
                            // Instant response as requested
                            onNavigateToProfile()
                            isProfileLoading = false
                        },
                        modifier = Modifier.graphicsLayer {
                            alpha = animatableAlpha.value
                            translationY = animatableSlide.value
                        },
                        profileModifier = Modifier.coachMark(
                            id = "profile_qr",
                            title = "Share Your Echo",
                            description = "Tap your profile picture to show your QR code and connect instantly."
                        ),
                    )

                    // Permission Denied Banner
                    if (permissionStatus == com.bhanit.apps.echo.core.permission.PermissionStatus.DENIED_FOREVER) {
                        EchoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                                .padding(horizontal = 24.dp),
                            onClick = { onOpenSettings() },
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {

                        // In-App Notification Card
                        state.notification?.let { notif ->
                            val cardColor = when (notif.sentiment) {
                                "POSITIVE" -> com.bhanit.apps.echo.core.theme.EmotionLove.copy(alpha = 0.8f) // Pinkish
                                "DIFFICULT" -> com.bhanit.apps.echo.core.theme.EmotionSad.copy(alpha = 0.8f)  // Blue
                                "HEAVY" -> com.bhanit.apps.echo.core.theme.EmotionSad.copy(alpha = 0.9f)      // Deep Blue (Slightly stronger)
                                "STREAK" -> com.bhanit.apps.echo.core.theme.EmotionJealousy.copy(
                                    alpha = 0.8f
                                ) // Red/Yellow
                                else -> MaterialTheme.colorScheme.tertiaryContainer // Default Fallback
                            }

                            EchoCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp), // Add spacing after
                                backgroundColor = cardColor
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (notif.category) {
                                            "STREAK" -> Icons.Default.Whatshot
                                            "REFLECTION" -> Icons.Default.Face
                                            else -> Icons.Default.Favorite
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Streak Overview
                        state.streakData?.let { streaks ->
                            EchoCard(
                                modifier = Modifier.coachMark(
                                    id = "streak_card",
                                    title = "Your Streaks",
                                    description = "Keep the flame alive! Build streaks by connecting daily."
                                ),
                                onClick = { onNavigateToStreaks() },
                                backgroundColor = MaterialTheme.colorScheme.surface
                            ) {
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
                                        icon = androidx.compose.material.icons.Icons.Filled.Forum, // Conversation icon
                                        color = SecondaryEcho
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }


                        // Dynamic Total Card (Glass EchoCard)
                        // User requested: "All(Sent+Received), total(sent) etc"
                        val totalLabel = when (state.filterType) {
                            "ALL" -> "Total Activity"
                            "SENT" -> "Total Sent"
                            "RECEIVED" -> "Total Received"
                            else -> "Total Activity"
                        }
                        val targetTotalValue = when (state.filterType) {
                            "ALL" -> (state.analytics?.totalSent
                                ?: 0) + (state.analytics?.totalReceived
                                ?: 0)

                            "SENT" -> state.analytics?.totalSent ?: 0
                            "RECEIVED" -> state.analytics?.totalReceived ?: 0
                            else -> 0
                        }
                        val animatedTotalValue by animateIntAsState(
                            targetValue = targetTotalValue,
                            animationSpec = tween(1000, easing = FastOutSlowInEasing)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Max).graphicsLayer {
                                alpha = animatableAlpha.value
                                translationY = animatableSlide.value
                            },
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                             // Left: Total Activity
                             EchoCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .coachMark(
                                        id = "total_activity",
                                        title = "Your Impact",
                                        description = "See the total echoes you've sent and received."
                                    ),
                                backgroundColor = MaterialTheme.colorScheme.primary
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = totalLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = animatedTotalValue.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            // Right: Premium / Journey Shortcut
                            EchoCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .coachMark(
                                        id = "dashboard_journey_tutorial",
                                        title = "Your Journey",
                                        description = "Track your credits, rewards, and progress here."
                                    ),
                                onClick = { onNavigateToJourney() },
                                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                                border = null
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star, // Premium/Journey Icon
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color(0xFFFFD700), // Gold
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Credits & Rewards",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "Your Journey",
                                        style = MaterialTheme.typography.titleMedium, // Slightly smaller to fit
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        // Activity Graph - Conditionally Visible
                        state.analytics?.let { analytics ->
                             if (state.showActivityOverview) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Activity Overview",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        // Emotion Toggle Switch
                                        val iconRotation by animateFloatAsState(
                                            targetValue = if (state.showEmotions) 180f else 0f,
                                            animationSpec = tween(500)
                                        )

                                        Switch(
                                            checked = state.showEmotions,
                                            onCheckedChange = { onToggleEmotions() },
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    scaleX = 0.7f
                                                    scaleY = 0.7f
                                                }, // Make it smaller
                                            thumbContent = {
                                                Icon(
                                                    imageVector = if (state.showEmotions) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .graphicsLayer { rotationZ = iconRotation }
                                                )
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }

                                    TextButton(onClick = onCycleFilter) {
                                        Text(
                                            text = state.filterType,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Box {
                                    EchoCard(
                                        modifier = Modifier.coachMark(
                                            id = "activity_graph",
                                            title = "Emotional Journey",
                                            description = "Visualize your emotional trends and activity patterns here."
                                        ),
                                        backgroundColor = MaterialTheme.colorScheme.surface,
                                        elevation = 1.dp,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            ActivityGraph(
                                                data = analytics.dailyActivity,
                                                filterType = state.filterType,
                                                showEmotions = state.showEmotions,
                                                availableEmotions = state.emotions,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .padding(16.dp)
                                                    .graphicsLayer {
                                                        alpha = animatableAlpha.value
                                                        scaleX = animatableAlpha.value
                                                        scaleY = animatableAlpha.value
                                                    }
                                            )
                                        }
                                    }

                                }
                                // Time Filters Chips (Glass Style)

                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .fadingEdge(
                                            Brush.horizontalGradient(
                                                0f to Color.Transparent,
                                                0.02f to MaterialTheme.colorScheme.onSurface,
                                                0.98f to MaterialTheme.colorScheme.onSurface,
                                                1f to Color.Transparent
                                            )
                                        ),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    items(TimeRange.entries) { range ->
                                        FilterChip(
                                            selected = state.selectedTimeRange == range,
                                            onClick = { onTimeRangeSelected(range) },
                                            label = {
                                                Text(
                                                    text = range.label,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                containerColor = Color.Transparent,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                borderColor = if (state.selectedTimeRange == range) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                                                    alpha = 0.3f
                                                ),
                                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                                enabled = true,
                                                selected = state.selectedTimeRange == range
                                            ),
                                            shape = CircleShape,
                                        )
                                    }

                                }
                                Spacer(Modifier.height(32.dp))
                             }

                            Text(
                                text = "Emotional Breakdown",
                                style = MaterialTheme.typography.titleLarge, // Using TitleLarge for section headers
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Start),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(16.dp))

                            val currentEmotions = when (state.filterType) {
                                "SENT" -> analytics.sentEmotionBreakdown
                                "RECEIVED" -> analytics.receivedEmotionBreakdown
                                else -> analytics.emotionBreakdown
                            }

                            val totalEmotions =
                                currentEmotions.values.sum().toFloat().coerceAtLeast(1f)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                currentEmotions.forEach { (emotionKey, count) ->
                                    val emotionName =
                                        state.emotions.find { it.id == emotionKey }?.displayName
                                            ?: "Unknown"
                                    val colorHex =
                                        state.emotions.find { it.id == emotionKey }?.colorHex
                                            ?: 0xFFCCCCCC
                                    StatRow(
                                        label = emotionName,
                                        value = count.toString(),
                                        color = Color(colorHex),
                                        percentage = count.toFloat() / totalEmotions
                                    )
                                }
                                if (currentEmotions.isEmpty()) {
                                    Text(
                                        "No echoes yet. Start the conversation!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } ?: run {
                            // Empty State
                            EchoCard(backgroundColor = MaterialTheme.colorScheme.surfaceVariant) {
                                Column(
                                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "No Activity",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "Start your Echo journey!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Send an Echo to a friend to see stats here.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    EchoButton(text = "Find Friends", onClick = onNavigateToSend)
                                }
                            }
                        }

                    Spacer(Modifier.height(100.dp)) // Data for scroll and bottom nav
                }
            }
        }
    }
}
}

@Composable
fun ActivityGraph(
    data: List<DailyActivity>,
    filterType: String,
    showEmotions: Boolean,
    availableEmotions: List<Emotion> = emptyList(),
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val sentData = data.map { it.sent.toFloat() }
    val receivedData = data.map { it.received.toFloat() }
    val showSent = filterType == "ALL" || filterType == "SENT"
    val showReceived = filterType == "ALL" || filterType == "RECEIVED"
    val maxVal = (sentData + receivedData).maxOrNull()?.coerceAtLeast(10f) ?: 10f // Avoid 0

    val density = androidx.compose.ui.platform.LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }

    val sentAlpha by animateFloatAsState(
        targetValue = if (showSent) 1f else 0f,
        animationSpec = tween(500)
    )
    val receivedAlpha by animateFloatAsState(
        targetValue = if (showReceived) 1f else 0f,
        animationSpec = tween(500)
    )
    val emotionsAlpha by animateFloatAsState(
        targetValue = if (showEmotions) 1f else 0f,
        animationSpec = tween(500)
    )

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val sentColor = PrimaryEcho
    val receivedColor = SecondaryEcho
    val gridColor = ColorSchemeScrim

    BoxWithConstraints(modifier = modifier) {
        val width = with(density) { maxWidth.toPx() }
        val height = with(density) { maxHeight.toPx() }
        val pointsCount = data.size
        val stepX = if (pointsCount > 1) width / (pointsCount - 1) else 0f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset: Offset ->
                            if (pointsCount > 1) {
                                val index = (offset.x / stepX).times(1f).let {
                                    if (it.isFinite()) kotlin.math.round(it).toInt()
                                        .coerceIn(0, pointsCount - 1) else 0
                                }
                                selectedIndex = index
                                tryAwaitRelease()
                                selectedIndex = null
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset: Offset ->
                            if (pointsCount > 1) {
                                val index = (offset.x / stepX).times(1f).let {
                                    if (it.isFinite()) kotlin.math.round(it).toInt()
                                        .coerceIn(0, pointsCount - 1) else 0
                                }
                                selectedIndex = index
                            }
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    ) { change: androidx.compose.ui.input.pointer.PointerInputChange, _: Float ->
                        if (pointsCount > 1) {
                            val index = (change.position.x / stepX).times(1f).let {
                                if (it.isFinite()) kotlin.math.round(it).toInt()
                                    .coerceIn(0, pointsCount - 1) else 0
                            }
                            selectedIndex = index
                        }
                    }
                }
        ) {
            // Draw Grid Lines (Cleaner)
            drawLine(color = gridColor, start = Offset(0f, height), end = Offset(width, height))
            drawLine(color = gridColor, start = Offset(0f, 0f), end = Offset(width, 0f))
            drawLine(
                color = gridColor,
                start = Offset(0f, height / 2),
                end = Offset(width, height / 2)
            )

            fun drawLinePath(
                dataPoints: List<Float>,
                color: Color,
                strokeWidth: Float = 3.dp.toPx(),
            ) {
                val path = Path()
                if (dataPoints.isEmpty()) return

                if (dataPoints.size == 1) {
                    val value = dataPoints.first()
                    val y = height - ((value / maxVal) * height)
                    path.moveTo(0f, y)
                    path.lineTo(width, y)
                } else {
                    dataPoints.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - ((value / maxVal) * height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Trace Animation: Clip based on progress
            val clipWidth = width * progress.value
            clipRect(right = clipWidth) {
                // Draw Emotion Lines
                if (emotionsAlpha > 0f) {
                    val allEmotions = data.flatMap { it.emotions.keys }.distinct()
                    allEmotions.forEach { emotionKey ->
                        val emotionData = data.map { (it.emotions[emotionKey] ?: 0).toFloat() }
                        // Find emotion in available list
                        val colorHex =
                            availableEmotions.find { it.id == emotionKey }?.colorHex
                                ?: 0xFFCCCCCC
                        drawLinePath(
                            emotionData,
                            Color(colorHex).copy(alpha = emotionsAlpha),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                if (sentAlpha > 0f) drawLinePath(sentData, sentColor.copy(alpha = sentAlpha))
                if (receivedAlpha > 0f) drawLinePath(
                    receivedData,
                    receivedColor.copy(alpha = receivedAlpha)
                )
            }

            // Draw Indicators
            selectedIndex?.let { index ->
                if (index in data.indices) {
                    val x = index * stepX
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(10f, 10f),
                            0f
                        )
                    )

                    val item = data[index]
                    fun drawDot(value: Int, color: Color) {
                        val y = height - ((value.toFloat() / maxVal) * height)
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
                    }



                    if (sentAlpha > 0f) drawDot(item.sent, sentColor.copy(alpha = sentAlpha))
                    if (receivedAlpha > 0f) drawDot(
                        item.received,
                        receivedColor.copy(alpha = receivedAlpha)
                    )
                    if (emotionsAlpha > 0f) {
                        item.emotions.forEach { (emotionKey, count) ->
                            val colorHex =
                                availableEmotions.find { it.id == emotionKey }?.colorHex
                                    ?: 0xFFCCCCCC
                            drawDot(count, Color(colorHex).copy(alpha = emotionsAlpha))
                        }
                    }
                }
            }
        }

        // Tooltip Overlay
        selectedIndex?.let { index ->
            val item = data.getOrNull(index)
            if (item != null) {
                val xOffset = with(density) { (index * stepX).toDp() }
                // Smart positioning: if x > width/2, tooltip goes to left, else right
                val isRight = index * stepX < width / 2
                val tooltipOffset =
                    if (isRight) xOffset + 10.dp else xOffset - 140.dp // approx width

                Box(
                    modifier = Modifier
                        .offset(x = tooltipOffset, y = 0.dp)
                        .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            Color.LightGray.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Column {
                        val formattedText = try {
                            // Try parsing as DateTime (Hourly data)
                            val localDateTime = LocalDateTime.parse(item.date)
                            val now =
                                Clock.System.now()
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                            val month = localDateTime.month.name.take(3).lowercase()
                                .replaceFirstChar { it.uppercase() }
                            val day = localDateTime.dayOfMonth

                            val datePart = if (localDateTime.year != now.year) {
                                "$month $day, ${localDateTime.year}"
                            } else {
                                "$month $day"
                            }

                            val hour = localDateTime.hour
                            val minute = localDateTime.minute.toString().padStart(2, '0')
                            val period = if (hour < 12) "AM" else "PM"
                            val hour12 = if (hour % 12 == 0) 12 else hour % 12
                            val timePart = "$hour12:$minute $period"

                            "$datePart, $timePart"
                        } catch (e: Exception) {
                            // Fallback to Date only (Daily data)
                            try {
                                val localDate = LocalDate.parse(item.date)
                                val now = Clock.System.now()
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                val month = localDate.month.name.take(3).lowercase()
                                    .replaceFirstChar { it.uppercase() }
                                val day = localDate.dayOfMonth

                                if (localDate.year != now.year) {
                                    "$month $day, ${localDate.year}"
                                } else {
                                    "$month $day"
                                }
                            } catch (e2: Exception) {
                                item.date
                            }
                        }

                        Text(
                            text = formattedText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(Modifier.height(4.dp))
                        if (showSent) Text(
                            "Sent: ${item.sent}",
                            style = MaterialTheme.typography.bodySmall,
                            color = sentColor
                        )
                        if (showReceived) Text(
                            "Received: ${item.received}",
                            style = MaterialTheme.typography.bodySmall,
                            color = receivedColor
                        )
                        if (showEmotions && item.emotions.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            item.emotions.forEach { (emotion, count) ->
                                Text(
                                    "$emotion: $count",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = getColorForEmotion(emotion)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CircularProgress(
    percentage: Float,
    color: Color,
    trackColor: Color? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val finalTrackColor = trackColor ?: ColorSchemeScrim
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(percentage, animationSpec = tween(1500))
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
        Canvas(modifier = Modifier.size(60.dp)) {
            // Background Circle
            drawArc(
                color = finalTrackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
            // Progress Circle using animate value
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.value,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${(progress.value * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

fun getColorForEmotion(emotion: String): Color {
    return when (emotion) {
        "LOVE" -> EmotionLove
        "ANGER" -> EmotionAnger
        "JEALOUSY" -> EmotionJealousy
        "HURT" -> EmotionSad
        "APPRECIATION" -> EmotionHappy
        "CUSTOM" -> AccentEcho
        else -> Color.Gray
    }
}

@Composable
fun StatRow(label: String, value: String, color: Color, percentage: Float) {
    var showPercentage by remember { mutableStateOf(false) }

    EchoCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPercentage = !showPercentage }
        ) {
            WaterWave(
                color = color.copy(alpha = 0.2f),
                progress = percentage,
                modifier = Modifier.matchParentSize()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Crossfade/Animate content change
                androidx.compose.animation.Crossfade(targetState = showPercentage) { isPercentage ->
                    Text(
                        text = if (isPercentage) "${(percentage * 100).toInt()}%" else value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun WaterWave(
    color: Color,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween<Float>(
                durationMillis = 5000,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        )
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val waterHeight = height * progress
        val waveAmplitude = 10.dp.toPx()
        val waveFrequency = 0.02f

        val path = Path()
        path.moveTo(0f, height)
        path.lineTo(width, height)

        val topY = height - waterHeight

        // Draw wave
        for (x in width.toInt() downTo 0) {
            val y = topY + kotlin.math.sin((x + translateAnim) * waveFrequency) * waveAmplitude
            path.lineTo(x.toFloat(), y.toFloat())
        }

        path.lineTo(0f, topY) // Close loop properly
        path.close()
        drawPath(path = path, color = color)
    }
}

fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
