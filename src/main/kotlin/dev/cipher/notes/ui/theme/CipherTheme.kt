package dev.cipher.notes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val VoidBg = Color(0xFF080A0E)
private val RaisedBg = Color(0xFF1A2130)
private val SurfaceBg = Color(0xFF131820)
private val BorderColor = Color(0xFF232D3F)
private val BorderHi = Color(0xFF2E3D55)
private val Muted = Color(0xFF4A5A72)
private val Subtle = Color(0xFF7A8FA8)
private val Body = Color(0xFFB8C8DC)
private val Heading = Color(0xFFE8F0FA)
private val White = Color(0xFFF4F8FF)
private val Accent = Color(0xFF00E5A0)
private val Danger = Color(0xFFFF4D6A)
private val Warn = Color(0xFFFFB830)
private val Info = Color(0xFF3B9EFF)

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = VoidBg,
    primaryContainer = RaisedBg,
    onPrimaryContainer = Accent,
    secondary = Info,
    tertiary = Warn,
    background = VoidBg,
    onBackground = Body,
    surface = SurfaceBg,
    onSurface = Heading,
    surfaceVariant = RaisedBg,
    onSurfaceVariant = Subtle,
    error = Danger,
    onError = White,
    errorContainer = RaisedBg,
    onErrorContainer = Danger,
    outline = BorderColor,
    outlineVariant = BorderHi
)

@Composable
fun CipherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> DarkScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}