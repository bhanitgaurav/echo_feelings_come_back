package com.bhanit.apps.echo.features.contact.presentation.available

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.bhanit.apps.echo.core.designsystem.components.EchoButton
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.core.tutorial.coachMark
import com.bhanit.apps.echo.core.presentation.components.LogicWrapper
import com.bhanit.apps.echo.core.presentation.components.SearchToggleComponent

import com.bhanit.apps.echo.core.theme.PrimaryEcho
import com.bhanit.apps.echo.core.util.ContactPermissionEffect
import com.bhanit.apps.echo.features.contact.domain.Connection
import com.bhanit.apps.echo.features.contact.domain.ConnectionStatus
import com.bhanit.apps.echo.features.contact.presentation.components.ContactShimmer
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AvailableContactsScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel = koinViewModel<AvailableContactsViewModel>()
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current
    val shareManager = org.koin.compose.koinInject<com.bhanit.apps.echo.core.util.ShareManager>()
    
    // Permission State
    var hasPermission by remember { mutableStateOf(false) } 
    
    ContactPermissionEffect { granted: Boolean ->
        hasPermission = granted
        if (granted) {
            viewModel.loadData()
        }
    }

    // Infinite Scroll
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasPermission &&
            !state.isLoading &&
            state.hasMore &&
            totalItems > 0 &&
            lastVisibleItemIndex >= totalItems - 5
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
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
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Search", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                
                if (!hasPermission) {
                     Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Permission required to find friends.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        EchoButton(
                            text = "Open Settings",
                            onClick = { viewModel.openSettings() }
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Bar (Always Visible)
                        SearchToggleComponent(
                            query = state.searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange,
                            searchMode = state.searchMode,
                            onSearchModeChange = viewModel::onSearchModeChange,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).coachMark(
                                id = "search_contacts_tutorial",
                                title = "Smart Search",
                                description = "Search by name for contacts. For others, use exact username or phone number."
                            ),
                            placeholder = "Search name, username, or phone..."
                        )

                        LogicWrapper(
                            isLoading = state.isLoading && state.resultsList.isEmpty() && state.incomingRequests.isEmpty(),
                            error = state.error,
                            onRetry = { viewModel.loadData() }, // Full reload
                            loadingContent = { ContactShimmer() }
                        ) {
                             LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Friend Requests Section
                                if (state.incomingRequests.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Friend Requests",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(
                                        items = state.incomingRequests,
                                        key = { it.userId }
                                    ) { request ->
                                        AvailableContactItem(
                                            modifier = Modifier.animateItemPlacement(),
                                            connection = request,
                                            onConnect = {},
                                            onInvite = {},
                                            onAdd = {},
                                            onAccept = { viewModel.onAcceptConnection(request.userId) },
                                            onCancel = {}
                                        )
                                    }
                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "People you may know", // Separator
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }

                                // 2. Results List (Add/Invite)
                                if (state.resultsList.isEmpty() && !state.isLoading) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                             Text(
                                                text = if (state.searchQuery.isNotEmpty()) "No results found." else "No contacts available.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(
                                        items = state.resultsList,
                                        key = { it.userId.takeIf { id -> id.isNotEmpty() } ?: it.phoneNumber }
                                    ) { connection ->
                                        AvailableContactItem(
                                            modifier = Modifier.animateItemPlacement(),
                                            connection = connection,
                                            onConnect = { viewModel.onConnect(connection.userId) },
                                            onInvite = { 
                                                // Handle Invite
                                                 val code = state.referralCode
                                                 val dynamicUrl = state.referralUrl
                                                 val downloadLink = if (!dynamicUrl.isNullOrEmpty()) {
                                                        if (dynamicUrl.contains("?")) "$dynamicUrl&m=invite" else "$dynamicUrl?m=invite"
                                                 } else {
                                                        "https://api-echo.bhanitgaurav.com/r/$code?m=invite" 
                                                 }
                                                 val message = "Hey, join me on Echo! Use my referral code to get started: ${code ?: ""} \n\nDownload: $downloadLink"
                                                 // Use ShareManager (Generic System Share) instead of SMS URI
                                                 // This ensures WhatsApp, Telegram, etc. work correctly.
                                                 shareManager.shareText(message)
                                            },
                                            onAdd = { viewModel.onAddConnection(connection.userId) },
                                            onAccept = { viewModel.onAcceptConnection(connection.userId) },
                                            onCancel = { viewModel.onCancelRequest(connection.userId) }
                                        )
                                    }
                                    
                                    if (state.isLoading && state.resultsList.isNotEmpty()) {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AvailableContactItem(
    modifier: Modifier = Modifier,
    connection: Connection,
    onConnect: () -> Unit,
    onInvite: () -> Unit,
    onAdd: () -> Unit,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    EchoCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!connection.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = connection.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = connection.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connection.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (connection.phoneNumber.isNotBlank()) {
                    Text(
                        text = connection.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Action Button
            val status = connection.status

            when {
                status == ConnectionStatus.CONNECTED -> {
                    OutlinedButton(
                        onClick = onConnect,
                        border = BorderStroke(1.dp, PrimaryEcho),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryEcho)
                    ) {
                        Text("Chat")
                    }
                }

                status == ConnectionStatus.PENDING_OUTGOING -> {
                    EchoButton(
                         text = "Undo",
                         onClick = onCancel,
                         modifier = Modifier.height(36.dp),
                         contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                         textStyle = MaterialTheme.typography.labelLarge,
                         containerColor = MaterialTheme.colorScheme.errorContainer,
                         contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                status == ConnectionStatus.PENDING_INCOMING -> {
                     EchoButton(
                        text = "Accept",
                        onClick = onAccept,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        textStyle = MaterialTheme.typography.labelLarge
                    )
                }

                // If user has ID (Registered) -> Connect
                connection.userId.isNotEmpty() && status != ConnectionStatus.BLOCKED -> {
                    EchoButton(
                        text = "Connect",
                        onClick = onAdd,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        textStyle = MaterialTheme.typography.labelLarge
                    )
                }

                else -> {
                    // Not Registered -> Invite
                    OutlinedButton(
                        onClick = onInvite,
                         modifier = Modifier.height(36.dp),
                         contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text("Invite", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
