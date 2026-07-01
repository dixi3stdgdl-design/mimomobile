# MiMo Mobile v4 — Neural Command Center + Remote Desktop + Auto-Restart

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform MiMo Mobile into a Neural Command Center with true remote desktop control, server auto-restart, and a living organic UI.

**Architecture:** Three independent subsystems: (1) Server watchdog for auto-restart, (2) True RDP with continuous screen streaming + touch-to-mouse + keyboard forwarding, (3) Complete UI redesign with Neural Command Center aesthetic (organic curves, neural network visualizations, bioluminescent accents).

**Tech Stack:** Kotlin + Jetpack Compose + Material3, Python stdlib WebSocket server, PowerShell screen capture, Canvas drawing

---

## Task 1: Server Auto-Restart Watchdog

**Covers:** Server reliability

**Files:**
- Create: `/home/DexTer/mimo-mobile-server/watchdog.sh`
- Create: `/home/DexTer/mimo-mobile-server/start-server.sh`

- [ ] **Step 1: Create watchdog script**

```bash
#!/bin/bash
# MiMo Server Watchdog - auto-restart on crash
SERVER_DIR="/home/DexTer/mimo-mobile-server"
LOG="/tmp/mimo-server.log"

while true; do
    echo "[$(date)] Starting MiMo Server..." >> "$LOG"
    cd "$SERVER_DIR"
    python3 -u server.py >> "$LOG" 2>&1
    EXIT_CODE=$?
    echo "[$(date)] Server exited with code $EXIT_CODE. Restarting in 3s..." >> "$LOG"
    sleep 3
done
```

- [ ] **Step 2: Create start script**

```bash
#!/bin/bash
# Kill any existing server
pkill -f "python3.*server.py" 2>/dev/null
sleep 1
# Start watchdog in background
nohup /home/DexTer/mimo-mobile-server/watchdog.sh > /dev/null 2>&1 &
echo "Watchdog started. Server will auto-restart on crash."
```

- [ ] **Step 3: Make executable and test**

```bash
chmod +x /home/DexTer/mimo-mobile-server/watchdog.sh
chmod +x /home/DexTer/mimo-mobile-server/start-server.sh
/home/DexTer/mimo-mobile-server/start-server.sh
```

- [ ] **Step 4: Verify server is running**

```bash
sleep 3 && ss -tlnp | grep -E "8765|8080"
```
Expected: Both ports listening

- [ ] **Step 5: Test auto-restart by killing server**

```bash
pkill -f "python3.*server.py" && sleep 5 && ss -tlnp | grep 8765
```
Expected: Server restarted automatically, port 8765 listening

---

## Task 2: True Remote Desktop — Continuous Screen Streaming

**Covers:** Remote Desktop continuous streaming

**Files:**
- Modify: `/home/DexTer/mimo-mobile-server/server.py` — Add `start_continuous_stream` handler
- Modify: `/home/DexTer/MiMoMobile/app/src/main/java/com/mimo/mobile/ui/screens/RemoteScreen.kt` — Add continuous streaming + touch gestures

- [ ] **Step 1: Add continuous streaming to server.py**

Add after `handle_screen_stream` function:

```python
async def handle_continuous_stream(msg, writer):
    """Continuous screen streaming at ~10 FPS"""
    action = msg.get("action", "start")
    msg_id = msg.get("id")
    if action == "start":
        import threading
        streaming = {"active": True, "writer": writer}
        
        def stream_loop():
            while streaming["active"]:
                try:
                    result = subprocess.run(
                        ["powershell.exe", "-Command",
                         "Add-Type -AssemblyName System.Windows.Forms; "
                         "$bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds; "
                         "$bmp = New-Object System.Drawing.Bitmap($bounds.Width, $bounds.Height); "
                         "$gfx = [System.Drawing.Graphics]::FromImage($bmp); "
                         "$gfx.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size); "
                         "$ms = New-Object System.IO.MemoryStream; "
                         "$bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Jpeg); "
                         "[Convert]::ToBase64String($ms.ToArray())"],
                        capture_output=True, text=True, timeout=10
                    )
                    if result.returncode == 0 and result.stdout.strip():
                        b64 = result.stdout.strip()
                        asyncio.run_coroutine_threadsafe(
                            send_json(streaming["writer"], {
                                "type": "screen_frame",
                                "id": msg_id,
                                "data": b64,
                                "format": "jpeg"
                            }),
                            asyncio.get_event_loop()
                        )
                except Exception as e:
                    print(f"Stream error: {e}", flush=True)
                time.sleep(0.1)  # ~10 FPS
        
        streaming["thread"] = threading.Thread(target=stream_loop, daemon=True)
        streaming["thread"].start()
        active_streams[msg_id] = streaming
        await send_json(writer, {"type": "stream_status", "id": msg_id, "data": "started"})
    elif action == "stop":
        if msg_id in active_streams:
            active_streams[msg_id]["active"] = False
            del active_streams[msg_id]
        await send_json(writer, {"type": "stream_status", "id": msg_id, "data": "stopped"})

active_streams = {}
```

