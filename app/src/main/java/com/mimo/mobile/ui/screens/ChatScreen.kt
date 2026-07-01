package com.mimo.mobile.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.network.ConnectionState
import com.mimo.mobile.ui.adaptive.rememberWindowWidthDp
import com.mimo.mobile.ui.components.MatplotlibBackground
import com.mimo.mobile.viewmodel.ChatInstance
import com.mimo.mobile.viewmodel.MiMoViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(vm: MiMoViewModel) {
    val state by vm.state.collectAsState()
    val activeId by vm.activeInstanceIdFlow.collectAsState()
    val instances = vm.instances
    val activeInstance = instances.find { it.id == activeId } ?: instances.firstOrNull()
    var inputText by remember { mutableStateOf("") }
    var showInstanceManager by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val widthDp = rememberWindowWidthDp()
    val isWide = widthDp >= 600

    val messageCount = activeInstance?.messages?.size ?: 0
    val lastMessageContent = activeInstance?.messages?.lastOrNull()?.content ?: ""
    val isProcessing = activeInstance?.isProcessing == true

    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            kotlinx.coroutines.delay(150)
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    LaunchedEffect(lastMessageContent.length) {
        if (isProcessing && messageCount > 0) {
            kotlinx.coroutines.delay(50)
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    val isReady = state.connectionState == ConnectionState.CONNECTED

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        MatplotlibBackground(
            modifier = Modifier.fillMaxSize(),
            isProcessing = isProcessing,
            messageCount = activeInstance?.messages?.size ?: 0
        )

        Column(modifier = Modifier.fillMaxSize()) {
            if (state.connectionState != ConnectionState.CONNECTED) {
                ConnectionBanner(state.connectionState)
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                instances.forEach { inst ->
                    val isActive = inst.id == activeId
                    FilterChip(
                        selected = isActive,
                        onClick = { vm.switchInstance(inst.id) },
                        label = {
                            Text(
                                inst.name,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = if (inst.isProcessing) {
                            { CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp) }
                        } else if (inst.id != "default") {
                            {
                                IconButton(onClick = { vm.removeInstance(inst.id) }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(12.dp))
                                }
                            }
                        } else null,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showInstanceManager = !showInstanceManager }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Add, "Add instance", modifier = Modifier.size(18.dp))
                }
            }
        }

        if (showInstanceManager) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Manage Instances", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    instances.forEach { inst ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(inst.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("${inst.messages.size} msgs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (inst.id != "default") {
                                IconButton(onClick = { vm.removeInstance(inst.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { vm.addInstance("Instance ${instances.size + 1}"); showInstanceManager = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Instance", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (activeInstance == null || activeInstance.messages.isEmpty()) {
                item { EmptyChatState() }
            }
            activeInstance?.messages?.forEach { msg ->
                item(key = msg.id) {
                    ChatBubble(msg, vm)
                }
            }
        }

        ChatInputBar(
            value = inputText,
            onValueChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank() && isReady) {
                    val instId = activeId
                    vm.sendChat(inputText.trim(), instId)
                    inputText = ""
                }
            },
            isProcessing = activeInstance?.isProcessing == true,
            enabled = isReady
        )
        }
    }
}

@Composable
fun ConnectionBanner(state: ConnectionState) {
    val (color, text, icon) = when (state) {
        ConnectionState.CONNECTING -> Triple(MaterialTheme.colorScheme.tertiary, "Connecting to server...", Icons.Filled.Sync)
        ConnectionState.ERROR -> Triple(MaterialTheme.colorScheme.error, "Connection failed. Check Settings.", Icons.Filled.ErrorOutline)
        ConnectionState.DISCONNECTED -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, "Offline. Go to Settings to connect.", Icons.Filled.CloudOff)
        else -> return
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, color = color, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EmptyChatState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "MiMo Mobile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your AI coding assistant on the go",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Connected to your PC, accessible from anywhere",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        QuickActionChips()
    }
}

@Composable
fun QuickActionChips() {
    val actions = listOf(
        "Build an app" to Icons.Filled.PhoneAndroid,
        "Debug code" to Icons.Filled.BugReport,
        "Write tests" to Icons.Filled.Science,
        "Explain code" to Icons.Filled.Lightbulb
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Try asking:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        actions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (action, icon) ->
                    AssistChip(
                        onClick = { },
                        label = { Text(action, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ChatBubble(msg: com.mimo.mobile.viewmodel.ChatMsg, vm: MiMoViewModel) {
    val isUser = msg.role == "user"
    val isError = msg.role == "error"
    val widthDp = rememberWindowWidthDp()
    val isWide = widthDp >= 600

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "M",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(modifier = Modifier.widthIn(max = if (isWide) 500.dp else 300.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = if (isUser) 14.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 14.dp,
                    bottomStart = 14.dp,
                    bottomEnd = 14.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    else if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                if (msg.isStreaming) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TypingIndicator()
                        Spacer(Modifier.width(8.dp))
                        Text("Thinking...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                if (msg.content.isNotBlank()) {
                    Text(
                        msg.content.trim(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                        else if (isError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            if (!isUser && !msg.isStreaming && msg.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    M3ActionChip("Copy", Icons.Filled.ContentCopy) { }
                    M3ActionChip("Explain", Icons.Filled.Lightbulb) { }
                    M3ActionChip("Run", Icons.Filled.PlayArrow) {
                        val code = msg.content.trim()
                        if (code.isNotBlank()) {
                            vm.sendExecute(code)
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("U", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}

@Composable
fun M3ActionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline) },
        icon = { Icon(icon, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline) }
    )
}

@Composable
fun TypingIndicator() {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "dot_$index")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    tween(500, delayMillis = index * 120), RepeatMode.Reverse
                ), label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean,
    enabled: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        if (enabled) "Ask MiMo..." else "Connecting...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 4,
                enabled = enabled
            )

            FilledIconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isProcessing && enabled,
                modifier = Modifier.size(36.dp),
                shape = CircleShape
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 1.5.dp
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(16.dp))
                }
            }
        }
        }
    }
}
