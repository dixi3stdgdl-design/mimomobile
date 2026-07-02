package com.mimo.mobile.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.viewmodel.MiMoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.roundToInt

enum class TouchMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TOUCH("Touch", Icons.Filled.TouchApp),
    KEYBOARD("Type", Icons.Filled.Keyboard)
}

@Composable
fun RemoteScreen(vm: MiMoViewModel) {
    var screenBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var isStreaming by remember { mutableStateOf(false) }
    var fps by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var streamId by remember { mutableStateOf<String?>(null) }
    var touchMode by remember { mutableStateOf(TouchMode.TOUCH) }
    var showControls by remember { mutableStateOf(false) }
    var showKeyboard by remember { mutableStateOf(false) }
    var keyboardType by remember { mutableStateOf("") }
    var cursorPos by remember { mutableStateOf(Offset(0f, 0f)) }
    var showCursor by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(Unit) {
        delay(500)
        isStreaming = true
        val id = vm.nextId()
        streamId = id
        vm.sendMessage(com.mimo.mobile.network.WsMessage(
            type = "continuous_stream", id = id,
            data = JSONObject().apply { put("action", "start") }.toString()
        ))
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isStreaming && streamId != null) {
                scope.launch {
                    vm.sendMessage(com.mimo.mobile.network.WsMessage(
                        type = "continuous_stream", id = vm.nextId(),
                        data = JSONObject().apply { put("action", "stop") }.toString()
                    ))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        var frameCount = 0
        var lastFpsTime = System.currentTimeMillis()
        vm.messages.collect { msg ->
            when (msg.type) {
                "screen_frame" -> {
                    if (msg.data != null) {
                        try {
                            val b64 = msg.data.toString()
                            val bytes = Base64.decode(b64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                screenBitmap = bitmap.asImageBitmap()
                                frameCount++
                                val now = System.currentTimeMillis()
                                if (now - lastFpsTime >= 1000) {
                                    fps = frameCount * 1000f / (now - lastFpsTime)
                                    frameCount = 0
                                    lastFpsTime = now
                                }
                            }
                        } catch (e: Exception) {
                            error = "Decode: ${e.message}"
                        }
                    }
                }
                "stream_status" -> {
                    if (msg.data?.toString() == "stopped") isStreaming = false
                }
                "mouse_ack", "keyboard_ack" -> {}
            }
        }
    }

    fun sendMouse(action: String, x: Int, y: Int, button: String = "left", delta: Int = 0) {
        scope.launch {
            vm.sendMessage(com.mimo.mobile.network.WsMessage(
                type = "mouse_event", id = vm.nextId(),
                data = JSONObject().apply {
                    put("action", action)
                    put("x", x)
                    put("y", y)
                    put("button", button)
                    if (delta != 0) put("delta", delta)
                }.toString()
            ))
        }
    }

    fun sendKey(action: String, key: String) {
        scope.launch {
            vm.sendMessage(com.mimo.mobile.network.WsMessage(
                type = "keyboard_event", id = vm.nextId(),
                data = JSONObject().apply {
                    put("action", action)
                    put("key", key)
                }.toString()
            ))
        }
    }

    fun mapCoords(offset: IntOffset, boxWidth: Float, boxHeight: Float): Pair<Int, Int> {
        val img = screenBitmap ?: return Pair(0, 0)
        val sx = img.width.toFloat() / boxWidth
        val sy = img.height.toFloat() / boxHeight
        return Pair((offset.x * sx).toInt(), (offset.y * sy).toInt())
    }

    // Fullscreen container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        if (screenBitmap != null) {
            // Screen stream - fills entire area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(touchMode) {
                        when (touchMode) {
                            TouchMode.TOUCH -> {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val (mx, my) = mapCoords(
                                            IntOffset(offset.x.roundToInt(), offset.y.roundToInt()),
                                            size.width.toFloat(), size.height.toFloat()
                                        )
                                        sendMouse("click", mx, my, "left")
                                        showCursor = true
                                        cursorPos = Offset(offset.x, offset.y)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val (mx, my) = mapCoords(
                                            IntOffset(change.position.x.roundToInt(), change.position.y.roundToInt()),
                                            size.width.toFloat(), size.height.toFloat()
                                        )
                                        sendMouse("move", mx, my)
                                        cursorPos = Offset(change.position.x, change.position.y)
                                    },
                                    onDragEnd = { showCursor = false }
                                )
                            }
                            TouchMode.KEYBOARD -> {
                                detectTapGestures(
                                    onTap = {
                                        showControls = !showControls
                                    }
                                )
                            }
                        }
                    }
            ) {
                Image(
                    bitmap = screenBitmap!!,
                    contentDescription = "PC Screen",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Cursor indicator
                if (showCursor && touchMode == TouchMode.TOUCH) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(cursorPos.x.roundToInt() - 8, cursorPos.y.roundToInt() - 8) }
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    )
                }
            }
        } else {
            // Loading state
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (error != null) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.height(8.dp))
                    Text("Conectando...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }

        // Floating controls - appear on tap, auto-hide
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.8f),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // FPS indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFF4CAF50), modifier = Modifier.size(8.dp)) {}
                            Spacer(Modifier.width(6.dp))
                            Text("${fps.roundToInt()} FPS", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                        Text("Remote", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    }

                    Spacer(Modifier.height(8.dp))

                    // Mode selector
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TouchMode.entries.forEach { mode ->
                            FilterChip(
                                selected = touchMode == mode,
                                onClick = { touchMode = mode },
                                label = { Text(mode.label, fontSize = 10.sp) },
                                leadingIcon = { Icon(mode.icon, null, modifier = Modifier.size(12.dp)) },
                                modifier = Modifier.weight(1f).height(36.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    // Keyboard shortcuts (when in keyboard mode)
                    if (touchMode == TouchMode.KEYBOARD) {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Tab" to "{TAB}", "Enter" to "{ENTER}", "Esc" to "{ESC}", "Bksp" to "{BACKSPACE}").forEach { (label, key) ->
                                AssistChip(
                                    onClick = { sendKey("hotkey", key) },
                                    label = { Text(label, fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f).height(32.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Ctrl+C" to "^(c)", "Ctrl+V" to "^(v)", "Ctrl+Z" to "^(z)", "Ctrl+A" to "^(a)").forEach { (label, key) ->
                                AssistChip(
                                    onClick = { sendKey("hotkey", key) },
                                    label = { Text(label, fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f).height(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Keyboard input (when typing)
        AnimatedVisibility(
            visible = showKeyboard && touchMode == TouchMode.KEYBOARD,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(color = Color.Black.copy(alpha = 0.9f), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = keyboardType,
                        onValueChange = { keyboardType = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type...", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = {
                        if (keyboardType.isNotBlank()) {
                            sendKey("type", keyboardType)
                            keyboardType = ""
                        }
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Send, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    }
                    IconButton(onClick = { showKeyboard = false }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    }
                }
            }
        }

        // Tap hint (fades out)
        if (screenBitmap != null && !showControls) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Text(
                    "Toca para controles",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
