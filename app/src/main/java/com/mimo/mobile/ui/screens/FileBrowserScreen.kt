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
