package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset // <-- FIX: Import Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke // <-- FIX: Import Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Custom modifier to create a "frosted glass" effect
fun Modifier.glassmorphism(
    blur: Dp = 10.dp,
    shadowRadius: Dp = 10.dp
): Modifier = this.then(
    drawBehind {
        drawIntoCanvas {
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            if (blur.toPx() > 0) {
                frameworkPaint.maskFilter = (BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL))
            }
            frameworkPaint.color = Color.Black.copy(alpha = 0.5f).toArgb()

            val spread = shadowRadius.toPx()
            val radius = size.height / 2 + spread

            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = radius,
                center = Offset(center.x, center.y - (size.height / 2)),
                style = Stroke(width = spread)
            )
        }
    }
)