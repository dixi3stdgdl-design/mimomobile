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
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.viewmodel.MiMoViewModel

private val CodeFont = FontFamily.Monospace
private val EditorBg = Color(0xFF0D1117)
private val EditorLineNum = Color(0xFF484F58)
private val EditorCursor = Color(0xFF58A6FF)
private val EditorGreen = Color(0xFF7EE787)
private val EditorBlue = Color(0xFF79C0FF)
private val EditorPurple = Color(0xFFD2A8FF)
private val EditorYellow = Color(0xFFE3B341)
private val EditorRed = Color(0xFFFF7B72)
private val EditorCyan = Color(0xFFA5D6FF)
private val EditorOrange = Color(0xFFFFA657)

data class SourceFile(
    val name: String,
    val language: String,
    val lines: List<String>,
    val isComplete: Boolean = false
)

@Composable
fun BuildVisualizerScreen(vm: MiMoViewModel) {
    var files by remember { mutableStateOf(listOf<SourceFile>()) }
    var activeFileIndex by remember { mutableIntStateOf(0) }
    var projectName by remember { mutableStateOf("Proyecto") }
    var isBuilding by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val infiniteTransition = rememberInfiniteTransition(label = "build")
    val cursorBlink by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursor"
    )

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            when (msg.type) {
                "chat_start" -> {
                    isBuilding = true
                    if (files.isEmpty()) {
                        files = listOf(SourceFile("Main.kt", "kotlin", emptyList()))
                        activeFileIndex = 0
                    }
                }
                "chat_chunk" -> {
                    val text = msg.data?.toString() ?: ""
                    if (text.isNotBlank()) {
                        val current = files.toMutableList()
                        if (current.isNotEmpty()) {
                            val idx = activeFileIndex.coerceIn(0, current.size - 1)
                            val file = current[idx]
                            val newLines = text.split("\n").filter { it.isNotBlank() }
                            current[idx] = file.copy(lines = file.lines + newLines)
                            files = current
                        }
                    }
                }
                "chat_end" -> {
                    isBuilding = false
                    val current = files.toMutableList()
                    if (current.isNotEmpty()) {
                        val idx = activeFileIndex.coerceIn(0, current.size - 1)
                        current[idx] = current[idx].copy(isComplete = true)
                        files = current
                    }
                }
                "build_progress" -> {
                    try {
                        val json = org.json.JSONObject(msg.data.toString())
                        projectName = json.optString("project", "Proyecto")
                    } catch (_: Exception) {}
                }
            }
        }
    }

    LaunchedEffect(files.size) {
        if (files.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            listState.animateScrollToItem(files.flatMap { it.lines }.size.coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorBg)
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
                    Icons.Filled.Build,
                    null,
                    tint = EditorPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Live Code Writer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC9D1D9)
                    )
                    Text(
                        projectName,
                        style = MaterialTheme.typography.labelSmall,
                        color = EditorCyan,
                        fontFamily = CodeFont,
                        fontSize = 10.sp
                    )
                }
                if (isBuilding) {
                    Surface(
                        shape = CircleShape,
                        color = EditorGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EditorGreen)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Escribiendo", style = MaterialTheme.typography.labelSmall, color = EditorGreen)
                        }
                    }
                } else if (files.isNotEmpty() && files.all { it.isComplete }) {
                    Surface(
                        shape = CircleShape,
                        color = EditorGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(12.dp), tint = EditorGreen)
                            Spacer(Modifier.width(4.dp))
                            Text("Completado", style = MaterialTheme.typography.labelSmall, color = EditorGreen)
                        }
                    }
                }
            }
        }

        if (files.size > 1) {
            Surface(color = Color(0xFF161B22)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    files.forEachIndexed { index, file ->
                        val isActive = index == activeFileIndex
                        Surface(
                            onClick = { activeFileIndex = index },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isActive) Color(0xFF21262D) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    when (file.language) {
                                        "kotlin" -> Icons.Filled.Code
                                        "python" -> Icons.Filled.DataObject
                                        "xml" -> Icons.Filled.Description
                                        else -> Icons.Filled.InsertDriveFile
                                    },
                                    null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isActive) EditorBlue else EditorCyan
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    file.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = CodeFont,
                                    fontSize = 10.sp,
                                    color = if (isActive) Color(0xFFC9D1D9) else EditorCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Code,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = EditorPurple.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Live Code Writer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC9D1D9)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "El código fuente aparecerá aquí línea por línea",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EditorLineNum
                    )
                }
            }
        } else {
            val activeFile = files.getOrNull(activeFileIndex) ?: files.first()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(activeFile.lines, key = { "${activeFile.name}_$it" }) { line ->
                    EditorCodeLine(
                        content = line,
                        lineNum = activeFile.lines.indexOf(line) + 1,
                        showCursor = isBuilding && line == activeFile.lines.lastOrNull()
                    )
                }

                if (isBuilding) {
                    item {
                        Row(
                            modifier = Modifier.padding(start = 48.dp, top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(14.dp)
                                    .background(EditorCursor.copy(alpha = cursorBlink))
                            )
                        }
                    }
                }
            }
        }

        Surface(
            color = Color(0xFF161B22)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${files.flatMap { it.lines }.size} líneas",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = CodeFont,
                    fontSize = 10.sp,
                    color = EditorLineNum
                )
                Spacer(Modifier.weight(1f))
                if (activeFileIndex < files.size) {
                    Text(
                        files[activeFileIndex].language.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = CodeFont,
                        fontSize = 10.sp,
                        color = EditorCyan
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "UTF-8",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = CodeFont,
                        fontSize = 10.sp,
                        color = EditorLineNum
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorCodeLine(content: String, lineNum: Int, showCursor: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 0.dp, vertical = 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .background(Color(0xFF0D1117))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "$lineNum",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = CodeFont,
                fontSize = 12.sp,
                color = EditorLineNum
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            Text(
                text = highlightCode(content),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = CodeFont,
                fontSize = 13.sp,
                color = Color(0xFFC9D1D9),
                lineHeight = 18.sp
            )
        }
    }
}

