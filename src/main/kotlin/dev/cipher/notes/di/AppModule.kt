package dev.cipher.notes.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.cipher.notes.data.NoteDao
import dev.cipher.notes.data.NoteDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): NoteDatabase =
        Room.databaseBuilder(ctx, NoteDatabase::class.java, "cipher_notes.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideNoteDao(db: NoteDatabase): NoteDao = db.noteDao()
}
