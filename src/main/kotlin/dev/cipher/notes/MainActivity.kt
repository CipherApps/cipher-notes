package dev.cipher.notes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.cipher.notes.ui.CipherMainApp
import dev.cipher.notes.ui.theme.CipherTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        var sharedText: String? = null
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                sharedText = text
            }
            intent.action = null
            intent.removeExtra(Intent.EXTRA_TEXT)
        }

        setContent {
            CipherTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CipherMainApp(sharedText = sharedText)
                }
            }
        }
    }
}