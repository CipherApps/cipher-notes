package dev.cipher.notes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.cipher.notes.data.NoteType
import dev.cipher.notes.ui.components.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onNoteClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    vm: NoteListViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }

    val sizeSpring = spring<IntSize>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMedium
    )

    val colorSpring = spring<Color>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val fadeSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cipher",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New note", modifier = Modifier.size(30.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = vm::setSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    placeholder = { Text("Search your thoughts...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                if (uiState.notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notes found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)
                    ) {
                        items(uiState.notes, key = { it.id }) { note ->
                            NoteCard(note = note, onClick = { onNoteClick(note.id) })
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .fillMaxWidth(0.9f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val navItems = listOf(
                        NavigationItemData("all", "All", Icons.Rounded.Description),
                        NavigationItemData("note", "Notes", Icons.Rounded.EditNote),
                        NavigationItemData("todo", "To-Do", Icons.Rounded.CheckBox),
                        NavigationItemData("encrypted", "Locked", Icons.Rounded.Lock)
                    )

                    navItems.forEach { item ->
                        val isSelected = uiState.filterBy == item.id

                        val backgroundColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            animationSpec = colorSpring,
                            label = "pill_color"
                        )

                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = colorSpring,
                            label = "content_color"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(backgroundColor)
                                .clickable { vm.setFilter(item.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.animateContentSize(animationSpec = sizeSpring)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )

                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(animationSpec = fadeSpring),
                                    exit = fadeOut(animationSpec = fadeSpring)
                                ) {
                                    Text(
                                        text = item.label,
                                        modifier = Modifier.padding(start = 6.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = contentColor,
                                        maxLines = 1
                                    )
                                }
                            }
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
                vm.createNote(type) { id -> onNoteClick(id) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteBottomSheet(
    onDismiss: () -> Unit,
    onCreateNote: (NoteType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                "Create New",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp, start = 4.dp)
            )

            Surface(
                onClick = { onCreateNote(NoteType.TEXT); onDismiss() },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("Text Note", style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("Capture quick thoughts or long notes") },
                    leadingContent = {
                        Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                onClick = { onCreateNote(NoteType.TODO); onDismiss() },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("Checklist", style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("Organize tasks into points") },
                    leadingContent = {
                        Icon(Icons.Rounded.CheckBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

data class NavigationItemData(
    val id: String,
    val label: String,
    val icon: ImageVector
)