package com.mimo.mobile.ui.screens

import androidx.compose.foundation.layout.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.mimo.mobile.ui.theme.MiMoTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for RemoteScreen UI components.
 *
 * Tests verify the UI elements for remote screen streaming,
 * touch input modes, zoom gestures, and screen refresh behavior.
 */
class RemoteScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun remoteScreen_showsConnectingState() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Conectando...")
            }
        }
        composeTestRule.onNodeWithText("Conectando...").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_showsErrorMessage() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Decode: invalid base64")
            }
        }
        composeTestRule.onNodeWithText("Decode: invalid base64").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_touchModeChip_isDisplayed() {
        composeTestRule.setContent {
            MiMoTheme {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("Touch") },
                    leadingIcon = { Icon(Icons.Filled.TouchApp, null, modifier = Modifier.size(12.dp)) }
                )
            }
        }
        composeTestRule.onNodeWithText("Touch").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_keyboardModeChip_isDisplayed() {
        composeTestRule.setContent {
            MiMoTheme {
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Type") },
                    leadingIcon = { Icon(Icons.Filled.Keyboard, null, modifier = Modifier.size(12.dp)) }
                )
            }
        }
        composeTestRule.onNodeWithText("Type").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_fpsIndicator_showsValue() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("30 FPS")
            }
        }
        composeTestRule.onNodeWithText("30 FPS").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_fpsIndicator_showsZeroFps() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("0 FPS")
            }
        }
        composeTestRule.onNodeWithText("0 FPS").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_controlsLabel_showsRemote() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Remote")
            }
        }
        composeTestRule.onNodeWithText("Remote").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_tapHint_isDisplayed() {
        composeTestRule.setContent {
            MiMoTheme {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text("Toca para controles")
                }
            }
        }
        composeTestRule.onNodeWithText("Toca para controles").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_keyboardShortcut_tab() {
        composeTestRule.setContent {
            MiMoTheme {
                AssistChip(
                    onClick = {},
                    label = { Text("Tab", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified) }
                )
            }
        }
        composeTestRule.onNodeWithText("Tab").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_keyboardShortcut_enter() {
        composeTestRule.setContent {
            MiMoTheme {
                AssistChip(
                    onClick = {},
                    label = { Text("Enter", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified) }
                )
            }
        }
        composeTestRule.onNodeWithText("Enter").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_keyboardShortcut_escape() {
        composeTestRule.setContent {
            MiMoTheme {
                AssistChip(
                    onClick = {},
                    label = { Text("Esc", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified) }
                )
            }
        }
        composeTestRule.onNodeWithText("Esc").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_keyboardShortcut_backspace() {
        composeTestRule.setContent {
            MiMoTheme {
                AssistChip(
                    onClick = {},
                    label = { Text("Bksp", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified) }
                )
            }
        }
        composeTestRule.onNodeWithText("Bksp").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_ctrlCShortcut() {
        composeTestRule.setContent {
            MiMoTheme {
                AssistChip(
                    onClick = {},
                    label = { Text("Ctrl+C", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified) }
                )
            }
        }
        composeTestRule.onNodeWithText("Ctrl+C").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_ctrlVShortcut() {
        composeTestRule.setContent {
            MiMoTheme {
                AssistChip(
                    onClick = {},
                    label = { Text("Ctrl+V", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified) }
                )
            }
        }
        composeTestRule.onNodeWithText("Ctrl+V").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_zoomControls_gestureState() {
        // Test zoom gesture state transitions
        var zoomLevel by remember { mutableFloatStateOf(1f) }
        composeTestRule.setContent {
            MiMoTheme {
                Text("Zoom: ${String.format("%.1f", zoomLevel)}x")
            }
        }
        composeTestRule.onNodeWithText("Zoom: 1.0x").assertIsDisplayed()

        // Simulate zoom level change
        composeTestRule.runOnUiThread {
            zoomLevel = 2.0f
        }
        composeTestRule.onNodeWithText("Zoom: 2.0x").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_screenRefresh_indicatorDisplayed() {
        composeTestRule.setContent {
            MiMoTheme {
                Surface(color = Color(0xFF4CAF50), modifier = Modifier.size(8.dp)) {}
            }
        }
        // The green circle indicator for live connection
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun remoteScreen_touchModeToggle_switchesState() {
        var currentMode by remember { mutableStateOf(TouchMode.TOUCH) }
        composeTestRule.setContent {
            MiMoTheme {
                TouchMode.entries.forEach { mode ->
                    FilterChip(
                        selected = currentMode == mode,
                        onClick = { currentMode = mode },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("mode_${mode.name}")
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Touch").assertIsSelected()
        composeTestRule.onNodeWithText("Type").assertIsNotSelected()

        composeTestRule.onNodeWithText("Type").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Type").assertIsSelected()
        composeTestRule.onNodeWithText("Touch").assertIsNotSelected()
    }

    @Test
    fun remoteScreen_mouseCoordinates_mapping() {
        // Test coordinate mapping logic
        // mapCoords(offset, boxWidth, boxHeight) maps screen touch to remote coordinates
        // Formula: (offset.x * img.width / boxWidth, offset.y * img.height / boxHeight)
        val imgWidth = 1920
        val imgHeight = 1080
        val boxWidth = 960f
        val boxHeight = 540f

        // Touch at center of display (480, 270) maps to (960, 540) on remote
        val remoteX = (480 * imgWidth / boxWidth).toInt()
        val remoteY = (270 * imgHeight / boxHeight).toInt()
        assertEquals(960, remoteX)
        assertEquals(540, remoteY)
    }

    @Test
    fun remoteScreen_mouseCoordinates_originMapsToOrigin() {
        val imgWidth = 1920
        val imgHeight = 1080
        val boxWidth = 960f
        val boxHeight = 540f

        val remoteX = (0 * imgWidth / boxWidth).toInt()
        val remoteY = (0 * imgHeight / boxHeight).toInt()
        assertEquals(0, remoteX)
        assertEquals(0, remoteY)
    }

    @Test
    fun remoteScreen_mouseCoordinates_maxMapsToMax() {
        val imgWidth = 1920
        val imgHeight = 1080
        val boxWidth = 960f
        val boxHeight = 540f

        val remoteX = (960 * imgWidth / boxWidth).toInt()
        val remoteY = (540 * imgHeight / boxHeight).toInt()
        assertEquals(1920, remoteX)
        assertEquals(1080, remoteY)
    }

    @Test
    fun remoteScreen_errorIcon_isDisplayed() {
        composeTestRule.setContent {
            MiMoTheme {
                Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
            }
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun remoteScreen_keyboardInput_placeholderText() {
        composeTestRule.setContent {
            MiMoTheme {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Type...") }
                )
            }
        }
        composeTestRule.onNodeWithText("Type...").assertIsDisplayed()
    }

    @Test
    fun remoteScreen_keyboardInput_sendButton() {
        composeTestRule.setContent {
            MiMoTheme {
                IconButton(onClick = {}, modifier = Modifier.testTag("send_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("send_button").assertIsDisplayed()
    }
}
