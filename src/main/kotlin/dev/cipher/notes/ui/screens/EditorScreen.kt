package dev.cipher.notes.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.cipher.notes.ui.components.EncryptDialog
import dev.cipher.notes.utils.DateUtils
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    vm: NoteEditorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by vm.uiState.collectAsState()
    val note = uiState.note ?: return
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var showEncryptDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var unlockPassword by remember { mutableStateOf("") }
    var annotatedContent by remember { mutableStateOf<AnnotatedString>(AnnotatedString("")) }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!uiState.isLocked) vm.save()
                        onBack()
                    }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isLocked) {
                        IconButton(onClick = { vm.exportNote(context) }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Export")
                        }
                        IconButton(onClick = { showEncryptDialog = true }) {
                            Icon(Icons.Rounded.Lock, contentDescription = "Encrypt")
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
        if (uiState.isLocked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Note Encrypted", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Enter password to decrypt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                TextField(
                    value = unlockPassword,
                    onValueChange = { input: String -> unlockPassword = input },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (uiState.error != null) {
                    Text(
                        uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { vm.unlock(unlockPassword) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.LockOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = uiState.title,
                    onValueChange = { newTitle: String -> vm.setTitle(newTitle) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Title…", style = MaterialTheme.typography.headlineSmall) },
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
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            DateUtils.formatRelative(note.modifiedAt),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(6.dp, 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                    if (uiState.content.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            val wordCount = uiState.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                            Text(
                                "${uiState.content.length} chars | $wordCount words",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(6.dp, 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (uiState.encrypted) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "🔒 Encrypted",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(6.dp, 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                LaunchedEffect(uiState.content, linkColor) {
                    val urlPattern = "(https?://[\\w\\d.#@/?=&%+-]+)".toRegex()
                    annotatedContent = buildAnnotatedString {
                        append(uiState.content)
                        urlPattern.findAll(uiState.content).forEach { match ->
                            addStyle(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                ),
                                start = match.range.first,
                                end = match.range.last + 1
                            )
                            addStringAnnotation(
                                tag = "URL",
                                annotation = match.value,
                                start = match.range.first,
                                end = match.range.last + 1
                            )
                        }
                    }
                }

                BasicTextField(
                    value = uiState.content,
                    onValueChange = { newContent: String -> vm.setContent(newContent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    onTextLayout = { result: TextLayoutResult -> textLayoutResult = result },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Default,
                        color = Color.Transparent
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (uiState.content.isNotEmpty()) {
                                Text(
                                    text = annotatedContent,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Default,
                                        color = MaterialTheme.colorScheme.onBackground
                                    ),
                                    onTextLayout = { result -> textLayoutResult = result },
                                    modifier = Modifier.pointerInput(annotatedContent) {
                                        detectTapGestures { offset ->
                                            textLayoutResult?.let { layout ->
                                                val position = layout.getOffsetForPosition(offset)
                                                annotatedContent
                                                    .getStringAnnotations("URL", position, position)
                                                    .firstOrNull()?.let { range ->
                                                        uriHandler.openUri(range.item)
                                                    }
                                            }
                                        }
                                    }
                                )
                            } else {
                                Text(
                                    "Start writing…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }

    if (showEncryptDialog) {
        EncryptDialog(
            onEncrypt = { pass: String ->
                vm.performEncrypt(pass)
                showEncryptDialog = false
            },
            onDismiss = { showEncryptDialog = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete note?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.delete()
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
