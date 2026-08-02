package dev.cipher.notes.ui.screens

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cipher.notes.data.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: NoteRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        private val DYNAMIC_COLORS_KEY = booleanPreferencesKey("use_dynamic_colors")
        private val APP_LOCK_KEY = booleanPreferencesKey("app_lock_enabled")
        private val APP_PIN_KEY = stringPreferencesKey("app_pin")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
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
        }
    }
}