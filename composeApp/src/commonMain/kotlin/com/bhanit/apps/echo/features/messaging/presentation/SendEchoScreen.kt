package com.bhanit.apps.echo.features.messaging.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.designsystem.components.EchoButton
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.core.tutorial.coachMark
import com.bhanit.apps.echo.core.network.AppException
import com.bhanit.apps.echo.core.presentation.components.DisableInputActions
import com.bhanit.apps.echo.core.presentation.components.LogicWrapper
import com.bhanit.apps.echo.core.theme.ColorSchemeGlass
import com.bhanit.apps.echo.core.theme.PrimaryEcho
import com.bhanit.apps.echo.features.messaging.domain.Emotion
import com.bhanit.apps.echo.features.messaging.domain.Message
import com.bhanit.apps.echo.features.messaging.presentation.components.EchoesShimmer
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendEchoScreen(
    userId: String? = null,
    username: String? = null,
    initialTab: Int = 0,
    onNavigateBack: () -> Unit,
    viewModel: SendEchoViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabs = listOf("Send Feeling", "History")
    
    LaunchedEffect(selectedTab, userId) {
        if (selectedTab == 1 && userId != null) {
            viewModel.loadHistory(userId)
        }
    }
    
    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.loadUsage(userId)
            viewModel.checkConnectionStatus(userId)
        }
    }

    LaunchedEffect(state.blockSuccess) {
        if (state.blockSuccess) {
            onNavigateBack()
            viewModel.resetState()
        }
    }

    LaunchedEffect(state.removeConnectionSuccess) {
        if (state.removeConnectionSuccess) {
            onNavigateBack()
            viewModel.resetState()
        }
    }
    
    var showBlockDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block User") },
            text = { Text("Are you sure you want to block this user? They will not be able to contact you.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (userId != null) {
                            viewModel.blockUser(userId)
                        }
                        showBlockDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Connection") },
            text = { Text("Are you sure you want to remove this connection? You will need to request to connect again.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (userId != null) {
                            viewModel.removeConnection(userId)
                        }
                        showRemoveDialog = false
                    },
                     colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
    
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { 
                keyboardController?.hide()
                focusManager.clearFocus()
            }
            .background(
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
                // Custom Top Bar with transparent bg
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Echo to ${username ?: "User"}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(Modifier.weight(1f))
                
                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    if (state.isConnected) {
                        androidx.compose.material3.IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {

                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Remove Connection", style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                showRemoveDialog = true
                                showMenu = false
                            },
                             leadingIcon = {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Block, // using Block icon or maybe Close/Delete if available. Block is fine or Clear.
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            colors = androidx.compose.material3.MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        androidx.compose.material3.HorizontalDivider(
                             modifier = Modifier.padding(horizontal = 12.dp),
                             color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Block", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showBlockDialog = true
                                showMenu = false
                            },
                             leadingIcon = {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            colors = androidx.compose.material3.MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error
                            )
                        )
//                        androidx.compose.material3.HorizontalDivider(
//                             modifier = Modifier.padding(horizontal = 12.dp),
//                             color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
//                        )
//                        androidx.compose.material3.DropdownMenuItem(
//                            text = { Text("Report", style = MaterialTheme.typography.bodyMedium) },
//                            onClick = {
//                                // Report functionality
//                                showMenu = false
//                            },
//                             leadingIcon = {
//                                Icon(
//                                    imageVector = androidx.compose.material.icons.Icons.Default.Report,
//                                    contentDescription = null,
//                                    modifier = Modifier.size(20.dp),
//                                    tint = MaterialTheme.colorScheme.onSurface
//                                )
//                            },
//                            colors = androidx.compose.material3.MenuDefaults.itemColors(
//                                textColor = MaterialTheme.colorScheme.onSurface,
//                                leadingIconColor = MaterialTheme.colorScheme.onSurface
//                            )
//                        )
                    }
                }
            }
            if (state.isConnected) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            modifier = if (index == 1) { // History Tab
                                 Modifier.coachMark(
                                     id = "send_echo_history_tutorial",
                                     title = "View History",
                                     description = "Check past sent echoes in the History tab.",
                                     order = 4
                                 )
                            } else Modifier,
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            } else {
                 // Not connected - Show Header
                 Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                     Text(
                        "You are not connected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                     )
                 }
            }

            Spacer(Modifier.height(16.dp))

            if (!state.isConnected) {
                 HistoryView(
                    history = state.history,
                    isLoading = state.isHistoryLoading,
                    error = state.historyError,
                    currentUsername = username,
                    onRetry = {
                         if (userId != null) viewModel.loadHistory(userId)
                    }
                )
            } else {
                when (selectedTab) {
                    0 -> SendView(
                        username = username,
                        isLoading = state.isSending,
                        sendSuccess = state.sendSuccess,
                        error = state.sendError,
    
                        emotions = state.emotions, // Pass emotions
                        remainingLimit = state.remainingDailyLimit,
                        totalLimit = state.dailyLimitTotal,
                        onSend = { emotion, content, isAnonymous ->
                            if (userId != null) {
                                viewModel.sendEcho(userId, emotion, content, isAnonymous)
                            }
                        },
                        onReset = {
                            viewModel.resetState()
                            selectedTab = 1
                        },
                        onBoost = {
                            if (userId != null) viewModel.boostLimit(userId)
                        }
                    )
                    1 -> {
                         HistoryView(
                            history = state.history,
                            isLoading = state.isHistoryLoading,
                            error = state.historyError,
                            currentUsername = username,
                            onRetry = {
                                 if (userId != null) viewModel.loadHistory(userId)
                            }
                        )
                    }
                }
            }
        }
        }
    }
}


