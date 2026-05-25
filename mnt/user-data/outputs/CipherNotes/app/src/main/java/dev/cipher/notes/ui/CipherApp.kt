package dev.cipher.notes.ui

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.cipher.notes.data.NoteRepository
import dev.cipher.notes.data.NoteType
import dev.cipher.notes.ui.screens.*
import dev.cipher.notes.ui.theme.CipherTheme
import kotlinx.coroutines.launch

@Composable
fun CipherApp(repo: NoteRepository? = null) {
    CipherTheme {
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        var showNewNoteDialog by remember { mutableStateOf(false) }

        NavHost(navController, startDestination = "list") {
            composable("list") {
                ListScreen(
                    onNoteClick = { noteId ->
                        navController.navigate("editor/$noteId")
                    },
                    onCreateNote = {
                        showNewNoteDialog = true
                    }
                )
            }

            composable(
                "editor/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable
                EditorScreen(
                    noteId = noteId,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                "todo/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable
                TodoScreen(
                    noteId = noteId,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        if (showNewNoteDialog) {
            NewNoteDialog(
                onCreateNote = { type ->
                    scope.launch {
                        showNewNoteDialog = false
                        // Creation handled in ViewModel
                    }
                },
                onDismiss = {
                    showNewNoteDialog = false
                }
            )
        }
    }
}
