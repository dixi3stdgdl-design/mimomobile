package com.mimo.mobile.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* 
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mimo.mobile.network.ConnectionState
import com.mimo.mobile.viewmodel.MiMoViewModel

@Composable
fun SettingsScreen(
    host: String, port: String,
    onHostChange: (String) -> Unit, onPortChange: (String) -> Unit,
    onReconnect: () -> Unit, connectionState: ConnectionState,
    vm: MiMoViewModel
) {
    val state by vm.state.collectAsState()
    var expandedSection by remember { mutableStateOf<String?>("basic") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Configure your MiMo Code connection", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        // Theme Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dark Theme Option
                    Surface(
                        onClick = { vm.updateToggle(MiMoViewModel.DARK_MODE_KEY, true) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (state.darkMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface,
                        border = if (state.darkMode) ButtonDefaults.outlinedButtonBorder(enabled = true) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1A1A2E),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.DarkMode,
                                        null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Dark", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Light Theme Option
                    Surface(
                        onClick = { vm.updateToggle(MiMoViewModel.DARK_MODE_KEY, false) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (!state.darkMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface,
                        border = if (!state.darkMode) ButtonDefaults.outlinedButtonBorder(enabled = true) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF5F5F5),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.LightMode,
                                        null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Light", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Server Connection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Router, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("MiMo Code Server", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = host, onValueChange = onHostChange,
                    label = { Text("Server Host") },
                    placeholder = { Text("127.0.0.1") },
                    leadingIcon = { Icon(Icons.Filled.Router, null) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = port, onValueChange = onPortChange,
                    label = { Text("WebSocket Port") },
                    placeholder = { Text("8765") },
                    leadingIcon = { Icon(Icons.Filled.Lan, null) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onReconnect,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.Link, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Connect", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Connection Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Connection Status", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                StatusRow("Connection", connectionState)
                StatusRow("Host", host.ifEmpty { "Not set" })
                StatusRow("Port", port)
                StatusRow("Protocol", "WebSocket")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Basic Settings
        SettingsSection(
            title = "Basic",
            icon = Icons.Filled.Settings,
            expanded = expandedSection == "basic",
            onToggle = { expandedSection = if (expandedSection == "basic") null else "basic" }
        ) {
            ToggleSetting("Auto Connect", "Connect automatically on startup", state.autoConnect) {
                vm.updateToggle(MiMoViewModel.AUTO_CONNECT_KEY, it)
            }
            ToggleSetting("Auto Reconnect", "Reconnect on connection loss", state.autoReconnect) {
                vm.updateToggle(MiMoViewModel.AUTO_RECONNECT_KEY, it)
            }
            ToggleSetting("Low Latency Mode", "Optimize for minimum input delay", state.lowLatencyMode) {
                vm.updateToggle(MiMoViewModel.LOW_LATENCY_KEY, it)
            }
            ToggleSetting("Keep Screen On", "Prevent screen timeout while connected", state.keepScreenOn) {
                vm.updateToggle(MiMoViewModel.KEEP_SCREEN_ON_KEY, it)
            }
            ToggleSetting("Screen Streaming", "Stream PC screen to device", state.screenStreaming) {
                vm.updateToggle(MiMoViewModel.SCREEN_STREAMING_KEY, it)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Medium Settings
        SettingsSection(
            title = "Medium",
            icon = Icons.Filled.Tune,
            expanded = expandedSection == "medium",
            onToggle = { expandedSection = if (expandedSection == "medium") null else "medium" }
        ) {
            ToggleSetting("Auto Scroll", "Auto-scroll chat to latest message", state.autoScroll) {
                vm.updateToggle(MiMoViewModel.AUTO_SCROLL_KEY, it)
            }
            ToggleSetting("Code Highlighting", "Syntax highlight code blocks", state.codeHighlighting) {
                vm.updateToggle(MiMoViewModel.CODE_HIGHLIGHTING_KEY, it)
            }
            ToggleSetting("Haptic Feedback", "Vibrate on actions", state.hapticFeedback) {
                vm.updateToggle(MiMoViewModel.HAPTIC_FEEDBACK_KEY, it)
            }
            ToggleSetting("Dark Mode", "Use dark theme", state.darkMode) {
                vm.updateToggle(MiMoViewModel.DARK_MODE_KEY, it)
            }
            ToggleSetting("Compact UI", "Denser layout for larger screens", state.compactUI) {
                vm.updateToggle(MiMoViewModel.COMPACT_UI_KEY, it)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Advanced Settings
        SettingsSection(
            title = "Advanced",
            icon = Icons.Filled.Code,
            expanded = expandedSection == "advanced",
            onToggle = { expandedSection = if (expandedSection == "advanced") null else "advanced" }
        ) {
            ToggleSetting("Notifications", "Show push notifications", state.notificationsEnabled) {
                vm.updateToggle(MiMoViewModel.NOTIFICATIONS_KEY, it)
            }
            ToggleSetting("Sound Effects", "Play sounds on interactions", state.soundEnabled) {
                vm.updateToggle(MiMoViewModel.SOUND_KEY, it)
            }
            ToggleSetting("Vibration", "System vibration feedback", state.vibrationEnabled) {
                vm.updateToggle(MiMoViewModel.VIBRATION_KEY, it)
            }
            ToggleSetting("Line Numbers", "Show line numbers in code view", state.showLineNumbers) {
                vm.updateToggle(MiMoViewModel.LINE_NUMBERS_KEY, it)
            }
            ToggleSetting("Word Wrap", "Wrap long lines in code", state.wordWrap) {
                vm.updateToggle(MiMoViewModel.WORD_WRAP_KEY, it)
            }
            ToggleSetting("Enable Cache", "Cache responses for faster loading", state.enableCache) {
                vm.updateToggle(MiMoViewModel.CACHE_KEY, it)
            }
            ToggleSetting("Experimental Features", "Enable unstable features", state.experimentalFeatures) {
                vm.updateToggle(MiMoViewModel.EXPERIMENTAL_KEY, it)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Premium Plans
        PremiumSection()

        Spacer(Modifier.height(16.dp))

        // Setup Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.HelpOutline, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Quick Setup Guide", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(12.dp))

                SetupStep("1", "Download the server from the MiMo Mobile repository")
                SetupStep("2", "Run: python3 install.sh (one-click setup)")
                SetupStep("3", "Run: python3 server.py")
                SetupStep("4", "Open this app — it will auto-discover your server!")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ToggleSetting(
    title: String,
    description: String,
    value: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(
            checked = value,
            onCheckedChange = onToggle
        )
    }
}

@Composable
fun SetupStep(number: String, text: String, command: String? = null) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = if (command != null) Alignment.Top else Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            command?.let {
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        it, modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun StatusRow(label: String, state: ConnectionState) {
    val (value, color) = when (state) {
        ConnectionState.CONNECTED -> "Connected" to MaterialTheme.colorScheme.primary
        ConnectionState.CONNECTING -> "Connecting..." to MaterialTheme.colorScheme.tertiary
        ConnectionState.ERROR -> "Error" to MaterialTheme.colorScheme.error
        ConnectionState.DISCONNECTED -> "Disconnected" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = color.copy(alpha = 0.12f)
        ) {
            Text(
                value, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PremiumSection() {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Upgrade to Pro", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Unlock all features and support development",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // Free Plan
            PlanCard(
                name = "Free",
                price = "$0",
                period = "forever",
                features = listOf("1 device", "Basic chat", "Community support"),
                isCurrentPlan = true,
                onClick = {}
            )

            Spacer(Modifier.height(12.dp))

            // Pro Plan
            PlanCard(
                name = "Pro",
                price = "$9.99",
                period = "/month",
                features = listOf("5 devices", "Remote desktop", "File manager", "Priority support"),
                isRecommended = true,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://link.mercadopago.com.mx/mimomobile"))
                    context.startActivity(intent)
                }
            )

            Spacer(Modifier.height(12.dp))

            // Team Plan
            PlanCard(
                name = "Team",
                price = "$29.99",
                period = "/month",
                features = listOf("Unlimited devices", "All Pro features", "Dedicated support", "Early access"),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://link.mercadopago.com.mx/mimomobile"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun PlanCard(
    name: String,
    price: String,
    period: String,
    features: List<String>,
    isCurrentPlan: Boolean = false,
    isRecommended: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isRecommended) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (isRecommended) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "RECOMMENDED",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Row {
                    Text(price, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(period, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Check,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(feature, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!isCurrentPlan) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecommended) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Get $name", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "Current Plan",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
