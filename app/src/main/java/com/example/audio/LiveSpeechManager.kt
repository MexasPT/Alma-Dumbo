package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "LiveSpeechManager"

data class LiveTranscriptSegment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val detectedLang: String = "",
    val translationPt: String = ""
)

sealed interface LiveStatus {
    object Idle : LiveStatus
    object Listening : LiveStatus
    object Paused : LiveStatus
    data class Error(val message: String) : LiveStatus
}

class LiveSpeechManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _status = MutableStateFlow<LiveStatus>(LiveStatus.Idle)
    val status: StateFlow<LiveStatus> = _status.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _fullTranscript = MutableStateFlow("")
    val fullTranscript: StateFlow<String> = _fullTranscript.asStateFlow()

    private val _segments = MutableStateFlow<List<LiveTranscriptSegment>>(emptyList())
    val segments: StateFlow<List<LiveTranscriptSegment>> = _segments.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _activeLanguage = MutableStateFlow("pt")
    val activeLanguage: StateFlow<String> = _activeLanguage.asStateFlow()

    private val _detectedLanguageMeta = MutableStateFlow<LanguageMeta>(SupportedLanguages.ALL.first())
    val detectedLanguageMeta: StateFlow<LanguageMeta> = _detectedLanguageMeta.asStateFlow()

    private val _detectionConfidence = MutableStateFlow(0.96f)
    val detectionConfidence: StateFlow<Float> = _detectionConfidence.asStateFlow()

    private val _isAutoDetectActive = MutableStateFlow(true)
    val isAutoDetectActive: StateFlow<Boolean> = _isAutoDetectActive.asStateFlow()

    private var shouldKeepListening = false
    private var restartRunnable: Runnable? = null

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _status.value = LiveStatus.Error("Reconhecimento de voz não suportado neste dispositivo.")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SpeechRecognizer", e)
            _status.value = LiveStatus.Error("Erro ao inicializar motor de voz: ${e.message}")
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
                if (shouldKeepListening) {
                    _status.value = LiveStatus.Listening
                }
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _rmsLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
            }

            override fun onError(error: Int) {
                val errorMsg = getErrorMessage(error)
                Log.w(TAG, "Speech recognition error: $error ($errorMsg)")

                if (shouldKeepListening) {
                    // Automatically schedule a silent reconnect to keep stream alive
                    scheduleRestart(300)
                } else {
                    _status.value = LiveStatus.Idle
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0].trim()
                    if (text.isNotBlank()) {
                        processAutoDetection(text)
                        commitSegment(text)
                    }
                }
                _partialText.value = ""

                if (shouldKeepListening) {
                    scheduleRestart(150)
                } else {
                    _status.value = LiveStatus.Idle
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0].trim()
                    _partialText.value = text
                    if (text.length >= 3) {
                        processAutoDetection(text)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun processAutoDetection(text: String) {
        if (!_isAutoDetectActive.value || text.isBlank()) return

        val result = LanguageAutoDetector.detect(text)
        val meta = SupportedLanguages.findByCode(result.languageCode)
            ?: SupportedLanguages.findByNameOrCode(result.languageName)
            ?: SupportedLanguages.ALL.first()

        _detectedLanguageMeta.value = meta
        _detectionConfidence.value = result.confidence

        if (_activeLanguage.value != result.languageCode && result.confidence > 0.70f) {
            _activeLanguage.value = result.languageCode
            Log.d(TAG, "Auto-detected language changed to: ${meta.namePt} (${meta.flag}) with confidence: ${result.confidence}")
        }
    }

    fun startLiveListening(initialLangCode: String? = null) {
        if (initialLangCode != null) {
            _activeLanguage.value = initialLangCode
            val meta = SupportedLanguages.findByCode(initialLangCode) ?: SupportedLanguages.ALL.first()
            _detectedLanguageMeta.value = meta
        }
        shouldKeepListening = true
        _status.value = LiveStatus.Listening

        mainHandler.post {
            startListeningInternal(_activeLanguage.value)
        }
    }

    private fun startListeningInternal(langCode: String) {
        try {
            if (speechRecognizer == null) {
                initRecognizer()
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                // Use broad multi-language matching and fallback preference
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra("android.speech.extra.DICTATION_MODE", true)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "startListening error", e)
            _status.value = LiveStatus.Error("Erro ao iniciar escuta em direto: ${e.message}")
        }
    }

    fun pauseLiveListening() {
        shouldKeepListening = false
        cancelScheduledRestart()
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {}
            _status.value = LiveStatus.Paused
            _rmsLevel.value = 0f
            _partialText.value = ""
        }
    }

    fun stopLiveListening() {
        shouldKeepListening = false
        cancelScheduledRestart()
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {}
            _status.value = LiveStatus.Idle
            _rmsLevel.value = 0f
            _partialText.value = ""
        }
    }

    fun clearTranscript() {
        _fullTranscript.value = ""
        _partialText.value = ""
        _segments.value = emptyList()
    }

    fun setLanguage(langCode: String) {
        _activeLanguage.value = langCode
        if (shouldKeepListening) {
            pauseLiveListening()
            startLiveListening(langCode)
        }
    }

    private fun commitSegment(text: String) {
        val current = _fullTranscript.value
        val updated = if (current.isBlank()) text else "$current\n$text"
        _fullTranscript.value = updated

        val segment = LiveTranscriptSegment(
            text = text,
            timestampMs = System.currentTimeMillis()
        )
        _segments.value = _segments.value + segment
    }

    private fun scheduleRestart(delayMs: Long) {
        cancelScheduledRestart()
        restartRunnable = Runnable {
            if (shouldKeepListening) {
                startListeningInternal(_activeLanguage.value)
            }
        }
        mainHandler.postDelayed(restartRunnable!!, delayMs)
    }

    private fun cancelScheduledRestart() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
    }

    fun destroy() {
        shouldKeepListening = false
        cancelScheduledRestart()
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {}
        }
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Erro de áudio do microfone"
            SpeechRecognizer.ERROR_CLIENT -> "Erro do cliente de reconhecimento"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone em falta"
            SpeechRecognizer.ERROR_NETWORK -> "Erro de rede"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tempo limite de rede excedido"
            SpeechRecognizer.ERROR_NO_MATCH -> "Nenhuma voz detetada"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconhecedor ocupado"
            SpeechRecognizer.ERROR_SERVER -> "Erro do servidor de voz"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Silêncio prolongado"
            else -> "Código de erro: $errorCode"
        }
    }
}
