package com.example.hyperlocal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint()
    private val matches = mutableListOf<MatchResult>()

    fun updateMatches(newMatches: List<MatchResult>) {
        matches.clear()
        matches.addAll(newMatches)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f

        val spacing = 50f
        matches.forEachIndexed { index, match ->
            paint.color = when (match.colorCode) {
                "Green" -> Color.GREEN
                "Yellow" -> Color.YELLOW
                else -> Color.GRAY
            }

            val angle = Math.toRadians((index * 360.0 / matches.size))
            val radius = spacing * (index + 1)
            val x = (centerX + radius * cos(angle)).toFloat()
            val y = (centerY + radius * sin(angle)).toFloat()

            canvas.drawCircle(x, y, 20f, paint)
        }
    }
}
