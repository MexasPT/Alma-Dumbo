package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.LavenderOnContainer
import com.example.ui.theme.ListeningCoral
import com.example.ui.theme.SophisticatedOutline
import kotlin.math.sin

@Composable
fun LiveAudioWaveform(
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f

        val barCount = 32
        val barSpacing = 4.dp.toPx()
        val totalBarWidth = (width - (barCount - 1) * barSpacing) / barCount
        val barWidth = totalBarWidth.coerceAtLeast(3.dp.toPx())

        val recentAmps = if (amplitudes.isNotEmpty()) {
            amplitudes.takeLast(barCount)
        } else {
            emptyList()
        }

        for (i in 0 until barCount) {
            val rawAmp = if (i < recentAmps.size) recentAmps[i] else 0.06f
            val baseAmp = if (isRecording) {
                (rawAmp * pulse + 0.08f * sin((i.toDouble() / 4.0) + System.currentTimeMillis() / 280.0).toFloat())
                    .coerceIn(0.08f, 1.0f)
            } else {
                0.1f
            }

            val barHeight = (canvasHeight * 0.85f * baseAmp).coerceAtLeast(6.dp.toPx())
            val x = i * (barWidth + barSpacing)
            val top = centerY - (barHeight / 2f)

            // Sophisticated Dark gradient: Lavender to Soft Violet
            val barBrush = Brush.verticalGradient(
                colors = if (isRecording) {
                    listOf(
                        LavenderPrimary,
                        LavenderOnContainer.copy(alpha = 0.8f),
                        LavenderPrimary.copy(alpha = 0.5f)
                    )
                } else {
                    listOf(
                        LavenderPrimary.copy(alpha = 0.35f),
                        SophisticatedOutline.copy(alpha = 0.4f)
                    )
                },
                startY = top,
                endY = top + barHeight
            )

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

@Composable
fun RecordingPulseRing(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    if (!isRecording) return

    val infiniteTransition = rememberInfiniteTransition(label = "ring_anim")
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = (this.size.minDimension / 2) * 0.65f

            // Outer ring 2 (Lavender Glow)
            drawCircle(
                color = LavenderPrimary.copy(alpha = alpha2),
                radius = baseRadius * scale2,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner ring 1 (Lavender/Coral Blend)
            drawCircle(
                color = ListeningCoral.copy(alpha = alpha1),
                radius = baseRadius * scale1,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}
