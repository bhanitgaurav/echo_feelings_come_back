package com.bhanit.apps.echo.features.settings.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween

import com.bhanit.apps.echo.core.theme.ThemeConfig
import com.bhanit.apps.echo.core.theme.BackgroundDark
import com.bhanit.apps.echo.core.theme.BackgroundGreenDark
import com.bhanit.apps.echo.core.theme.BackgroundLight
import com.bhanit.apps.echo.core.tutorial.coachMark

@Composable
fun ThemeScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel = org.koin.compose.viewmodel.koinViewModel<ThemeViewModel>()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val currentFont by viewModel.currentFont.collectAsState()

    val customThemeColors by viewModel.customThemeColors.collectAsState()

    ThemeSelectionContent(
        currentTheme = currentTheme,
        currentFont = currentFont,
        customThemeColors = customThemeColors,
        onThemeSelected = { viewModel.setTheme(it) },
        onFontSelected = { viewModel.setFont(it) },
        onCustomColorsChanged = { viewModel.updateCustomTheme(it) },
        onNavigateBack = onNavigateBack
    )
}



@Composable
private fun ThemeSelectionContent(
    currentTheme: ThemeConfig,
    currentFont: com.bhanit.apps.echo.core.theme.AppFont,
    customThemeColors: com.bhanit.apps.echo.core.theme.CustomThemeColors,
    onThemeSelected: (ThemeConfig) -> Unit,
    onFontSelected: (com.bhanit.apps.echo.core.theme.AppFont) -> Unit,
    onCustomColorsChanged: (com.bhanit.apps.echo.core.theme.CustomThemeColors) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showCustomEditDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(top = 48.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            // Custom Header with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
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
                    text = "App Theme",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExpandableSection(
                    title = "App Theme", 
                    subtitle = "Choose your vibe",
                    initiallyExpanded = true
                ) {


                // Cycle Option: System -> Light -> Dark
                val toggleTheme = when (currentTheme) {
                    ThemeConfig.SYSTEM -> ThemeConfig.LIGHT
                    ThemeConfig.LIGHT -> ThemeConfig.DARK
                    ThemeConfig.DARK -> ThemeConfig.SYSTEM
                    else -> ThemeConfig.SYSTEM
                }

                val (toggleTitle, toggleDesc, togglePreview) = when (currentTheme) {
                     ThemeConfig.LIGHT -> Triple(
                        "Echo Light",
                        "Clean, bright, and airy.",
                        Color(0xFFE0E0E0) // Light Preview
                    )
                     ThemeConfig.DARK -> Triple(
                        "Echo Dark (Premium)",
                        "Deep violet for focus and calm.",
                        Color(0xFF1E1B2E) // Dark Preview
                    )
                    else -> Triple( // Default/System state or Fallback
                        "System Default",
                        "Adapts to your device settings.",
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                ThemeOptionCard(
                    modifier = Modifier.coachMark(
                        id = "theme_toggle_tutorial",
                        title = "Quick Toggle",
                        description = "Tap to cycle between System, Light, and Dark modes.",
                        order = 1
                    ),
                    title = toggleTitle,
                    description = toggleDesc,
                    previewContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(togglePreview)
                        )
                    },
                    isSelected = currentTheme == ThemeConfig.SYSTEM || currentTheme == ThemeConfig.LIGHT || currentTheme == ThemeConfig.DARK,
                    onClick = { onThemeSelected(toggleTheme) }
                )

                // Option 2: Green Dark (Nature)
                ThemeOptionCard(
                    modifier = Modifier.coachMark(
                        id = "theme_vibe_tutorial",
                        title = "Choose Your Vibe",
                        description = "Select a premium theme that matches your mood.",
                        order = 2
                    ),
                    title = "Forest Echo",
                    description = "Deep emerald for healing and growth.",
                    previewContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BackgroundGreenDark)
                        )
                    },
                    isSelected = currentTheme == ThemeConfig.GREEN_DARK,
                    onClick = { onThemeSelected(ThemeConfig.GREEN_DARK) }
                )

                // Option 3: Solar (Yellow/Warm)
                ThemeOptionCard(
                    title = "Sunbeam", // Changed from Solar for better naming
                    description = "Warm warmth and positivity.",
                    previewContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(com.bhanit.apps.echo.core.theme.BackgroundSolar)
                        )
                    }, 
                    isSelected = currentTheme == ThemeConfig.SOLAR,
                    onClick = { onThemeSelected(ThemeConfig.SOLAR) }
                )




                // Option 5: Custom
                ThemeOptionCard(
                    modifier = Modifier.coachMark(
                        id = "theme_custom_tutorial",
                        title = "Create Your Own",
                        description = "Design a unique theme with your favorite colors.",
                        order = 3
                    ),
                    title = "My Custom Theme",
                    description = "Build your own vibe with hex codes.",
                    previewContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(customThemeColors.background))
                        ) {
                             Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(customThemeColors.primary))
                                    .align(Alignment.Center)
                            )
                        }
                    }, 
                    isSelected = currentTheme == ThemeConfig.CUSTOM,
                    onClick = { onThemeSelected(ThemeConfig.CUSTOM) },
                    onEditClick =  { 
                        showCustomEditDialog = true
                    }
                )

                if (currentTheme == ThemeConfig.CUSTOM && showCustomEditDialog) {
                     CustomThemeEditDialog(
                        colors = customThemeColors,
                        onColorsChanged = onCustomColorsChanged,
                        onDismissRequest = { showCustomEditDialog = false }
                    )
                }

                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                ExpandableSection(
                    modifier = Modifier.coachMark(
                        id = "theme_typography_tutorial",
                        title = "Typography",
                        description = "Change the app font to suit your style.",
                        order = 4
                    ),
                    title = "Typography", 
                    subtitle = "Readability & Accessibility"
                ) {
                    // Typography Options
                    com.bhanit.apps.echo.core.theme.AppFont.entries.forEach { font ->
                        val (title, description) = when(font) {
                            com.bhanit.apps.echo.core.theme.AppFont.INTER -> "Modern (Default)" to "Clean and universal sans-serif."
                            com.bhanit.apps.echo.core.theme.AppFont.MERRIWEATHER -> "Classic" to "Formal serif."
                            com.bhanit.apps.echo.core.theme.AppFont.JETBRAINS_MONO -> "Code" to "Monospace."
                            com.bhanit.apps.echo.core.theme.AppFont.ROBOTO_SLAB -> "Strong" to "Bold slab serif with character."
                        }
                        
                        ThemeOptionCard(
                            title = title,
                            description = description,
                            previewContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Aa",
                                        style = com.bhanit.apps.echo.core.theme.getTypography(font).headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            isSelected = currentFont == font,
                            onClick = { onFontSelected(font) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeOptionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    previewContent: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 300)
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        animationSpec = tween(durationMillis = 300)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Preview Box
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
            previewContent()
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }



        if (isSelected && onEditClick != null) {
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Theme",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String, 
    subtitle: String,
    modifier: Modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CustomThemeEditDialog(
    colors: com.bhanit.apps.echo.core.theme.CustomThemeColors,
    onColorsChanged: (com.bhanit.apps.echo.core.theme.CustomThemeColors) -> Unit,
    onDismissRequest: () -> Unit
) {
    var showColorPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var colorPickerInitialColor by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Color.White) }
    var activeColorCallback by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<((Color) -> Unit)?>(null) }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = colorPickerInitialColor,
            onColorSelected = { color ->
                activeColorCallback?.invoke(color)
            },
            onDismissRequest = { showColorPicker = false }
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Edit Custom Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onDismissRequest) {
                        Text("Done")
                    }
                }

            // Primary Color Input
            ColorInputRow(
                label = "Primary Color",
                helper = "Buttons, Icons, Active States",
                currentColor = Color(colors.primary),
                onColorChanged = { newColor ->
                     onColorsChanged(colors.copy(primary = newColor.toArgbU()))
                },
                onPickColorClick = {
                    colorPickerInitialColor = Color(colors.primary)
                    activeColorCallback = { newColor ->
                        onColorsChanged(colors.copy(primary = newColor.toArgbU()))
                    }
                    showColorPicker = true
                }
            )

            // Background Color Input
            ColorInputRow(
                label = "Background Color",
                helper = "Main app background",
                currentColor = Color(colors.background),
                onColorChanged = { newColor ->
                     onColorsChanged(colors.copy(background = newColor.toArgbU()))
                },
                onPickColorClick = {
                    colorPickerInitialColor = Color(colors.background)
                    activeColorCallback = { newColor ->
                        onColorsChanged(colors.copy(background = newColor.toArgbU()))
                    }
                    showColorPicker = true
                }
            )

            // Surface Color Input
            ColorInputRow(
                label = "Surface Color",
                helper = "Cards, Dialogs, Top Bar",
                currentColor = Color(colors.surface),
                onColorChanged = { newColor ->
                     onColorsChanged(colors.copy(surface = newColor.toArgbU()))
                },
                onPickColorClick = {
                    colorPickerInitialColor = Color(colors.surface)
                    activeColorCallback = { newColor ->
                        onColorsChanged(colors.copy(surface = newColor.toArgbU()))
                    }
                    showColorPicker = true
                }
            )
            
            // Dark Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dark Mode Base?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Determines text color (White for Dark, Black for Light)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = colors.isDark,
                    onCheckedChange = { onColorsChanged(colors.copy(isDark = it)) }
                )
            }
        }
    }
}
}

