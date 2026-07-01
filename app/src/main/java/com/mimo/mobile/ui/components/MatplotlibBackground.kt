package com.mimo.mobile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun MatplotlibBackground(
    modifier: Modifier = Modifier,
    isProcessing: Boolean = false,
    messageCount: Int = 0
) {
    val transition = rememberInfiniteTransition(label = "bg")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val pulse by transition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(if (isProcessing) 600 else 1500), RepeatMode.Reverse), label = "pulse")

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val maxR = minOf(w, h) * 0.45f
        val time = t * 360f

        // 4 data lines
        val colors = intArrayOf(
            0xFF7C4DFF.toInt(), 0xFF00E5FF.toInt(), 0xFFFF4081.toInt(),
            0xFF69F0AE.toInt(), 0xFFFFAB40.toInt(), 0xFFE040FB.toInt()
        )
        val lineCount = 6
        for (li in 0 until lineCount) {
            val freq = 1.5f + li * 0.8f
            val amp = 0.08f + li * 0.015f
            val baseY = 0.3f + li * 0.08f
            val phase = li * 1.2f
            val speed = 0.7f + li * 0.15f
            val color = Color(colors[li])

            val path = Path()
            var started = false
            for (i in 0..50) {
                val px = i / 50f
                val y = baseY + amp * sin(freq * px * PI.toFloat() * 2f + time * speed * 0.01f + phase) +
                        amp * 0.3f * cos(freq * 1.7f * px * PI.toFloat() * 2f + phase * 0.6f)
                val sx = 40f + px * (w - 80f)
                val sy = 30f + y.coerceIn(0.05f, 0.95f) * (h - 60f)
                if (!started) { path.moveTo(sx, sy); started = true } else path.lineTo(sx, sy)
            }
            drawPath(path, color.copy(alpha = 0.12f * pulse), style = Stroke(2.dp.toPx()))
            drawPath(path, color.copy(alpha = 0.35f * pulse), style = Stroke(0.8.dp.toPx()))
        }

        // 5 floating dots
        for (i in 0 until 5) {
            val angle = (time * 0.3f + i * 72f) * PI.toFloat() / 180f
            val r = maxR * (0.3f + 0.15f * sin(i * 1.5f + time * 0.02f))
            val x = cx + cos(angle) * r
            val y = cy + sin(angle) * r * 0.6f
            val dotColor = Color(colors[i % colors.size])
            drawCircle(dotColor.copy(alpha = 0.2f * pulse), 3.dp.toPx(), Offset(x, y))
            drawCircle(dotColor.copy(alpha = 0.08f * pulse), 8.dp.toPx(), Offset(x, y))
        }

        // Scan line
        val scanX = 40f + (t % 1f) * (w - 80f)
        drawLine(Color(0xFF7C4DFF).copy(alpha = 0.06f * pulse), Offset(scanX, 30f), Offset(scanX, h - 30f), 0.5f)
    }
}
