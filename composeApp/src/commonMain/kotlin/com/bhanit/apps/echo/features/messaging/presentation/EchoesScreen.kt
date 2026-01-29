package com.bhanit.apps.echo.features.messaging.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.bhanit.apps.echo.core.tutorial.coachMark
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.FilterList
import echo.composeapp.generated.resources.Res
import echo.composeapp.generated.resources.echo_base
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.designsystem.components.EchoButton
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.core.presentation.components.LogicWrapper
import com.bhanit.apps.echo.core.presentation.components.DisableInputActions
import com.bhanit.apps.echo.core.theme.PrimaryEcho
import com.bhanit.apps.echo.features.messaging.domain.Message
import com.bhanit.apps.echo.features.messaging.presentation.components.EchoesShimmer
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EchoesScreen(
    onNavigateBack: () -> Unit,
    viewModel: EchoesViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var expandedMessageId by remember { mutableStateOf<String?>(null) }
    // Test Mode: Preview UI directly


    // Share Manager Strategy
    val context = androidx.compose.runtime.remember { 
        // Platform Context, we need to get it via koin or composition local if simple
        // Since we are in commonMain, we can't easily get 'Context' for constructor, 
        // BUT our ShareManager impls are Expect/Actual.
        // If the 'actual' constructor requires Context (Android), we need to handle that.
        // Wait, 'expect class ShareManager' construction might differ per platform if we didn't standarize it.
        // We defined 'expect class ShareManager' which implies a common constructor.
        // But Android implementation added 'constructor(context: Context)'. This is a mismatch invalidating commonMain usage.
        // We should use a factory or a @Composable helper to get it.
    } 
    // FIX: The Android implementation I wrote takes a Context in constructor. The common expect class implied empty constructor.
    // I need to fix the instantiation strategy.
    // Best way in Compose Multiplatform: use a Composable function `rememberShareManager()` that helps.
    
    // Changing strategy: I will inject ShareManager, or better, make a Composable function `rememberShareManager()` in common 
    // that handles the platform nuances (LocalContext on Android).
    
    // Share Manager Logic
    val shareManager = com.bhanit.apps.echo.features.messaging.presentation.components.rememberShareManager()
    val settingsViewModel = koinViewModel<com.bhanit.apps.echo.features.settings.presentation.settings.SettingsViewModel>()
    val shareAspectRatio by settingsViewModel.shareAspectRatio.collectAsState()
    
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    // Tutorial Controller
    val coachMarkController = com.bhanit.apps.echo.core.tutorial.LocalCoachMarkController.current
    val isTutorialActive = coachMarkController.activeTarget?.id == "echo_interaction_tutorial"
    var delayedTutorialReveal by remember { mutableStateOf(false) }

    LaunchedEffect(isTutorialActive) {
        if (isTutorialActive) {
            kotlinx.coroutines.delay(1000)
            delayedTutorialReveal = true
        } else {
            delayedTutorialReveal = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.trackHomeScreenView()
        viewModel.loadEchoes()
    }

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
            ).padding(top = 48.dp)
    ) {
        DisableInputActions {
            Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Header Row
            com.bhanit.apps.echo.core.presentation.components.EchoScreenHeader(
                title = "Echoes",
                actions = {
                    IconButton(
                        onClick = { if (state.activeTab == 0) showFilterSheet = true },
                        enabled = state.activeTab == 0,
                        modifier = Modifier.alpha(if (state.activeTab == 0) 1f else 0f)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )

            // 2. Filter Status (Active Filters Indicator) - Moved inside content
            // Spacer removed to allow cleaner layout


            // 3. Tabs (Echoes / Echoed Back)
            com.bhanit.apps.echo.core.designsystem.components.EchoTabRow(
                selectedTabIndex = state.activeTab,
                tabs = listOf("Echoes", "Echoed Back"),
                onTabSelected = viewModel::setActiveTab,
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)
            )

            LogicWrapper(
                isLoading = state.isLoading && (if(state.activeTab == 0) state.echoesMessages else state.repliesMessages).isEmpty(),
                error = state.error,
                onRetry = { 
                    if (state.activeTab == 0) viewModel.loadEchoes(isNextPage = false) 
                    else viewModel.loadRepliedMessages(isNextPage = false)
                },
                loadingContent = { EchoesShimmer() }
            ) {
                // Animate content switching between tabs
                AnimatedContent(
                    targetState = state.activeTab,
                    transitionSpec = {
                        (slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut())
                    },
                    label = "EchoesContentTransition"
                ) { targetTab ->
                    // Determine which list to show based on active tab
                    val currentMessages = if (targetTab == 0) state.echoesMessages else state.repliesMessages
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filter Status (Moved here)
                        if (targetTab == 0 && (state.selectedFeelings.isNotEmpty() || state.startDate != null)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Filters Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PrimaryEcho
                                )
                                TextButton(
                                    onClick = viewModel::clearFilters,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Clear", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        if (currentMessages.isEmpty()) {
                             Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                     Icon(
                                         painter = painterResource(Res.drawable.echo_base),
                                         contentDescription = null,
                                         modifier = Modifier.size(64.dp),
                                         tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                     )
                                     Spacer(modifier = Modifier.height(16.dp))
                                     Text(
                                         text = if (targetTab == 0) "No echoes yet" else "No replies received yet",
                                         style = MaterialTheme.typography.titleMedium,
                                         color = MaterialTheme.colorScheme.onSurfaceVariant
                                     )
                                 }
                             }
                        } else {
                             // Show Progress for pagination overlapping list if needed, or LogicWrapper handles main loading
                             // We need the LinearProgressIndicator for pagination here if list is not empty
                             
                             Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                                 val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                                 // Mutual Exclusion State for Swipe-to-Reveal
                                 var revealedCardId by remember { mutableStateOf<String?>(null) }
                            
                            // Infinite Scroll Logic
                            val shouldLoadMore = remember {
                                androidx.compose.runtime.derivedStateOf {
                                    val totalItemsCount = listState.layoutInfo.totalItemsCount
                                    val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    // Trigger when 3 items left
                                    !state.endReached && !state.isPaginationLoading && totalItemsCount > 0 && (lastVisibleItemIndex >= totalItemsCount - 3)
                                }
                            }

                            LaunchedEffect(shouldLoadMore.value) {
                                if (shouldLoadMore.value) {
                                    if (targetTab == 0) viewModel.loadEchoes(isNextPage = true) 
                                    else viewModel.loadRepliedMessages(isNextPage = true)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState,
                                contentPadding = PaddingValues(bottom = 80.dp),
                            ) {
                                // Grouped Content
                                val grouped = currentMessages.groupBy { 
                                   com.bhanit.apps.echo.core.util.DateTimeUtils.formatMessageTime(it.timestamp).split(",").firstOrNull()?.trim() ?: "Recent"
                                }
                                
                                grouped.forEach { (dateHeader, messages) ->
                                    stickyHeader {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = dateHeader,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    itemsIndexed(messages, key = { _, item -> item.id }) { index, message ->
                                        // Determine if this is the very first item overall to show tutorial
                                        // Since we have sticky headers, we need to be careful.
                                        // Easier way: Store a transient "isFirst" boolean based on global index if possible?
                                        // Or just check if it's the first message of the first group.
                                        val isFirstItem = index == 0 && messages == grouped.values.first()

                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                                .animateItem()
                                                .then(
                                                    if (isFirstItem) {
                                                        Modifier.coachMark(
                                                            id = "echo_interaction_tutorial",
                                                            title = "Interact with Echoes",
                                                            description = "Tap anywhere to reply. Swipe left to share."
                                                        )
                                                    } else Modifier
                                                )
                                        ) {
                                            EchoesItem(
                                                message = message,
                                                isExpanded = message.id == expandedMessageId,
                                                onToggleExpand = {
                                                    expandedMessageId =
                                                        if (expandedMessageId == message.id) null else message.id
                                                },
                                                canReply = targetTab == 0 && (state.replyStatus[message.id] == true),
                                                onReply = { content ->
                                                    viewModel.replyToMessage(
                                                        message.id,
                                                        content
                                                    )
                                                },
                                                isMyEchoInfo = targetTab == 1,
                                                onShare = { msg ->
                                                    scope.launch {
                                                        shareManager.captureAndShare(
                                                            com.bhanit.apps.echo.features.messaging.domain.EchoShareModel(
                                                                receivedText = msg.content,
                                                                repliedText = msg.replyContent, // Only share if reply exists
                                                                moodColor = msg.emotion.colorHex,
                                                                aspectRatio = shareAspectRatio
                                                                // Defaults for context/footer are already in the model
                                                            )
                                                        )
                                                    }
                                                },
                                                isRevealed = revealedCardId == message.id || (isFirstItem && delayedTutorialReveal),
                                                onRevealChange = { isRevealed ->
                                                    revealedCardId = if (isRevealed) message.id else null
                                                },
                                                contactsMap = state.contactsMap // Pass map
                                            )
                                        }
                                    }
                                }
                            }
                            
                            if (state.isLoading && state.echoesMessages.isNotEmpty()) {
                                androidx.compose.material3.LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                                    color = PrimaryEcho,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            }
        }
        }

        if (showFilterSheet) {
            com.bhanit.apps.echo.features.messaging.presentation.components.EchoesFilterSheet(
                selectedFeelings = state.selectedFeelings,
                availableEmotions = state.availableEmotions,
                onFeelingToggle = viewModel::toggleFeelingFilter,
                startDate = state.startDate,
                endDate = state.endDate,
                onDateRangeSelected = viewModel::setDateRange,
                onClearFilters = viewModel::clearFilters,
                onDismiss = { showFilterSheet = false }
            )
        }


    }
}

@Composable
fun EchoesItem(
    message: Message,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    canReply: Boolean,
    onReply: (String) -> Unit,
    isMyEchoInfo: Boolean = false, // Added
    onShare: (Message) -> Unit, // Added callback
    isRevealed: Boolean, // Added state
    onRevealChange: (Boolean) -> Unit, // Added callback
    contactsMap: Map<String, String> = emptyMap() // Added map
) {

    // var expanded by remember { mutableStateOf(false) } // Lifted up to parent
    val expanded = isExpanded
    var replyContent by remember { mutableStateOf("") }
    var isReplying by remember { mutableStateOf(false) }
    val formattedTime = remember(message.timestamp) {
        com.bhanit.apps.echo.core.util.DateTimeUtils.formatMessageTime(message.timestamp)
    }

    // Swipe to Reveal Logic
    com.bhanit.apps.echo.core.designsystem.components.SwipeToRevealCard(
        isRevealed = isRevealed,
        onRevealChange = onRevealChange,
        isSwipeEnabled = !isMyEchoInfo,
        onShareClick = {
            onShare(message)
            onRevealChange(false) // Close card on action
        },
        revealedContent = {
            // Background Share Action
            Box(
                modifier = Modifier
                    .fillMaxSize() // Fill the matchParentSize of background
                    .padding(end = 24.dp) // Align icon properly
                    .clickable {
                        onShare(message)
                        onRevealChange(false) // Close card on action
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                 Box(
                     modifier = Modifier
                         .size(40.dp)
                         .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                     contentAlignment = Alignment.Center
                 ) {
                     Icon(
                         imageVector = androidx.compose.material.icons.Icons.Default.Share,
                         contentDescription = "Share",
                         tint = MaterialTheme.colorScheme.onPrimaryContainer
                     )
                 }
            }
        }
    ) {
        EchoCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onToggleExpand() },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                
                // 1. Message Content (Primary, Enhanced Typography)
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Normal,
                        lineHeight = 28.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (expanded) Int.MAX_VALUE else 3, // Show a bit more preview
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.animateContentSize()
                )
    
                Spacer(modifier = Modifier.height(20.dp))
    
                // 2. Footer Metadata (Emotion Chip, Sender, Time, Actions)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Emotion Chip
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(message.emotion.colorHex).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(message.emotion.colorHex).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = message.emotion.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(message.emotion.colorHex)
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
    
                    // Sender & Time Group
                    Column(horizontalAlignment = Alignment.End) {
                        // RESOLVE NAME LOGIC
                        val displayName = if (isMyEchoInfo) {
                            // "Echoed Back" Tab handled here (I am sender)
                            // Show "To: [Receiver Name]"
                            val receiverName = contactsMap[message.receiverId] ?: message.receiverUsername ?: "Unknown"
                            "To: $receiverName"
                        } else {
                            // Inbox Tab (I am receiver)
                            // Show "[Sender Name]"
                            if (message.isAnonymous) "Anonymous" else message.senderUsername ?: "Unknown"
                        }
                        
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 160.dp) // Prevent pushing left content too much
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
    
                    // Actions Group
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         // Share Button Removed as per request "share icon... show only when swiped"
                         // Only Expansion Indicator remains
                    
                        // Expansion Indicator
                        IconButton(
                            onClick = { onToggleExpand() },
                             modifier = Modifier.padding(start = 4.dp).size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (expanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
    
                // 3. Expandable Reply Section
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))
    
                        if (message.replyContent != null) {
                            val replyTime = message.replyTimestamp?.let {
                                com.bhanit.apps.echo.core.util.DateTimeUtils.formatMessageTime(it)
                            } ?: ""
    
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Column {
                                    // RESOLVE REPLY HEADER
                                    val replyTitle = if (isMyEchoInfo) {
                                         // Reply from the person I sent to
                                         val receiverName = contactsMap[message.receiverId] ?: message.receiverUsername ?: "Someone"
                                         "Reply from $receiverName"
                                    } else {
                                         // My Reply to them
                                         "Your Reply"
                                    }
                                    
                                    Text(
                                        replyTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        message.replyContent!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (replyTime.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            replyTime,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (canReply) {
                            if (isReplying) {
                                 OutlinedTextField(
                                    value = replyContent,
                                    onValueChange = { 
                                        if (it.length <= 50) replyContent = it 
                                    },
                                    label = { Text("Share your response...") },
                                     modifier = Modifier.fillMaxWidth(),
                                     colors = OutlinedTextFieldDefaults.colors(
                                         focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                         unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                         focusedBorderColor = MaterialTheme.colorScheme.primary,
                                         unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                     ),
                                     shape = RoundedCornerShape(12.dp)
                                 )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { isReplying = false }) {
                                        Text(
                                            "Cancel",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
    
    
                                    Spacer(Modifier.width(8.dp))
                                    EchoButton(
                                        text = "Send response",
                                        onClick = {
                                            onReply(replyContent.trim())
                                            isReplying = false
                                        },
                                        // Remove fixed height to allow text to fit
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { isReplying = true },
                                    modifier = Modifier.align(Alignment.End),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Share your response")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



