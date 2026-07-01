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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.network.ConnectionState
import com.mimo.mobile.ui.adaptive.rememberWindowWidthDp
import com.mimo.mobile.ui.components.MatplotlibBackground
import com.mimo.mobile.viewmodel.ChatInstance
import com.mimo.mobile.viewmodel.MiMoViewModel
import kotlinx.coroutines.launch

private val CodeFont = FontFamily.Monospace
private val CodeBackground = Color(0xFF1E1E2E)
private val CodeKeyword = Color(0xFFCBA6F7)
private val CodeString = Color(0xFFA6E3A1)
private val CodeComment = Color(0xFF6C7086)
private val CodeFunction = Color(0xFF89B4FA)
private val CodeNumber = Color(0xFFFAB387)

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
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.padding(horizontal = 2.dp),
                            leadingIcon = if (isActive) {
                                { Icon(Icons.Filled.Circle, null, modifier = Modifier.size(6.dp), tint = MaterialTheme.colorScheme.primary) }
                            } else null
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showInstanceManager = !showInstanceManager }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Add, "New instance", modifier = Modifier.size(16.dp))
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (activeInstance?.messages.isNullOrEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.SmartToy,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "MiMo Mobile",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Escribe un mensaje para comenzar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(activeInstance?.messages ?: emptyList(), key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        isUser = msg.role == "user"
                    )
                }

                if (isProcessing) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
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
                        placeholder = { Text("Escribe tu mensaje...", fontSize = 14.sp) },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank() && isReady) {
                                vm.sendChat(inputText.trim())
                                vm.incrementMessageCount()
                                inputText = ""
                            }
                        })
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank() && isReady) {
                                vm.sendChat(inputText.trim())
                                vm.incrementMessageCount()
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && isReady,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: com.mimo.mobile.viewmodel.ChatMsg, isUser: Boolean) {
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = shape,
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "MiMo",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                RenderMessageContent(message.content, textColor)
            }
        }
    }
}

@Composable
private fun RenderMessageContent(content: String, baseColor: Color) {
    val codeBlockRegex = Regex("```(\\w+)?\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
    val parts = mutableListOf<Pair<Boolean, String>>()
    var lastIndex = 0

    codeBlockRegex.findAll(content).forEach { match ->
        if (match.range.first > lastIndex) {
            parts.add(Pair(false, content.substring(lastIndex, match.range.first)))
        }
        parts.add(Pair(true, match.value))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < content.length) {
        parts.add(Pair(false, content.substring(lastIndex)))
    }

    if (parts.isEmpty()) {
        parts.add(Pair(false, content))
    }

    parts.forEach { (isCode, text) ->
        if (isCode) {
            val lang = Regex("```(\\w+)").find(text)?.groupValues?.get(1) ?: ""
            val code = text.removePrefix("```$lang\n").removeSuffix("```").trim()
            CodeBlock(code = code, language = lang)
        } else {
            Text(
                text = text.trim(),
                color = baseColor,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun CodeBlock(code: String, language: String) {
    var expanded by remember { mutableStateOf(true) }
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CodeBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifEmpty { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = CodeComment,
                    fontFamily = CodeFont,
                    fontSize = 10.sp
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(20.dp)) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = CodeComment
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    code.lines().forEachIndexed { index, line ->
                        Row {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = CodeFont,
                                fontSize = 11.sp,
                                color = CodeComment.copy(alpha = 0.5f),
                                modifier = Modifier.width(24.dp)
                            )
                            Text(
                                text = highlightSyntax(line),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = CodeFont,
                                fontSize = 11.sp,
                                color = Color(0xFFCDD6F4),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Pensando",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
        }
    }
}

@Composable
private fun ConnectionBanner(state: ConnectionState) {
    val color = when (state) {
        ConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiaryContainer
        ConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val text = when (state) {
        ConnectionState.CONNECTING -> "Conectando..."
        ConnectionState.ERROR -> "Error de conexión"
        else -> ""
    }

    if (text.isNotEmpty()) {
        Surface(color = color, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun highlightSyntax(line: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < line.length) {
            when {
                line.substring(i).startsWith("//") -> {
                    withStyle(SpanStyle(color = CodeComment)) { append(line.substring(i)) }
                    i = line.length
                }
                line.substring(i).startsWith("#") -> {
                    withStyle(SpanStyle(color = CodeComment)) { append(line.substring(i)) }
                    i = line.length
                }
                line.substring(i).startsWith("\"") || line.substring(i).startsWith("'") -> {
                    val quote = line[i]
                    val end = line.indexOf(quote, i + 1).let { if (it == -1) line.length else it + 1 }
                    withStyle(SpanStyle(color = CodeString)) { append(line.substring(i, end)) }
                    i = end
                }
                line.substring(i).startsWith("fun ") || line.substring(i).startsWith("def ") ||
                line.substring(i).startsWith("class ") || line.substring(i).startsWith("import ") ||
                line.substring(i).startsWith("val ") || line.substring(i).startsWith("var ") ||
                line.substring(i).startsWith("return ") || line.substring(i).startsWith("if ") ||
                line.substring(i).startsWith("else ") || line.substring(i).startsWith("for ") ||
                line.substring(i).startsWith("while ") -> {
                    val keywords = listOf("fun ", "def ", "class ", "import ", "val ", "var ", "return ", "if ", "else ", "for ", "while ", "in ", "is ", "as ", "try ", "catch ", "throw ", "when ", "object ", "interface ", "data ", "sealed ", "override ", "private ", "public ", "internal ", "protected ", "suspend ", "async ", "await ", "true ", "false ", "null ", "this ", "super ")
                    val keyword = keywords.find { line.substring(i).startsWith(it) }
                    if (keyword != null) {
                        withStyle(SpanStyle(color = CodeKeyword)) { append(keyword.trimEnd()) }
                        i += keyword.length
                    } else {
                        withStyle(SpanStyle(color = CodeFunction)) { append(line[i]) }
                        i++
                    }
                }
                line[i].isDigit() -> {
                    val end = (i until line.length).firstOrNull { !line[it].isDigit() && line[it] != '.' } ?: line.length
                    withStyle(SpanStyle(color = CodeNumber)) { append(line.substring(i, end)) }
                    i = end
                }
                else -> {
                    append(line[i])
                    i++
                }
            }
        }
    }
}
