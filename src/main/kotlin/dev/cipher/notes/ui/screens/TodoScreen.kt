package dev.cipher.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.cipher.notes.data.TodoItem
import dev.cipher.notes.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    noteId: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    vm: NoteEditorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by vm.uiState.collectAsState()
    val note = uiState.note ?: return

    var showLockConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val completedCount = uiState.items.count { it.done }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { if (!uiState.isLocked) vm.save(); onBack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isLocked) {
                        IconButton(onClick = { showLockConfirm = true }) {
                            Icon(Icons.Rounded.Lock, contentDescription = "Lock Checklist")
                        }
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        // Inside TodoScreen.kt
        if (uiState.isLocked) {
            var passwordInput by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(64.dp))
                Text("Checklist Protected", style = MaterialTheme.typography.headlineMedium)

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Enter Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { vm.unlock(passwordInput) },
                    modifier = Modifier.padding(top = 16.dp),
                    enabled = passwordInput.isNotEmpty()
                ) {
                    Text("Unlock")
                }
            }
        } else {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {
                TextField(
                    value = uiState.title,
                    onValueChange = vm::setTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Checklist title…", style = MaterialTheme.typography.headlineSmall) },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                        Text(
                            DateUtils.formatRelative(note.modifiedAt),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(6.dp, 4.dp)
                        )
                    }
                    Text("$completedCount/${uiState.items.size} done", style = MaterialTheme.typography.labelSmall)
                    if (uiState.encrypted) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                            Text(
                                "Locked (On Device)",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(6.dp, 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        TodoItemRow(
                            item = item,
                            onToggle = { vm.updateTodoItem(item.id, done = !item.done) },
                            onTextChange = { vm.updateTodoItem(item.id, text = it) },
                            onDelete = { vm.deleteTodoItem(item.id) }
                        )
                    }
                    item {
                        Button(
                            onClick = { vm.addTodoItem() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("+ Add item")
                        }
                    }
                }
            }
        }
    }

    if (showLockConfirm) {
        var passwordToSet by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showLockConfirm = false },
            title = { Text("Protect Checklist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Set a password to encrypt this checklist. You will need this to unlock it later.")

                    OutlinedTextField(
                        value = passwordToSet,
                        onValueChange = {
                            passwordToSet = it
                            passwordError = null
                        },
                        label = { Text("Set Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        isError = passwordError != null
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordError = null
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        isError = passwordError != null
                    )

                    if (passwordError != null) {
                        Text(
                            text = passwordError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordToSet == confirmPassword) {
                            vm.performEncrypt(passwordToSet)
                            showLockConfirm = false
                        } else {
                            passwordError = "Passwords do not match"
                        }
                    },
                    enabled = passwordToSet.isNotEmpty() && confirmPassword.isNotEmpty()
                ) {
                    Text("Encrypt & Lock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete checklist?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { vm.delete(); showDeleteConfirm = false; onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { 
                    Text("Delete") 
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TodoItemRow(item: TodoItem, onToggle: () -> Unit, onTextChange: (String) -> Unit, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        Checkbox(checked = item.done, onCheckedChange = { onToggle() })
        TextField(
            value = item.text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, 
                unfocusedIndicatorColor = Color.Transparent
            ),
            placeholder = { Text("Item...") }
        )
        IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp)) }
    }
}