private fun highlightCode(code: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < code.length) {
            when {
                code.substring(i).startsWith("//") -> {
                    withStyle(SpanStyle(color = EditorLineNum)) { append(code.substring(i)) }
                    i = code.length
                }
                code.substring(i).startsWith("/*") -> {
                    withStyle(SpanStyle(color = EditorLineNum)) { append(code.substring(i)) }
                    i = code.length
                }
                code.substring(i).startsWith("\"") || code.substring(i).startsWith("'") -> {
                    val quote = code[i]
                    val end = code.indexOf(quote, i + 1).let { if (it == -1) code.length else it + 1 }
                    withStyle(SpanStyle(color = EditorGreen)) { append(code.substring(i, end)) }
                    i = end
                }
                code.substring(i).startsWith("fun ") || code.substring(i).startsWith("val ") ||
                code.substring(i).startsWith("var ") || code.substring(i).startsWith("class ") ||
                code.substring(i).startsWith("import ") || code.substring(i).startsWith("return ") ||
                code.substring(i).startsWith("if ") || code.substring(i).startsWith("else ") ||
                code.substring(i).startsWith("for ") || code.substring(i).startsWith("while ") ||
                code.substring(i).startsWith("when ") || code.substring(i).startsWith("object ") ||
                code.substring(i).startsWith("interface ") || code.substring(i).startsWith("data ") ||
                code.substring(i).startsWith("sealed ") || code.substring(i).startsWith("override ") ||
                code.substring(i).startsWith("private ") || code.substring(i).startsWith("public ") ||
                code.substring(i).startsWith("internal ") || code.substring(i).startsWith("protected ") ||
                code.substring(i).startsWith("suspend ") || code.substring(i).startsWith("async ") ||
                code.substring(i).startsWith("await ") || code.substring(i).startsWith("try ") ||
                code.substring(i).startsWith("catch ") || code.substring(i).startsWith("throw ") -> {
                    val keywords = listOf("fun ", "val ", "var ", "class ", "import ", "return ", "if ", "else ", "for ", "while ", "when ", "object ", "interface ", "data ", "sealed ", "override ", "private ", "public ", "internal ", "protected ", "suspend ", "async ", "await ", "try ", "catch ", "throw ", "in ", "is ", "as ", "true", "false", "null", "this", "super")
                    val keyword = keywords.find { code.substring(i).startsWith(it) }
                    if (keyword != null) {
                        withStyle(SpanStyle(color = EditorPurple)) { append(keyword) }
                        i += keyword.length
                    } else {
                        withStyle(SpanStyle(color = EditorBlue)) { append(code[i]) }
                        i++
                    }
                }
                code[i].isDigit() -> {
                    val end = (i until code.length).firstOrNull { !code[it].isDigit() && code[it] != '.' } ?: code.length
                    withStyle(SpanStyle(color = EditorOrange)) { append(code.substring(i, end)) }
                    i = end
                }
                code.substring(i).startsWith("@") -> {
                    val end = (i + 1 until code.length).firstOrNull { !code[it].isLetterOrDigit() && code[it] != '_' } ?: code.length
                    withStyle(SpanStyle(color = EditorYellow)) { append(code.substring(i, end)) }
                    i = end
                }
                else -> {
                    append(code[i])
                    i++
                }
            }
        }
    }
}
