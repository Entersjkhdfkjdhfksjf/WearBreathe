package com.example.breathe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.util.lerp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private val Cyan = Color(0xFF4DB4FF)
private const val DOTS = 96
private const val GOLDEN_ANGLE = 2.3999632f // radians (~137.5 deg): sunflower / phyllotaxis spiral

private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * progress: 0 = collapsed glowing white orb (end of exhale), 1 = fully bloomed blue flower (top of inhale).
 * Read lazily so only the draw phase is invalidated each frame, not the whole composition.
 */
@Composable
fun BloomCanvas(progress: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val p = progress()
        val c = Offset(size.width / 2f, size.height / 2f)
        val rMax = min(size.width, size.height) / 2f

        // Keep a hint of spread at p=0 so the orb has the slightly lumpy shape of the original.
        val e = lerp(0.06f, 1f, smoothstep(0f, 1f, p))
        val rotation = p * 0.9f
        val tint = colorLerp(Color.White, Cyan, smoothstep(0f, 0.55f, p))

        for (i in 0 until DOTS) {
            val rr = sqrt((i + 0.5f) / DOTS)                 // 0 at centre -> 1 at rim
            val a = i * GOLDEN_ANGLE + rotation
            val dist = rMax * 0.80f * e * rr
            val radius = rMax * (0.03f + 0.11f * rr) * (0.30f + 0.70f * e)
            drawCircle(
                color = tint,
                radius = radius,
                center = Offset(c.x + cos(a) * dist, c.y + sin(a) * dist),
                alpha = lerp(0.9f, 0.5f, rr),
                blendMode = BlendMode.Plus                    // overlaps glow brighter, like the rim in the video
            )
        }

        // Soft white halo that dominates when the flower is closed.
        val orbAlpha = 1f - smoothstep(0f, 0.35f, p)
        if (orbAlpha > 0f) {
            val orbR = rMax * 0.30f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = c,
                    radius = orbR
                ),
                radius = orbR,
                center = c,
                alpha = orbAlpha,
                blendMode = BlendMode.Plus
            )
        }
    }
}
