package dev.cipher.notes.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cipher.notes.crypto.CryptoManager
import dev.cipher.notes.data.Note
import dev.cipher.notes.data.NoteRepository
import dev.cipher.notes.data.NoteType
import dev.cipher.notes.data.TodoItem
import dev.cipher.notes.utils.JsonUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

data class EditorUiState(
    val note: Note? = null,
    val title: String = "",
    val content: TextFieldValue = TextFieldValue(""),
    val items: List<TodoItem> = emptyList(),
    val encrypted: Boolean = false,
    val isLocked: Boolean = false,
    val hasBiometric: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repo: NoteRepository,
    private val crypto: CryptoManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String? = savedStateHandle["noteId"]
    private var currentUserPassword: String? = null
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        loadNote()
    }

    private fun loadNote() {
        if (noteId != null) {
            viewModelScope.launch {
                val note = repo.getNoteById(noteId) ?: return@launch

                val rawSharedText = savedStateHandle.get<String>("sharedText")

                val incomingSharedText = rawSharedText?.let {
                    val decoded = android.net.Uri.decode(it)
                    sanitizeSharedText(decoded)
                }

                val finalContentText = incomingSharedText ?: note.content

                _uiState.value = EditorUiState(
                    note = note,
                    title = note.title,
                    content = TextFieldValue(text = finalContentText),
                    items = if (note.type == NoteType.TODO) JsonUtils.jsonToTodoItems(note.itemsJson) else emptyList(),
                    encrypted = note.encrypted,
                    isLocked = note.encrypted,
                    hasBiometric = crypto.hasBiometricPassword(note.id)
                )

                if (incomingSharedText != null) {
                    save()
                }
            }
        }
    }

    private var _sharedTextProcessed = false

    fun shouldProcessSharedText(text: String?): Boolean {
        if (text == null) return false
        if (_sharedTextProcessed) return false
        return true
    }

    fun markSharedTextAsProcessed() {
        _sharedTextProcessed = true
    }

    fun setTitle(t: String) {
        _uiState.update { it.copy(title = t) }
        save()
    }

    fun setContent(newValue: TextFieldValue) {
        _uiState.update { it.copy(content = newValue) }
        save()
    }

    fun addTodoItem(text: String = "") {
        val currentItems = _uiState.value.items.toMutableList()
        currentItems.add(JsonUtils.newTodoItem(text))
        _uiState.update { it.copy(items = currentItems) }
        save()
    }

    fun updateTodoItem(id: String, text: String? = null, done: Boolean? = null) {
        val currentItems = _uiState.value.items.toMutableList()
        val idx = currentItems.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val item = currentItems[idx]
            currentItems[idx] = item.copy(text = text ?: item.text, done = done ?: item.done)
            _uiState.update { it.copy(items = currentItems) }
            save()
        }
    }

    fun deleteTodoItem(id: String) {
        val currentItems = _uiState.value.items.filter { it.id != id }
        _uiState.update { it.copy(items = currentItems) }
        save()
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isLocked) return@launch
            val note = state.note ?: return@launch

            try {
                val updatedCiphertext = if (note.encrypted && currentUserPassword != null) {
                    val payload = JSONObject().apply {
                        put("title", state.title)
                        if (note.type == NoteType.TODO) {
                            put("items", JsonUtils.todoItemsToJson(state.items))
                        } else {
                            put("content", state.content.text)
                        }
                    }

                    withContext(Dispatchers.Default) {
                        crypto.encrypt(payload.toString(), currentUserPassword!!)
                    }
                } else {
                    note.ciphertext
                }

                val updated = note.copy(
                    title = state.title.trim(),
                    content = if (!note.encrypted && note.type == NoteType.TEXT) state.content.text else "",
                    itemsJson = if (!note.encrypted && note.type == NoteType.TODO) JsonUtils.todoItemsToJson(state.items) else "[]",
                    ciphertext = updatedCiphertext,
                    modifiedAt = System.currentTimeMillis()
                )
                repo.saveNote(updated)
                _uiState.update { it.copy(note = updated) }
            } catch (e: Exception) {
            }
        }
    }

    fun performEncrypt(password: String, enableBiometric: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            val note = state.note ?: return@launch
            try {
                val payload = JSONObject().apply {
                    put("title", state.title)
                    if (note.type == NoteType.TEXT) {
                        put("content", state.content.text)
                    } else {
                        put("items", JsonUtils.todoItemsToJson(state.items))
                    }
                }

                val cipher = withContext(Dispatchers.Default) {
                    crypto.encrypt(payload.toString(), password)
                }

                currentUserPassword = password

                if (enableBiometric) {
                    crypto.savePasswordForBiometric(note.id, password)
                } else {
                    crypto.removeBiometricPassword(note.id)
                }

                val encryptedNote = note.copy(
                    ciphertext = cipher,
                    encrypted = true,
                    content = "",
                    itemsJson = "[]",
                    modifiedAt = System.currentTimeMillis()
                )
                repo.saveNote(encryptedNote)

                _uiState.update { it.copy(
                    note = encryptedNote,
                    encrypted = true,
                    isLocked = true,
                    hasBiometric = enableBiometric,
                    error = null
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Encryption failed") }
            }
        }
    }

    fun unlock(password: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val note = state.note ?: return@launch

            try {
                val ciphertext = note.ciphertext ?: return@launch
                val decryptedJson = withContext(Dispatchers.Default) {
                    crypto.decrypt(ciphertext, password)
                }

                val payload = JSONObject(decryptedJson)
                currentUserPassword = password

                _uiState.update { it.copy(
                    isLocked = false,
                    title = payload.optString("title", note.title),
                    content = TextFieldValue(text = payload.optString("content", "")),
                    items = if (note.type == NoteType.TODO) {
                        JsonUtils.jsonToTodoItems(payload.optString("items", "[]"))
                    } else emptyList(),
                    hasBiometric = crypto.hasBiometricPassword(note.id),
                    error = null
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Wrong password",
                    isLocked = true
                ) }
            }
        }
    }

    fun unlockWithBiometric() {
        val note = _uiState.value.note ?: return
        val savedPassword = crypto.getPasswordFromBiometric(note.id)
        if (savedPassword != null) {
            unlock(savedPassword)
        } else {
            _uiState.update { it.copy(error = "Biometric data missing or invalid") }
        }
    }

    fun exportNote(context: Context) {
        val state = _uiState.value
        if (state.isLocked) {
            Toast.makeText(context, "Unlock note first to export", Toast.LENGTH_SHORT).show()
            return
        }
        val textToExport = "Title: ${state.title}\n\n${state.content.text}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, state.title)
            putExtra(Intent.EXTRA_TEXT, textToExport)
        }
        context.startActivity(Intent.createChooser(intent, "Export Note"))
    }

    fun delete() {
        viewModelScope.launch {
            val note = _uiState.value.note ?: return@launch
            crypto.removeBiometricPassword(note.id)
            repo.deleteNote(note.id)
        }
    }

    private fun sanitizeSharedText(text: String): String {
        var raw = text

        if (raw.contains("#:~:text=")) {
            raw = raw.replace(Regex("#:~:text=.*"), "")
        }

        val httpUrlRegex = Regex("https?://[^\\s]+")
        val matchResult = httpUrlRegex.find(raw)

        val extractedUrl = matchResult?.value
        var cleanText = if (extractedUrl != null) raw.replace(extractedUrl, "") else raw

        cleanText = cleanText
            .replace(Regex("\\[\\d+\\]"), "")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex(" +([.,!?:;])"), "$1")
            .replace(",.", ".")
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .trim()

        return if (!extractedUrl.isNullOrBlank()) {
            if (cleanText.isNotEmpty()) {
                "$cleanText\n\nSource: $extractedUrl"
            } else {
                extractedUrl
            }
        } else {
            cleanText
        }
    }
}