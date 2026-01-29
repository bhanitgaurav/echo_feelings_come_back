package com.bhanit.apps.echo.features.credits.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.bhanit.apps.echo.data.model.MilestoneStatusDto
import com.bhanit.apps.echo.data.model.MilestoneStatus
import com.bhanit.apps.echo.core.tutorial.coachMark



import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
enum class JourneySection {
    SEASONAL, STREAKS, CONSISTENCY, ACHIEVED
}
@Composable
fun JourneyTab(
    milestones: List<MilestoneStatusDto>
) {
    if (milestones.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Start your journey to see milestones.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    var selectedMilestone by remember { mutableStateOf<MilestoneStatusDto?>(null) }
    var expandedSection by remember { mutableStateOf<JourneySection?>(null) }
    
    if (selectedMilestone != null) {
        MilestoneDetailDialog(
            milestone = selectedMilestone!!,
            onDismiss = { selectedMilestone = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column {
            Text(
                "Your Journey",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.coachMark(
                    id = "journey_title_tutorial",
                    title = "Your Journey",
                    description = "Track your progress and rewards.",
                    order = 1
                )
            )
            Text(
                "Consistency builds identity. Rewards follow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 1. Seasonal (Active)
        val seasonalMilestones = milestones
            .filter { it.id.startsWith("SEASON_") }
            .sortedBy { it.status == MilestoneStatus.LOCKED } // False (Active) first, True (Locked/Upcoming) last
        if (seasonalMilestones.isNotEmpty()) {
             Column {
                 SectionHeader(
                     "Seasonal Rewards", 
                     isExpanded = expandedSection == JourneySection.SEASONAL,
                     modifier = Modifier.coachMark(
                         id = "journey_seasonal_tutorial",
                         title = "Seasonal Rewards",
                         description = "Join limited-time events.",
                         order = 2
                     ),
                     onToggle = { expandedSection = if (expandedSection == JourneySection.SEASONAL) null else JourneySection.SEASONAL }
                 ) 
                 AnimatedVisibility(
                     visible = expandedSection == JourneySection.SEASONAL,
                     enter = expandVertically() + fadeIn(),
                     exit = shrinkVertically() + fadeOut()
                 ) {
                     Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                         seasonalMilestones.forEach { milestone ->
                             MilestoneCard(milestone, isSeasonal = true, onClick = { selectedMilestone = milestone })
                         }
                     }
                 }
             }
        }

        // 2. Current Streaks (Standard)
        val activeMilestones = milestones.filter { 
            !it.id.startsWith("SEASON_") && 
            !it.id.startsWith("CONSISTENCY_") &&
            it.status == MilestoneStatus.IN_PROGRESS 
        }
        if (activeMilestones.isNotEmpty()) {
            Column {
                SectionHeader(
                    "Current Streaks", 
                    isExpanded = expandedSection == JourneySection.STREAKS,
                    modifier = Modifier.coachMark(
                        id = "journey_streaks_tutorial",
                        title = "Current Streaks",
                        description = "Maintain streaks to unlock tiers.",
                        order = 3
                    ),
                    onToggle = { expandedSection = if (expandedSection == JourneySection.STREAKS) null else JourneySection.STREAKS }
                ) 
                AnimatedVisibility(
                     visible = expandedSection == JourneySection.STREAKS,
                     enter = expandVertically() + fadeIn(),
                     exit = shrinkVertically() + fadeOut()
                 ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        activeMilestones.forEach { milestone ->
                            MilestoneCard(milestone, onClick = { selectedMilestone = milestone })
                        }
                    }
                }
            }
        }
        
        // 3. Consistency
        val consistency = milestones.find { it.id == "CONSISTENCY_TRACKER" }
        if (consistency != null) {
            Column {
                SectionHeader(
                    "Consistency", 
                    isExpanded = expandedSection == JourneySection.CONSISTENCY,
                    modifier = Modifier.coachMark(
                        id = "journey_consistency_tutorial",
                        title = "Consistency",
                        description = "Daily activity counts.",
                        order = 4
                    ),
                    onToggle = { expandedSection = if (expandedSection == JourneySection.CONSISTENCY) null else JourneySection.CONSISTENCY }
                ) 
                AnimatedVisibility(
                     visible = expandedSection == JourneySection.CONSISTENCY,
                     enter = expandVertically() + fadeIn(),
                     exit = shrinkVertically() + fadeOut()
                 ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MilestoneCard(consistency, isConsistency = true, onClick = { selectedMilestone = consistency })
                    }
                }
            }
        }
        
        // 4. History / Claimed
        val claimedMilestones = milestones.filter { 
            !it.id.startsWith("SEASON_") && // Don't show claimed seasonal here, keep them in seasonal section? Or move?
            // Actually, if seasonal is claimed, we still want it in "Seasonal" section to show completion.
            // So exclude seasonal from "General Claimed"
            it.status == MilestoneStatus.CLAIMED 
        }
        if (claimedMilestones.isNotEmpty()) {
            Column {
                SectionHeader(
                    "Milestones Achieved",
                    isExpanded = expandedSection == JourneySection.ACHIEVED,
                    onToggle = { expandedSection = if (expandedSection == JourneySection.ACHIEVED) null else JourneySection.ACHIEVED }
                )
                AnimatedVisibility(
                     visible = expandedSection == JourneySection.ACHIEVED,
                     enter = expandVertically() + fadeIn(),
                     exit = shrinkVertically() + fadeOut()
                 ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        claimedMilestones.forEach { milestone ->
                            MilestoneCard(milestone, onClick = { selectedMilestone = milestone })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun MilestoneCard(
    milestone: MilestoneStatusDto, 
    isSeasonal: Boolean = false,
    isConsistency: Boolean = false,
    onClick: () -> Unit = {}
) {
    val isClaimed = milestone.status == MilestoneStatus.CLAIMED || (milestone.required > 0 && milestone.progress >= milestone.required)
    val isLocked = milestone.status == MilestoneStatus.LOCKED

    // Custom Visuals
    // Parse color if available (format 0xFFE91E63 or #FFE91E63)
    val customColor = remember(milestone.colorHex) {
        try {
            if (!milestone.colorHex.isNullOrBlank()) {
                val hex = milestone.colorHex.removePrefix("0x").removePrefix("#")
                Color(hex.toLong(16)) // Ensure opacity is handled if needed
            } else null
        } catch (e: Exception) {
            null
        }
    }

    val cardColor = when {
        isLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) // Greyed out
        isClaimed -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) // Gold/Amber-ish if secondary is such, or just distinct
        isSeasonal && customColor != null -> customColor.copy(alpha = 0.15f)
        isSeasonal -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        isConsistency -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    val iconVector = when {
        isClaimed -> Icons.Default.CheckCircle
        isLocked -> Icons.Default.Lock
        isSeasonal -> {
            when (milestone.category?.uppercase()) {
                "RELIGIOUS" -> Icons.Default.Star // Or a lamp/prayer icon if available
                "SOCIAL" -> Icons.Default.Favorite // Heart
                "GLOBAL" -> Icons.Default.Public // Or Globe/World
                else -> Icons.Default.DateRange
            }
        }

        isConsistency -> Icons.Default.Refresh
        else -> Icons.Default.Star
    }

    val iconTint = when {
        isLocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        isClaimed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        isSeasonal && customColor != null -> customColor
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if (isClaimed) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        milestone.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isClaimed || isLocked) MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.6f
                        ) else MaterialTheme.colorScheme.onSurface
                    )

                    // Show Date Range for Seasonal
                    if (isSeasonal && milestone.startDate != null) {
                        val rangeText = try {
                            val start = kotlinx.datetime.LocalDate.parse(milestone.startDate)
                            val end =
                                milestone.endDate?.let { kotlinx.datetime.LocalDate.parse(it) }
                            if (isLocked) {
                                "Coming: ${
                                    start.month.name.lowercase().replaceFirstChar { it.uppercase() }
                                } ${start.dayOfMonth}"
                            } else if (end != null) {
                                "${
                                    start.month.name.lowercase().replaceFirstChar { it.uppercase() }
                                } ${start.dayOfMonth} - ${
                                    end.month.name.lowercase().replaceFirstChar { it.uppercase() }
                                } ${end.dayOfMonth}"
                            } else {
                                "Active now"
                            }
                        } catch (e: Exception) {
                            "Seasonal Event"
                        }

                        Text(
                            rangeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = iconTint, // Match event color
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (isClaimed) {
                        Text(
                            "Reward Unlocked! You have completed this.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (!isLocked) {
                        Text(
                            milestone.description, 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (!isClaimed && !isLocked) {
                    Text(
                        "+${milestone.rewardCredits} Credits",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconTint
                    )
                } else if (isClaimed) {
                    Text(
                        "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!isClaimed && !isLocked) {
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${milestone.progress} / ${milestone.required}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${milestone.percentage}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { milestone.percentage / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = iconTint,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun MilestoneDetailDialog(
    milestone: MilestoneStatusDto,
    onDismiss: () -> Unit
) {
    val isClaimed = milestone.status == MilestoneStatus.CLAIMED || (milestone.required > 0 && milestone.progress >= milestone.required)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            if (isClaimed) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("Milestone Achieved!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                 }
            } else {
                Text(milestone.displayName, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column {
                if (isClaimed) {
                     Text(
                        "You have already completed/unlocked this milestone.", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        milestone.instruction ?: milestone.description, 
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isClaimed) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                         Spacer(modifier = Modifier.width(8.dp))
                         Text("Completed", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                     }
                } else if (milestone.status == MilestoneStatus.LOCKED) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                         Spacer(modifier = Modifier.width(8.dp))
                         Text("Coming Soon", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                     }
                } else {
                     // Progress
                     Text(
                         "Progress: ${milestone.progress} / ${milestone.required}", 
                         style = MaterialTheme.typography.labelMedium,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                     Spacer(modifier = Modifier.height(8.dp))
                     LinearProgressIndicator(
                         progress = { milestone.percentage / 100f },
                         modifier = Modifier.fillMaxWidth(),
                     )
                }
                
                if (!isClaimed) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Earn +${milestone.rewardCredits} Credits upon completion", 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}