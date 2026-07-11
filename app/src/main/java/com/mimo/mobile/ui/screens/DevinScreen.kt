package com.mimo.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.ui.theme.MiMoPurple
import com.mimo.mobile.ui.theme.MiMoCyan
import com.mimo.mobile.ui.theme.MiMoDarkBg

data class DevinTask(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val category: String,
    val command: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevinScreen(
    serverUrl: String,
    onNavigateBack: () -> Unit
) {
    var selectedTask by remember { mutableStateOf<DevinTask?>(null) }
    var customTask by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }
    var outputLog by remember { mutableStateOf(listOf<String>()) }
    var showOutput by remember { mutableStateOf(false) }

    val tasks = listOf(
        DevinTask(
            id = "android_build",
            title = "Optimizar Build Android",
            description = "Revisa warnings de compilación y optimiza el build",
            icon = Icons.Default.Build,
            category = "Build",
            command = "android build"
        ),
        DevinTask(
            id = "fix_bugs",
            title = "Corregir Bugs",
            description = "Detecta y corrige errores en el código",
            icon = Icons.Default.BugReport,
            category = "Debug",
            command = "bug fix"
        ),
        DevinTask(
            id = "code_review",
            title = "Revisión de Código",
            description = "Analiza calidad del código y sugiere mejoras",
            icon = Icons.Default.Policy,
            category = "Review",
            command = "code review"
        ),
        DevinTask(
            id = "write_tests",
            title = "Escribir Tests",
            description = "Genera tests unitarios para el código reciente",
            icon = Icons.Default.Science,
            category = "Testing",
            command = "write tests"
        ),
        DevinTask(
            id = "optimize",
            title = "Optimizar Rendimiento",
            description = "Identifica cuellos de botella y optimiza",
            icon = Icons.Default.Speed,
            category = "Performance",
            command = "optimize performance"
        ),
        DevinTask(
            id = "security_audit",
            title = "Auditoría de Seguridad",
            description = "Revisa vulnerabilidades y problemas de seguridad",
            icon = Icons.Default.Security,
            category = "Security",
            command = "security audit"
        ),
        DevinTask(
            id = "documentation",
            title = "Generar Documentación",
            description = "Crea documentación para el código existente",
            icon = Icons.Default.Description,
            category = "Docs",
            command = "generate documentation"
        ),
        DevinTask(
            id = "deploy",
            title = "Preparar Deploy",
            description = "Prepara el código para despliegue en producción",
            icon = Icons.Default.CloudUpload,
            category = "DevOps",
            command = "prepare deployment"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MiMoCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Devin AI", color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MiMoDarkBg
                )
            )
        },
        containerColor = MiMoDarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Circle,
                        contentDescription = null,
                        tint = if (isExecuting) Color(0xFFFFD700) else Color(0xFF4CAF50),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExecuting) "Ejecutando tarea..." else "Listo para recibir tareas",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            // Custom Task Input
            OutlinedTextField(
                value = customTask,
                onValueChange = { customTask = it },
                label = { Text("Tarea personalizada", color = Color.White.copy(alpha = 0.6f)) },
                placeholder = { Text("Ej: Revisa el último push y corrige warnings", color = Color.White.copy(alpha = 0.3f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MiMoCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (customTask.isNotEmpty()) {
                                isExecuting = true
                                outputLog = listOf("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] Iniciando tarea personalizada...")
                                showOutput = true
                                // TODO: Execute task via API
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Execute",
                            tint = if (customTask.isNotEmpty()) MiMoCyan else Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            )

            // Task Categories
            Text(
                text = "TAREAS RÁPIDAS",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Tasks Grid
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks.chunked(2)) { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { task ->
                            TaskCard(
                                task = task,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedTask = task
                                    isExecuting = true
                                    outputLog = listOf("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] Iniciando: ${task.title}")
                                    showOutput = true
                                    // TODO: Execute task via API
                                }
                            )
                        }
                        // Fill remaining space if odd number of items
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Output Panel
            if (showOutput) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0D1117)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Salida",
                                color = MiMoCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showOutput = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(outputLog) { line ->
                                Text(
                                    text = line,
                                    color = Color(0xFF00FF00),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: DevinTask,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    task.icon,
                    contentDescription = null,
                    tint = MiMoCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.category,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
            Text(
                text = task.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = task.description,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                maxLines = 2
            )
        }
    }
}
