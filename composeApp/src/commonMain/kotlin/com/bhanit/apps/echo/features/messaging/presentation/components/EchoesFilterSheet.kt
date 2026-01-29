package com.bhanit.apps.echo.features.messaging.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.theme.PrimaryEcho
import com.bhanit.apps.echo.core.theme.SurfaceGlass
import com.bhanit.apps.echo.features.messaging.domain.Emotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoesFilterSheet(
    selectedFeelings: Set<Emotion>,
    availableEmotions: List<Emotion>, // Added parameter
    onFeelingToggle: (Emotion) -> Unit,
    startDate: Long?,
    endDate: Long?,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Filter Echoes",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Date Range
            Text(
                "Date Range",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (startDate != null) PrimaryEcho.copy(alpha = 0.1f) else Color.Transparent
                ),
                border = BorderStroke(1.dp, if (startDate != null) PrimaryEcho else MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (startDate != null && endDate != null) {
                        "${com.bhanit.apps.echo.core.util.DateTimeUtils.formatDateShort(startDate)} - ${com.bhanit.apps.echo.core.util.DateTimeUtils.formatDateShort(endDate)}"
                    } else "Select Dates",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Feelings
            Text(
                "Feelings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableEmotions) { emotion ->
                    FilterChip(
                        selected = selectedFeelings.contains(emotion),
                        onClick = { onFeelingToggle(emotion) },
                        label = { Text(emotion.displayName) },
                        leadingIcon = if (selectedFeelings.contains(emotion)) {
                            { Icon(androidx.compose.material.icons.Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(emotion.colorHex).copy(alpha = 0.2f),
                            selectedLabelColor = Color(emotion.colorHex),
                            selectedLeadingIconColor = Color(emotion.colorHex)
                        )
                    )
                }
            }

            if (selectedFeelings.isNotEmpty() || startDate != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        onClearFilters()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear All Filters")
                }
            }
            
             Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate,
            initialSelectedEndDateMillis = endDate
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateRangeSelected(datePickerState.selectedStartDateMillis, datePickerState.selectedEndDateMillis)
                    showDatePicker = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(state = datePickerState)
        }
    }
}
