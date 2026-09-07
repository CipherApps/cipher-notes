package dev.cipher.notes.ui.screens

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.cipher.notes.data.Note
import dev.cipher.notes.data.NoteRepository
import dev.cipher.notes.widget.NotesWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: NoteRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        private val DYNAMIC_COLORS_KEY = booleanPreferencesKey("use_dynamic_colors")
        private val APP_LOCK_KEY = booleanPreferencesKey("app_lock_enabled")
        private val APP_PIN_KEY = stringPreferencesKey("app_pin")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        private val WIDGET_CONTENT_VISIBLE_KEY = booleanPreferencesKey("widget_content_visible")

        val SELECTED_NOTE_IDS_KEY = stringSetPreferencesKey("selected_note_ids")
    }

    val allNotes: Flow<List<Note>> = repo.getAllNotes()


    val pinnedNoteIds: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[SELECTED_NOTE_IDS_KEY] ?: emptySet()
    }

    fun updateSelectedWidgetNotes(selectedIds: Set<String>) {
        viewModelScope.launch {
            val limitedIds = selectedIds.take(4).toSet()


            dataStore.edit { preferences ->
                preferences[SELECTED_NOTE_IDS_KEY] = limitedIds
            }


            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(NotesWidget::class.java)

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[SELECTED_NOTE_IDS_KEY] = limitedIds
                    }
                }
                NotesWidget().update(context, glanceId)
            }
        }
    }

    val useDynamicColors: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DYNAMIC_COLORS_KEY] ?: true
        }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[DYNAMIC_COLORS_KEY] = enabled
            }
        }
    }

    val isAppLockEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[APP_LOCK_KEY] ?: false
        }

    fun setAppLock(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[APP_LOCK_KEY] = enabled
            }
        }
    }

    val isBiometricEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] ?: true
        }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[BIOMETRIC_ENABLED_KEY] = enabled
            }
        }
    }

    val isWidgetContentVisible: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[WIDGET_CONTENT_VISIBLE_KEY] ?: false
        }

    fun setWidgetContentVisible(visible: Boolean) {
        viewModelScope.launch {

            dataStore.edit { preferences ->
                preferences[WIDGET_CONTENT_VISIBLE_KEY] = visible
            }


            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(NotesWidget::class.java)

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WIDGET_CONTENT_VISIBLE_KEY] = visible
                    }
                }
                NotesWidget().update(context, glanceId)
            }
        }
    }

    val appPin: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[APP_PIN_KEY]
        }

    fun setAppPin(pin: String?) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                if (pin == null) {
                    preferences.remove(APP_PIN_KEY)
                } else {
                    preferences[APP_PIN_KEY] = pin
                }
            }
        }
    }

    fun nuclearWipe() {
        viewModelScope.launch {
            repo.deleteAllNotes()
            dataStore.edit { preferences ->
                preferences.clear()
            }
            updateSelectedWidgetNotes(emptySet())
        }
    }
}