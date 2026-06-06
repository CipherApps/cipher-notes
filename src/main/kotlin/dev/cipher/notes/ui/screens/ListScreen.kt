package dev.cipher.notes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.cipher.notes.data.NoteType
import dev.cipher.notes.ui.components.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteBottomSheet(
    onDismiss: () -> Unit,
    onCreateNote: (NoteType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "Create New",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CreateOptionItem(
                title = "Text Note",
                subtitle = "Capture your thoughts and ideas",
                icon = Icons.Rounded.Description,
                onClick = {
                    onCreateNote(NoteType.TEXT)
                    onDismiss()
                }
            )

            CreateOptionItem(
                title = "Checklist",
                subtitle = "Manage tasks and to-dos",
                icon = Icons.Rounded.CheckBox,
                onClick = {
                    onCreateNote(NoteType.TODO)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun CreateOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onNoteClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    vm: NoteListViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cipher", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${uiState.notes.size} notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New note")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = vm::setSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    placeholder = { Text("Search notes…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("all" to "All", "note" to "Notes", "todo" to "To-Do", "encrypted" to "Encrypted")
                        .forEach { (filter, label) ->
                            FilterChip(
                                selected = uiState.filterBy == filter,
                                onClick = { vm.setFilter(filter) },
                                label = { Text(label) }
                            )
                        }
                }

                if (uiState.notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notes yet", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(uiState.notes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { onNoteClick(note.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateNoteBottomSheet(
            onDismiss = { showCreateSheet = false },
            onCreateNote = { type ->
                showCreateSheet = false
                vm.createNote(type) { id ->
                    onNoteClick(id)
                }
            }
        )
    }
}