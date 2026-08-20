package dev.cipher.notes.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.cipher.notes.data.Note
import dev.cipher.notes.data.NoteRepository
import dev.cipher.notes.data.NoteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val filterBy: String = "all",
    val sortBy: String = "modified",
    val pinnedIds: Set<String> = emptySet()
)

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val repo: NoteRepository,
    @ApplicationContext context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("pinned_notes_prefs", Context.MODE_PRIVATE)

    private val _searchQuery = MutableStateFlow("")
    private val _filterBy = MutableStateFlow("all")
    private val _sortBy = MutableStateFlow("modified")
    private val _pinnedIds = MutableStateFlow(getPinnedIdsFromPrefs())

    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repo.getAllNotes(),
                _searchQuery,
                _filterBy,
                _sortBy,
                _pinnedIds
            ) { allNotes, query, filter, sort, pinnedIds ->
                val filteredNotes = applyFiltersAndSort(allNotes, query, filter, sort, pinnedIds)
                ListUiState(
                    notes = filteredNotes,
                    searchQuery = query,
                    filterBy = filter,
                    sortBy = sort,
                    pinnedIds = pinnedIds
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun togglePin(noteId: String) {
        val currentPinned = _pinnedIds.value.toMutableSet()
        if (currentPinned.contains(noteId)) {
            currentPinned.remove(noteId)
        } else {
            currentPinned.add(noteId)
        }
        prefs.edit().putStringSet("pinned_ids", currentPinned).apply()
        _pinnedIds.value = currentPinned
    }

    private fun getPinnedIdsFromPrefs(): Set<String> {
        return prefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setFilter(filter: String) { _filterBy.value = filter }
    fun setSortBy(sort: String) { _sortBy.value = sort }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repo.deleteNote(id)
            if (_pinnedIds.value.contains(id)) {
                togglePin(id)
            }
        }
    }

    fun createNote(type: NoteType, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newNote = repo.createNote(type)
            onCreated(newNote.id)
        }
    }

    private fun applyFiltersAndSort(
        notes: List<Note>,
        query: String,
        filter: String,
        sort: String,
        pinnedIds: Set<String>
    ): List<Note> {
        var filtered = notes

        filtered = when (filter) {
            "note"      -> filtered.filter { it.type == NoteType.TEXT }
            "todo"      -> filtered.filter { it.type == NoteType.TODO }
            "encrypted" -> filtered.filter { it.encrypted }
            else        -> filtered
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        (!it.encrypted && it.content.contains(query, ignoreCase = true))
            }
        }

        filtered = when (sort) {
            "created" -> filtered.sortedByDescending { it.createdAt }
            "title"   -> filtered.sortedBy { it.title }
            else      -> filtered.sortedByDescending { it.modifiedAt }
        }
        return filtered.sortedByDescending { pinnedIds.contains(it.id) }
    }
}