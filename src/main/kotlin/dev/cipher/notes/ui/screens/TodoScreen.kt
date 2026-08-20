package dev.cipher.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import dev.cipher.notes.crypto.BiometricPromptManager
import dev.cipher.notes.data.TodoItem
import dev.cipher.notes.ui.components.EncryptDialog
import dev.cipher.notes.utils.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.onFocusChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    noteId: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    vm: NoteEditorViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val note = uiState.note ?: return
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showEncryptDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val completedCount = uiState.items.count { it.done }
    val focusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.isLocked, uiState.items) {
        if (!uiState.isLocked && uiState.items.isEmpty()) {
            vm.addTodoItem()
        }
    }

    fun triggerBiometricUnlock() {
        val activity = context as? FragmentActivity
        if (activity != null && BiometricPromptManager.canAuthenticate(context)) {
            val promptManager = BiometricPromptManager(activity)
            promptManager.showBiometricPrompt(
                title = "Unlock Checklist",
                subtitle = "Confirm your identity to decrypt checklist",
                negativeButtonText = "Use Password",
                onSuccess = { vm.unlockWithBiometric() },
                onError = { }
            )
        }
    }

    LaunchedEffect(uiState.isLocked, uiState.hasBiometric) {
        if (uiState.isLocked && uiState.hasBiometric) {
            triggerBiometricUnlock()
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
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
                        IconButton(onClick = { showEncryptDialog = true }) {
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (uiState.isLocked) {
            var passwordInput by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Checklist Protected", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Enter Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.hasBiometric) {
                            IconButton(onClick = { triggerBiometricUnlock() }) {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = "Unlock with Biometrics",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
                if (uiState.error != null) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                }
                Button(
                    onClick = { vm.unlock(passwordInput) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = passwordInput.isNotEmpty()
                ) {
                    Icon(Icons.Rounded.LockOpen, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TextField(
                    value = uiState.title,
                    onValueChange = vm::setTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .focusRequester(titleFocusRequester),
                    placeholder = { Text("Checklist title…", style = MaterialTheme.typography.headlineSmall) },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            uiState.items.firstOrNull()?.let { firstItem ->
                                focusRequesters[firstItem.id]?.requestFocus()
                            }
                        }
                    )
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
                            modifier = Modifier.padding(6.dp, 4.dp),
                            fontSize = 10.sp
                        )
                    }
                    Text("$completedCount/${uiState.items.size} tasks done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        val itemFocusRequester = focusRequesters.getOrPut(item.id) { FocusRequester() }

                        TodoItemRow(
                            item = item,
                            focusRequester = itemFocusRequester,
                            onToggle = { vm.updateTodoItem(item.id, done = !item.done) },
                            onTextChange = { vm.updateTodoItem(item.id, text = it) },
                            onDelete = { vm.deleteTodoItem(item.id) },
                            onAddNewItemBelow = {
                                vm.addTodoItem()
                                scope.launch {
                                    delay(50)
                                    val lastItem = vm.uiState.value.items.lastOrNull()
                                    if (lastItem != null) {
                                        listState.animateScrollToItem(vm.uiState.value.items.lastIndex)
                                        focusRequesters[lastItem.id]?.requestFocus()
                                    }
                                }
                            }
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Button(
                        onClick = {
                            vm.addTodoItem()
                            scope.launch {
                                delay(50)
                                val lastItem = vm.uiState.value.items.lastOrNull()
                                if (lastItem != null) {
                                    listState.animateScrollToItem(vm.uiState.value.items.lastIndex)
                                    focusRequesters[lastItem.id]?.requestFocus()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Item")
                    }
                }
            }
        }
    }

    if (showEncryptDialog) {
        EncryptDialog(
            onEncrypt = { pass, enableBiometric ->
                vm.performEncrypt(password = pass, enableBiometric = enableBiometric)
                showEncryptDialog = false
            },
            onDismiss = { showEncryptDialog = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete checklist?") },
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

@Composable
fun TodoItemRow(
    item: TodoItem,
    focusRequester: FocusRequester,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
    onAddNewItemBelow: () -> Unit
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var annotatedContent by remember { mutableStateOf(AnnotatedString("")) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(item.text, linkColor) {
        val urlPattern = "((https?://|www\\.)[\\w\\d.#@/?=&%+-]+|\\b[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}/?[\\w\\d.#@/?=&%+-]*)".toRegex(RegexOption.IGNORE_CASE)

        annotatedContent = buildAnnotatedString {
            append(item.text)
            urlPattern.findAll(item.text).forEach { match ->
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(4.dp)
    ) {
        Checkbox(checked = item.done, onCheckedChange = { onToggle() })

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            BasicTextField(
                value = item.text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { onAddNewItemBelow() }
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Default,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    color = if (item.done)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (item.text.isEmpty()) {
                        Text(
                            "Task...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            )

            if (!isFocused && item.text.isNotEmpty()) {
                Text(
                    text = annotatedContent,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                        color = if (item.done)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    ),
                    onTextLayout = { textLayoutResult = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(annotatedContent) {
                            detectTapGestures { offset ->
                                var clickedUrl = false
                                textLayoutResult?.let { layout ->
                                    val position = layout.getOffsetForPosition(offset)
                                    annotatedContent
                                        .getStringAnnotations("URL", position, position)
                                        .firstOrNull()?.let { range ->
                                            clickedUrl = true
                                            var url = range.item
                                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                                url = "https://$url"
                                            }
                                            try {
                                                uriHandler.openUri(url)
                                            } catch (_: Exception) {}
                                        }
                                }
                                if (!clickedUrl) {
                                    focusRequester.requestFocus()
                                }
                            }
                        }
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}