# MiMo Mobile v2 - App Builder Environment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform MiMoMobile into a visual app development environment with construction-style background, visual app builder, unified file management, and immersive remote desktop.

**Architecture:** Each screen becomes a specialized workspace. Background provides constant "construction from nothing" motion. Build section shows wireframe→UI visual construction instead of code. Files unifies phone+PC with full operations. Remote fills entire tab with tap-to-show controls.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Canvas animations, WebSocket

---

## Global Constraints

- Target device: OnePlus 8 (Android 13+)
- Min SDK: 24, Target SDK: 34
- All animations must be `infiniteRepeatable` with NO pauses for background
- WebSocket messages: existing protocol, no server changes needed
- Build and install command: `JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug && /home/DexTer/Android/Sdk/platform-tools/adb -s 192.168.100.166:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk`

---

### Task 1: Construction-Style Background Animation

**Covers:** S6 (Background must feel like construction from nothing)

**Files:**
- Modify: `app/src/main/java/com/mimo/mobile/ui/components/PerspectiveGridBackground.kt`

**Goal:** Rewrite background with construction theme - building blocks falling, construction lines, progress particles. Everything in CONSTANT motion.

- [ ] **Step 1: Rewrite PerspectiveGridBackground with continuous construction animation**

Replace entire file content with enhanced version:

