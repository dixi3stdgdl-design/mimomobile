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
    var showDevin by remember { mutableStateOf(false) }
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
                // Devin AI Button
                IconButton(
                    onClick = { showDevin = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.SmartToy,
                        contentDescription = "Devin AI",
                        tint = Color(0xFF58A6FF),
                        modifier = Modifier.size(20.dp)
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

    // Devin AI Dialog
    if (showDevin) {
        DevinScreen(
            serverUrl = "http://${state.serverHost}:${state.serverPort}",
            onNavigateBack = { showDevin = false }
        )
    }
}
