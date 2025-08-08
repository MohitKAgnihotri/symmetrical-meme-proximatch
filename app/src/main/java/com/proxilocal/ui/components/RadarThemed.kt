package com.proxilocal.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.proxilocal.hyperlocal.Gender
import com.proxilocal.hyperlocal.MatchResult
import kotlin.math.min

enum class RingStyle { SOLID, DASHED, GRADIENT }

data class RadarTheme(
    val bgGradient: Pair<Color, Color>,
    val ringStyle: RingStyle,
    val ringPrimary: Color,
    val ringAccent: Color,
    val sweepColor: Color,
    val sweepWidth: Float,
    val labelTextStyle: TextStyle,
    val circleCount: Int = 4,
    val dotRadiusDp: Dp = 8.dp,
    val maleColor: Color = Color(0xFF03A9F4),
    val femaleColor: Color = Color(0xFFE91E63),
    val privateColor: Color = Color.Gray,
    val rainbowColors: List<Color> = listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta)
)

object ThemeProvider {
    val NeonTech = RadarTheme(
        bgGradient = Color(0xFF1B1F3B) to Color(0xFF101229),
        ringStyle = RingStyle.GRADIENT,
        ringPrimary = Color.Cyan,
        ringAccent = Color.Green,
        sweepColor = Color.Cyan.copy(alpha = 0.5f),
        sweepWidth = 60f,
        labelTextStyle = TextStyle(color = Color.White)
    )
    val CorporatePulse = RadarTheme(
        bgGradient = Color.White to Color(0xFFEEEEEE),
        ringStyle = RingStyle.GRADIENT,
        ringPrimary = Color(0xFF333333),
        ringAccent = Color(0xFF7E57C2),
        sweepColor = Color(0xFF7E57C2).copy(alpha = 0.5f),
        sweepWidth = 30f,
        labelTextStyle = TextStyle(color = Color.DarkGray)
    )
}

