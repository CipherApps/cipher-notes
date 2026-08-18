package dev.cipher.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(
    isBiometricEnabled: Boolean,
    onUnlock: () -> Unit
) {
    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled) {
            onUnlock()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080A0E)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Shield,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color(0xFF00E5A0)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "CipherNotes is Locked",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall
        )

        if (isBiometricEnabled) {
            Button(
                onClick = onUnlock,
                modifier = Modifier.padding(top = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2130))
            ) {
                Text("Unlock with Biometrics")
            }
        }
    }
}