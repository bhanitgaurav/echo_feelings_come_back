package com.bhanit.apps.echo.features.contact.presentation.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.core.tutorial.coachMark
import com.bhanit.apps.echo.core.presentation.components.LogicWrapper
import com.bhanit.apps.echo.core.theme.ColorSchemeGlass
import com.bhanit.apps.echo.features.contact.domain.Connection
import com.bhanit.apps.echo.features.contact.presentation.components.ContactShimmer
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PeopleScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToFindFriends: () -> Unit,
    onNavigateToProfile: (String) -> Unit,

) {
    val viewModel = koinViewModel<PeopleViewModel>()
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadConnections(reset = true)
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
            ).padding(top = 48.dp)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToFindFriends,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 80.dp).coachMark(
                        id = "people_fab_tutorial",
                        title = "Expand Your Circle",
                        description = "Tap here to find and add new people.",
                        order = 2
                    )
                ) {
                    Icon(Icons.Default.AddBox, "Find Friends")
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                LogicWrapper(
                    isLoading = state.isLoading && state.connections.isEmpty() && state.pendingRequests.isEmpty(),
                    error = state.error,
                    onRetry = viewModel::refresh,
                    loadingContent = { ContactShimmer() }
                ) {
                if (state.connections.isEmpty() && state.pendingRequests.isEmpty() && state.searchQuery.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No people yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                             com.bhanit.apps.echo.core.presentation.components.EchoScreenHeader(
                                 title = "Connections",
                                 actions = {
                                     // Optional: Filter or Add Friend icon if not covered by FAB
                                 }
                             )

                             com.bhanit.apps.echo.core.presentation.components.SearchToggleComponent(
                                query = state.searchQuery,
                                onQueryChange = viewModel::onSearchQueryChange,
                                searchMode = state.searchMode,
                                onSearchModeChange = viewModel::onSearchModeChange,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                placeholder = "Search people..."
                            )

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxWidth().weight(1f).coachMark(
                                    id = "people_list_tutorial",
                                    title = "Your Connections",
                                    description = "View your friends and pending requests here.",
                                    order = 1
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                        // Friend Requests Section
                        if (state.pendingRequests.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Requests",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            items(
                                items = state.pendingRequests,
                                key = { it.userId }
                            ) { request ->
                                FriendRequestItem(
                                    modifier = Modifier.animateItemPlacement(),
                                    connection = request,
                                    onAccept = { viewModel.onAccept(request) },
                                    onDeny = { viewModel.onDeny(request) }
                                )
                            }

                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Connections Section
                        if (state.connections.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Connections",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        items(
                            items = state.connections,
                            key = { it.userId }
                        ) { connection ->
                            ConnectionItem(
                                modifier = Modifier.animateItemPlacement(),
                                connection = connection,
                                onClick = {
                                    onNavigateToChat(
                                        connection.userId,
                                        connection.username
                                    )
                                }
                            )
                        }

                        if (state.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        // Add bottom padding for FAB
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    } // End LazyColumn
                    
                    // Infinite Scroll Trigger
                    LaunchedEffect(listState) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                            .collect { index ->
                                if (index != null && index >= state.connections.size - 5) {
                                    viewModel.loadNextPage()
                                }
                            }
                    }
                    } // End Column
                }
            } // End Box (LogicWrapper Content)
            } // End LogicWrapper

        }
    }
}

@Composable
fun ConnectionItem(
    modifier: Modifier = Modifier,
    connection: Connection,
    onClick: () -> Unit
) {
    EchoCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,

        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .size(48.dp) // Re-adding size/clip details to match original if needed, or just cleaner
                    .clip(CircleShape)
                    .background(ColorSchemeGlass),
                contentAlignment = Alignment.Center
            ) {
                if (connection.avatarUrl != null) {
                    AsyncImage(
                        model = connection.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = connection.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.primary, // Reverted color
                        style = MaterialTheme.typography.titleMedium, // Kept style
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = connection.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (connection.phoneNumber.isNotEmpty()) {
                    Text(
                        text = connection.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun FriendRequestItem(
    modifier: Modifier = Modifier,
    connection: Connection,
    onAccept: () -> Unit,
    onDeny: () -> Unit
) {
    EchoCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .size(48.dp) // Re-adding size/clip details to match original if needed, or just cleaner
                    .clip(CircleShape)
                    .background(ColorSchemeGlass),
                contentAlignment = Alignment.Center
            ) {
                if (connection.avatarUrl != null) {
                    AsyncImage(
                        model = connection.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = connection.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.primary, // Reverted color
                        style = MaterialTheme.typography.titleMedium, // Kept style
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connection.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface

                )
                Text(
                    text = "Wants to connect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Actions
            IconButton(onClick = onAccept) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Accept",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDeny) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Deny",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
