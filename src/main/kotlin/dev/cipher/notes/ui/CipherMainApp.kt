package dev.cipher.notes.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.cipher.notes.data.NoteType
import dev.cipher.notes.ui.screens.*

@Composable
fun CipherMainApp() {
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(navController = navController, startDestination = "list") {
            composable("list") {
                ListScreen(
                    onNoteClick = { id -> 
                        Log.d("CipherNotes", "Navigating to detail: $id")
                        navController.navigate("detail/$id") 
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onBack = { 
                        navController.popBackStack()
                    }
                )
            }
            
            composable(
                route = "detail/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) {
                val viewModel: NoteEditorViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()
                
                val note = uiState.note
                if (note == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (uiState.error != null) {
                            Text("Error: ${uiState.error}")
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    val onSettings: () -> Unit = { 
                        navController.navigate("settings") 
                    }
                    val onBack: () -> Unit = { 
                        navController.popBackStack()
                        Unit
                    }

                    if (note.type == NoteType.TODO) {
                        TodoScreen(
                            noteId = note.id,
                            onBack = onBack,
                            onSettingsClick = onSettings,
                            vm = viewModel
                        )
                    } else {
                        EditorScreen(
                            noteId = note.id,
                            onBack = onBack,
                            onSettingsClick = onSettings,
                            vm = viewModel
                        )
                    }
                }
            }
        }
    }
}
