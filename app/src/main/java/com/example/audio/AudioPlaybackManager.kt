package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "AudioPlaybackManager"

data class PlaybackInfo(
    val recordId: Long? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val speed: Float = 1.0f,
    val isCompleted: Boolean = false
)

class AudioPlaybackManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingId: Long? = null
    private var currentSpeed: Float = 1.0f
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playbackState = MutableStateFlow(PlaybackInfo())
    val playbackState: StateFlow<PlaybackInfo> = _playbackState.asStateFlow()

    fun play(recordId: Long, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "Audio file does not exist: $filePath")
            return
        }

        // If it's already the same recording and paused, simply resume
        if (currentPlayingId == recordId && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                pause()
                return
            } else {
                mediaPlayer?.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true, isCompleted = false)
                startProgressTracker()
                return
            }
        }

        // Otherwise stop previous and play new
        stop()

        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = playbackParams.setSpeed(currentSpeed)
                }
                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = duration.toLong(),
                        isCompleted = true
                    )
                    progressJob?.cancel()
                }
                start()
            }

            mediaPlayer = player
            currentPlayingId = recordId

            _playbackState.value = PlaybackInfo(
                recordId = recordId,
                isPlaying = true,
                currentPositionMs = 0L,
                totalDurationMs = player.duration.toLong(),
                speed = currentSpeed,
                isCompleted = false
            )

            startProgressTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file", e)
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
            progressJob?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audio", e)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _playbackState.value = _playbackState.value.copy(
                currentPositionMs = positionMs,
                isCompleted = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio", e)
        }
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null) {
            try {
                val params = mediaPlayer?.playbackParams ?: PlaybackParams()
                params.speed = speed
                mediaPlayer?.playbackParams = params
            } catch (e: Exception) {
                Log.e(TAG, "Error changing playback speed", e)
            }
        }
        _playbackState.value = _playbackState.value.copy(speed = speed)
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        currentPlayingId = null
        _playbackState.value = PlaybackInfo(speed = currentSpeed)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val current = try {
                    mediaPlayer?.currentPosition?.toLong() ?: 0L
                } catch (e: Exception) {
                    0L
                }
                val total = try {
                    mediaPlayer?.duration?.toLong() ?: 0L
                } catch (e: Exception) {
                    0L
                }

                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = current,
                    totalDurationMs = total
                )
                delay(100)
            }
        }
    }
}
