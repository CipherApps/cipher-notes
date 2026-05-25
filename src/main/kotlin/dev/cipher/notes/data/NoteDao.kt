package dev.cipher.notes.data

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY modifiedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): Note?

    @Query("""
        SELECT * FROM notes WHERE
        (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY modifiedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE type = :type ORDER BY modifiedAt DESC")
    fun getNotesByType(type: NoteType): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE encrypted = 1 ORDER BY modifiedAt DESC")
    fun getEncryptedNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
    @RawQuery
    suspend fun runRawQuery(query: androidx.sqlite.db.SupportSQLiteQuery): Int
}
