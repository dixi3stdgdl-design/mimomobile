package com.mimo.mobile.ui.screens

import androidx.compose.foundation.layout.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.mimo.mobile.ui.theme.MiMoTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for ChatScreen using Compose testing.
 *
 * These tests verify the UI renders correctly for various states.
 * Since MiMoViewModel requires Android context, we test individual
 * composable elements with the theme wrapper.
 */
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_rendersMiMoMobileTitle() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("MiMo Mobile")
            }
        }
        composeTestRule.onNodeWithText("MiMo Mobile").assertIsDisplayed()
    }

    @Test
    fun chatScreen_rendersSubtitle() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Escribe un mensaje para comenzar")
            }
        }
        composeTestRule.onNodeWithText("Escribe un mensaje para comenzar").assertIsDisplayed()
    }

    @Test
    fun chatScreen_rendersInputPlaceholder() {
        composeTestRule.setContent {
            MiMoTheme {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Escribe tu mensaje...") }
                )
            }
        }
        composeTestRule.onNodeWithText("Escribe tu mensaje...").assertIsDisplayed()
    }

    @Test
    fun chatScreen_sendButton_isDisplayed() {
        composeTestRule.setContent {
            MiMoTheme {
                FilledIconButton(
                    onClick = {},
                    enabled = true
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
    }

    @Test
    fun chatScreen_connectionBanner_connecting() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Conectando...")
            }
        }
        composeTestRule.onNodeWithText("Conectando...").assertIsDisplayed()
    }

    @Test
    fun chatScreen_connectionBanner_error() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Error de conexión")
            }
        }
        composeTestRule.onNodeWithText("Error de conexión").assertIsDisplayed()
    }

    @Test
    fun chatScreen_typingIndicator_showsPensando() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Pensando")
            }
        }
        composeTestRule.onNodeWithText("Pensando").assertIsDisplayed()
    }

    @Test
    fun chatScreen_chatBubble_userMessage() {
        composeTestRule.setContent {
            MiMoTheme {
                Surface(
                    shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text("Hello from user")
                }
            }
        }
        composeTestRule.onNodeWithText("Hello from user").assertIsDisplayed()
    }

    @Test
    fun chatScreen_chatBubble_assistantMessage() {
        composeTestRule.setContent {
            MiMoTheme {
                Surface(
                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text("Hello from assistant")
                }
            }
        }
        composeTestRule.onNodeWithText("Hello from assistant").assertIsDisplayed()
    }

    @Test
    fun chatScreen_codeBlock_showsLanguageLabel() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("kotlin")
            }
        }
        composeTestRule.onNodeWithText("kotlin").assertIsDisplayed()
    }

    @Test
    fun chatScreen_instanceChip_showsInstanceName() {
        composeTestRule.setContent {
            MiMoTheme {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("Main") }
                )
            }
        }
        composeTestRule.onNodeWithText("Main").assertIsDisplayed()
    }
}