/** Pure visual radar renderer. Touch handling belongs to a parent container. */
@Composable
fun ThemedRadarCanvas(
    theme: RadarTheme,
    matches: List<MatchResult>,
    isSweeping: Boolean,
    pingingMatchId: String?,
    onPingCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val dotRadiusPx = with(density) { theme.dotRadiusDp.toPx() }

    BoxWithConstraints(modifier = modifier) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val center = Offset(w / 2f, h / 2f)
        val maxRadius = min(w, h) / 2f

        val infiniteTransition = rememberInfiniteTransition(label = "RadarAnimations")
        val sweepAngle by if (isSweeping) {
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "SweepAngle"
            )
        } else remember { mutableFloatStateOf(0f) }

        val gridPulse by if (isSweeping) {
            infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "GridPulse"
            )
        } else remember { mutableFloatStateOf(0.1f) }

        val sweepColor = if (isSweeping) theme.sweepColor else Color.Transparent

        // Background rings + sweep
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isSweeping) {
                (1..theme.circleCount).forEach { i ->
                    drawCircle(
                        color = theme.ringPrimary.copy(alpha = gridPulse),
                        center = center,
                        radius = maxRadius * i / theme.circleCount,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        sweepColor.copy(alpha = 0.1f),
                        sweepColor.copy(alpha = 0.4f)
                    ),
                    center = center
                ),
                startAngle = sweepAngle,
                sweepAngle = theme.sweepWidth,
                useCenter = true
            )
        }

        // Compute & cache dot positions once
        val positions = remember(matches, w, h, dotRadiusPx) {
            if (w <= 1f || h <= 1f) {
                // Too early, layout not ready — keep everything centered
                matches.associate { it.id to androidx.compose.ui.geometry.Offset(w / 2f, h / 2f) }
            } else {
                DotLayout.computePositions(context, matches, w, h, dotRadiusPx)
            }
        }

        // Fade dots after they age out
        val now by produceState(System.currentTimeMillis()) {
            while (true) {
                value = System.currentTimeMillis()
                kotlinx.coroutines.delay(250)
            }
        }
        val TIMEOUT_MS = 5_000L
        val FADE_MS = 1_000L

        androidx.compose.ui.layout.Layout(
            content = {
                matches.forEach { match ->
                    val age = now - match.lastSeen
                    val alphaFactor = when {
                        age <= TIMEOUT_MS -> 1f
                        age in (TIMEOUT_MS + 1)..(TIMEOUT_MS + FADE_MS) ->
                            1f - ((age - TIMEOUT_MS).toFloat() / FADE_MS.toFloat())
                        else -> 0f
                    }
                    MatchDot(
                        theme = theme,
                        match = match,
                        isPinging = match.id == pingingMatchId,
                        onPingCompleted = onPingCompleted,
                        alphaFactor = alphaFactor
                    )
                }
            }
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEachIndexed { index, placeable ->
                    val match = matches[index]
                    val pos = positions[match.id]
                    if (pos != null) {
                        placeable.placeRelative(
                            x = (pos.x - placeable.width / 2).toInt(),
                            y = (pos.y - placeable.height / 2).toInt()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchDot(
    theme: RadarTheme,
    match: MatchResult,
    isPinging: Boolean,
    onPingCompleted: () -> Unit,
    alphaFactor: Float
) {
    val dotRadiusPx = with(LocalDensity.current) { theme.dotRadiusDp.toPx() }

    var hasAppeared by remember { mutableStateOf(false) }
    val animatedDotRadius by animateFloatAsState(
        targetValue = if (hasAppeared) dotRadiusPx else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 50f),
        label = "AnimateFromCenter"
    )
    LaunchedEffect(Unit) { hasAppeared = true }

    var pingTrigger by remember { mutableStateOf(false) }
    val pingTransition = updateTransition(targetState = pingTrigger, label = "PingTransition")
    val pingRadius by pingTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 400, easing = LinearOutSlowInEasing) },
        label = "PingRadius"
    ) { if (it) dotRadiusPx * 4 else dotRadiusPx }
    val pingAlpha by pingTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 400, easing = LinearOutSlowInEasing) },
        label = "PingAlpha"
    ) { if (it) 0f else 0.8f }

    LaunchedEffect(isPinging) {
        if (isPinging) {
            pingTrigger = !pingTrigger
            onPingCompleted()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "DotPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.size(theme.dotRadiusDp * 8)) {
        if (alphaFactor <= 0f) return@Canvas

        val dotCenter = center

        // Ping ripple
        if (pingTrigger || pingTransition.isRunning) {
            drawCircle(
                color = theme.ringPrimary.copy(alpha = pingAlpha * alphaFactor),
                radius = pingRadius,
                center = dotCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Glow for strong matches
        if (match.matchPercentage > 85) {
            val baseGlowColor = when (match.gender) {
                com.proxilocal.hyperlocal.Gender.MALE -> theme.maleColor
                com.proxilocal.hyperlocal.Gender.FEMALE -> theme.femaleColor
                com.proxilocal.hyperlocal.Gender.PRIVATE -> theme.privateColor
                com.proxilocal.hyperlocal.Gender.LGBTQ_PLUS -> theme.rainbowColors.first()
            }
            val glowColor = lerp(baseGlowColor.copy(alpha = 0.4f), baseGlowColor, match.matchPercentage / 100f)
            drawCircle(
                color = glowColor.copy(alpha = pulseAlpha * alphaFactor),
                center = dotCenter,
                radius = dotRadiusPx * 2.0f
            )
        }

        // Main dot
        val baseDotColor = when (match.gender) {
            com.proxilocal.hyperlocal.Gender.MALE -> theme.maleColor
            com.proxilocal.hyperlocal.Gender.FEMALE -> theme.femaleColor
            com.proxilocal.hyperlocal.Gender.PRIVATE -> theme.privateColor
            com.proxilocal.hyperlocal.Gender.LGBTQ_PLUS -> Color.Transparent
        }
        val paleColor = baseDotColor.copy(alpha = 0.4f * alphaFactor)
        val matchColor = lerp(paleColor, baseDotColor.copy(alpha = alphaFactor), match.matchPercentage / 100f)

        if (match.gender == com.proxilocal.hyperlocal.Gender.LGBTQ_PLUS) {
            drawCircle(
                brush = Brush.sweepGradient(theme.rainbowColors, center = dotCenter),
                center = dotCenter,
                radius = animatedDotRadius
            )
        } else {
            drawCircle(
                color = matchColor,
                center = dotCenter,
                radius = animatedDotRadius
            )
        }
    }
}
