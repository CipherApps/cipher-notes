package dev.cipher.notes

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.cipher.notes.crypto.BiometricPromptManager
import dev.cipher.notes.ui.CipherMainApp
import dev.cipher.notes.ui.screens.SettingsViewModel
import dev.cipher.notes.ui.theme.CipherTheme

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        val sharedText = extractSharedText(intent)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val useDynamicColors by settingsViewModel.useDynamicColors.collectAsState(initial = true)
            val isAppLockEnabled by settingsViewModel.isAppLockEnabled.collectAsState(initial = false)
            val isBiometricEnabledState by settingsViewModel.isBiometricEnabled.collectAsState(initial = null)
            val appPin by settingsViewModel.appPin.collectAsState(initial = null)

            var isAuthenticated by remember { mutableStateOf(false) }

            CipherTheme(dynamicColor = useDynamicColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isAppLockEnabled && !isAuthenticated) {
                        val biometricEnabled = (isBiometricEnabledState == true) &&
                                BiometricPromptManager.canAuthenticate(this@MainActivity)

                        LockScreen(
                            correctPin = appPin,
                            biometricEnabled = biometricEnabled,
                            onUnlockRequest = {
                                if (biometricEnabled) {
                                    showBiometricPrompt(
                                        onSuccess = { isAuthenticated = true },
                                        onError = { /* or type PIN */ }
                                    )
                                }
                            },
                            onAuthenticated = { isAuthenticated = true }
                        )
                    } else {
                        CipherMainApp(sharedText = sharedText)
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit, onError: () -> Unit) {
        val promptManager = BiometricPromptManager(this)
        promptManager.showBiometricPrompt(
            title = "CipherNotes Locked",
            subtitle = "Authenticate to unlock application",
            negativeButtonText = "Use App PIN",
            onSuccess = onSuccess,
            onError = { onError() }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent == null || intent.action != Intent.ACTION_SEND) return null
        if (intent.type?.startsWith("text/") != true) return null

        return runCatching {
            val directString = intent.getStringExtra(Intent.EXTRA_TEXT)
            val charSequence = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            val htmlText = intent.getStringExtra(Intent.EXTRA_HTML_TEXT)
                ?: intent.getCharSequenceExtra(Intent.EXTRA_HTML_TEXT)?.toString()

            val resultText = directString ?: charSequence?.toString() ?: htmlText
            resultText?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}

@Composable
fun LockScreen(
    correctPin: String?,
    biometricEnabled: Boolean,
    onUnlockRequest: () -> Unit,
    onAuthenticated: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            onUnlockRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = "Biometric Lock",
                modifier = Modifier
                    .size(64.dp)
                    .clickable(enabled = biometricEnabled) { onUnlockRequest() },
                tint = if (biometricEnabled) primaryColor else primaryColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "CipherNotes Locked",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { index ->
                    val isFilled = index < enteredPin.length
                    Surface(
                        modifier = Modifier.size(16.dp),
                        shape = CircleShape,
                        color = if (isFilled) primaryColor else primaryColor.copy(alpha = 0.2f),
                        border = if (!isFilled) BorderStroke(1.dp, primaryColor) else null
                    ) {}
                }
            }
        }

        Column(
            modifier = Modifier.width(280.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "C")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { digit ->
                        if (digit.isEmpty()) {
                            Spacer(modifier = Modifier.size(64.dp))
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    if (digit == "C") {
                                        if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                    } else if (enteredPin.length < 4) {
                                        enteredPin += digit
                                        if (enteredPin.length == 4) {
                                            if (correctPin == null || enteredPin == correctPin) {
                                                onAuthenticated()
                                            } else {
                                                enteredPin = ""
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(digit, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }

        if (biometricEnabled) {
            TextButton(onClick = onUnlockRequest) {
                Text("Use Biometrics", color = primaryColor, fontWeight = FontWeight.Medium)
            }
        } else {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}