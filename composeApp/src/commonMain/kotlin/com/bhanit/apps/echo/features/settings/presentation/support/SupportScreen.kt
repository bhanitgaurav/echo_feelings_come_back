package com.bhanit.apps.echo.features.settings.presentation.support

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.designsystem.components.EchoButton
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.data.model.SupportTicketDto
import com.bhanit.apps.echo.data.model.TicketStatus
import com.bhanit.apps.echo.core.util.DateTimeUtils
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SupportScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateTicket: () -> Unit
) {
    val viewModel = koinViewModel<SupportViewModel>()
    val state by viewModel.state.collectAsState()

    var expandedTicketId by remember { mutableStateOf<String?>(null) }

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
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header (Matches ProfileScreen)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                    text = "My Tickets",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            com.bhanit.apps.echo.core.presentation.components.LogicWrapper(
                isLoading = state.isLoading && state.tickets.isEmpty(),
                error = state.error,
                onRetry = viewModel::loadTickets,
                loadingContent = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground) } }
            ) {
                if (state.tickets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No tickets yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp + 48.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.tickets) { ticket ->
                            TicketItem(
                                ticket = ticket,
                                isExpanded = ticket.id == expandedTicketId,
                                onToggleExpand = {
                                    expandedTicketId = if (expandedTicketId == ticket.id) null else ticket.id
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Footer Button with Navigation Bar Insets
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .windowInsetsPadding(WindowInsets.navigationBars) 
                .padding(16.dp)
        ) {
            EchoButton(
                text = "Create New Ticket",
                onClick = onNavigateToCreateTicket,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
        }
    }
}

@Composable
fun TicketItem(
    ticket: SupportTicketDto,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    EchoCard {
        Column(modifier = Modifier
            .padding(16.dp)
            .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.category.name.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                StatusChip(ticket.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = ticket.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Expandable Description
            val isLongText = ticket.description.length > 100
            
            if (isExpanded) {
                Text(
                    text = ticket.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Show Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onToggleExpand() }
                )
            } else {
                Text(
                    text = if (isLongText) ticket.description.take(100) + "..." else ticket.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (isLongText) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Read More",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onToggleExpand() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Created: ${DateTimeUtils.formatDateShort(ticket.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                if (ticket.updatedAt != null && ticket.updatedAt != ticket.createdAt) {
                    Text(
                        text = "Updated: ${DateTimeUtils.formatDateShort(ticket.updatedAt!!)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: TicketStatus) {
    val color = when(status) {
        TicketStatus.OPEN -> Color(0xFF4CAF50) // Green
        TicketStatus.IN_REVIEW -> Color(0xFFFF9800) // Orange
        TicketStatus.RESOLVED -> Color(0xFF2196F3) // Blue
        TicketStatus.CLOSED -> Color(0xFF9E9E9E) // Grey
    }
    
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.Bold
        )
    }
}
