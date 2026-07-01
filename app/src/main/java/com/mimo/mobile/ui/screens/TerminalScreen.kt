package com.mimo.mobile.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.viewmodel.MiMoViewModel

private val CodeFont = FontFamily.Monospace
private val TerminalBg = Color(0xFF0D1117)
private val TerminalGreen = Color(0xFF7EE787)
private val TerminalBlue = Color(0xFF79C0FF)
private val TerminalYellow = Color(0xFFE3B341)
private val TerminalRed = Color(0xFFFF7B72)
private val TerminalPurple = Color(0xFFD2A8FF)
private val TerminalCyan = Color(0xFFA5D6FF)
private val TerminalGray = Color(0xFF8B949E)

data class CodeLine(
    val content: String,
    val lineNumber: Int,
    val type: CodeLineType,
    val fileName: String? = null
)

enum class CodeLineType { COMMAND, OUTPUT, ERROR, CODE, COMMENT, SUCCESS }

@Composable
fun TerminalScreen(vm: MiMoViewModel) {
    val state by vm.state.collectAsState()
    var input by remember { mutableStateOf("") }
    var codeLines by remember { mutableStateOf(listOf<CodeLine>()) }
    var currentFile by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var lineCounter by remember { mutableIntStateOf(1) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            when (msg.type) {
                "exec_output" -> {
                    val text = msg.data?.toString() ?: ""
                    if (text.isNotBlank()) {
                        val type = when {
                            text.startsWith("error") || text.startsWith("Error") -> CodeLineType.ERROR
                            text.startsWith("$") || text.startsWith(">") -> CodeLineType.COMMAND
                            text.contains("BUILD SUCCESS") || text.contains("Success") -> CodeLineType.SUCCESS
                            else -> CodeLineType.OUTPUT
                        }
                        codeLines = codeLines + CodeLine(text, lineCounter++, type)
                    }
                }
                "chat_chunk" -> {
                    val text = msg.data?.toString() ?: ""
                    if (text.isNotBlank() && text.length > 10) {
                        codeLines = codeLines + CodeLine(text, lineCounter++, CodeLineType.CODE, currentFile)
                    }
                }
                "chat_start" -> {
                    currentFile = "main.kt"
                    codeLines = codeLines + CodeLine("── MiMo está trabajando ──", lineCounter++, CodeLineType.COMMENT)
                }
                "exec_end" -> {
                    isRunning = false
                    val exitCode = msg.exitCode
                    val error = msg.error
                    if (error != null) {
                        codeLines = codeLines + CodeLine("✗ Error: $error", lineCounter++, CodeLineType.ERROR)
                    } else if (exitCode != null && exitCode == 0) {
                        codeLines = codeLines + CodeLine("✓ Completado", lineCounter++, CodeLineType.SUCCESS)
                    }
                }
            }
        }
    }

    LaunchedEffect(codeLines.size) {
        if (codeLines.isNotEmpty()) {
            kotlinx.coroutines.delay(50)
            listState.animateScrollToItem(codeLines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        Surface(
            color = Color(0xFF161B22),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Code,
                    null,
                    tint = TerminalGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Live Code Viewer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFC9D1D9)
                    )
                    if (currentFile.isNotEmpty()) {
                        Text(
                            currentFile,
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalCyan,
                            fontFamily = CodeFont,
                            fontSize = 10.sp
                        )
                    } else {
                        Text(
                            if (isRunning) "Escribiendo..." else "Esperando...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalGray
                        )
                    }
                }
                if (codeLines.isNotEmpty()) {
                    IconButton(
                        onClick = { codeLines = emptyList(); lineCounter = 1 },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.DeleteSweep, "Clear", modifier = Modifier.size(16.dp), tint = TerminalGray)
                    }
                }
            }
        }

        if (state.connectionState != com.mimo.mobile.network.ConnectionState.CONNECTED) {
            Surface(
                color = TerminalRed.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, null, tint = TerminalRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Desconectado", style = MaterialTheme.typography.labelMedium, color = TerminalRed)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (codeLines.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Terminal,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = TerminalGreen.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Live Code Viewer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC9D1D9)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "El código que MiMo escribe aparecerá aquí",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TerminalGray
                            )
                        }
                    }
                }
            }

            items(codeLines, key = { it.lineNumber }) { line ->
                CodeLineRow(line)
            }
        }

        Surface(
            color = Color(0xFF161B22)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = CodeFont,
                    color = TerminalGreen,
                    modifier = Modifier.padding(end = 8.dp)
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Comando...", fontFamily = CodeFont, fontSize = 13.sp, color = TerminalGray)
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = CodeFont,
                        fontSize = 13.sp,
                        color = Color(0xFFC9D1D9)
                    ),
                    maxLines = 1,
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TerminalGreen.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color(0xFF30363D)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (input.isNotBlank() && state.connectionState == com.mimo.mobile.network.ConnectionState.CONNECTED) {
                            codeLines = codeLines + CodeLine("$ ${input.trim()}", lineCounter++, CodeLineType.COMMAND)
                            vm.sendExecute(input.trim())
                            isRunning = true
                            input = ""
                        }
                    })
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank() && state.connectionState == com.mimo.mobile.network.ConnectionState.CONNECTED) {
                            codeLines = codeLines + CodeLine("$ ${input.trim()}", lineCounter++, CodeLineType.COMMAND)
                            vm.sendExecute(input.trim())
                            isRunning = true
                            input = ""
                        }
                    },
                    enabled = input.isNotBlank() && state.connectionState == com.mimo.mobile.network.ConnectionState.CONNECTED,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(18.dp), tint = TerminalGreen)
                }
            }
        }
    }
}

@Composable
private fun CodeLineRow(line: CodeLine) {
    val textColor = when (line.type) {
        CodeLineType.COMMAND -> TerminalGreen
        CodeLineType.ERROR -> TerminalRed
        CodeLineType.SUCCESS -> TerminalGreen
        CodeLineType.COMMENT -> TerminalGray
        CodeLineType.CODE -> TerminalCyan
        CodeLineType.OUTPUT -> Color(0xFFC9D1D9)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .background(if (line.type == CodeLineType.ERROR) TerminalRed.copy(alpha = 0.05f) else Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "%4d".format(line.lineNumber),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = CodeFont,
            fontSize = 11.sp,
            color = TerminalGray.copy(alpha = 0.4f),
            modifier = Modifier.width(32.dp)
        )

        if (line.type == CodeLineType.COMMAND) {
            Text(
                text = "❯ ",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = CodeFont,
                fontSize = 12.sp,
                color = TerminalGreen
            )
        } else if (line.type == CodeLineType.SUCCESS) {
            Text(
                text = "✓ ",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = CodeFont,
                fontSize = 12.sp,
                color = TerminalGreen
            )
        } else if (line.type == CodeLineType.ERROR) {
            Text(
                text = "✗ ",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = CodeFont,
                fontSize = 12.sp,
                color = TerminalRed
            )
        }

        if (line.fileName != null) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = TerminalPurple.copy(alpha = 0.15f)
            ) {
                Text(
                    text = line.fileName,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = CodeFont,
                    fontSize = 9.sp,
                    color = TerminalPurple,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
        }

        Text(
            text = line.content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = CodeFont,
            fontSize = 12.sp,
            color = textColor,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