@Composable
fun SendView(
    username: String?,
    isLoading: Boolean,
    sendSuccess: Boolean,
    error: AppException?,
    emotions: List<Emotion>, // Added emotions parameter
    remainingLimit: Int?,
    totalLimit: Int?,
    onSend: (Emotion, String, Boolean) -> Unit,
    onReset: () -> Unit,
    onBoost: () -> Unit // Added onBoost
) {
    var selectedEmotion by remember { mutableStateOf<Emotion?>(null) } // Nullable initially
    var content by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf<Emotion?>(null) } // Track which locked emotion tried

    // Pre-select first emotion if available and none selected
    LaunchedEffect(emotions) {
        if (selectedEmotion == null && emotions.isNotEmpty()) {
            selectedEmotion = emotions.firstOrNull { it.isUnlocked } // Pick first unlocked
        }
    }

    if (sendSuccess) {
        AlertDialog(
            onDismissRequest = onReset,
            title = { Text("Success") },
            text = { Text("Echo sent successfully!") },
            confirmButton = {
                Button(onClick = {
                    onReset()
                    content = ""
                }) { Text("OK") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = onReset,
            title = { Text(error?.title ?: "Error") },
            text = { Text(error?.message ?: "Something went wrong") },
            confirmButton = { Button(onClick = onReset) { Text("OK") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
    
    if (showUnlockDialog != null) {
        AlertDialog(
            onDismissRequest = { showUnlockDialog = null },
            title = { Text("Premium Emotion") },
            text = { Text("'${showUnlockDialog?.displayName}' is a premium emotion. Visit the Premiums to unlock it!") },
            confirmButton = { 
                Button(onClick = { showUnlockDialog = null }) { Text("OK") } 
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp) // Increased spacing
    ) {
    // Emotion Picker
    EchoCard(
        modifier = Modifier.coachMark(
            id = "send_echo_emotions_tutorial",
            title = "1. Select Feeling",
            description = "Start by choosing an emotion that matches how you feel.",
            order = 1
        )
    ) {
        Column(Modifier.padding(16.dp)) {
                // Header with Expand Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Select Feeling:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (emotions.isNotEmpty()) {
                        androidx.compose.material3.TextButton(onClick = { isExpanded = !isExpanded }) {
                            Text(if (isExpanded) "Less" else "More")
                        }
                    }
                }
                
                if (remainingLimit != null && totalLimit != null) {
                    Text(
                        "Remaining: $remainingLimit/$totalLimit",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (remainingLimit == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(16.dp))

                if (emotions.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().height(56.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            "No emotions available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(modifier = Modifier.animateContentSize().padding(4.dp)) {
                        if (isExpanded) {
                             // GRID VIEW
                             Column(
                                 modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                 verticalArrangement = Arrangement.spacedBy(16.dp)
                             ) {
                                 emotions.chunked(5).forEach { rowItems ->
                                     Row(
                                         modifier = Modifier.fillMaxWidth(), 
                                         horizontalArrangement = Arrangement.spacedBy(16.dp)
                                     ) {
                                         rowItems.forEach { emotion -> 
                                             EmotionItem(
                                                 emotion = emotion,
                                                 isSelected = selectedEmotion?.id == emotion.id,
                                                 onClick = { 
                                                     if (emotion.isUnlocked) {
                                                         selectedEmotion = emotion 
                                                     } else {
                                                         showUnlockDialog = emotion
                                                     }
                                                 }
                                             )
                                         }
                                     }
                                 }
                             }
                        } else {
                             // LAZY ROW (Collapsed)
                             androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fadingEdge(
                                        Brush.horizontalGradient(
                                            0f to Color.Transparent,
                                            0.02f to MaterialTheme.colorScheme.onSurface,
                                            0.98f to MaterialTheme.colorScheme.onSurface,
                                            1f to Color.Transparent
                                        )
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                items(emotions) { emotion ->
                                    EmotionItem(
                                         emotion = emotion,
                                         isSelected = selectedEmotion?.id == emotion.id,
                                         onClick = { 
                                             if (emotion.isUnlocked) {
                                                 selectedEmotion = emotion 
                                             } else {
                                                 showUnlockDialog = emotion
                                             }
                                         }
                                     )
                                }
                            }
                        }
                    }
                    
                    if (emotions.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Selected: ${selectedEmotion?.displayName ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Content
        OutlinedTextField(
            value = content,
            onValueChange = { 
                if (it.length <= 200) content = it 
            },
            label = { Text("One line about how you felt...") },
            placeholder = {
                Text(
                    selectedEmotion?.exampleText ?: "One line about how you felt...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            modifier = Modifier.fillMaxWidth().height(120.dp).coachMark(
                id = "send_echo_write_tutorial",
                title = "2. Express Yourself",
                description = "Write a short message describing your feeling (max 200 chars).",
                order = 2
            ),
            maxLines = 5,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Anonymity Notice
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
             Icon(
                 imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                 contentDescription = "Anonymous",
                 modifier = Modifier.size(16.dp),
                 tint = MaterialTheme.colorScheme.onSurfaceVariant
             )
             Spacer(androidx.compose.ui.Modifier.width(8.dp))
             Text(
                 "This feeling will be sent anonymously",
                 style = MaterialTheme.typography.labelSmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant
             )
        }

        EchoButton(
            text = if (isLoading) "Sending..." else "Send Echo",
            onClick = {
                selectedEmotion?.let { 
                    if (it.isUnlocked) {
                        onSend(it, content.trim(), true) 
                    } else {
                        // Should have been handled by selection logic, but safety check
                    }
                }
            },
            enabled = !isLoading && selectedEmotion != null && selectedEmotion?.isUnlocked == true && content.isNotBlank() && (remainingLimit == null || remainingLimit > 0),
            modifier = Modifier.fillMaxWidth().coachMark(
                id = "send_echo_send_tutorial",
                title = "3. Send Echo",
                description = "Tap to send. Your Echo is always anonymous.",
                order = 3
            )
        )
        
        if (remainingLimit == 0) {
            Button(
                onClick = { onBoost() },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                 Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowUp, contentDescription = null)
                 Spacer(Modifier.width(8.dp))
                 Text("Boost Limit (1 Credit)")
            }
        }
    }
}

@Composable
fun HistoryView(
    history: List<Message>,
    isLoading: Boolean,
    error: AppException?,
    currentUsername: String?,
    onRetry: () -> Unit
) {
    LogicWrapper(
        isLoading = isLoading && history.isEmpty(),
        error = error,
        onRetry = onRetry,
        loadingContent = { EchoesShimmer() }
    ) {
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No feelings/emotions shared yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            var expandedMessageId by remember { mutableStateOf<String?>(null) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history) { message ->
                    HistoryItem(
                        message = message,
                        receiverName = currentUsername ?: "User",
                        isExpanded = message.id == expandedMessageId,
                        onToggleExpand = {
                           expandedMessageId = if (expandedMessageId == message.id) null else message.id
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    message: Message,
    receiverName: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    // var expanded by remember { mutableStateOf(false) } // Lifted state
    val expanded = isExpanded
    
    val formattedTime = remember(message.timestamp) {
        com.bhanit.apps.echo.core.util.DateTimeUtils.formatMessageTime(message.timestamp)
    }
    EchoCard(
        modifier = Modifier.fillMaxWidth().clickable { onToggleExpand() },
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            
            // 1. Content (Primary)
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.animateContentSize()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Footer Metadata
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Emotion Pill
                 Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = Color(message.emotion.colorHex).copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(message.emotion.colorHex))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.emotion.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(message.emotion.colorHex)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))
                
                // Receiver Name
                Text(
                    "To: $receiverName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Time
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                 // Expansion Indicator
                Icon(
                    imageVector = if (expanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp).size(20.dp)
                )
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
                                    ColorSchemeGlass,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    "Reply Received:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
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
                    } else {
                         Text(
                            "No reply yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmotionItem(
    emotion: Emotion,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(if (isSelected) 1.2f else 1.0f)

    Box(
        modifier = Modifier
            .scale(scale)
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(emotion.colorHex.toInt()).copy(alpha = if (emotion.isUnlocked) 1f else 0.5f))
            .then(
                if (isSelected) {
                    Modifier.border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        if (!emotion.isUnlocked) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
