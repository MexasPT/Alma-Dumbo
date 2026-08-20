package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackInfo
import com.example.ui.theme.DeepPurpleOnPrimary
import com.example.ui.theme.LavenderContainer
import com.example.ui.theme.LavenderOnContainer
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.SophisticatedOutline
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AudioPlayerCard(
    recordId: Long,
    playbackInfo: PlaybackInfo,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isThisRecordPlaying = playbackInfo.recordId == recordId && playbackInfo.isPlaying
    val isThisRecordActive = playbackInfo.recordId == recordId
    val currentPosition = if (isThisRecordActive) playbackInfo.currentPositionMs else 0L
    val totalDuration = if (isThisRecordActive && playbackInfo.totalDurationMs > 0L) {
        playbackInfo.totalDurationMs
    } else {
        1000L
    }

    val progress = if (totalDuration > 0) {
        (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SophisticatedSurfaceVariant.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedOutline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Slider
            Slider(
                value = progress,
                onValueChange = { newProg ->
                    val targetMs = (newProg * totalDuration).toLong()
                    onSeek(targetMs)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .testTag("audio_slider_$recordId"),
                colors = SliderDefaults.colors(
                    thumbColor = LavenderPrimary,
                    activeTrackColor = LavenderPrimary,
                    inactiveTrackColor = SophisticatedOutline
                )
            )

            // Timestamps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = LavenderPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDuration(totalDuration),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentSpeed = playbackInfo.speed
                    val nextSpeed = when (currentSpeed) {
                        0.75f -> 1.0f
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 2.0f
                        else -> 1.0f
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(LavenderContainer)
                            .border(1.dp, SophisticatedOutline, RoundedCornerShape(10.dp))
                            .clickable { onSpeedChange(nextSpeed) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("speed_button_$recordId"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${currentSpeed}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = LavenderOnContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Main Playback Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val target = (currentPosition - 10000L).coerceAtLeast(0L)
                            onSeek(target)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SophisticatedSurface)
                            .border(1.dp, SophisticatedOutline, CircleShape)
                            .testTag("replay_10_$recordId")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Recuar 10 segundos",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("play_pause_button_$recordId"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = LavenderPrimary,
                            contentColor = DeepPurpleOnPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isThisRecordPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isThisRecordPlaying) "Pausar" else "Reproduzir",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val target = (currentPosition + 10000L).coerceAtMost(totalDuration)
                            onSeek(target)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SophisticatedSurface)
                            .border(1.dp, SophisticatedOutline, CircleShape)
                            .testTag("forward_10_$recordId")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Avançar 10 segundos",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(44.dp))
            }
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
