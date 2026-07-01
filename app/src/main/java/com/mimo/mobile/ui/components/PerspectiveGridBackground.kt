package com.mimo.mobile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*
import kotlin.random.Random

private data class GridBlock(
    val x: Float, val y: Float,
    val w: Float, val h: Float,
    val alpha: Float, val speed: Float,
    val phase: Float
)

private data class LightParticle(
    var x: Float, var y: Float,
    var speed: Float, var alpha: Float,
    var length: Float, var width: Float
)

@Composable
fun PerspectiveGridBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "grid")

    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "scroll"
    )

    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "shimmer"
    )

    val blocks = remember {
        List(18) {
            GridBlock(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.85f + 0.05f,
                w = Random.nextFloat() * 0.06f + 0.015f,
                h = Random.nextFloat() * 0.03f + 0.01f,
                alpha = Random.nextFloat() * 0.5f + 0.15f,
                speed = Random.nextFloat() * 0.3f + 0.1f,
                phase = Random.nextFloat() * 2f * PI.toFloat()
            )
        }
    }

    val particles = remember {
        Array(30) {
            LightParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.004f + 0.001f,
                alpha = Random.nextFloat() * 0.6f + 0.15f,
                length = Random.nextFloat() * 0.04f + 0.015f,
                width = Random.nextFloat() * 1.2f + 0.5f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Dark background
        drawRect(Color(0xFF0B0E17))

        // Vanishing point
        val vpX = w * 0.5f
        val vpY = h * 0.28f

        // Grid color
        val gridColor = Color(0xFF1A2744)
        val gridColorBright = Color(0xFF243556)

        // Horizontal grid lines (perspective)
        for (i in 1..14) {
            val t = i.toFloat() / 14f
            val y = vpY + (h - vpY) * t * t
            val spread = 0.1f + t * 0.9f
            val left = vpX - w * 0.8f * spread
            val right = vpX + w * 0.8f * spread
            val alpha = (0.15f + t * 0.25f) * shimmer * 0.5f
            drawLine(gridColorBright.copy(alpha = alpha), Offset(left, y), Offset(right, y), strokeWidth = 0.8f)
        }

        // Vertical grid lines (converge to vanishing point)
        for (i in -8..8) {
            val spread = i.toFloat() / 8f
            val bottomX = vpX + w * 0.7f * spread
            val alpha = (0.2f + (1f - abs(spread)) * 0.2f) * shimmer * 0.5f
            drawLine(gridColor.copy(alpha = alpha), Offset(vpX, vpY), Offset(bottomX, h), strokeWidth = 0.8f)
        }

        // Glowing cyan blocks on the grid
        blocks.forEach { block ->
            val bx = w * block.x
            val depth = block.y
            val by = vpY + (h - vpY) * depth * depth
            val perspectiveScale = 0.1f + depth * 0.9f
            val bw = w * block.w * perspectiveScale
            val bh = h * block.h * perspectiveScale
            val pulseAlpha = block.alpha * (0.6f + 0.4f * sin(scrollOffset * 2f * PI.toFloat() + block.phase))

            // Glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A3F8F).copy(alpha = pulseAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(bx, by),
                    radius = bw * 2f
                ),
                topLeft = Offset(bx - bw, by - bh),
                size = androidx.compose.ui.geometry.Size(bw * 2, bh * 2)
            )

            // Block rectangle
            drawRect(
                color = Color(0xFF1E4D8C).copy(alpha = pulseAlpha),
                topLeft = Offset(bx, by),
                size = androidx.compose.ui.geometry.Size(bw, bh)
            )

            // Top highlight
            drawLine(
                color = Color(0xFF2A5FA0).copy(alpha = pulseAlpha * 1.2f),
                start = Offset(bx, by),
                end = Offset(bx + bw, by),
                strokeWidth = 1.2f
            )
        }

        // Falling light particles
        particles.forEach { p ->
            p.y += p.speed
            if (p.y > 1.1f) {
                p.y = -0.1f
                p.x = Random.nextFloat()
                p.alpha = Random.nextFloat() * 0.6f + 0.15f
            }

            val px = w * p.x
            val startY = h * p.y
            val endY = startY + h * p.length
            val fadeIn = (p.y / 0.1f).coerceIn(0f, 1f)
            val fadeOut = ((1f - p.y) / 0.1f).coerceIn(0f, 1f)
            val finalAlpha = p.alpha * fadeIn * fadeOut

            // Light streak
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4FC3F7).copy(alpha = 0f),
                        Color(0xFF4FC3F7).copy(alpha = finalAlpha),
                        Color(0xFF81D4FA).copy(alpha = finalAlpha * 0.5f),
                        Color(0xFF4FC3F7).copy(alpha = 0f)
                    ),
                    startY = startY,
                    endY = endY
                ),
                start = Offset(px, startY),
                end = Offset(px, endY),
                strokeWidth = p.width
            )
        }

        // Subtle horizontal scan line
        val scanY = h * scrollOffset
        drawLine(
            color = Color(0xFF00D4FF).copy(alpha = 0.04f),
            start = Offset(0f, scanY),
            end = Offset(w, scanY),
            strokeWidth = 1f
        )

        // Vignette edges
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B0E17).copy(alpha = 0.7f),
                    Color.Transparent,
                    Color.Transparent,
                    Color(0xFF0B0E17).copy(alpha = 0.5f)
                ),
                startY = 0f,
                endY = h
            )
        )
    }
}
