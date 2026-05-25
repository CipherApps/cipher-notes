package dev.cipher.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NoteType { TEXT, TODO }

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String,
    val type: NoteType = NoteType.TEXT,
    val title: String = "",
    val content: String = "",
    val itemsJson: String = "[]",
    val encrypted: Boolean = false,
    val ciphertext: String? = null,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

data class TodoItem(
    val id: String,
    val text: String,
    val done: Boolean
)
