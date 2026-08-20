package com.example.audio

import android.content.Context
import android.media.MediaRecorder
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

private const val TAG = "AudioRecorderManager"

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var startTimeMillis: Long = 0L
    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _amplitudeHistory = MutableStateFlow<List<Float>>(emptyList())
    val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory.asStateFlow()

    fun startRecording(): File? {
        try {
            stopRecording()

            val recordingsDir = File(context.filesDir, "recordings").apply {
                if (!exists()) mkdirs()
            }
            val fileName = "rec_${System.currentTimeMillis()}.m4a"
            val outputFile = File(recordingsDir, fileName)
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            startTimeMillis = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationMs.value = 0L
            _amplitudeHistory.value = emptyList()

            startAmplitudeTicker()
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            cleanUp()
            return null
        }
    }

    private fun startAmplitudeTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _isRecording.value) {
                val elapsed = System.currentTimeMillis() - startTimeMillis
                _recordingDurationMs.value = elapsed

                val maxAmp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }

                // Normalize maxAmp (0 to 32767) to 0.0f - 1.0f
                val normalized = (maxAmp / 32767f).coerceIn(0f, 1f)
                _amplitude.value = normalized

                val currentList = _amplitudeHistory.value.toMutableList()
                currentList.add(normalized)
                if (currentList.size > 50) {
                    currentList.removeAt(0)
                }
                _amplitudeHistory.value = currentList

                delay(75)
            }
        }
    }

    fun stopRecording(): RecordingResult? {
        tickerJob?.cancel()
        tickerJob = null

        val recorder = mediaRecorder ?: return null
        val file = currentOutputFile
        val duration = System.currentTimeMillis() - startTimeMillis

        return try {
            recorder.stop()
            recorder.release()
            mediaRecorder = null
            _isRecording.value = false
            _amplitude.value = 0f

            if (file != null && file.exists() && file.length() > 0) {
                RecordingResult(
                    file = file,
                    durationMs = duration.coerceAtLeast(100L),
                    fileSizeBytes = file.length()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            cleanUp()
            null
        }
    }

    fun cancelRecording() {
        tickerJob?.cancel()
        tickerJob = null
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {}
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        _isRecording.value = false
        _amplitude.value = 0f

        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null
    }

    private fun cleanUp() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        _isRecording.value = false
        _amplitude.value = 0f
    }
}

data class RecordingResult(
    val file: File,
    val durationMs: Long,
    val fileSizeBytes: Long
)
