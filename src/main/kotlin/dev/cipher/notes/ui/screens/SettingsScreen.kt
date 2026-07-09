package dev.cipher.notes.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Nuclear Option") },
            text = { Text("This will permanently delete ALL notes and checklists. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.nuclearWipe()
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Privacy & Safety",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Surface(
                onClick = { showDeleteDialog = true },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            "Nuclear Wipe",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    supportingContent = {
                        Text(
                            "Permanently destroy all data stored in this app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("Encryption") },
                    supportingContent = { Text("Hardware-backed AES-256 GCM") },
                    leadingContent = { Icon(Icons.Rounded.Shield, null, tint = MaterialTheme.colorScheme.primary) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CipherNotes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Version 1.4.3",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = "© 2026 CipherApps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}