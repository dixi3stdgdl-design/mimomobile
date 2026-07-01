package com.mimo.mobile.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.mobile.viewmodel.MiMoViewModel
import kotlin.math.*

data class BuildSegment(
    val name: String,
    val platform: String,
    val status: SegmentStatus,
    val progress: Float,
    val files: List<String>
)

enum class SegmentStatus { PENDING, BUILDING, COMPLETE, ERROR }

@Composable
fun BuildVisualizerScreen(vm: MiMoViewModel) {
    var projectName by remember { mutableStateOf("") }
    var segments by remember { mutableStateOf(listOf<BuildSegment>()) }
    var loaded by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "build")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse"
    )
    val scanLine by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "scan"
    )
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "orbit"
    )
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "breathe"
    )

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            if (msg.type == "build_progress" && msg.data != null) {
                try {
                    val json = org.json.JSONObject(msg.data.toString())
                    projectName = json.optString("project", "Project")
                    val arr = json.optJSONArray("segments")
                    if (arr != null) {
                        val list = mutableListOf<BuildSegment>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val statusName = obj.optString("status", "PENDING")
                            list.add(BuildSegment(
                                name = obj.getString("name"),
                                platform = obj.optString("platform", "Android"),
                                status = try { SegmentStatus.valueOf(statusName) } catch (_: Exception) { SegmentStatus.PENDING },
                                progress = obj.optDouble("progress", 0.0).toFloat(),
                                files = emptyList()
                            ))
                        }
                        segments = list
                    }
                    loaded = true
                } catch (_: Exception) {}
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2
            val cy = h / 2

            for (i in 0..20) {
                val x = w * i / 20
                drawLine(Color.White.copy(alpha = 0.02f), Offset(x, 0f), Offset(x, h), 0.5f)
                val y = h * i / 20
                drawLine(Color.White.copy(alpha = 0.02f), Offset(0f, y), Offset(w, y), 0.5f)
            }

            drawLine(
                color = primaryColor.copy(alpha = 0.08f),
                start = Offset(0f, h * scanLine),
                end = Offset(w, h * scanLine),
                strokeWidth = 1f
            )

            if (segments.isNotEmpty()) {
                val centerX = cx
                val centerY = cy
                val nodePositions = mutableListOf<Offset>()
                val colors = listOf(primaryColor, secondaryColor, tertiaryColor)

                segments.forEachIndexed { idx, seg ->
                    val angle = (2.0 * PI * idx / segments.size + orbitAngle * PI / 180).toFloat()
                    val radius = w * 0.18f
                    val px = centerX + cos(angle) * radius
                    val py = centerY + sin(angle) * radius * 0.6f
                    nodePositions.add(Offset(px, py))

                    val nodeColor = colors[idx % colors.size]
                    val alpha = when (seg.status) {
                        SegmentStatus.COMPLETE -> 0.4f + pulse * 0.3f
                        SegmentStatus.BUILDING -> 0.2f + pulse * 0.5f
                        SegmentStatus.ERROR -> 0.3f
                        SegmentStatus.PENDING -> 0.1f
                    }

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(nodeColor.copy(alpha = 0.25f * alpha), Color.Transparent),
                            center = Offset(px, py), radius = 35f
                        ),
                        radius = 35f, center = Offset(px, py)
                    )
                    drawCircle(color = nodeColor.copy(alpha = alpha * 0.6f), radius = 14f, center = Offset(px, py), style = Stroke(2f))
                    drawCircle(color = nodeColor.copy(alpha = alpha), radius = 5f, center = Offset(px, py))

                    if (seg.status == SegmentStatus.BUILDING) {
                        val arcAngle = 360f * seg.progress
                        drawArc(
                            color = nodeColor.copy(alpha = 0.5f),
                            startAngle = 0f,
                            sweepAngle = arcAngle,
                            useCenter = false,
                            topLeft = Offset(px - 18f, py - 18f),
                            size = androidx.compose.ui.geometry.Size(36f, 36f),
                            style = Stroke(2.5f)
                        )
                    }
                }

                for (i in nodePositions.indices) {
                    for (j in i + 1 until nodePositions.size) {
                        val a = nodePositions[i]
                        val b = nodePositions[j]
                        val dx = a.x - b.x
                        val dy = a.y - b.y
                        val dist = sqrt(dx * dx + dy * dy)
                        val maxDist = w * 0.3f
                        if (dist < maxDist) {
                            val edgeAlpha = (1f - dist / maxDist) * 0.12f * pulse
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        colors[i % colors.size].copy(alpha = edgeAlpha),
                                        colors[j % colors.size].copy(alpha = edgeAlpha * 0.5f)
                                    )
                                ),
                                start = a, end = b, strokeWidth = 1f
                            )
                        }
                    }
                }
            } else {
                for (i in 0..5) {
                    val angle = orbitAngle * PI / 180 + i * PI / 3
                    val r = w * 0.2f + i * 30f
                    val px = cx + cos(angle).toFloat() * r * 0.3f
                    val py = cy + sin(angle).toFloat() * r * 0.2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.04f * breathe), Color.Transparent),
                            center = Offset(px, py), radius = 40f
                        ),
                        radius = 40f, center = Offset(px, py)
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.06f * breathe), Color.Transparent),
                        center = Offset(cx, cy), radius = 60f
                    ),
                    radius = 60f, center = Offset(cx, cy)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Build Visualizer", style = MaterialTheme.typography.headlineSmall)
                    if (projectName.isNotEmpty()) {
                        Text(projectName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Architecture flow & construction progress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val orbCx = size.width / 2
                        val orbCy = size.height / 2
                        val orbR = size.width / 2 - 4f
                        drawCircle(color = primaryColor.copy(alpha = 0.15f * breathe), radius = orbR, style = Stroke(1.5f))
                        val dotX = orbCx + cos(Math.toRadians(orbitAngle.toDouble())).toFloat() * orbR * 0.7f
                        val dotY = orbCy + sin(Math.toRadians(orbitAngle.toDouble())).toFloat() * orbR * 0.7f
                        drawCircle(color = primaryColor, radius = 3f, center = Offset(dotX, dotY))
                        drawCircle(color = primaryColor.copy(alpha = 0.7f), radius = 3f, center = Offset(orbCx, orbCy))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!loaded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.AccountTree, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No active build", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Architecture flow diagrams appear here as MiMo constructs your app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                segments.forEach { seg ->
                    SegmentCard(seg, pulse)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun SegmentCard(seg: BuildSegment, pulse: Float) {
    val color = when (seg.status) {
        SegmentStatus.COMPLETE -> MaterialTheme.colorScheme.primary
        SegmentStatus.BUILDING -> MaterialTheme.colorScheme.tertiary
        SegmentStatus.ERROR -> MaterialTheme.colorScheme.error
        SegmentStatus.PENDING -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(22.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = if (seg.status == SegmentStatus.BUILDING) pulse * 0.2f else 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when (seg.status) {
                            SegmentStatus.COMPLETE -> Icon(
                                Icons.Filled.CheckCircle, null,
                                tint = color, modifier = Modifier.size(14.dp)
                            )
                            SegmentStatus.BUILDING -> CircularProgressIndicator(
                                modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = color
                            )
                            SegmentStatus.ERROR -> Icon(
                                Icons.Filled.Error, null,
                                tint = color, modifier = Modifier.size(14.dp)
                            )
                            SegmentStatus.PENDING -> Box(
                                modifier = Modifier.size(5.dp).clip(CircleShape).background(color.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(seg.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                    Text(seg.platform, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Text(
                    when (seg.status) {
                        SegmentStatus.COMPLETE -> "Done"
                        SegmentStatus.BUILDING -> "${(seg.progress * 100).toInt()}%"
                        SegmentStatus.ERROR -> "Error"
                        SegmentStatus.PENDING -> "Queue"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (seg.status == SegmentStatus.BUILDING) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { seg.progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f)
                )
            }

            if (seg.files.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                seg.files.take(5).forEach { file ->
                    Text(
                        file,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
