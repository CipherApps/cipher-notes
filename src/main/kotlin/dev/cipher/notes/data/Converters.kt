package dev.cipher.notes.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromNoteType(value: NoteType): String {
        return value.name
    }

    @TypeConverter
    fun toNoteType(value: String): NoteType {
        return try {
            NoteType.valueOf(value)
        } catch (e: Exception) {
            NoteType.TEXT
        }
    }
}
