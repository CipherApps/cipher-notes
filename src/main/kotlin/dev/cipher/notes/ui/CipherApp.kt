package dev.cipher.notes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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
import dev.cipher.notes.ui.screens.EditorScreen
import dev.cipher.notes.ui.screens.ListScreen
import dev.cipher.notes.ui.screens.NoteEditorViewModel
import dev.cipher.notes.ui.screens.SettingsScreen
import dev.cipher.notes.ui.screens.TodoScreen

@Composable
fun CipherApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            ListScreen(
                onNoteClick = { id -> navController.navigate("detail/$id") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
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
                    CircularProgressIndicator()
                }
            } else {
                if (note.type == NoteType.TODO) {
                    TodoScreen(
                        noteId = note.id,
                        onBack = { navController.popBackStack() },
                        onSettingsClick = { navController.navigate("settings") },
                        vm = viewModel
                    )
                } else {
                    EditorScreen(
                        noteId = note.id,
                        onBack = { navController.popBackStack() },
                        onSettingsClick = { navController.navigate("settings") },
                        vm = viewModel
                    )
                }
            }
        }
    }
}
