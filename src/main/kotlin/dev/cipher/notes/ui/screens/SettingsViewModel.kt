package dev.cipher.notes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cipher.notes.data.NoteRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: NoteRepository
) : ViewModel() {

    fun nuclearWipe() {
        viewModelScope.launch {
            repo.deleteAllNotes()
        }
    }
}

