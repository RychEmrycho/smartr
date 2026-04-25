package com.smartr.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun Sparkline(
    data: List<Int>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val strokePath = remember(data) { Path() }
    val fillPath = remember(data) { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        if (data.size > 1) {
            val spacing = width / (data.size - 1)
            val maxVal = data.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
            val minVal = data.minOrNull()?.toFloat() ?: 0f
            val range = (maxVal - minVal).coerceAtLeast(1f)

            val points = data.mapIndexed { index, value ->
                val x = index * spacing
                val normalizedY = (value - minVal) / range
                val y = height - (normalizedY * height)
                androidx.compose.ui.geometry.Offset(x, y)
            }

            strokePath.reset()
            strokePath.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                strokePath.lineTo(points[i].x, points[i].y)
            }

            fillPath.reset()
            fillPath.addPath(strokePath)
            fillPath.lineTo(points.last().x, height)
            fillPath.lineTo(points.first().x, height)
            fillPath.close()

            // Draw the filled area with a gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw the line
            drawPath(
                path = strokePath,
                color = color,
                style = Stroke(
                    width = 2.dp.toPx()
                )
            )
        }
    }
}
