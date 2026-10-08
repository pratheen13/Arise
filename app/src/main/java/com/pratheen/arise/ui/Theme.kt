package com.pratheen.arise.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pratheen.arise.R
import kotlin.math.sin
import kotlin.random.Random

/** The reference app's own colours, pulled from its shipped stylesheet. */
object Ink {
    val Void = Color(0xFF08060E)
    val Plum = Color(0xFF392449)
    val Violet = Color(0xFFA944EF)
    val VioletDim = Color(0xFF6B2FA0)
    val Mint = Color(0xFF7CE9C8)
    val TextBright = Color(0xFFF5F0FF)
    val TextDim = Color(0xFF9C8FB5)
    val Panel = Color(0xE6160C24)
    val Danger = Color(0xFFFF6B81)
}

private data class Particle(val x: Float, val delay: Float, val size: Float)

/**
 * The character stage: hunter art with two counter-pulsing aura rings and rising particles,
 * matching the reference app's `.character-stage` / `.aura-ring` / `.particles` composition.
 */
@Composable
fun CharacterStage(modifier: Modifier = Modifier) {
    val particles = remember { List(20) { Particle(x = (it * 37 % 100) / 100f, delay = it * -0.71f, size = 2f + it % 3) } }
    val t = rememberInfiniteTransition(label = "stage")
    val auraA by t.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "auraA")
    val auraB by t.animateFloat(0.55f, 0.25f, infiniteRepeatable(tween(4200), RepeatMode.Reverse), label = "auraB")
    val rise by t.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "rise")

    Box(modifier, contentAlignment = Alignment.Center) {
        // Aura rings
        Box(
            Modifier.fillMaxWidth(0.7f).aspectRatio(1f).blur(30.dp)
                .background(Brush.radialGradient(listOf(Ink.Violet.copy(alpha = auraA * 0.4f), Color.Transparent)))
        )
        Box(
            Modifier.fillMaxWidth(0.5f).aspectRatio(1f).blur(20.dp)
                .background(Brush.radialGradient(listOf(Ink.Mint.copy(alpha = auraB * 0.35f), Color.Transparent)))
        )

        // Rising particles
        Canvas(Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val phase = ((rise + p.delay) % 1f + 1f) % 1f
                val y = size.height * (1f - phase)
                val alpha = (sin(phase * Math.PI).toFloat()).coerceIn(0f, 1f)
                val cx = size.width * p.x + sin(phase * 6.28f + p.x * 10) * 8f
                drawCircle(
                    color = (if (p.size.toInt() % 2 == 0) Ink.Violet else Ink.Mint).copy(alpha = alpha * 0.8f),
                    radius = p.size,
                    center = Offset(cx, y)
                )
            }
        }

        Image(
            painter = painterResource(R.drawable.hero),
            contentDescription = "Hunter",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(0.68f).aspectRatio(0.5f)
        )
    }
}

@Composable
fun rememberFloat(range: ClosedFloatingPointRange<Float>, durationMs: Int, label: String): State<Float> {
    val t = rememberInfiniteTransition(label = label)
    return t.animateFloat(range.start, range.endInclusive, infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = label)
}
