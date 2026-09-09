package dev.cipher.notes.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.cipher.notes.MainActivity
import dev.cipher.notes.data.Note
import dev.cipher.notes.data.NoteRepository
import kotlinx.coroutines.flow.firstOrNull

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotesWidgetEntryPoint {
    fun noteRepository(): NoteRepository
}

class NotesWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val WIDGET_CONTENT_VISIBLE_KEY = booleanPreferencesKey("widget_content_visible")
        val SELECTED_NOTE_IDS_KEY = stringSetPreferencesKey("selected_note_ids")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            NotesWidgetEntryPoint::class.java
        )

        val repository = entryPoint.noteRepository()

        val allNotes = runCatching {
            repository.getAllNotes().firstOrNull() ?: emptyList()
        }.getOrDefault(emptyList())


        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            val state = currentState<Preferences>()
            val selectedNoteIds = state[SELECTED_NOTE_IDS_KEY] ?: emptySet()
            val isContentVisible = state[WIDGET_CONTENT_VISIBLE_KEY] ?: false

            val displayedNotes = allNotes
                .filter { note -> selectedNoteIds.contains(note.id) }
                .take(4)

            GlanceTheme {
                WidgetContent(
                    notes = displayedNotes,
                    isContentVisible = isContentVisible,
                    clickIntent = mainActivityIntent
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(notes: List<Note>, isContentVisible: Boolean, clickIntent: Intent) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = Color(0xFF080A0E), night = Color(0xFF080A0E)))
                .padding(12.dp)
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable(actionStartActivity(intent = clickIntent)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pinned Notes",
                    style = TextStyle(
                        color = ColorProvider(day = Color(0xFF00E5A0), night = Color(0xFF00E5A0)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (notes.isEmpty()) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(clickIntent)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No notes pinned\nSelect in settings",
                        style = TextStyle(
                            color = ColorProvider(day = Color.Gray, night = Color.Gray),
                            fontSize = 12.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    items(
                        items = notes,
                        itemId = { note -> note.id.hashCode().toLong() }
                    ) { note ->
                        NoteWidgetItem(
                            note = note,
                            isContentVisible = isContentVisible,
                            clickIntent = clickIntent
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun NoteWidgetItem(note: Note, isContentVisible: Boolean, clickIntent: Intent) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(ColorProvider(day = Color(0xFF121820), night = Color(0xFF121820)))
                .padding(8.dp)
                .clickable(actionStartActivity(clickIntent))
        ) {
            Text(
                text = note.title.ifEmpty { "Untitled" },
                style = TextStyle(
                    color = ColorProvider(day = Color.White, night = Color.White),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))

            val displayText = when {
                note.encrypted -> "Encrypted note"
                isContentVisible -> note.content.ifEmpty { "No text content" }
                else -> "••• Hidden •••"
            }

            Text(
                text = displayText,
                style = TextStyle(
                    color = ColorProvider(day = Color(0xFFA0AAB0), night = Color(0xFFA0AAB0)),
                    fontSize = 11.sp
                ),
                maxLines = 5
            )
        }
    }
}

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}