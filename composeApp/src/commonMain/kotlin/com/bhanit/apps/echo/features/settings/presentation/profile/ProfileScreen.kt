package com.bhanit.apps.echo.features.settings.presentation.profile

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.bhanit.apps.echo.core.presentation.components.LogicWrapper
import com.bhanit.apps.echo.features.settings.presentation.components.ProfileShimmer
import com.bhanit.apps.echo.core.designsystem.components.EchoButton
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.core.theme.PrimaryEcho

import com.bhanit.apps.echo.core.presentation.components.DisableInputActions
import com.bhanit.apps.echo.presentation.components.rememberImagePicker
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.Star

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<ProfileViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.reload()
    }
    
    val imagePicker = rememberImagePicker { bytes ->
        viewModel.uploadPhoto(bytes)
    }

    val glassColor = MaterialTheme.colorScheme.surfaceVariant

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
        DisableInputActions {
            Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Custom Transparent Header
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
                    text = "Edit Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LogicWrapper(
                isLoading = state.isLoading,
                error = state.error,
                onRetry = viewModel::reload,
                loadingContent = { ProfileShimmer() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    
                    // 2. Avatar Placeholder
                    Box(
                        modifier = Modifier
                            .size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { imagePicker.launch() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = state.photoUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = state.username.take(2).uppercase(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            if (state.isPhotoUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        
                        // Edit Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 12.dp, end = 12.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .clickable { imagePicker.launch() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Photo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    val photoUploadError = state.photoUploadError
                    if (photoUploadError != null) {
                         Text(
                            text = photoUploadError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // 3. Form Card
                    EchoCard(
                       backgroundColor = glassColor
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Mobile Number (Non-editable)
                            OutlinedTextField(
                                value = state.phoneNumber,
                                onValueChange = {},
                                label = { Text("Mobile Number") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                enabled = false,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            // Username
                            var showInspirationDialog by remember { mutableStateOf(false) }
                            
                            if (showInspirationDialog) {
                                AlertDialog(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                                supportingText = { Text("8 characters, unique") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !state.isSaving && !state.isGeneratingUsername,
                                shape = MaterialTheme.shapes.medium,
                                trailingIcon = {
                                     IconButton(onClick = { showInspirationDialog = true }, enabled = !state.isSaving) {
                                         Icon(
                                             imageVector = androidx.compose.material.icons.Icons.Default.Star,
                                             contentDescription = "Generate Username",
                                             tint = MaterialTheme.colorScheme.primary
                                         )
                                     }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )

                            // Full Name
                            OutlinedTextField(
                                value = state.fullName,
                                onValueChange = viewModel::onFullNameChange,
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !state.isSaving,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )

                            // Gender Dropdown
                            var expanded by remember { mutableStateOf(false) }
                            val genderOptions = listOf("Male", "Female", "Other")

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { if (!state.isSaving) expanded = !expanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = state.gender,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Gender") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    enabled = !state.isSaving,
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
                                    onDismissRequest = { expanded = false }
                                ) {
                                    genderOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                viewModel.onGenderChange(option)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Email
                            OutlinedTextField(
                                value = state.email,
                                onValueChange = viewModel::onEmailChange,
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !state.isSaving,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Save Button
                    EchoButton(
                        text = if (state.isSaving) "Saving..." else "Save Changes",
                        onClick = viewModel::saveProfile,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !state.isSaving
                    )

                    val validationError = state.validationError
                    if (validationError != null) {
                        Text(
                            text = validationError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    val saveError = state.saveError
                    if (saveError != null) {
                         Text(
                            text = saveError,
                            color = MaterialTheme.colorScheme.error,
                             style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } 
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
}
