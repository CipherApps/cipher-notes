package dev.cipher.notes.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import dev.cipher.notes.data.NoteType
import dev.cipher.notes.widget.NotesWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
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


        private val HEADER_MAGIC = "CIPHER_N".toByteArray(Charsets.UTF_8)
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


    fun exportBackup(context: Context, uri: Uri, password: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val notes = repo.getAllNotes().first()
                val jsonArray = JSONArray()

                notes.forEach { note ->
                    val jsonNote = JSONObject().apply {
                        put("id", note.id)
                        put("title", note.title)
                        put("content", note.content)
                        put("itemsJson", note.itemsJson)
                        put("createdAt", note.createdAt)
                        put("modifiedAt", note.modifiedAt)
                        put("encrypted", note.encrypted)
                        put("ciphertext", note.ciphertext ?: JSONObject.NULL)
                        put("type", note.type.name)
                    }
                    jsonArray.put(jsonNote)
                }

                val backupObject = JSONObject().apply {
                    put("version", 1)
                    put("notes", jsonArray)
                }

                val rawBytes = backupObject.toString(2).toByteArray(Charsets.UTF_8)


                val bytesToWrite = if (!password.isNullOrEmpty()) {
                    encryptData(rawBytes, password)
                } else {
                    rawBytes
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytesToWrite)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    fun importBackup(
        context: Context,
        uri: Uri,
        password: String? = null,
        onPasswordRequired: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw Exception("Unable to read file")

                val jsonString = if (isCipherFile(bytes)) {
                    if (password.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) { onPasswordRequired() }
                        return@launch
                    }
                    val decryptedBytes = decryptData(bytes, password)
                    String(decryptedBytes, Charsets.UTF_8)
                } else {
                    String(bytes, Charsets.UTF_8)
                }

                val backupObject = JSONObject(jsonString)
                val jsonArray = backupObject.optJSONArray("notes") ?: JSONArray()

                var importedCount = 0
                for (i in 0 until jsonArray.length()) {
                    val jsonNote = jsonArray.getJSONObject(i)
                    val noteType = try {
                        NoteType.valueOf(jsonNote.optString("type", NoteType.entries.first().name))
                    } catch (e: Exception) {
                        NoteType.entries.first()
                    }

                    val ciphertextValue = if (jsonNote.isNull("ciphertext")) null else jsonNote.optString("ciphertext", null)

                    val note = Note(
                        id = jsonNote.optString("id", java.util.UUID.randomUUID().toString()),
                        title = jsonNote.optString("title", ""),
                        content = jsonNote.optString("content", ""),
                        itemsJson = jsonNote.optString("itemsJson", "[]"),
                        createdAt = jsonNote.optLong("createdAt", System.currentTimeMillis()),
                        modifiedAt = jsonNote.optLong("modifiedAt", System.currentTimeMillis()),
                        encrypted = jsonNote.optBoolean("encrypted", false),
                        ciphertext = ciphertextValue,
                        type = noteType
                    )

                    repo.insertOrUpdateNote(note)
                    importedCount++
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Imported $importedCount notes!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (password != null) "Incorrect password or corrupted file!" else "Import failed: Invalid JSON file",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun isCipherFile(bytes: ByteArray): Boolean {
        if (bytes.size < HEADER_MAGIC.size) return false
        return bytes.copyOfRange(0, HEADER_MAGIC.size).contentEquals(HEADER_MAGIC)
    }

    private fun encryptData(data: ByteArray, password: String): ByteArray {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val encryptedData = cipher.doFinal(data)


        return HEADER_MAGIC + salt + iv + encryptedData
    }

    private fun decryptData(data: ByteArray, password: String): ByteArray {
        var offset = HEADER_MAGIC.size
        val salt = data.copyOfRange(offset, offset + 16)
        offset += 16
        val iv = data.copyOfRange(offset, offset + 12)
        offset += 12
        val encryptedData = data.copyOfRange(offset, data.size)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(encryptedData)
    }
}