```kotlin
package com.mimo.mobile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.*
import kotlin.random.Random

private data class ConstructionBlock(
    var x: Float, var y: Float,
    val targetY: Float,
    var w: Float, var h: Float,
    val alpha: Float, val speed: Float,
    val phase: Float, var placed: Boolean = false
)

private data class ConstructionLine(
    var progress: Float,
    val y: Float, val speed: Float,
    val alpha: Float, val width: Float
)

private data class ProgressParticle(
    var x: Float, var y: Float,
    var speed: Float, var alpha: Float,
    var length: Float, var width: Float
)

@Composable
fun PerspectiveGridBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "grid")

    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "scroll"
    )

    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "shimmer"
    )

    val blockPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "blockPulse"
    )

    val blocks = remember {
        List(24) {
            ConstructionBlock(
                x = Random.nextFloat(),
                y = -0.1f,
                targetY = Random.nextFloat() * 0.85f + 0.05f,
                w = Random.nextFloat() * 0.06f + 0.02f,
                h = Random.nextFloat() * 0.03f + 0.015f,
                alpha = Random.nextFloat() * 0.5f + 0.3f,
                speed = Random.nextFloat() * 0.15f + 0.05f,
                phase = Random.nextFloat() * 2f * PI.toFloat()
            )
        }
    }

    val constructionLines = remember {
        List(12) {
            ConstructionLine(
                progress = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.004f + 0.002f,
                alpha = Random.nextFloat() * 0.3f + 0.1f,
                width = Random.nextFloat() * 1.5f + 0.5f
            )
        }
    }

    val particles = remember {
        Array(60) {
            ProgressParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.012f + 0.004f,
                alpha = Random.nextFloat() * 0.8f + 0.2f,
                length = Random.nextFloat() * 0.08f + 0.02f,
                width = Random.nextFloat() * 1.8f + 0.8f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(Color(0xFF0B0E17))

        val vpX = w * 0.5f
        val vpY = h * 0.28f

        val gridColor = Color(0xFF1A2744)
        val gridColorBright = Color(0xFF243556)

        // Horizontal grid lines - faster scrolling
        for (i in 1..20) {
            val t = (i.toFloat() / 20f + scrollOffset) % 1f
            val y = vpY + (h - vpY) * t * t
            val spread = 0.1f + t * 0.9f
            val left = vpX - w * 0.8f * spread
            val right = vpX + w * 0.8f * spread
            val alpha = (0.2f + t * 0.35f) * shimmer * 0.6f
            drawLine(gridColorBright.copy(alpha = alpha), Offset(left, y), Offset(right, y), strokeWidth = 1.2f)
        }

        // Vertical grid lines
        for (i in -8..8) {
            val spread = i.toFloat() / 8f
            val bottomX = vpX + w * 0.7f * spread
            val alpha = (0.25f + (1f - abs(spread)) * 0.25f) * shimmer * 0.6f
            drawLine(gridColor.copy(alpha = alpha), Offset(vpX, vpY), Offset(bottomX, h), strokeWidth = 1f)
        }

        // Construction lines drawing left to right
        constructionLines.forEach { line ->
            line.progress += line.speed
            if (line.progress > 1.2f) {
                line.progress = -0.2f
                line.y = Random.nextFloat()
                line.alpha = Random.nextFloat() * 0.3f + 0.1f
            }

            val ly = vpY + (h - vpY) * line.y
            val lineEnd = w * line.progress.coerceIn(0f, 1f)
            val lineStart = (w * (line.progress - 0.3f)).coerceAtLeast(0f)

            if (lineEnd > lineStart) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00D4FF).copy(alpha = 0f),
                            Color(0xFF00D4FF).copy(alpha = line.alpha * shimmer),
                            Color(0xFF00D4FF).copy(alpha = line.alpha * shimmer),
                            Color(0xFF00D4FF).copy(alpha = 0f)
                        ),
                        startX = lineStart,
                        endX = lineEnd
                    ),
                    start = Offset(lineStart, ly),
                    end = Offset(lineEnd, ly),
                    strokeWidth = line.width
                )
            }
        }

        // Falling construction blocks
        blocks.forEach { block ->
            if (!block.placed) {
                block.y += block.speed
                if (block.y >= block.targetY) {
                    block.y = block.targetY
                    block.placed = true
                }
            } else {
                // Subtle shift when placed
                block.x += sin(scrollOffset * PI.toFloat() * 2f + block.phase) * 0.0003f
                block.x = block.x.coerceIn(0.05f, 0.95f)
            }

            val bx = w * block.x
            val depth = block.y
            val by = vpY + (h - vpY) * depth * depth
            val perspectiveScale = 0.1f + depth * 0.9f
            val bw = w * block.w * perspectiveScale
            val bh = h * block.h * perspectiveScale
            val pulseAlpha = block.alpha * (0.6f + 0.4f * sin(scrollOffset * 2f * PI.toFloat() + block.phase)) * blockPulse

            // Glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E4D8C).copy(alpha = pulseAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(bx, by),
                    radius = bw * 2.5f
                ),
                topLeft = Offset(bx - bw, by - bh),
                size = Size(bw * 2, bh * 2)
            )

            // Block rectangle
            drawRect(
                color = Color(0xFF1E4D8C).copy(alpha = pulseAlpha),
                topLeft = Offset(bx, by),
                size = Size(bw, bh)
            )

            // Top highlight
            drawLine(
                color = Color(0xFF2A5FA0).copy(alpha = (pulseAlpha * 1.3f).coerceAtMost(1f)),
                start = Offset(bx, by),
                end = Offset(bx + bw, by),
                strokeWidth = 1.5f
            )
        }

        // Falling light particles - faster and more
        particles.forEach { p ->
            p.y += p.speed
            if (p.y > 1.1f) {
                p.y = -0.1f
                p.x = Random.nextFloat()
                p.alpha = Random.nextFloat() * 0.8f + 0.2f
            }

            val px = w * p.x
            val startY = h * p.y
            val endY = startY + h * p.length
            val fadeIn = (p.y / 0.15f).coerceIn(0f, 1f)
            val fadeOut = ((1f - p.y) / 0.15f).coerceIn(0f, 1f)
            val finalAlpha = p.alpha * fadeIn * fadeOut

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4FC3F7).copy(alpha = 0f),
                        Color(0xFF4FC3F7).copy(alpha = finalAlpha),
                        Color(0xFF81D4FA).copy(alpha = finalAlpha * 0.6f),
                        Color(0xFF4FC3F7).copy(alpha = 0f)
                    ),
                    startY = startY,
                    endY = endY
                ),
                start = Offset(px, startY),
                end = Offset(px, endY),
                strokeWidth = p.width
            )
        }

        // Horizontal scan line - continuous sweep
        val scanY = h * scrollOffset
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF00D4FF).copy(alpha = 0.15f),
                    Color(0xFF00D4FF).copy(alpha = 0.15f),
                    Color.Transparent
                )
            ),
            start = Offset(0f, scanY),
            end = Offset(w, scanY),
            strokeWidth = 1.5f
        )

        // Vignette edges
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B0E17).copy(alpha = 0.8f),
                    Color.Transparent,
                    Color.Transparent,
                    Color(0xFF0B0E17).copy(alpha = 0.6f)
                ),
                startY = 0f,
                endY = h
            )
        )
    }
}
```

- [ ] **Step 2: Build and install**

