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
import kotlin.random.Random

private data class WaveLine(
    val color: Int,
    val freq: Float,
    val amp: Float,
    val baseY: Float,
    val phase: Float,
    val speed: Float,
    val thickness: Float
)

private data class FloatingDot(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Int,
    val size: Float
)

private data class GlowOrb(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Int,
    val phase: Float
)

@Composable
fun MatplotlibBackground(
    modifier: Modifier = Modifier,
    isProcessing: Boolean = false,
    messageCount: Int = 0
) {
    val transition = rememberInfiniteTransition(label = "bg")

    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )

    val pulse by transition.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(if (isProcessing) 400 else 1000), RepeatMode.Reverse),
        label = "pulse"
    )

    val breathe by transition.animateFloat(
        0.85f, 1f,
        infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "breathe"
    )

    val scanProgress by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "scan"
    )

    val waveLines = remember {
        listOf(
            WaveLine(0xFF6366F1.toInt(), 1.2f, 0.06f, 0.25f, 0f, 0.8f, 2.5f),
            WaveLine(0xFF8B5CF6.toInt(), 1.5f, 0.05f, 0.35f, 1.0f, 0.9f, 2f),
            WaveLine(0xFF06B6D4.toInt(), 1.8f, 0.04f, 0.45f, 2.0f, 1.0f, 1.8f),
            WaveLine(0xFF10B981.toInt(), 2.0f, 0.035f, 0.55f, 3.0f, 1.1f, 1.5f),
            WaveLine(0xFFF59E0B.toInt(), 2.3f, 0.03f, 0.65f, 4.0f, 1.2f, 1.3f),
            WaveLine(0xFFEC4899.toInt(), 2.5f, 0.025f, 0.75f, 5.0f, 1.3f, 1f),
        )
    }

    val dots = remember {
        Array(25) {
            FloatingDot(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.002f,
                vy = (Random.nextFloat() - 0.5f) * 0.002f,
                color = intArrayOf(0xFF6366F1.toInt(), 0xFF8B5CF6.toInt(), 0xFF06B6D4.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt())[Random.nextInt(5)],
                size = Random.nextFloat() * 2f + 1f
            )
        }
    }

    val orbs = remember {
        listOf(
            GlowOrb(0.2f, 0.3f, 80f, 0xFF6366F1.toInt(), 0f),
            GlowOrb(0.8f, 0.2f, 60f, 0xFF06B6D4.toInt(), 1.5f),
            GlowOrb(0.5f, 0.7f, 70f, 0xFF8B5CF6.toInt(), 3.0f),
            GlowOrb(0.15f, 0.8f, 50f, 0xFF10B981.toInt(), 4.5f),
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val time = t * 2f * PI.toFloat()

        // Deep dark gradient background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A0A1A),
                    Color(0xFF0F0F23),
                    Color(0xFF0A0A1A)
                )
            )
        )

        // Subtle grid
        val gridAlpha = 0.03f * breathe
        for (i in 0..20) {
            val x = w * i / 20f
            drawLine(Color(0xFF6366F1).copy(alpha = gridAlpha), Offset(x, 0f), Offset(x, h), 0.5f)
        }
        for (i in 0..30) {
            val y = h * i / 30f
            drawLine(Color(0xFF6366F1).copy(alpha = gridAlpha), Offset(0f, y), Offset(w, y), 0.5f)
        }

        // Glow orbs
        orbs.forEach { orb ->
            val ox = w * orb.x
            val oy = h * orb.y
            val pulseR = orb.radius * (0.8f + 0.2f * sin(time + orb.phase))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(orb.color).copy(alpha = 0.08f * pulse),
                        Color(orb.color).copy(alpha = 0.02f * pulse),
                        Color.Transparent
                    ),
                    center = Offset(ox, oy),
                    radius = pulseR * 2
                ),
                radius = pulseR * 2,
                center = Offset(ox, oy)
            )
        }

        // Wave lines
        waveLines.forEach { line ->
            val path = Path()
            var started = false
            for (i in 0..80) {
                val px = i / 80f
                val y = line.baseY + line.amp * sin(
                    line.freq * px * PI.toFloat() * 2f + time * line.speed + line.phase
                ) + line.amp * 0.4f * cos(
                    line.freq * 1.7f * px * PI.toFloat() * 2f + line.phase * 0.7f
                )
                val sx = 20f + px * (w - 40f)
                val sy = 20f + y.coerceIn(0.05f, 0.95f) * (h - 40f)
                if (!started) {
                    path.moveTo(sx, sy)
                    started = true
                } else {
                    path.lineTo(sx, sy)
                }
            }
            // Glow layer
            drawPath(
                path,
                Color(line.color).copy(alpha = 0.08f * pulse * breathe),
                style = Stroke(line.thickness * 3f)
            )
            // Main line
            drawPath(
                path,
                Color(line.color).copy(alpha = 0.25f * pulse * breathe),
                style = Stroke(line.thickness * 1.5f)
            )
            // Bright core
            drawPath(
                path,
                Color(line.color).copy(alpha = 0.5f * pulse * breathe),
                style = Stroke(line.thickness * 0.5f)
            )
        }

        // Floating particles
        dots.forEach { dot ->
            dot.x += dot.vx
            dot.y += dot.vy
            if (dot.x < 0f || dot.x > 1f) dot.vx *= -1f
            if (dot.y < 0f || dot.y > 1f) dot.vy *= -1f
            dot.x = dot.x.coerceIn(0f, 1f)
            dot.y = dot.y.coerceIn(0f, 1f)

            val dx = w * dot.x
            val dy = h * dot.y
            val dotPulse = (0.6f + 0.4f * sin(time * 2f + dot.x * 10f)) * pulse

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(dot.color).copy(alpha = 0.3f * dotPulse),
                        Color(dot.color).copy(alpha = 0.05f * dotPulse),
                        Color.Transparent
                    ),
                    center = Offset(dx, dy),
                    radius = dot.size * 4f
                ),
                radius = dot.size * 4f,
                center = Offset(dx, dy)
            )
            drawCircle(
                Color(dot.color).copy(alpha = 0.6f * dotPulse),
                dot.size.dp.toPx(),
                Offset(dx, dy)
            )
        }

        // Horizontal scan line
        val scanY = h * scanProgress
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF6366F1).copy(alpha = 0.1f * pulse),
                    Color(0xFF8B5CF6).copy(alpha = 0.15f * pulse),
                    Color(0xFF6366F1).copy(alpha = 0.1f * pulse),
                    Color.Transparent
                )
            ),
            start = Offset(0f, scanY),
            end = Offset(w, scanY),
            strokeWidth = 1.5f
        )

        // Vignette
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A0A1A).copy(alpha = 0.6f),
                    Color.Transparent,
                    Color.Transparent,
                    Color(0xFF0A0A1A).copy(alpha = 0.4f)
                ),
                startY = 0f,
                endY = h
            )
        )
    }
}
