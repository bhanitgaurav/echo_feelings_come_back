package com.bhanit.apps.echo.features.settings.presentation.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.designsystem.components.EchoButton
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.data.model.TicketCategory
import androidx.compose.material.icons.filled.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSupportScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel = koinViewModel<SupportViewModel>()
    val state by viewModel.state.collectAsState()

    // Success Dialog
    if (state.isSuccess) {
        AlertDialog(
            onDismissRequest = { 
                viewModel.resetState()
                onNavigateBack() 
            },
            title = { Text("Ticket Submitted") },
            text = { Text("Thank you! Your ticket has been received. We'll get back to you shortly.") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.resetState()
                    onNavigateBack() 
                }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
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
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Header (Consistent with Profile/Support)
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
                    text = "New Ticket",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 2. Scrollable Form
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EchoCard {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        
                        // Category Dropdown
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = state.category.name.replace("_", " "),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(state.category),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface) // Ensure contrast
                            ) {
                                TicketCategory.entries.forEach { category ->
                                    val isSelected = category == state.category
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = category.name.replace("_", " "),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = getCategoryIcon(category),
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        trailingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            viewModel.onCategoryChange(category)
                                            expanded = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                        modifier = Modifier.background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
                                            else Color.Transparent
                                        )
                                    )
                                }
                            }
                        }

                        // Subject
                        OutlinedTextField(
                            value = state.subject,
                            onValueChange = viewModel::onSubjectChange,
                            label = { Text("Subject") },
                            placeholder = { Text("Brief summary") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            supportingText = {
                                Text(
                                    text = "${state.subject.length}/50",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )

                        // Description
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = viewModel::onDescriptionChange,
                            label = { Text("Description") },
                            placeholder = { Text("Please describe your issue in detail...") },
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            shape = MaterialTheme.shapes.medium,
                            supportingText = {
                                Text(
                                    text = "${state.description.length}/500",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            minLines = 3,
                            maxLines = 10
                        )
                    }
                }
                
                if (state.error != null) {
                    Text(
                        text = state.error?.message ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                
                // Bottom spacing for content
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // 3. Footer Button (Pinned + IME handling)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .windowInsetsPadding(WindowInsets.safeDrawing) // Handles IME + Nav bars
                .padding(16.dp)
        ) {
            EchoButton(
                text = if (state.isLoading) "Submitting..." else "Submit Ticket",
                onClick = viewModel::submitTicket,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !state.isLoading
            )
        }
    }
}

private fun getCategoryIcon(category: TicketCategory) = when (category) {
    TicketCategory.BUG -> Icons.Default.BugReport
    TicketCategory.FEEDBACK -> Icons.Default.Feedback
    TicketCategory.ACCOUNT -> Icons.Default.Person
    TicketCategory.REQUEST_EMOTION -> Icons.Default.SentimentSatisfied
    TicketCategory.OTHER -> Icons.Default.Category
}
