package com.mimo.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.viewmodel.MiMoViewModel
import java.io.File

data class FileEntry(val name: String, val isDir: Boolean, val size: Long = 0)

@Composable
fun FileBrowserScreen(vm: MiMoViewModel) {
    var currentPath by remember { mutableStateOf(".") }
    var entries by remember { mutableStateOf(listOf<FileEntry>()) }
    var selectedFile by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editingFile by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editContent by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<FileEntry?>(null) }
    val pathHistory = remember { mutableStateListOf<String>() }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            when (msg.type) {
                "dir_listing" -> {
                    val rawEntries = msg.entries ?: emptyList()
                    entries = rawEntries.map {
                        FileEntry(
                            name = it["name"] as? String ?: "",
                            isDir = it["is_dir"] as? Boolean ?: false,
                            size = (it["size"] as? Number)?.toLong() ?: 0L
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
            }
        }
    }

    LaunchedEffect(state.connectionState) {
        if (state.connectionState == com.mimo.mobile.network.ConnectionState.CONNECTED) {
            vm.sendListDir(".")
        }
    }

    when {
        showDeleteDialog != null -> {
            val entry = showDeleteDialog!!
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete ${if (entry.isDir) "Folder" else "File"}") },
                text = { Text("Are you sure you want to delete '${entry.name}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val fullPath = if (currentPath == ".") entry.name else "$currentPath/${entry.name}"
                            vm.sendDeleteFile(fullPath)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                }
            )
        }
        showCreateDialog -> {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false; newFileName = "" },
                title = { Text("Create New File") },
                text = {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Filename") },
                        placeholder = { Text("example.txt") },
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
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false; newFileName = "" }) { Text("Cancel") }
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
                onEdit = { editingFile = selectedFile; editContent = selectedFile!!.second; selectedFile = null }
            )
        }
        else -> {
            FileListView(
                path = currentPath, entries = entries, pathHistory = pathHistory,
                onNavigate = { entry ->
                    val newPath = if (currentPath == ".") entry.name else "$currentPath/${entry.name}"
                    if (entry.isDir) { pathHistory.add(currentPath); vm.sendListDir(newPath) }
                    else vm.sendReadFile(newPath)
                },
                onBack = { if (pathHistory.isNotEmpty()) vm.sendListDir(pathHistory.removeLast()) },
                onRefresh = { vm.sendListDir(currentPath) },
                onCreateFile = { showCreateDialog = true },
                onDeleteFile = { showDeleteDialog = it },
                canGoBack = pathHistory.isNotEmpty()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListView(
    path: String, entries: List<FileEntry>, pathHistory: List<String>,
    onNavigate: (FileEntry) -> Unit, onBack: () -> Unit, onRefresh: () -> Unit,
    onCreateFile: () -> Unit, onDeleteFile: (FileEntry) -> Unit, canGoBack: Boolean
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                Column {
                    Text("Files", style = MaterialTheme.typography.titleMedium)
                    Text(path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            },
            navigationIcon = {
                IconButton(onClick = { if (canGoBack) onBack() }) {
                    Icon(
                        if (canGoBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Folder,
                        contentDescription = if (canGoBack) "Back" else "Root"
                    )
                }
            },
            actions = {
                IconButton(onClick = onCreateFile, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Add, "Create file", modifier = Modifier.size(18.dp))
                }
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Refresh", style = MaterialTheme.typography.labelSmall)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(entries, key = { "${it.isDir}_${it.name}" }) { entry ->
                FileEntryRow(entry, onClick = { onNavigate(entry) }, onLongPress = { onDeleteFile(entry) })
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FileInfoItem("Folders", entries.count { it.isDir }.toString(), MaterialTheme.colorScheme.primary)
                FileInfoItem("Files", entries.count { !it.isDir }.toString(), MaterialTheme.colorScheme.secondary)
                FileInfoItem("Total", entries.size.toString(), MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun FileEntryRow(entry: FileEntry, onClick: () -> Unit, onLongPress: () -> Unit) {
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
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                if (!entry.isDir && entry.size > 0) {
                    Text(formatFileSize(entry.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
fun FileViewer(path: String, content: String, onClose: () -> Unit, onEdit: () -> Unit) {
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