- [ ] **Step 2: Add continuous stream handler to message router**

In `handle_message`, add:
```python
    elif msg_type == "continuous_stream":
        await handle_continuous_stream(msg, writer)
```

- [ ] **Step 3: Add drag gesture support to server mouse_event**

In `handle_mouse_event`, the existing `move` action already works. Just verify.

- [ ] **Step 4: Update RemoteScreen.kt for continuous streaming + gestures**

Replace the entire RemoteScreen.kt with the new version that includes:
- Continuous streaming toggle (start/stop)
- Drag gesture (touch drag = mouse move)
- Long press = right click
- Pinch = scroll
- Full-screen mode

```kotlin
@Composable
fun RemoteScreen(vm: MiMoViewModel) {
    var screenBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var isStreaming by remember { mutableStateOf(false) }
    var lastFrameTime by remember { mutableLongStateOf(0L) }
    var fps by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var screenRes by remember { mutableStateOf("...") }
    val scope = rememberCoroutineScope()

    // Collect screen frames
    LaunchedEffect(Unit) {
        var frameCount = 0
        var lastFpsTime = System.currentTimeMillis()
        vm.messages.collect { msg ->
            if (msg.type == "screen_frame" && msg.data != null) {
                try {
                    val b64 = msg.data.toString()
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        screenBitmap = bitmap.asImageBitmap()
                        lastFrameTime = System.currentTimeMillis()
                        screenRes = "${bitmap.width}x${bitmap.height}"
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
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Header with status
        // Full-screen remote view with gestures
        // Gesture handling: drag, long press, pinch
        // Control buttons: Stream, Capture, Keyboard shortcuts
    }
}
```

- [ ] **Step 5: Add drag gesture handling**

In the RemoteScreen Canvas/Image area, add:

```kotlin
var lastTouchPos by remember { mutableStateOf(Offset.Zero) }
var isDragging by remember { mutableStateOf(false) }

Modifier.pointerInput(Unit) {
    detectDragGestures(
        onDragStart = { offset ->
            isDragging = true
            lastTouchPos = offset
            // Send initial click
            scope.launch {
                val img = screenBitmap ?: return@launch
                val scaleX = img.width.toFloat() / size.width
                val scaleY = img.height.toFloat() / size.height
                vm.sendMessage(WsMessage(
                    type = "mouse_event", id = vm.nextId(),
                    data = JSONObject().apply {
                        put("action", "click")
                        put("x", (offset.x * scaleX).toInt())
                        put("y", (offset.y * scaleY).toInt())
                        put("button", "left")
                    }.toString()
                ))
            }
        },
        onDrag = { change, _ ->
            change.consume()
            scope.launch {
                val img = screenBitmap ?: return@launch
                val scaleX = img.width.toFloat() / size.width
                val scaleY = img.height.toFloat() / size.height
                vm.sendMessage(WsMessage(
                    type = "mouse_event", id = vm.nextId(),
                    data = JSONObject().apply {
                        put("action", "move")
                        put("x", (change.position.x * scaleX).toInt())
                        put("y", (change.position.y * scaleY).toInt())
                    }.toString()
                ))
            }
        },
        onDragEnd = { isDragging = false }
    )
}
```

- [ ] **Step 6: Add long press for right click**

