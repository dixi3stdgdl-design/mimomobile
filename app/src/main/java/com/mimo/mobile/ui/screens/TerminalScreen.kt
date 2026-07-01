package com.mimo.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.viewmodel.MiMoViewModel

@Composable
fun TerminalScreen(vm: MiMoViewModel) {
    val state by vm.state.collectAsState()
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf(listOf<TerminalLine>()) }
    var isRunning by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            when (msg.type) {
                "exec_output" -> {
                    val text = msg.data?.toString() ?: ""
                    if (text.isNotBlank()) {
                        output = output + TerminalLine(text, false)
                    }
                }
                "exec_end" -> {
                    isRunning = false
                    val exitCode = msg.exitCode
                    val error = msg.error
                    if (error != null) {
                        output = output + TerminalLine("Error: $error", true)
                    } else if (exitCode != null && exitCode != 0) {
                        output = output + TerminalLine("Exit code: $exitCode", true)
                    }
                    output = output + TerminalLine("", false)
                }
            }
        }
    }

    LaunchedEffect(output.size) {
        if (output.isNotEmpty()) {
            kotlinx.coroutines.delay(50)
            listState.animateScrollToItem(output.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Terminal, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Terminal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    if (isRunning) {
                        Text("Running...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    } else {
                        Text("Ready", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                if (output.isNotEmpty()) {
                    IconButton(onClick = { output = emptyList() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.DeleteSweep, "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (state.connectionState != com.mimo.mobile.network.ConnectionState.CONNECTED) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CloudOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Not connected to server", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (output.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Terminal, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Terminal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Type commands to execute on your PC",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
            items(output) { line ->
                Text(
                    text = line.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = if (line.isError) MaterialTheme.colorScheme.error
                    else Color(0xFF22C55E)
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$ ",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("command...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (input.isNotBlank() && !isRunning) {
                            output = output + TerminalLine("$ ${input.trim()}", false)
                            vm.sendExecute(input.trim())
                            isRunning = true
                            input = ""
                        }
                    }),
                    singleLine = true,
                    enabled = state.connectionState == com.mimo.mobile.network.ConnectionState.CONNECTED
                )
                IconButton(
                    onClick = {
                        if (input.isNotBlank() && !isRunning) {
                            output = output + TerminalLine("$ ${input.trim()}", false)
                            vm.sendExecute(input.trim())
                            isRunning = true
                            input = ""
                        }
                    },
                    enabled = input.isNotBlank() && !isRunning && state.connectionState == com.mimo.mobile.network.ConnectionState.CONNECTED,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 1.5.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, "Execute", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

data class TerminalLine(val text: String, val isError: Boolean)
