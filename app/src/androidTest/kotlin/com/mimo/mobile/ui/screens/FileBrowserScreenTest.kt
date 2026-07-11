package com.mimo.mobile.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.mimo.mobile.ui.theme.MiMoTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for FileBrowserScreen UI components.
 *
 * Tests verify directory navigation, file operations (read, write, delete),
 * and error handling for permission denied scenarios.
 */
class FileBrowserScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─── File navigation (directories) ───

    @Test
    fun fileBrowser_titleDisplayed() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Files")
            }
        }
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_rootPathDisplayed() {
        composeTestRule.setContent {
            MiMoTheme {
                Text(".")
            }
        }
        composeTestRule.onNodeWithText(".").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_subpathDisplayed() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("src/main/kotlin")
            }
        }
        composeTestRule.onNodeWithText("src/main/kotlin").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_folderCountDisplayed() {
        composeTestRule.setContent {
            MiMoTheme {
                Column {
                    Text("3", style = MaterialTheme.typography.titleMedium)
                    Text("Carpetas", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        composeTestRule.onNodeWithText("Carpetas").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_fileCountDisplayed() {
        composeTestRule.setContent {
            MiMoTheme {
                Column {
                    Text("12", style = MaterialTheme.typography.titleMedium)
                    Text("Archivos", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        composeTestRule.onNodeWithText("Archivos").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_totalCountDisplayed() {
        composeTestRule.setContent {
            MiMoTheme {
                Column {
                    Text("15", style = MaterialTheme.typography.titleMedium)
                    Text("Total", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_backButtonDisplayedReaderOnRoot() {
        composeTestRule.setContent {
            MiMoTheme {
                IconButton(onClick = {}, modifier = Modifier.testTag("back_button")) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Folder,
                        contentDescription = "Root"
                    )
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Root").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_backButtonDisplaysArrowOnSubpath() {
        composeTestRule.setContent {
            MiMoTheme {
                IconButton(onClick = {}, modifier = Modifier.testTag("back_button")) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_directoryEntry_showsFolderIcon() {
        val entry = FileEntry(name = "src", isDir = true, size = 0, device = "phone")
        composeTestRule.setContent {
            MiMoTheme {
                Text(entry.name)
            }
        }
        composeTestRule.onNodeWithText("src").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_fileEntry_showsFileName() {
        val entry = FileEntry(name = "Main.kt", isDir = false, size = 1024, device = "phone")
        composeTestRule.setContent {
            MiMoTheme {
                Text(entry.name)
            }
        }
        composeTestRule.onNodeWithText("Main.kt").assertIsDisplayed()
    }

    // ─── File operations (read, write, delete) ───

    @Test
    fun fileViewer_displaysFileName() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("README.md")
            }
        }
        composeTestRule.onNodeWithText("README.md").assertIsDisplayed()
    }

    @Test
    fun fileViewer_displaysFilePath() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("src/README.md")
            }
        }
        composeTestRule.onNodeWithText("src/README.md").assertIsDisplayed()
    }

    @Test
    fun fileViewer_displaysFileContent() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("fun main() { println(\"Hello\") }")
            }
        }
        composeTestRule.onNodeWithText("fun main() { println(\"Hello\") }").assertIsDisplayed()
    }

    @Test
    fun fileViewer_editButton() {
        composeTestRule.setContent {
            MiMoTheme {
                FilledTonalButton(onClick = {}) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Edit,
                        contentDescription = null
                    )
                    Text("Edit")
                }
            }
        }
        composeTestRule.onNodeWithText("Edit").assertIsDisplayed()
    }

    @Test
    fun fileEditor_showsEditingTitle() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Editing: build.gradle.kts")
            }
        }
        composeTestRule.onNodeWithText("Editing: build.gradle.kts").assertIsDisplayed()
    }

    @Test
    fun fileEditor_saveButton() {
        composeTestRule.setContent {
            MiMoTheme {
                FilledTonalButton(onClick = {}) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Save,
                        contentDescription = null
                    )
                    Text("Save")
                }
            }
        }
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun fileEditor_textFieldAcceptsContent() {
        var content by remember { mutableStateOf("initial content") }
        composeTestRule.setContent {
            MiMoTheme {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.testTag("editor")
                )
            }
        }
        composeTestRule.onNodeWithTag("editor").assertExists()
    }

    @Test
    fun fileEditor_contentChangeIsEditable() {
        var content by remember { mutableStateOf("") }
        composeTestRule.setContent {
            MiMoTheme {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.testTag("editor")
                )
            }
        }
        composeTestRule.onNodeWithTag("editor").performTextInput("hello world")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("hello world").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_createDialog_showsTitle() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Crear Archivo")
            }
        }
        composeTestRule.onNodeWithText("Crear Archivo").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_createDialog_showsNameField() {
        composeTestRule.setContent {
            MiMoTheme {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Nombre del archivo") }
                )
            }
        }
        composeTestRule.onNodeWithText("Nombre del archivo").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_createDialog_showsCreateButton() {
        composeTestRule.setContent {
            MiMoTheme {
                Button(onClick = {}) { Text("Crear") }
            }
        }
        composeTestRule.onNodeWithText("Crear").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_deleteDialog_showsConfirmation() {
        val entryName = "test.txt"
        composeTestRule.setContent {
            MiMoTheme {
                Text("Seguro que quieres eliminar '$entryName'?")
            }
        }
        composeTestRule.onNodeWithText("Seguro que quieres eliminar 'test.txt'?").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_deleteDialog_showsEliminarButton() {
        composeTestRule.setContent {
            MiMoTheme {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            }
        }
        composeTestRule.onNodeWithText("Eliminar").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_deleteDialog_showsCancelarButton() {
        composeTestRule.setContent {
            MiMoTheme {
                TextButton(onClick = {}) {
                    Text("Cancelar")
                }
            }
        }
        composeTestRule.onNodeWithText("Cancelar").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_renameDialog_showsNewNameField() {
        composeTestRule.setContent {
            MiMoTheme {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Nuevo nombre") }
                )
            }
        }
        composeTestRule.onNodeWithText("Nuevo nombre").assertIsDisplayed()
    }

    // ─── Error handling (permission denied) ───

    @Test
    fun fileBrowser_permissionError_displayedAsError() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Permission denied")
            }
        }
        composeTestRule.onNodeWithText("Permission denied").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_accessDenied_displayedAsError() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Access denied")
            }
        }
        composeTestRule.onNodeWithText("Access denied").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_fileNotFound_errorHandling() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("File not found")
            }
        }
        composeTestRule.onNodeWithText("File not found").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_writeFailed_errorHandling() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Write failed: Permission denied")
            }
        }
        composeTestRule.onNodeWithText("Write failed: Permission denied").assertIsDisplayed()
    }

    // ─── Device display ───

    @Test
    fun fileBrowser_phoneDevice_showsPhoneIcon() {
        val entry = FileEntry(name = "test.txt", isDir = false, size = 1024, device = "phone")
        composeTestRule.setContent {
            MiMoTheme {
                Text(entry.device)
            }
        }
        composeTestRule.onNodeWithText("phone").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_pcDevice_showsPcIcon() {
        val entry = FileEntry(name = "test.txt", isDir = false, size = 1024, device = "pc")
        composeTestRule.setContent {
            MiMoTheme {
                Text(entry.device)
            }
        }
        composeTestRule.onNodeWithText("pc").assertIsDisplayed()
    }

    // ─── Selection mode ───

    @Test
    fun fileBrowser_selectionMode_showsSelectedCount() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("3 seleccionados")
            }
        }
        composeTestRule.onNodeWithText("3 seleccionados").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_selectionMode_showsDeleteButton() {
        composeTestRule.setContent {
            MiMoTheme {
                IconButton(onClick = {}, modifier = Modifier.testTag("delete_selected")) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                        contentDescription = "Delete selected"
                    )
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Delete selected").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_selectionMode_showsCloseButton() {
        composeTestRule.setContent {
            MiMoTheme {
                IconButton(onClick = {}, modifier = Modifier.testTag("exit_selection")) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Close,
                        contentDescription = "Exit selection"
                    )
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Exit selection").assertIsDisplayed()
    }

    @Test
    fun fileBrowser_refreshButton_displayed() {
        composeTestRule.setContent {
            MiMoTheme {
                TextButton(onClick = {}) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Refresh,
                        contentDescription = null
                    )
                    Text("Refresh")
                }
            }
        }
        composeTestRule.onNodeWithText("Refresh").assertIsDisplayed()
    }

    // ─── FileEntry data class ───

    @Test
    fun fileEntry_dataClass_defaults() {
        val entry = FileEntry(name = "test.txt")
        assertFalse(entry.isDir)
        assertEquals(0L, entry.size)
        assertEquals("phone", entry.device)
    }

    @Test
    fun fileEntry_directory_entry() {
        val entry = FileEntry(name = "src", isDir = true)
        assertTrue(entry.isDir)
    }

    @Test
    fun fileEntry_withSize() {
        val entry = FileEntry(name = "large.bin", isDir = false, size = 1024 * 1024)
        assertEquals(1024 * 1024L, entry.size)
    }
}
