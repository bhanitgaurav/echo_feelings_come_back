package com.bhanit.apps.echo.features.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.bhanit.apps.echo.presentation.components.rememberImagePicker
import coil3.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onNavigateToDashboard: () -> Unit,
    onLogout: () -> Unit
) {
    val viewModel = koinViewModel<OnboardingViewModel>()
    val state by viewModel.state.collectAsState()
    
    // Animation States
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            onNavigateToDashboard()
        }
    }
    
    val imagePicker = rememberImagePicker { bytes ->
        viewModel.uploadPhoto(bytes)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp)
    ) {


        // Main Content
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hero Icon / Avatar Picker
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                            .clip(CircleShape)
                            .clickable { imagePicker.launch() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.photoUrl != null) {
                            AsyncImage(
                                model = state.photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp) // Increased size
                            )
                        }
                        
                         if (state.isPhotoUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    
                    // Add Photo Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable { imagePicker.launch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Edit,
                            contentDescription = "Add Photo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                val photoUploadError = state.photoUploadError
                if (photoUploadError != null) {
                     Spacer(modifier = Modifier.height(8.dp))
                     Text(
                        text = photoUploadError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Welcome to Echo",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Let's set up your profile to get started.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Username Field
                var showInspirationDialog by remember { mutableStateOf(false) }

                if (showInspirationDialog) {
                    AlertDialog(
                        onDismissRequest = { if (!state.isGeneratingUsername) showInspirationDialog = false },
                        title = { Text("Generate Username") },
                        text = {
                            Column {
                                Text("Choose a theme for your new username:", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(16.dp))
                                
                                if (state.isGeneratingUsername) {
                                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                } else {
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val options = com.bhanit.apps.echo.shared.domain.model.Inspiration.entries
                                        options.forEach { inspiration ->
                                            FilterChip(
                                                selected = false,
                                                onClick = {
                                                    viewModel.generateUsername(inspiration)
                                                    showInspirationDialog = false
                                                },
                                                label = { Text(inspiration.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) }
                                            )
                                        }
                                        // Surprise Me Option
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                viewModel.generateUsername(null)
                                                showInspirationDialog = false
                                            },
                                            label = { Text("Surprise Me!") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showInspirationDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                OutlinedTextField(
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange,
                    label = { Text("Username") },
                    placeholder = { Text("8-20 characters") },
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Filled.Person, contentDescription = null) },
                    trailingIcon = { 
                         IconButton(onClick = { showInspirationDialog = true }) {
                             Icon(
                                 imageVector = androidx.compose.material.icons.Icons.Default.Star,
                                 contentDescription = "Generate Username",
                                 tint = MaterialTheme.colorScheme.primary
                             )
                         }
                    },
                    singleLine = true,
                    isError = state.error != null && !state.username.matches(Regex("^[a-zA-Z0-9_]{8,20}$")) && state.username.isNotEmpty(),
                    enabled = !state.isGeneratingUsername && !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Referral Code Field
                OutlinedTextField(
                    value = state.referralCode,
                    onValueChange = viewModel::onReferralCodeChange,
                    label = { Text("Referral Code (Optional)") },
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Filled.Star, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
                
                val error = state.error
                if (error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (state.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    com.bhanit.apps.echo.core.designsystem.components.EchoButton(
                        text = "Complete Setup",
                        onClick = { viewModel.completeOnboarding() },
                        enabled = state.username.length in 8..20,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Switch Account", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