```kotlin
Modifier.pointerInput(Unit) {
    detectTapGestures(
        onLongPress = { offset ->
            scope.launch {
                val img = screenBitmap ?: return@launch
                val scaleX = img.width.toFloat() / size.width
                val scaleY = img.height.toFloat() / size.height
                vm.sendMessage(WsMessage(
                    type = "mouse_event", id = vm.nextId(),
                    data = JSONObject().apply {
                        put("action", "click")
                        put("x", (offset.x * scaleX).toInt())
                        put("y", (offset.y * scaleY).toInt())
                        put("button", "right")
                    }.toString()
                ))
            }
        },
        onTap = { offset ->
            scope.launch {
                val img = screenBitmap ?: return@launch
                val scaleX = img.width.toFloat() / size.width
                val scaleY = img.height.toFloat() / size.height
                vm.sendMessage(WsMessage(
                    type = "mouse_event", id = vm.nextId(),
                    data = JSONObject().apply {
                        put("action", "click")
                        put("x", (offset.x * scaleX).toInt())
                        put("y", (offset.y * scaleY).toInt())
                        put("button", "left")
                    }.toString()
                ))
            }
        }
    )
}
```

- [ ] **Step 7: Build and test**

```bash
ANDROID_HOME=/home/DexTer/Android/Sdk JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug --no-daemon
```
Expected: BUILD SUCCESSFUL

---

## Task 3: Neural Command Center — Theme & Colors

**Covers:** Design overhaul — foundation

**Files:**
- Modify: `/home/DexTer/MiMoMobile/app/src/main/java/com/mimo/mobile/ui/theme/Theme.kt`

- [ ] **Step 1: Replace theme colors with Neural Command Center palette**

```kotlin
package com.mimo.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Neural Command Center Palette
val DarkBackground = Color(0xFF05050A)      // Deep void
val DarkSurface = Color(0xFF0A0B12)         // Neural surface
val DarkCard = Color(0xFF0E1018)            // Organic card
val DarkCardHover = Color(0xFF141620)       // Hover state

// Bioluminescent accents
val AccentOrange = Color(0xFF00E5A0)        // Neural green (primary)
val AccentGreen = Color(0xFF00D4FF)         // Synapse blue
val AccentBlue = Color(0xFF7B61FF)          // Cortex purple
val AccentRed = Color(0xFFFF3366)           // Alert pulse
val AccentPurple = Color(0xFFFF6B9D)        // Organic pink
val AccentCyan = Color(0xFF00FFC8)          // Bioluminescent cyan
val AccentGold = Color(0xFFFFD93D)          // Neural gold

// Text
val TextPrimary = Color(0xFFE8F0F8)         // Neural white
val TextSecondary = Color(0xFF8B9CC0)       // Synapse gray
val TextMuted = Color(0xFF4A5568)           // Dendrite gray
val BorderColor = Color(0xFF1A2040)         // Neural border

// Glow colors
val GlowGreen = Color(0x3000E5A0)
val GlowBlue = Color(0x3000D4FF)
val GlowPurple = Color(0x307B61FF)
```

- [ ] **Step 2: Build to verify colors compile**

```bash
ANDROID_HOME=/home/DexTer/Android/Sdk JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug --no-daemon 2>&1 | grep -E "^e:|BUILD"
```
Expected: BUILD SUCCESSFUL

---

## Task 4: Neural Command Center — Core Components

**Covers:** Design overhaul — components

**Files:**
- Create: `/home/DexTer/MiMoMobile/app/src/main/java/com/mimo/mobile/ui/components/NeuralComponents.kt`

- [ ] **Step 1: Create NeuralComponents.kt with organic composables**

