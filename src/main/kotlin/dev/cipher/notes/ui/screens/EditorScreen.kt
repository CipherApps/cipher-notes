package dev.cipher.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.cipher.notes.ui.components.EncryptDialog
import dev.cipher.notes.utils.DateUtils

private fun renderMarkdown(text: String, primaryColor: Color): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        Regex("(?m)^#\\s+(.*)$").findAll(text).forEach { match ->
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = primaryColor), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), match.range.first, match.range.first + 2)
        }
        Regex("(?s)\\*\\*(.*?)\\*\\*").findAll(text).forEach { match ->
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), match.range.first, match.range.first + 2)
            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), match.range.last - 1, match.range.last + 1)
        }
        Regex("(?s)(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)").findAll(text).forEach { match ->
            addStyle(SpanStyle(fontStyle = FontStyle.Italic, fontFamily = FontFamily.Serif), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), match.range.first, match.range.first + 1)
            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), match.range.last, match.range.last + 1)
        }
    }
}
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
    val linkColor = MaterialTheme.colorScheme.primary

    var showEncryptDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var unlockPassword by remember { mutableStateOf("") }
    var isPreviewMode by remember { mutableStateOf(false) }

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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isLocked) {
                        IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                            Icon(
                                imageVector = if (isPreviewMode) Icons.Rounded.Edit else Icons.Rounded.Visibility,
                                contentDescription = "Toggle Preview"
                            )
                        }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (uiState.isLocked) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(modifier = Modifier.size(100.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape) {}
                        Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Note Encrypted", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Enter password to decrypt", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedTextField(
                        value = unlockPassword,
                        onValueChange = { unlockPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    if (uiState.error != null) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { vm.unlock(unlockPassword) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.LockOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                if (isPreviewMode) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = uiState.title.ifEmpty { "No Title" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = renderMarkdown(uiState.content.text, MaterialTheme.colorScheme.primary),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextField(
                            value = uiState.title,
                            onValueChange = { vm.setTitle(it) },
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
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                                Text(DateUtils.formatRelative(note.modifiedAt), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp, 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (uiState.content.text.isNotEmpty()) {
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                                    val wordCount = uiState.content.text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                                    Text("${uiState.content.text.length} chars | $wordCount words", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp, 4.dp))
                                }
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val mdButtons = listOf(
                                Icons.Rounded.Title to "# ",
                                Icons.Rounded.FormatBold to "**",
                                Icons.Rounded.FormatItalic to "*",
                            )

                            mdButtons.forEach { (icon, symbol) ->
                                IconButton(
                                    onClick = {
                                        val textFieldValue = uiState.content
                                        val text = textFieldValue.text
                                        val selection = textFieldValue.selection.start
                                        val (toInsert, cursorShift) = when(symbol) {
                                            "# " -> (if (selection == 0 || text[selection-1] == '\n') "# " else "\n# ") to 2
                                            "**" -> "** **" to 2
                                            "*"  -> "* *" to 1
                                            else -> symbol to symbol.length
                                        }
                                        val newText = StringBuilder(text).insert(selection, toInsert).toString()
                                        vm.setContent(TextFieldValue(text = newText, selection = TextRange(selection + cursorShift)))
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        BasicTextField(
                            value = uiState.content,
                            onValueChange = { vm.setContent(it) },
                            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = FontFamily.SansSerif
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (uiState.content.text.isEmpty()) {
                                        Text("Start writing…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEncryptDialog) {
        EncryptDialog(
            onEncrypt = { pass ->
                vm.performEncrypt(pass)
                showEncryptDialog = false
            },
            onDismiss = { showEncryptDialog = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete note?") },
            text = { Text("This action is permanent and cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { vm.delete(); showDeleteConfirm = false; onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}