package dev.cipher.notes.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

data class EditorUiState(
    val note: Note? = null,
    val title: String = "",
    val content: String = "",
    val items: List<TodoItem> = emptyList(),
    val encrypted: Boolean = false,
    val isLocked: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repo: NoteRepository,
    private val crypto: CryptoManager,
    savedStateHandle: SavedStateHandle
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

                _uiState.value = EditorUiState(
                    note = note,
                    title = note.title,
                    content = note.content,
                    items = if (note.type == NoteType.TODO) JsonUtils.jsonToTodoItems(note.itemsJson) else emptyList(),
                    encrypted = note.encrypted,
                    isLocked = note.encrypted
                )
            }
        }
    }

    fun setTitle(t: String) {
        _uiState.value = _uiState.value.copy(title = t)
        save()
    }

    fun setContent(c: String) {
        _uiState.value = _uiState.value.copy(content = c)
        save()
    }

    fun addTodoItem(text: String = "") {
        val currentItems = _uiState.value.items.toMutableList()
        currentItems.add(JsonUtils.newTodoItem(text))
        _uiState.value = _uiState.value.copy(items = currentItems)
        save()
    }

    fun updateTodoItem(id: String, text: String? = null, done: Boolean? = null) {
        val currentItems = _uiState.value.items.toMutableList()
        val idx = currentItems.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val it = currentItems[idx]
            currentItems[idx] = it.copy(text = text ?: it.text, done = done ?: it.done)
            _uiState.value = _uiState.value.copy(items = currentItems)
            save()
        }
    }

    fun deleteTodoItem(id: String) {
        val currentItems = _uiState.value.items.filter { it.id != id }
        _uiState.value = _uiState.value.copy(items = currentItems)
        save()
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isLocked) return@launch
            val note = state.note ?: return@launch

            try {
                val updatedCiphertext = if (note.encrypted && currentUserPassword != null) {
                    val payload = JSONObject()
                    payload.put("title", state.title)
                    if (note.type == NoteType.TODO) {
                        payload.put("items", JsonUtils.todoItemsToJson(state.items))
                    } else {
                        payload.put("content", state.content)
                    }

                    withContext(Dispatchers.Default) {
                        crypto.encrypt(payload.toString(), currentUserPassword!!)
                    }
                } else {
                    note.ciphertext
                }

                val updated = note.copy(
                    title = state.title.trim(),
                    content = if (!note.encrypted && note.type == NoteType.TEXT) state.content else "",
                    itemsJson = if (!note.encrypted && note.type == NoteType.TODO) JsonUtils.todoItemsToJson(state.items) else "[]",
                    ciphertext = updatedCiphertext,
                    modifiedAt = System.currentTimeMillis()
                )
                repo.saveNote(updated)
                _uiState.value = _uiState.value.copy(note = updated)
            } catch (e: Exception) {
            }
        }
    }

    fun performEncrypt(password: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val note = state.note ?: return@launch
            try {
                val payload = JSONObject()
                payload.put("title", state.title)

                if (note.type == NoteType.TEXT) {
                    payload.put("content", state.content)
                } else {
                    payload.put("items", JsonUtils.todoItemsToJson(state.items))
                }

                val cipher = withContext(Dispatchers.Default) {
                    crypto.encrypt(payload.toString(), password)
                }

                currentUserPassword = password

                val encryptedNote = note.copy(
                    ciphertext = cipher,
                    encrypted = true,
                    content = "",
                    itemsJson = "[]",
                    modifiedAt = System.currentTimeMillis()
                )
                repo.saveNote(encryptedNote)

                _uiState.value = _uiState.value.copy(
                    note = encryptedNote,
                    encrypted = true,
                    isLocked = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Encryption failed")
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
                currentUserPassword = password // Cache the password

                _uiState.value = _uiState.value.copy(
                    isLocked = false,
                    title = payload.optString("title", note.title),
                    content = payload.optString("content", ""),
                    items = if (note.type == NoteType.TODO) {
                        JsonUtils.jsonToTodoItems(payload.optString("items", "[]"))
                    } else emptyList(),
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Wrong password",
                    isLocked = true
                )
            }
        }
    }

    fun exportNote(context: Context) {
        val state = _uiState.value
        if (state.isLocked) {
            Toast.makeText(context, "Unlock note first to export", Toast.LENGTH_SHORT).show()
            return
        }

        val textToExport = "Title: ${state.title}\n\n${state.content}"
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
            repo.deleteNote(note.id)
        }
    }
}