```kotlin
package com.mimo.mobile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.ui.theme.*
import kotlin.math.*

@Composable
fun NeuralPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neural_panel")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "breathe"
    )
    
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = AccentOrange.copy(alpha = 0.1f)),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(AccentOrange.copy(alpha = breathe), AccentBlue.copy(alpha = breathe * 0.5f))
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun NeuralButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AccentOrange,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neural_btn")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow"
    )
    
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.shadow(12.dp, RoundedCornerShape(14.dp), ambientColor = color.copy(alpha = glow * 0.3f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = if (enabled) 0.9f else 0.3f),
            disabledContainerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(14.dp),
        content = content
    )
}

@Composable
fun NeuralOrb(
    size: Int = 80,
    color: Color = AccentOrange,
    label: String = "M"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "pulse"
    )
    val rotate by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "rotate"
    )
    
    Box(modifier = Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = size.width / 2 - 4f
            
            // Outer ring
            drawCircle(color = color.copy(alpha = 0.15f * pulse), radius = r, style = Stroke(2f))
            
            // Orbiting particles
            for (i in 0..2) {
                val angle = rotate + i * 120f
                val px = cx + cos(Math.toRadians(angle.toDouble())).toFloat() * r * 0.7f
                val py = cy + sin(Math.toRadians(angle.toDouble())).toFloat() * r * 0.7f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.5f * pulse), Color.Transparent),
                        center = Offset(px, py), radius = 16f
                    ),
                    radius = 8f, center = Offset(px, py)
                )
                drawCircle(color = color.copy(alpha = 0.8f * pulse), radius = 2.5f, center = Offset(px, py))
            }
            
            // Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.25f * pulse), Color.Transparent),
                    center = Offset(cx, cy), radius = 20f
                ),
                radius = 20f, center = Offset(cx, cy)
            )
            drawCircle(color = color.copy(alpha = 0.7f), radius = 5f, center = Offset(cx, cy))
        }
        
        Text(label, color = color, fontWeight = FontWeight.Black, fontSize = (size / 3).sp)
    }
}

@Composable
fun NeuralStatusDot(color: Color, active: Boolean = true) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (active) 800 else 2000), RepeatMode.Reverse), label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .shadow(if (active) 6.dp else 0.dp, CircleShape, ambientColor = color.copy(alpha = alpha * 0.5f))
            .clip(CircleShape)
            .background(color.copy(alpha = if (active) alpha else 0.2f))
    )
}

@Composable
fun NeuralDivider(color: Color = AccentOrange) {
    val infiniteTransition = rememberInfiniteTransition(label = "divider")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "shimmer"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, color.copy(alpha = shimmer), Color.Transparent)
                )
            )
    )
}
```

- [ ] **Step 2: Build to verify components compile**

```bash
ANDROID_HOME=/home/DexTer/Android/Sdk JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug --no-daemon 2>&1 | grep -E "^e:|BUILD"
```
Expected: BUILD SUCCESSFUL

---

## Task 5: Neural Command Center — Redesign MainActivity

**Covers:** Design overhaul — navigation shell

**Files:**
- Modify: `/home/DexTer/MiMoMobile/app/src/main/java/com/mimo/mobile/MainActivity.kt`

- [ ] **Step 1: Update top bar with neural orb + organic nav**

Replace the top bar section with:

```kotlin
topBar = {
    Surface(color = DarkSurface, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuralOrb(size = 36, color = AccentOrange, label = "M")
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("MiMo", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AccentOrange)
                    Text(" Mobile", fontWeight = FontWeight.Light, fontSize = 18.sp, color = TextPrimary)
                }
                ConnectionBadge(state.connectionState)
            }
            if (state.connectionState == ConnectionState.CONNECTED) {
                NeuralStatusDot(AccentGreen, active = true)
            }
        }
    }
}
```

- [ ] **Step 2: Update bottom nav with organic shapes**

Replace bottom nav with:

