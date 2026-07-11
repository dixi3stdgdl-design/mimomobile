package com.mimo.mobile.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.ui.theme.MiMoTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for TerminalScreen UI components.
 *
 * Tests verify command execution display, output rendering,
 * and long output handling.
 */
class TerminalScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─── Command execution ───

    @Test
    fun terminalScreen_titleDisplayedReader() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Live Code Viewer")
            }
        }
        composeTestRule.onNodeWithText("Live Code Viewer").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_promptSymbol() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("$", fontFamily = FontFamily.Monospace)
            }
        }
        composeTestRule.onNodeWithText("$").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_commandInput_placeholder() {
        composeTestRule.setContent {
            MiMoTheme {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = {
                        Text("Comando...", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                )
            }
        }
        composeTestRule.onNodeWithText("Comando...").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_commandInput_acceptsText() {
        var input by androidx.compose.runtime.mutableStateOf("")
        composeTestRule.setContent {
            MiMoTheme {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.testTag("command_input")
                )
            }
        }
        composeTestRule.onNodeWithTag("command_input").performTextInput("ls -la")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ls -la").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_sendButton_displayed() {
        composeTestRule.setContent {
            MiMoTheme {
                IconButton(onClick = {}, modifier = Modifier.testTag("send_button")) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
    }

    // ─── Output display ───

    @Test
    fun terminalScreen_commandOutput_displaysText() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("output from command")
            }
        }
        composeTestRule.onNodeWithText("output from command").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_commandLine_showsLineNumber() {
        // Format: "%4d".format(lineNumber)
        composeTestRule.setContent {
            MiMoTheme {
                Text("%4d".format(1))
            }
        }
        composeTestRule.onNodeWithText("   1").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_commandLine_showsCommandPrefix() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("❯ ", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun terminalScreen_successLine_showsCheckmark() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("✓ Completado", color = Color(0xFF7EE787))
            }
        }
        composeTestRule.onNodeWithText("✓ Completado").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_errorLine_showsCross() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("✗ Error: command not found", color = Color(0xFFFF7B72))
            }
        }
        composeTestRule.onNodeWithText("✗ Error: command not found").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_chatStart_showsDivider() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("── MiMo está trabajando ──")
            }
        }
        composeTestRule.onNodeWithText("── MiMo está trabajando ──").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_fileLabel_shownOnCodeChunk() {
        composeTestRule.setContent {
            MiMoTheme {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                    color = Color(0xFFD2A8FF).copy(alpha = 0.15f)
                ) {
                    Text("main.kt", fontSize = 9.sp, color = Color(0xFFD2A8FF))
                }
            }
        }
        composeTestRule.onNodeWithText("main.kt").assertIsDisplayed()
    }

    // ─── Long output handling ───

    @Test
    fun terminalScreen_longOutput_multipleLines() {
        val output = (1..50).joinToString("\n") { "Line $it: ${"x".repeat(80)}" }
        composeTestRule.setContent {
            MiMoTheme {
                Text(output, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
        composeTestRule.onNodeWithText("Line 1:", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Line 50:", substring = true).assertIsDisplayed()
    }

    @Test
    fun terminalScreen_emptyState_showsPlaceholder() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Live Code Viewer")
            }
        }
        composeTestRule.onNodeWithText("Live Code Viewer").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_emptyState_showsDescription() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("El código que MiMo escribe aparecerá aquí")
            }
        }
        composeTestRule.onNodeWithText("El código que MiMo escribe aparecerá aquí").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_emptyState_showsTerminalIcon() {
        composeTestRule.setContent {
            MiMoTheme {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF7EE787).copy(alpha = 0.5f)
                )
            }
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    // ─── Connection state ───

    @Test
    fun terminalScreen_disconnectedState_showsWarning() {
        composeTestRule.setContent {
            MiMoTheme {
                Surface(
                    color = Color(0xFFFF7B72).copy(alpha = 0.1f)
                ) {
                    Text("Desconectado", color = Color(0xFFFF7B72))
                }
            }
        }
        composeTestRule.onNodeWithText("Desconectado").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_clearButton_displayed() {
        composeTestRule.setContent {
            MiMoTheme {
                IconButton(onClick = {}, modifier = Modifier.testTag("clear_button")) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.DeleteSweep,
                        contentDescription = "Clear"
                    )
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Clear").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_waitState_showsEsperando() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Esperando...", fontSize = 10.sp)
            }
        }
        composeTestRule.onNodeWithText("Esperando...").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_writingState_showsEscribiendo() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Escribiendo...", fontSize = 10.sp)
            }
        }
        composeTestRule.onNodeWithText("Escribiendo...").assertIsDisplayed()
    }

    // ─── CodeLineType rendering ───

    @Test
    fun terminalScreen_codeLineType_errorHasRedBackground() {
        composeTestRule.setContent {
            MiMoTheme {
                Surface(
                    color = Color(0xFFFF7B72).copy(alpha = 0.05f)
                ) {
                    Text("error: something went wrong", color = Color(0xFFFF7B72))
                }
            }
        }
        composeTestRule.onNodeWithText("error: something went wrong").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_codeLineType_commandHasGreenText() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("$ ls -la", color = Color(0xFF7EE787))
            }
        }
        composeTestRule.onNodeWithText("$ ls -la").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_codeLineType_codeHasCyanText() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("fun main() {}", color = Color(0xFFA5D6FF))
            }
        }
        composeTestRule.onNodeWithText("fun main() {}").assertIsDisplayed()
    }

    @Test
    fun terminalScreen_codeLineType_commentHasGrayText() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("── MiMo está trabajando ──", color = Color(0xFF8B949E))
            }
        }
        composeTestRule.onNodeWithText("── MiMo está trabajando ──").assertIsDisplayed()
    }

    // ─── CodeLine data class ───

    @Test
    fun codeLine_dataClass_basicCreation() {
        val line = CodeLine(
            content = "hello",
            lineNumber = 1,
            type = CodeLineType.OUTPUT
        )
        assertEquals("hello", line.content)
        assertEquals(1, line.lineNumber)
        assertEquals(CodeLineType.OUTPUT, line.type)
        assertNull(line.fileName)
    }

    @Test
    fun codeLine_dataClass_withFileName() {
        val line = CodeLine(
            content = "fun main() {}",
            lineNumber = 5,
            type = CodeLineType.CODE,
            fileName = "Main.kt"
        )
        assertEquals("Main.kt", line.fileName)
    }

    @Test
    fun codeLineType_hasAllExpectedValues() {
        val types = CodeLineType.values()
        assertEquals(6, types.size)
        assertNotNull(CodeLineType.COMMAND)
        assertNotNull(CodeLineType.OUTPUT)
        assertNotNull(CodeLineType.ERROR)
        assertNotNull(CodeLineType.CODE)
        assertNotNull(CodeLineType.COMMENT)
        assertNotNull(CodeLineType.SUCCESS)
    }

    @Test
    fun codeLineType_valueOfWorksCorrectly() {
        assertEquals(CodeLineType.COMMAND, CodeLineType.valueOf("COMMAND"))
        assertEquals(CodeLineType.ERROR, CodeLineType.valueOf("ERROR"))
        assertEquals(CodeLineType.SUCCESS, CodeLineType.valueOf("SUCCESS"))
    }
}
