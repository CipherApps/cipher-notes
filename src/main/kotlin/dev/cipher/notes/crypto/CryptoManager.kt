package dev.cipher.notes.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val PBKDF2_ITERATIONS = 200_000
        private const val PBKDF2_KEY_LENGTH = 256

        private const val BIOMETRIC_PREFS_NAME = "biometric_key_store"
        private const val PASS_PREFIX = "pass_"
    }

    fun encrypt(plaintext: String, password: String): String {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = salt + iv + ciphertext
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    fun decrypt(cipherB64: String, password: String): String {
        val combined = android.util.Base64.decode(cipherB64, android.util.Base64.NO_WRAP)
        val salt = combined.sliceArray(0 until SALT_LENGTH)
        val iv = combined.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val ciphertext = combined.sliceArray(SALT_LENGTH + IV_LENGTH until combined.size)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val plainBytes = cipher.doFinal(ciphertext)
        return String(plainBytes, Charsets.UTF_8)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val secretKey = factory.generateSecret(spec)
        spec.clearPassword()
        return SecretKeySpec(secretKey.encoded, 0, secretKey.encoded.size, "AES")
    }

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val biometricPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            BIOMETRIC_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun savePasswordForBiometric(noteId: String, password: String) {
        biometricPrefs.edit()
            .putString("$PASS_PREFIX$noteId", password)
            .apply()
    }

    fun getPasswordFromBiometric(noteId: String): String? {
        return biometricPrefs.getString("$PASS_PREFIX$noteId", null)
    }

    fun removeBiometricPassword(noteId: String) {
        biometricPrefs.edit()
            .remove("$PASS_PREFIX$noteId")
            .apply()
    }

    fun hasBiometricPassword(noteId: String): Boolean {
        return biometricPrefs.contains("$PASS_PREFIX$noteId")
    }
}