```kotlin
bottomBar = {
    Surface(color = DarkSurface, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(Screen.Chat, Screen.Build, Screen.Remote, Screen.Terminal, Screen.Files, Screen.Settings).forEach { screen ->
                val selected = currentScreen == screen
                Surface(
                    onClick = { currentScreen = screen },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) AccentOrange.copy(alpha = 0.12f) else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (selected) {
                            NeuralStatusDot(AccentOrange, active = true)
                            Spacer(Modifier.height(2.dp))
                        }
                        Icon(
                            if (selected) screen.selectedIcon else screen.icon,
                            contentDescription = screen.label,
                            modifier = Modifier.size(if (selected) 22.dp else 20.dp),
                            tint = if (selected) AccentOrange else TextMuted
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            screen.label,
                            fontSize = 9.sp,
                            color = if (selected) AccentOrange else TextMuted,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Update ConnectionBadge to use neural styling**

```kotlin
@Composable
fun ConnectionBadge(state: ConnectionState) {
    val (color, text) = when (state) {
        ConnectionState.CONNECTED -> AccentGreen to "ONLINE"
        ConnectionState.CONNECTING -> AccentOrange to "CONNECTING"
        ConnectionState.ERROR -> AccentRed to "ERROR"
        ConnectionState.DISCONNECTED -> TextMuted to "OFFLINE"
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (state == ConnectionState.CONNECTING) 600 else 2000),
            RepeatMode.Reverse
        ), label = "pulse"
    )
    
    Row(
        modifier = Modifier.padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeuralStatusDot(color, active = state == ConnectionState.CONNECTED)
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            fontSize = 9.sp,
            color = color.copy(alpha = pulseAlpha),
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}
```

- [ ] **Step 4: Build to verify**

```bash
ANDROID_HOME=/home/DexTer/Android/Sdk JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug --no-daemon 2>&1 | grep -E "^e:|BUILD"
```
Expected: BUILD SUCCESSFUL

---

## Task 6: Neural Command Center — Redesign ChatScreen

**Covers:** Design overhaul — chat

**Files:**
- Modify: `/home/DexTer/MiMoMobile/app/src/main/java/com/mimo/mobile/ui/screens/ChatScreen.kt`

- [ ] **Step 1: Update ChatBubble with neural styling**

Replace ChatBubble with organic shapes and neural glow effects.

- [ ] **Step 2: Update EmptyChatState with neural orb animation**

Use NeuralOrb component instead of the current floating circle.

- [ ] **Step 3: Update ChatInputBar with neural styling**

Organic pill shape, bioluminescent cursor, neural glow on focus.

- [ ] **Step 4: Build to verify**

```bash
ANDROID_HOME=/home/DexTer/Android/Sdk JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug --no-daemon 2>&1 | grep -E "^e:|BUILD"
```
Expected: BUILD SUCCESSFUL

---

## Task 7: Neural Command Center — Redesign All Remaining Screens

**Covers:** Design overhaul — all screens

**Files:**
- Modify: `/home/DexTer/MiMoMobile/app/src/main/java/com/mimo/mobile/ui/screens/BuildVisualizerScreen.kt`
- Modify: `/home/DexTer/MiMoMobile/app/src/main/java/com/mimo/mobile/ui/screens/TerminalScreen.kt`
- Modify: `/home/DexTer/MiMoMobile/app/src/main/java/com/mimo/mobile/ui/screens/RemoteScreen.kt`
- Modify: `/home/DexTer/MiMoMobile/app/src/main/java/com/mimo/mobile/ui/screens/FileBrowserScreen.kt`
- Modify: `/home/DexTer/MiMoMobile/app/src/main/java/com/mimo/mobile/ui/screens/SettingsScreen.kt`

- [ ] **Step 1: Update all screens to use NeuralPanel, NeuralButton, NeuralDivider**

Replace all Card, Button, and Surface components with neural equivalents.

- [ ] **Step 2: Update all Canvas drawings to use neural colors**

Replace AccentOrange/AccentBlue with the new bioluminescent palette.

- [ ] **Step 3: Build and verify all screens compile**

```bash
ANDROID_HOME=/home/DexTer/Android/Sdk JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug --no-daemon 2>&1 | grep -E "^e:|BUILD"
```
Expected: BUILD SUCCESSFUL

---

## Task 8: Final Build + Install on Tab S9 + Wireless Setup

**Covers:** Deployment

**Files:** None (deployment only)

- [ ] **Step 1: Final build**

```bash
ANDROID_HOME=/home/DexTer/Android/Sdk JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug --no-daemon
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Install on Tab S9**

```bash
/home/DexTer/Android/Sdk/platform-tools/adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/mimo.apk
/home/DexTer/Android/Sdk/platform-tools/adb shell pm install -r /data/local/tmp/mimo.apk
```

- [ ] **Step 3: Launch and verify**

```bash
/home/DexTer/Android/Sdk/platform-tools/adb shell am start -n com.mimo.mobile.debug/com.mimo.mobile.MainActivity
```

- [ ] **Step 4: Take screenshot for verification**

```bash
/home/DexTer/Android/Sdk/platform-tools/adb shell screencap -p /sdcard/mimo_v4.png
/home/DexTer/Android/Sdk/platform-tools/adb pull /sdcard/mimo_v4.png /tmp/mimo_v4.png
```
