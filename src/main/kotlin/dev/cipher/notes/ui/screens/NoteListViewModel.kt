package dev.cipher.notes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val sortBy: String = "modified"
)

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val repo: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterBy = MutableStateFlow("all")
    private val _sortBy = MutableStateFlow("modified")

    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repo.getAllNotes(),
                _searchQuery,
                _filterBy,
                _sortBy
            ) { allNotes, query, filter, sort ->
                val filteredNotes = applyFiltersAndSort(allNotes, query, filter, sort)
                ListUiState(
                    notes = filteredNotes,
                    searchQuery = query,
                    filterBy = filter,
                    sortBy = sort
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setFilter(filter: String) { _filterBy.value = filter }
    fun setSortBy(sort: String) { _sortBy.value = sort }

    fun deleteNote(id: String) {
        viewModelScope.launch { repo.deleteNote(id) }
    }

    fun createNote(type: NoteType, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newNote = repo.createNote(type)
            onCreated(newNote.id)
        }
    }

    private fun applyFiltersAndSort(notes: List<Note>, query: String, filter: String, sort: String): List<Note> {
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

        return filtered
    }
}
