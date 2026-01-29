package com.bhanit.apps.echo.features.auth.presentation.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun CountryCodePickerDialog(
    onDismiss: () -> Unit,
    onCountrySelected: (String) -> Unit
) {
    val countries = remember {
        listOf(
            Country("India", "+91", "🇮🇳"),
            Country("USA", "+1", "🇺🇸"),
            Country("UK", "+44", "🇬🇧"),
            Country("Canada", "+1", "🇨🇦"),
            Country("Australia", "+61", "🇦🇺"),
            Country("Germany", "+49", "🇩🇪"),
            Country("France", "+33", "🇫🇷"),
            Country("Japan", "+81", "🇯🇵"),
            Country("China", "+86", "🇨🇳"),
            Country("Brazil", "+55", "🇧🇷")
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Select Country",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(countries) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCountrySelected(country.code) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(country.flag, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(country.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(country.code, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

data class Country(val name: String, val code: String, val flag: String)
