package com.bhanit.apps.echo.features.settings.presentation.blocked

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedListScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel = koinViewModel<BlockedListViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Users") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                 CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.blockedUsers.isEmpty()) {
                 Text("No blocked users", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.blockedUsers) { user ->
                        ListItem(
                            headlineContent = { Text(user.username ?: "Unknown") },
                            supportingContent = { Text(user.phoneHash.take(8) + "...") },
                            trailingContent = {
                                Button(onClick = { viewModel.unblockUser(user.id) }) {
                                    Text("Unblock")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
