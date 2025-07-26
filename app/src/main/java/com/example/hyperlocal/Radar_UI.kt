import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import com.example.hyperlocal.MatchResult


@Composable
fun RadarCanvas(matches: List<MatchResult>) {
    val centerX = 200f
    val centerY = 200f
    val spacing = 50f

    // Radar sweep animation state
    val sweepRadius = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            sweepRadius.snapTo(0f)
            sweepRadius.animateTo(
                targetValue = 300f,
                animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.Black)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Radar sweep
            drawCircle(
                color = Color(0x33FFFFFF), // translucent white
                radius = sweepRadius.value,
                center = Offset(centerX, centerY)
            )

            // Match dots
            matches.forEachIndexed { index, match ->
                val angle = Math.toRadians((index * 360.0 / matches.size))
                val radius = spacing * (index + 1)
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                val color = when (match.colorCode) {
                    "Green" -> Color.Green
                    "Yellow" -> Color.Yellow
                    else -> Color.Gray
                }
                drawCircle(color = color, radius = 20f, center = Offset(x, y))
            }
        }
    }
}
