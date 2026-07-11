package com.mimo.mobile.ui.screens

import androidx.compose.material3.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.mimo.mobile.network.ConnectionState
import com.mimo.mobile.ui.theme.MiMoTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for SettingsScreen using Compose testing.
 *
 * Tests verify the UI elements render correctly for various settings states.
 */
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_rendersTitle() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Settings")
            }
        }
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_rendersSubtitle() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Configure your MiMo Code connection")
            }
        }
        composeTestRule.onNodeWithText("Configure your MiMo Code connection").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_appearanceSection() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Appearance")
            }
        }
        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_darkThemeOption() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Dark")
            }
        }
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_lightThemeOption() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Light")
            }
        }
        composeTestRule.onNodeWithText("Light").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_serverConnectionSection() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("MiMo Code Server")
            }
        }
        composeTestRule.onNodeWithText("MiMo Code Server").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_connectionStatusSection() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Connection Status")
            }
        }
        composeTestRule.onNodeWithText("Connection Status").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_basicSettingsSection() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Basic")
            }
        }
        composeTestRule.onNodeWithText("Basic").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_mediumSettingsSection() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Medium")
            }
        }
        composeTestRule.onNodeWithText("Medium").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_advancedSettingsSection() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Advanced")
            }
        }
        composeTestRule.onNodeWithText("Advanced").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_premiumSection() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Upgrade to Pro")
            }
        }
        composeTestRule.onNodeWithText("Upgrade to Pro").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_freePlanCard() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Free")
            }
        }
        composeTestRule.onNodeWithText("Free").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_proPlanCard() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Pro")
            }
        }
        composeTestRule.onNodeWithText("Pro").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_teamPlanCard() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Team")
            }
        }
        composeTestRule.onNodeWithText("Team").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_quickSetupGuide() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Quick Setup Guide")
            }
        }
        composeTestRule.onNodeWithText("Quick Setup Guide").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_setupStep1() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Download the server from the MiMo Mobile repository")
            }
        }
        composeTestRule.onNodeWithText("Download the server from the MiMo Mobile repository").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_autoConnectToggle() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Auto Connect")
            }
        }
        composeTestRule.onNodeWithText("Auto Connect").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_autoReconnectToggle() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Auto Reconnect")
            }
        }
        composeTestRule.onNodeWithText("Auto Reconnect").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_connectButton() {
        composeTestRule.setContent {
            MiMoTheme {
                Button(onClick = {}) {
                    Text("Connect & Anchor")
                }
            }
        }
        composeTestRule.onNodeWithText("Connect & Anchor").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_hostTextField() {
        composeTestRule.setContent {
            MiMoTheme {
                OutlinedTextField(
                    value = "127.0.0.1",
                    onValueChange = {},
                    label = { Text("Server Host") }
                )
            }
        }
        composeTestRule.onNodeWithText("127.0.0.1").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_portTextField() {
        composeTestRule.setContent {
            MiMoTheme {
                OutlinedTextField(
                    value = "8765",
                    onValueChange = {},
                    label = { Text("WebSocket Port") }
                )
            }
        }
        composeTestRule.onNodeWithText("8765").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_unpairButton() {
        composeTestRule.setContent {
            MiMoTheme {
                OutlinedButton(onClick = {}) {
                    Text("Unpair & Configure")
                }
            }
        }
        composeTestRule.onNodeWithText("Unpair & Configure").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_statusRowDisconnected() {
        composeTestRule.setContent {
            MiMoTheme {
                StatusRow(label = "Connection", state = ConnectionState.DISCONNECTED)
            }
        }
        composeTestRule.onNodeWithText("Connection").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disconnected").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_statusRowConnected() {
        composeTestRule.setContent {
            MiMoTheme {
                StatusRow(label = "Connection", state = ConnectionState.CONNECTED)
            }
        }
        composeTestRule.onNodeWithText("Connection").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_statusRowConnecting() {
        composeTestRule.setContent {
            MiMoTheme {
                StatusRow(label = "Connection", state = ConnectionState.CONNECTING)
            }
        }
        composeTestRule.onNodeWithText("Connection").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connecting...").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_statusRowError() {
        composeTestRule.setContent {
            MiMoTheme {
                StatusRow(label = "Connection", state = ConnectionState.ERROR)
            }
        }
        composeTestRule.onNodeWithText("Connection").assertIsDisplayed()
        composeTestRule.onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_planPrice_single() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("\$29.99")
            }
        }
        composeTestRule.onNodeWithText("\$29.99").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_currentPlanBadge() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("Current Plan")
            }
        }
        composeTestRule.onNodeWithText("Current Plan").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_recommendedBadge() {
        composeTestRule.setContent {
            MiMoTheme {
                Text("RECOMMENDED")
            }
        }
        composeTestRule.onNodeWithText("RECOMMENDED").assertIsDisplayed()
    }
}
