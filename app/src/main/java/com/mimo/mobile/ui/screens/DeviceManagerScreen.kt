package com.mimo.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.viewmodel.MiMoViewModel
import kotlinx.coroutines.launch

data class DeviceInfo(
    val serial: String,
    val state: String,
    val model: String,
    var expanded: Boolean = false
)

@Composable
fun DeviceManagerScreen(vm: MiMoViewModel) {
    var devices by remember { mutableStateOf(listOf<DeviceInfo>()) }
    var isLoading by remember { mutableStateOf(false) }
    var lastCommand by remember { mutableStateOf<String?>(null) }
    var commandOutput by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        vm.sendDeviceList()
        while (true) {
            kotlinx.coroutines.delay(10000)
            vm.sendDeviceList()
        }
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            when (msg.type) {
                "device_list" -> {
                    try {
                        val arr = org.json.JSONArray(msg.data.toString())
                        val list = mutableListOf<DeviceInfo>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(DeviceInfo(
                                serial = obj.getString("serial"),
                                state = obj.getString("state"),
                                model = obj.optString("model", "Unknown")
                            ))
                        }
                        devices = list
                        isLoading = false
                    } catch (_: Exception) {}
                }
                "device_output" -> {
                    val output = msg.data?.toString() ?: ""
                    commandOutput = output
                    lastCommand = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Devices", style = MaterialTheme.typography.headlineSmall)
                Text("${devices.size} connected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text("ON", fontSize = 9.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isLoading) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Detecting devices...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Icon(Icons.Filled.PhoneAndroid, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No devices found", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Connect devices via USB or WiFi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices) { device ->
                    DeviceCard(
                        device = device,
                        onRefresh = { scope.launch { vm.sendDeviceList() } },
                        onConfigure = { scope.launch { vm.sendAdbConfigure(device.serial) } },
                        onCommand = { command, action ->
                            lastCommand = command
                            commandOutput = null
                            scope.launch { vm.sendDeviceCommand(device.serial, command, action) }
                        },
                        onInput = { type, value ->
                            scope.launch { vm.sendDeviceInput(device.serial, type, value) }
                        },
                        onInstall = { apkPath ->
                            lastCommand = "Installing..."
                            scope.launch { vm.sendDeviceInstall(device.serial, apkPath) }
                        }
                    )
                }
            }
        }

        if (commandOutput != null) {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("OUTPUT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(commandOutput!!, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: DeviceInfo,
    onRefresh: () -> Unit,
    onConfigure: () -> Unit,
    onCommand: (String, String) -> Unit,
    onInput: (String, String) -> Unit,
    onInstall: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isOnline = device.state == "device"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOnline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (device.model.contains("Tab", true)) Icons.Filled.Tablet
                            else Icons.Filled.PhoneAndroid,
                            null,
                            tint = if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.model.ifEmpty { device.serial }, style = MaterialTheme.typography.titleSmall)
                    Text(device.serial, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isOnline) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                ) {
                    Text(
                        if (isOnline) "Online" else "Offline",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("QUICK ACTIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickAction("Screenshot", Icons.Filled.CameraAlt) {
                        onCommand("screencap -p /sdcard/mimo_ss.png && pull /sdcard/mimo_ss.png /tmp/mimo_ss.png", "shell")
                    }
                    QuickAction("Wake", Icons.Filled.LightMode) { onInput("keyevent", "KEYCODE_WAKEUP") }
                    QuickAction("Home", Icons.Filled.Home) { onInput("keyevent", "KEYCODE_HOME") }
                    QuickAction("Back", Icons.Filled.ArrowBack) { onInput("keyevent", "KEYCODE_BACK") }
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickAction("Settings", Icons.Filled.Settings) {
                        onCommand("am start -a android.settings.SETTINGS", "shell")
                    }
                    QuickAction("WiFi On", Icons.Filled.Wifi) { onCommand("svc wifi enable", "shell") }
                    QuickAction("Max Bright", Icons.Filled.BrightnessHigh) {
                        onCommand("settings put system screen_brightness 255", "shell")
                    }
                    QuickAction("Config ADB", Icons.Filled.CastConnected) { onConfigure() }
                }

                Spacer(Modifier.height(8.dp))
                Text("CUSTOM COMMAND", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))

                var customCmd by remember { mutableStateOf("") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customCmd,
                        onValueChange = { customCmd = it },
                        placeholder = { Text("adb shell command...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = {
                        if (customCmd.isNotBlank()) {
                            onCommand(customCmd, "shell")
                            customCmd = ""
                        }
                    }) {
                        Icon(Icons.Filled.Send, null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(14.dp)) }
    )
}
