package dev.cipher.notes.data

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(private val dao: NoteDao) {

    fun getAllNotes(): Flow<List<Note>> = dao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> = dao.searchNotes(query)

    suspend fun getNoteById(id: String): Note? = withContext(Dispatchers.IO) {
        Log.d("CipherNotes", "Repository: Fetching note $id")
        dao.getNoteById(id)
    }

    suspend fun createNote(type: NoteType): Note = withContext(Dispatchers.IO) {
        Log.d("CipherNotes", "Repository: Creating new $type note")
        val note = Note(
            id = UUID.randomUUID().toString(),
            type = type,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
        try {
            dao.insertNote(note)
            Log.d("CipherNotes", "Repository: Note saved successfully with ID ${note.id}")
        } catch (e: Exception) {
            Log.e("CipherNotes", "Repository: Failed to save note", e)
            throw e
        }
        note
    }

    suspend fun saveNote(note: Note) = withContext(Dispatchers.IO) {
        dao.insertNote(note.copy(modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        dao.deleteNoteById(id)
    }

    suspend fun deleteAllNotes() = withContext(Dispatchers.IO) {
        dao.deleteAllNotes()

        try {
            dao.runRawQuery(SimpleSQLiteQuery("VACUUM"))
        } catch (e: Exception) {
            Log.e("CipherNotes", "Vacuum failed", e)
        }
    }
}