```bash
cd /home/DexTer/MiMoMobile
JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug
/home/DexTer/Android/Sdk/platform-tools/adb -s 192.168.100.166:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Verify on OnePlus 8**

Take screenshot. Verify:
- Blocks falling from top and placing on grid
- Construction lines drawing left-to-right
- Particles flowing downward continuously
- Scan line sweeping horizontally
- ALL elements in constant motion - never static

---

### Task 2: Build Section - Visual App Constructor

**Covers:** S3 (Build shows visual construction, not code)

**Files:**
- Modify: `app/src/main/java/com/mimo/mobile/ui/screens/BuildVisualizerScreen.kt`

**Goal:** Build section becomes visual app constructor. User types prompt, sees wireframes appearing, then transforming into Material3 UI components.

- [ ] **Step 1: Rewrite BuildVisualizerScreen with input widget and visual construction**

Replace entire file content:

```kotlin
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.viewmodel.MiMoViewModel

private val EditorBg = Color(0xFF0D1117)
private val WireframeColor = Color(0xFF30363D)
private val WireframeAccent = Color(0xFF58A6FF)
private val UICardBg = Color(0xFF161B22)
private val UIBlue = Color(0xFF58A6FF)
private val UIGreen = Color(0xFF7EE787)
private val UIPurple = Color(0xFFD2A8FF)
private val UIOrange = Color(0xFFFFA657)

data class VisualComponent(
    val type: String,
    val phase: String = "wireframe",
    val delayMs: Long = 0
)

@Composable
fun BuildVisualizerScreen(vm: MiMoViewModel) {
    var inputText by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("Mi Proyecto") }
    var isBuilding by remember { mutableStateOf(false) }
    var components by remember { mutableStateOf(listOf<VisualComponent>()) }
    var currentPhase by remember { mutableStateOf("idle") }
    val listState = rememberLazyListState()
    val state by vm.state.collectAsState()
    val isReady = state.connectionState == com.mimo.mobile.network.ConnectionState.CONNECTED

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            when (msg.type) {
                "chat_start" -> {
                    isBuilding = true
                    currentPhase = "wireframe"
                    components = emptyList()
                }
                "build_progress" -> {
                    try {
                        val json = org.json.JSONObject(msg.data.toString())
                        projectName = json.optString("project", "Mi Proyecto")
                        val phase = json.optString("phase", "wireframe")
                        currentPhase = phase
                        val comps = json.optJSONArray("components")
                        if (comps != null) {
                            val newComponents = mutableListOf<VisualComponent>()
                            for (i in 0 until comps.length()) {
                                val comp = comps.getJSONObject(i)
                                newComponents.add(
                                    VisualComponent(
                                        type = comp.optString("type", "card"),
                                        phase = phase,
                                        delayMs = i * 200L
                                    )
                                )
                            }
                            components = newComponents
                        }
                    } catch (_: Exception) {}
                }
                "chat_end" -> {
                    isBuilding = false
                    currentPhase = "complete"
                }
            }
        }
    }

    LaunchedEffect(components.size) {
        if (components.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            listState.animateScrollToItem(components.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorBg)
    ) {
        // Header
        Surface(
            color = Color(0xFF161B22),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Build,
                    null,
                    tint = UIPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "App Constructor",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC9D1D9)
                    )
                    Text(
                        projectName,
                        style = MaterialTheme.typography.labelSmall,
                        color = WireframeAccent,
                        fontSize = 10.sp
                    )
                }
                if (isBuilding) {
                    Surface(
                        shape = CircleShape,
                        color = UIGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(UIGreen)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Construyendo", style = MaterialTheme.typography.labelSmall, color = UIGreen)
                        }
                    }
                } else if (currentPhase == "complete") {
                    Surface(
                        shape = CircleShape,
                        color = UIGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(12.dp), tint = UIGreen)
                            Spacer(Modifier.width(4.dp))
                            Text("Completado", style = MaterialTheme.typography.labelSmall, color = UIGreen)
                        }
                    }
                }
            }
        }

        // Visual Construction Canvas
        if (components.isEmpty() && !isBuilding) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = UIPurple.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Constructor de Apps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC9D1D9)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Describe tu app y observa como se construye",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF484F58)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ej: \"Construye una app de venta de tecnologia\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = WireframeAccent.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(components.size) { index ->
                    val comp = components[index]
                    val delay = comp.delayMs
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = delay.toInt())) +
                                slideInVertically(
                                    animationSpec = tween(300, delayMillis = delay.toInt()),
                                    initialOffsetY = { it / 4 }
                                )
                    ) {
                        when (comp.type) {
                            "appbar" -> AppBarPreview(comp.phase)
                            "card" -> CardPreview(comp.phase)
                            "button" -> ButtonPreview(comp.phase)
                            "list" -> ListPreview(comp.phase)
                            "form" -> FormPreview(comp.phase)
                            "nav" -> NavPreview(comp.phase)
                            else -> CardPreview(comp.phase)
                        }
                    }
                }
            }
        }

        // Input Widget
        Surface(
            color = Color(0xFF161B22)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Describe tu app...", fontSize = 14.sp) },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WireframeAccent,
                        unfocusedBorderColor = WireframeColor,
                        focusedContainerColor = Color(0xFF0D1117),
                        unfocusedContainerColor = Color(0xFF0D1117)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank() && isReady) {
                            vm.sendChat(inputText.trim())
                            inputText = ""
                        }
                    })
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank() && isReady) {
                            vm.sendChat(inputText.trim())
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && isReady,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = WireframeAccent
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(20.dp))
                }
            }
        }

        // Status bar
        Surface(color = Color(0xFF161B22)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${components.size} componentes",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color(0xFF484F58)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    currentPhase.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (currentPhase == "complete") UIGreen else WireframeAccent
                )
            }
        }
    }
}