@Composable
fun ColorInputRow(
    label: String,
    helper: String,
    currentColor: Color,
    onColorChanged: (Color) -> Unit,
    onPickColorClick: () -> Unit = {}
) {
    var text by androidx.compose.runtime.remember(currentColor) { androidx.compose.runtime.mutableStateOf(currentColor.toHex()) }
    
    // Validate and update only if valid hex
    fun updateColor(hex: String) {
        text = hex
        if (hex.length == 7 && hex.startsWith("#")) {
            try {
                val color = Color(hex.drop(1).toLong(16) or 0xFF00000000)
                onColorChanged(color)
            } catch (e: Exception) { /* Ignore invalid parse */ }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Preview Circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(currentColor)
                .border(1.dp, MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape)
                .clickable { onPickColorClick() }
        )
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(helper, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.width(8.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { newValue ->
                 // Simple filter to keep it looking like hex
                 if (newValue.length <= 7) updateColor(newValue.uppercase()) 
            },
            modifier = Modifier.width(100.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

// Extension to convert Color to Hex string #RRGGBB
fun Color.toHex(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    
    fun Int.toHexString(): String {
        return this.toString(16).uppercase().padStart(2, '0')
    }
    
    return "#${red.toHexString()}${green.toHexString()}${blue.toHexString()}"
}

// Extension to get Long ARGB (Compose Color constructs from Long as ARGB, but 0xFF......)
fun Color.toArgbU(): Long {
     val alpha = (this.alpha * 255).toInt()
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    return ((alpha.toLong() and 0xFF) shl 24) or
           ((red.toLong() and 0xFF) shl 16) or
           ((green.toLong() and 0xFF) shl 8) or
           (blue.toLong() and 0xFF)
}



@Composable
fun ExpandableSection(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initiallyExpanded) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionHeader(
                title = title, 
                subtitle = subtitle,
                modifier = Modifier.weight(1f).padding(bottom = 8.dp)
            )
            
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}
