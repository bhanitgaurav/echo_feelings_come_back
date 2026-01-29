package com.bhanit.apps.echo.core.presentation.components


import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp


import org.jetbrains.compose.ui.tooling.preview.Preview

enum class SearchMode {
    NAME_ID, PHONE
}

@Preview
@Composable
fun SearchToggleComponentPreview() {
    SearchToggleComponent(
        query = "Bhanit",
        onQueryChange = {},
        searchMode = SearchMode.NAME_ID,
        onSearchModeChange = {},
        modifier = Modifier
    )

}

@Composable
fun SearchToggleComponent(
    query: String,
    onQueryChange: (String) -> Unit,
    searchMode: SearchMode,
    onSearchModeChange: (SearchMode) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    onSearch: () -> Unit = {}
) {
    val regex = Regex("^[a-zA-Z0-9_]*$")

    // Search Input
    OutlinedTextField(
        value = query,
        onValueChange = { newValue ->
            if (newValue.length <= 20 && regex.matches(newValue)) {
                onQueryChange(newValue)
            }
        },
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(if (searchMode == SearchMode.PHONE) "Search by Phone..." else "Search by Name/ID...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Clear Button (Only when query not empty)
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Mode Toggle Button
                IconButton(onClick = {
                    val newMode = if (searchMode == SearchMode.NAME_ID) SearchMode.PHONE else SearchMode.NAME_ID
                    onSearchModeChange(newMode)
                }) {
                     // If Mode is NAME (Text input) -> Show Dialpad (switch to Phone)
                     // If Mode is PHONE (Number input) -> Show Keyboard (switch to Text)
                     val icon = if (searchMode == SearchMode.NAME_ID) Icons.Default.Dialpad else Icons.Default.Keyboard
                     
                     Icon(
                        imageVector = icon,
                        contentDescription = "Switch Search Mode",
                        tint = MaterialTheme.colorScheme.primary
                     )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (searchMode == SearchMode.PHONE) KeyboardType.Number else KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        )
    )
}