@Composable
private fun AppBarPreview(phase: String) {
    val bgColor = if (phase == "wireframe") Color.Transparent else MaterialTheme.colorScheme.primaryContainer
    val borderColor = if (phase == "wireframe") WireframeColor else Color.Transparent

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        border = if (phase == "wireframe") {
            androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (phase == "wireframe") {
                Box(modifier = Modifier.size(24.dp).background(WireframeColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.height(12.dp).width(80.dp).background(WireframeColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
            } else {
                Icon(Icons.Filled.Menu, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(12.dp))
                Text("TechStore", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun CardPreview(phase: String) {
    val bgColor = if (phase == "wireframe") Color.Transparent else UICardBg
    val borderColor = if (phase == "wireframe") WireframeColor else Color.Transparent

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth(),
        border = if (phase == "wireframe") {
            androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        } else null,
        shadowElevation = if (phase == "wireframe") 0.dp else 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (phase == "wireframe") {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(WireframeColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)))
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.height(12.dp).width(120.dp).background(WireframeColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.height(10.dp).width(200.dp).background(WireframeColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
                Spacer(Modifier.height(8.dp))
                Text("iPhone 15 Pro", fontWeight = FontWeight.Medium, color = Color(0xFFC9D1D9))
                Spacer(Modifier.height(4.dp))
                Text("$999", color = UIGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ButtonPreview(phase: String) {
    val bgColor = if (phase == "wireframe") Color.Transparent else UIBlue
    val borderColor = if (phase == "wireframe") WireframeColor else Color.Transparent

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        border = if (phase == "wireframe") {
            androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        } else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (phase == "wireframe") {
                Box(modifier = Modifier.height(10.dp).width(80.dp).background(WireframeColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
            } else {
                Text("Agregar al Carrito", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ListPreview(phase: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) {
            val bgColor = if (phase == "wireframe") Color.Transparent else UICardBg
            val borderColor = if (phase == "wireframe") WireframeColor else Color.Transparent

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = bgColor,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border = if (phase == "wireframe") {
                    androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                } else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (phase == "wireframe") {
                        Box(modifier = Modifier.size(32.dp).background(WireframeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(12.dp))
                        Box(modifier = Modifier.height(10.dp).width(100.dp).background(WireframeColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                    } else {
                        Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(12.dp))
                        Text("Item ${it + 1}", color = Color(0xFFC9D1D9))
                    }
                }
            }
        }
    }
}

@Composable
private fun FormPreview(phase: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(2) { index ->
            val borderColor = if (phase == "wireframe") WireframeColor else MaterialTheme.colorScheme.outline

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (phase == "wireframe") {
                        Box(modifier = Modifier.height(8.dp).width(60.dp).background(WireframeColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                        Spacer(Modifier.height(6.dp))
                        Box(modifier = Modifier.height(10.dp).fillMaxWidth().background(WireframeColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                    } else {
                        Text(
                            if (index == 0) "Email" else "Password",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(modifier = Modifier.height(10.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
    }
}

@Composable
private fun NavPreview(phase: String) {
    val bgColor = if (phase == "wireframe") Color.Transparent else MaterialTheme.colorScheme.surface

    Surface(
        color = bgColor,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        border = if (phase == "wireframe") {
            androidx.compose.foundation.BorderStroke(1.dp, WireframeColor)
        } else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) {
                if (phase == "wireframe") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(20.dp).background(WireframeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                        Spacer(Modifier.height(4.dp))
                        Box(modifier = Modifier.height(6.dp).width(30.dp).background(WireframeColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            when (it) {
                                0 -> Icons.Filled.Home
                                1 -> Icons.Filled.Search
                                2 -> Icons.Filled.FavoriteBorder
                                else -> Icons.Filled.Person
                            },
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = if (it == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build and install**

```bash
cd /home/DexTer/MiMoMobile
JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug
/home/DexTer/Android/Sdk/platform-tools/adb -s 192.168.100.166:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Verify on OnePlus 8**

Take screenshot. Verify:
- Empty state shows "Constructor de Apps" with example prompt
- Text input at bottom with send button
- When prompt sent, wireframe components appear first
- Components transform to UI with colors
- Header shows project name and status
- Status bar shows component count and phase

---

### Task 3: Files Section - Unified Dual-Device Explorer

**Covers:** S4 (Files with share, copy, delete operations for phone + PC)

**Files:**
- Modify: `app/src/main/java/com/mimo/mobile/ui/screens/FileBrowserScreen.kt`

**Goal:** Full file manager with operations: share, copy, move, delete, rename, create folder. Shows both phone and PC files unified.

- [ ] **Step 1: Rewrite FileBrowserScreen with unified dual-device view and operations**

Replace entire file content:

```kotlin
package com.mimo.mobile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.viewmodel.MiMoViewModel
import java.io.File

data class FileEntry(
    val name: String,
    val isDir: Boolean,
    val size: Long = 0,
    val device: String = "phone"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileBrowserScreen(vm: MiMoViewModel) {
    var currentPath by remember { mutableStateOf(".") }
    var currentDevice by remember { mutableStateOf("phone") }
    var entries by remember { mutableStateOf(listOf<FileEntry>()) }
    var selectedFile by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editingFile by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editContent by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<FileEntry?>(null) }
    var showRenameDialog by remember { mutableStateOf<FileEntry?>(null) }
    var renameName by remember { mutableStateOf("") }
    var showMoveDialog by remember { mutableStateOf<FileEntry?>(null) }
    var moveTargetPath by remember { mutableStateOf(".") }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<String>()) }
    val pathHistory = remember { mutableStateListOf<String>() }
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            when (msg.type) {
                "dir_listing" -> {
                    val rawEntries = msg.entries ?: emptyList()
                    entries = rawEntries.map {
                        FileEntry(
                            name = it["name"] as? String ?: "",
                            isDir = it["is_dir"] as? Boolean ?: false,
                            size = (it["size"] as? Number)?.toLong() ?: 0L,
                            device = it["device"] as? String ?: currentDevice
                        )
                    }
                    currentPath = msg.path ?: "."
                }
                "file_content" -> {
                    val path = msg.path ?: ""
                    val content = msg.data?.toString() ?: ""
                    if (editingFile != null && editingFile?.first == path) {
                        editContent = content
                    } else {
                        selectedFile = Pair(path, content)
                    }
                }
                "file_written" -> {
                    editingFile = null
                    vm.sendListDir(currentPath)
                }
                "file_copied", "file_moved", "file_renamed" -> {
                    vm.sendListDir(currentPath)
                }
            }
        }
    }

    LaunchedEffect(state.connectionState) {
        if (state.connectionState == com.mimo.mobile.network.ConnectionState.CONNECTED) {
            vm.sendListDir(".")
        }
    }

    // Dialogs
    when {
        showDeleteDialog != null -> {
            val entry = showDeleteDialog!!
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Eliminar ${if (entry.isDir) "Carpeta" else "Archivo"}") },
                text = { Text("Seguro que quieres eliminar '${entry.name}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val fullPath = if (currentPath == ".") entry.name else "$currentPath/${entry.name}"
                            vm.sendDeleteFile(fullPath)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancelar") }
                }
            )
        }
        showRenameDialog != null -> {
            val entry = showRenameDialog!!
            AlertDialog(
                onDismissRequest = { showRenameDialog = null; renameName = "" },
                title = { Text("Renombrar") },
                text = {
                    OutlinedTextField(
                        value = renameName,
                        onValueChange = { renameName = it },
                        label = { Text("Nuevo nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (renameName.isNotBlank()) {
                            val oldPath = if (currentPath == ".") entry.name else "$currentPath/${entry.name}"
                            val newPath = if (currentPath == ".") renameName else "$currentPath/$renameName"
                            vm.sendMessage(com.mimo.mobile.network.WsMessage(
                                type = "rename_file",
                                id = vm.nextId(),
                                data = org.json.JSONObject().apply {
                                    put("old_path", oldPath)
                                    put("new_path", newPath)
                                }.toString()
                            ))
                            showRenameDialog = null
                            renameName = ""
                        }
                    }) { Text("Renombrar") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = null; renameName = "" }) { Text("Cancelar") }
                }
            )
        }
        showCreateDialog -> {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false; newFileName = "" },
                title = { Text("Crear Archivo") },
                text = {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Nombre del archivo") },
                        placeholder = { Text("ejemplo.txt") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newFileName.isNotBlank()) {
                            val fullPath = if (currentPath == ".") newFileName else "$currentPath/$newFileName"
                            vm.sendWriteFile(fullPath, "")
                            showCreateDialog = false
                            newFileName = ""
                        }
                    }) { Text("Crear") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false; newFileName = "" }) { Text("Cancelar") }
                }
            )
        }
        editingFile != null -> {
            FileEditor(
                path = editingFile!!.first, content = editContent,
                onContentChange = { editContent = it },
                onSave = { vm.sendWriteFile(editingFile!!.first, editContent) },
                onClose = { editingFile = null }
            )
        }
        selectedFile != null -> {
            FileViewer(
                path = selectedFile!!.first, content = selectedFile!!.second,
                onClose = { selectedFile = null },
                onEdit = { editingFile = selectedFile; editContent = selectedFile!!.second; selectedFile = null },
                onShare = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, selectedFile!!.second)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir archivo"))
                },
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(File(selectedFile!!.first).name, selectedFile!!.second)
                    clipboard.setPrimaryClip(clip)
                }
            )
        }
        else -> {
            FileListView(
                path = currentPath,
                entries = entries,
                pathHistory = pathHistory,
                currentDevice = currentDevice,
                isSelectionMode = isSelectionMode,
                selectedItems = selectedItems,
                onNavigate = { entry ->
                    val newPath = if (currentPath == ".") entry.name else "$currentPath/${entry.name}"
                    if (entry.isDir) {
                        pathHistory.add(currentPath)
                        currentDevice = entry.device
                        vm.sendListDir(newPath)
                    } else {
                        vm.sendReadFile(newPath)
                    }
                },
                onBack = {
                    if (pathHistory.isNotEmpty()) {
                        vm.sendListDir(pathHistory.removeLast())
                    }
                },
                onRefresh = { vm.sendListDir(currentPath) },
                onCreateFile = { showCreateDialog = true },
                onDeleteFile = { showDeleteDialog = it },
                onRenameFile = { showRenameDialog = it; renameName = it.name },
                onShareFile = { entry ->
                    val fullPath = if (currentPath == ".") entry.name else "$currentPath/${entry.name}"
                    vm.sendReadFile(fullPath)
                },
                onCopyFile = { entry ->
                    val fullPath = if (currentPath == ".") entry.name else "$currentPath/${entry.name}"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(entry.name, fullPath)
                    clipboard.setPrimaryClip(clip)
                },
                onLongPress = { entry ->
                    isSelectionMode = true
                    selectedItems = setOf(entry.name)
                },
                onSelectToggle = { entry ->
                    selectedItems = if (entry.name in selectedItems) {
                        selectedItems - entry.name
                    } else {
                        selectedItems + entry.name
                    }
                },
                onExitSelection = {
                    isSelectionMode = false
                    selectedItems = emptySet()
                },
                onDeleteSelected = {
                    selectedItems.forEach { name ->
                        val fullPath = if (currentPath == ".") name else "$currentPath/$name"
                        vm.sendDeleteFile(fullPath)
                    }
                    isSelectionMode = false
                    selectedItems = emptySet()
                },
                canGoBack = pathHistory.isNotEmpty()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileListView(
    path: String,
    entries: List<FileEntry>,
    pathHistory: List<String>,
    currentDevice: String,
    isSelectionMode: Boolean,
    selectedItems: Set<String>,
    onNavigate: (FileEntry) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCreateFile: () -> Unit,
    onDeleteFile: (FileEntry) -> Unit,
    onRenameFile: (FileEntry) -> Unit,
    onShareFile: (FileEntry) -> Unit,
    onCopyFile: (FileEntry) -> Unit,
    onLongPress: (FileEntry) -> Unit,
    onSelectToggle: (FileEntry) -> Unit,
    onExitSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    canGoBack: Boolean
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                if (isSelectionMode) {
                    Text("${selectedItems.size} seleccionados", style = MaterialTheme.typography.titleMedium)
                } else {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (currentDevice == "phone") Icons.Filled.PhoneAndroid else Icons.Filled.Computer,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Files", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            navigationIcon = {
                if (isSelectionMode) {
                    IconButton(onClick = onExitSelection) {
                        Icon(Icons.Filled.Close, "Exit selection")
                    }
                } else {
                    IconButton(onClick = { if (canGoBack) onBack() }) {
                        Icon(
                            if (canGoBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Folder,
                            contentDescription = if (canGoBack) "Back" else "Root"
                        )
                    }
                }
            },
            actions = {
                if (isSelectionMode) {
                    IconButton(onClick = onDeleteSelected) {
                        Icon(Icons.Filled.Delete, "Delete selected", tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    IconButton(onClick = onCreateFile, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Add, "Create file", modifier = Modifier.size(18.dp))
                    }
                    TextButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Refresh", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(entries, key = { "${it.isDir}_${it.name}" }) { entry ->
                FileEntryRow(
                    entry = entry,
                    isSelected = entry.name in selectedItems,
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (isSelectionMode) onSelectToggle(entry) else onNavigate(entry)
                    },
                    onLongPress = { onLongPress(entry) },
                    onShare = { onShareFile(entry) },
                    onCopy = { onCopyFile(entry) },
                    onRename = { onRenameFile(entry) },
                    onDelete = { onDeleteFile(entry) }
                )
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FileInfoItem("Carpetas", entries.count { it.isDir }.toString(), MaterialTheme.colorScheme.primary)
                FileInfoItem("Archivos", entries.count { !it.isDir }.toString(), MaterialTheme.colorScheme.secondary)
                FileInfoItem("Total", entries.size.toString(), MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileEntryRow(
    entry: FileEntry,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val (icon, iconColor) = when {
        entry.isDir -> Pair(Icons.Filled.Folder, MaterialTheme.colorScheme.primary)
        entry.name.endsWith(".kt") || entry.name.endsWith(".java") -> Pair(Icons.Filled.Code, MaterialTheme.colorScheme.secondary)
        entry.name.endsWith(".py") -> Pair(Icons.Filled.Code, MaterialTheme.colorScheme.tertiary)
        entry.name.endsWith(".json") || entry.name.endsWith(".xml") -> Pair(Icons.Filled.DataObject, MaterialTheme.colorScheme.primary)
        entry.name.endsWith(".md") || entry.name.endsWith(".txt") -> Pair(Icons.Filled.Description, MaterialTheme.colorScheme.onSurfaceVariant)
        entry.name.endsWith(".png") || entry.name.endsWith(".jpg") -> Pair(Icons.Filled.Image, MaterialTheme.colorScheme.secondary)
        entry.name.endsWith(".gradle") || entry.name.endsWith(".gradle.kts") -> Pair(Icons.Filled.Build, MaterialTheme.colorScheme.tertiary)
        entry.name.endsWith(".sh") -> Pair(Icons.Filled.Terminal, MaterialTheme.colorScheme.tertiary)
        entry.name.endsWith(".html") || entry.name.endsWith(".css") -> Pair(Icons.Filled.Web, MaterialTheme.colorScheme.secondary)
        else -> Pair(Icons.Filled.InsertDriveFile, MaterialTheme.colorScheme.outline)
    }

    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = iconColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.name, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (entry.isDir) FontWeight.Medium else FontWeight.Normal
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (entry.device == "phone") Icons.Filled.PhoneAndroid else Icons.Filled.Computer,
                        null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(4.dp))
                    if (!entry.isDir && entry.size > 0) {
                        Text(formatFileSize(entry.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.MoreVert, "Options", modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Compartir") },
                        onClick = { showMenu = false; onShare() },
                        leadingIcon = { Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Copiar") },
                        onClick = { showMenu = false; onCopy() },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Renombrar") },
                        onClick = { showMenu = false; onRename() },
                        leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
            if (entry.isDir) {
                Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FileInfoItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewer(path: String, content: String, onClose: () -> Unit, onEdit: () -> Unit, onShare: () -> Unit, onCopy: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                Column {
                    Text(File(path).name, style = MaterialTheme.typography.titleMedium)
                    Text(path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, "Share")
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Filled.ContentCopy, "Copy")
                }
                FilledTonalButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        Card(
            modifier = Modifier.weight(1f).padding(8.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
                content.lines().forEachIndexed { index, line ->
                    Row {
                        Text(
                            "${index + 1}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.width(36.dp), lineHeight = 16.sp
                        )
                        Text(
                            line, style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace, lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileEditor(path: String, content: String, onContentChange: (String) -> Unit, onSave: () -> Unit, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                Column {
                    Text("Editing: ${File(path).name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close")
                }
            },
            actions = {
                FilledTonalButton(onClick = onSave) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save", style = MaterialTheme.typography.labelMedium)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        OutlinedTextField(
            value = content, onValueChange = onContentChange,
            modifier = Modifier.weight(1f).padding(8.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
        )
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024 * 1024 * 1024)} GB"
}
```

- [ ] **Step 2: Build and install**

```bash
cd /home/DexTer/MiMoMobile
JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug
/home/DexTer/Android/Sdk/platform-tools/adb -s 192.168.100.166:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Verify on OnePlus 8**

Take screenshot. Verify:
- Header shows device icon (phone/computer) and path
- Files have device badges (phone/PC icons)
- Each file has three-dot menu with Share, Copy, Rename, Delete
- Long-press enters selection mode with checkboxes
- Bottom shows folder/file/total counts

---

### Task 4: Remote Section - Fullscreen Immersive

**Covers:** S5 (Remote fullscreen within tab margins)

**Files:**
- Modify: `app/src/main/java/com/mimo/mobile/ui/screens/RemoteScreen.kt`

**Goal:** Remote desktop fills entire tab area. Controls appear as floating overlay on tap, auto-hide after 3 seconds.

- [ ] **Step 1: Rewrite RemoteScreen with fullscreen and tap-to-show controls**

Replace entire file content:

```kotlin
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
```

- [ ] **Step 2: Build and install**

```bash
cd /home/DexTer/MiMoMobile
JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug
/home/DexTer/Android/Sdk/platform-tools/adb -s 192.168.100.166:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Verify on OnePlus 8**

Take screenshot. Verify:
- Stream fills entire tab (no header/footer)
- "Toca para controles" hint at top
- Tap shows floating control bar at bottom
- Controls auto-hide after 3 seconds
- Touch mode: drag moves cursor
- Keyboard mode: tap shows text input

---

### Task 5: Final Verification

**Covers:** S10 (Success Criteria)

**Files:** None (verification only)

**Goal:** Verify all improvements work together on OnePlus 8.

- [ ] **Step 1: Build final APK**

```bash
cd /home/DexTer/MiMoMobile
JAVA_HOME=/home/DexTer/android-studio/jbr ./gradlew assembleDebug
```

- [ ] **Step 2: Install on OnePlus 8**

```bash
/home/DexTer/Android/Sdk/platform-tools/adb -s 192.168.100.166:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Verify Background**

Open app. Verify background has:
- Blocks falling from top
- Construction lines drawing
- Particles flowing downward
- Scan line sweeping
- ALL elements in constant motion

- [ ] **Step 4: Verify Build Section**

Go to Build tab. Verify:
- Empty state shows "Constructor de Apps"
- Text input at bottom
- Send prompt, see wireframes appear
- Wireframes transform to colored UI

- [ ] **Step 5: Verify Files Section**

Go to Files tab. Verify:
- Device icon in header
- Files have device badges
- Three-dot menu on each file
- Share, Copy, Rename, Delete options work
- Long-press enters selection mode

- [ ] **Step 6: Verify Remote Section**

Go to Remote tab. Verify:
- Stream fills entire tab
- "Toca para controles" hint
- Tap shows floating controls
- Controls auto-hide

- [ ] **Step 7: Take final screenshots**

Capture screenshots of each screen for documentation.

- [ ] **Step 8: Commit changes**

```bash
cd /home/DexTer/MiMoMobile
git add -A
git commit -m "feat: MiMo Mobile v2 - visual app builder, unified files, fullscreen remote, construction background"
```

---

## Execution Order

1. Task 1 (Background) - Foundation
2. Task 2 (Build) - Major feature
3. Task 3 (Files) - Important functionality
4. Task 4 (Remote) - Enhancement
5. Task 5 (Verification) - Final testing
