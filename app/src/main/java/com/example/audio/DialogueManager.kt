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
import com.example.api.GeminiSpeechService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "DialogueManager"

enum class DialogueSpeaker {
    ME,       // Host speaking Portuguese
    PARTNER   // Foreign guest/partner
}

data class DialogueMessage(
    val id: String = UUID.randomUUID().toString(),
    val speaker: DialogueSpeaker,
    val originalText: String,
    val originalLangCode: String,
    val originalLangName: String,
    val originalFlag: String,
    val translatedText: String,
    val targetLangCode: String,
    val targetLangName: String,
    val targetFlag: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface DialogueListeningStatus {
    object Idle : DialogueListeningStatus
    object Listening : DialogueListeningStatus
    object Translating : DialogueListeningStatus
    data class Speaking(val text: String) : DialogueListeningStatus
    data class Error(val message: String) : DialogueListeningStatus
}

class DialogueManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val speechService: GeminiSpeechService,
    private val ttsManager: TtsManager
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _messages = MutableStateFlow<List<DialogueMessage>>(emptyList())
    val messages: StateFlow<List<DialogueMessage>> = _messages.asStateFlow()

    private val _status = MutableStateFlow<DialogueListeningStatus>(DialogueListeningStatus.Idle)
    val status: StateFlow<DialogueListeningStatus> = _status.asStateFlow()

    private val _currentPartnerLang = MutableStateFlow<LanguageMeta>(SupportedLanguages.findByCode("en-US") ?: SupportedLanguages.ALL.first())
    val currentPartnerLang: StateFlow<LanguageMeta> = _currentPartnerLang.asStateFlow()

    private val _isAutoSpeakEnabled = MutableStateFlow(true)
    val isAutoSpeakEnabled: StateFlow<Boolean> = _isAutoSpeakEnabled.asStateFlow()

    private val _liveSpokenText = MutableStateFlow("")
    val liveSpokenText: StateFlow<String> = _liveSpokenText.asStateFlow()

    private val _detectedSpeaker = MutableStateFlow(DialogueSpeaker.PARTNER)
    val detectedSpeaker: StateFlow<DialogueSpeaker> = _detectedSpeaker.asStateFlow()

    private var shouldKeepListening = false
    private var restartRunnable: Runnable? = null
    private var customApiKey: String? = null
    private var allowedLanguageCodes: Set<String>? = null

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    fun setAllowedLanguages(codes: Set<String>?) {
        allowedLanguageCodes = codes
    }

    fun setApiKeyOverride(key: String?) {
        customApiKey = key
    }

    fun toggleAutoSpeak() {
        _isAutoSpeakEnabled.value = !_isAutoSpeakEnabled.value
    }

    fun setPartnerLanguage(meta: LanguageMeta) {
        _currentPartnerLang.value = meta
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _status.value = DialogueListeningStatus.Error("Reconhecimento de voz não suportado.")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SpeechRecognizer", e)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (shouldKeepListening && _status.value !is DialogueListeningStatus.Translating && _status.value !is DialogueListeningStatus.Speaking) {
                    _status.value = DialogueListeningStatus.Listening
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (shouldKeepListening) {
                    scheduleRestart(350)
                } else {
                    _status.value = DialogueListeningStatus.Idle
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spoken = matches[0].trim()
                    if (spoken.isNotBlank()) {
                        processDialogueTurn(spoken)
                    }
                }
                _liveSpokenText.value = ""
                if (shouldKeepListening && _status.value !is DialogueListeningStatus.Translating && _status.value !is DialogueListeningStatus.Speaking) {
                    scheduleRestart(150)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val partial = matches[0].trim()
                    _liveSpokenText.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun processDialogueTurn(spokenText: String) {
        scope.launch(Dispatchers.IO) {
            _status.value = DialogueListeningStatus.Translating

            // 1. Language & Speaker Detection
            val detection = LanguageAutoDetector.detect(spokenText, allowedLanguageCodes)
            val isPortuguese = detection.languageCode.startsWith("pt", ignoreCase = true)

            val speaker: DialogueSpeaker
            val sourceLangCode: String
            val sourceLangMeta: LanguageMeta
            val targetLangCode: String
            val targetLangMeta: LanguageMeta

            if (isPortuguese) {
                // Portuguese user is speaking -> translate to the Partner's current language
                speaker = DialogueSpeaker.ME
                sourceLangCode = "pt"
                sourceLangMeta = SupportedLanguages.findByCode("pt-PT") ?: SupportedLanguages.ALL.first()
                targetLangMeta = _currentPartnerLang.value
                targetLangCode = targetLangMeta.code
            } else {
                // Partner is speaking a foreign language -> translate to Portuguese!
                speaker = DialogueSpeaker.PARTNER
                val detectedMeta = SupportedLanguages.findByCode(detection.languageCode)
                    ?: SupportedLanguages.findByNameOrCode(detection.languageName)
                    ?: _currentPartnerLang.value
                
                // Update Partner's detected language dynamically
                _currentPartnerLang.value = detectedMeta
                sourceLangMeta = detectedMeta
                sourceLangCode = detectedMeta.code
                targetLangMeta = SupportedLanguages.findByCode("pt-PT") ?: SupportedLanguages.ALL.first()
                targetLangCode = "pt"
            }

            _detectedSpeaker.value = speaker

            // 2. Perform lightning-fast translation
            val translationResult = speechService.translateText(
                text = spokenText,
                sourceLang = sourceLangCode,
                targetLang = targetLangCode,
                apiKeyOverride = customApiKey
            )

            val translated = translationResult.getOrDefault(spokenText)

            val newMessage = DialogueMessage(
                speaker = speaker,
                originalText = spokenText,
                originalLangCode = sourceLangCode,
                originalLangName = sourceLangMeta.namePt,
                originalFlag = sourceLangMeta.flag,
                translatedText = translated,
                targetLangCode = targetLangCode,
                targetLangName = targetLangMeta.namePt,
                targetFlag = targetLangMeta.flag
            )

            _messages.value = listOf(newMessage) + _messages.value

            // 3. Spoken Audio Playback in real-time
            if (_isAutoSpeakEnabled.value && translated.isNotBlank()) {
                _status.value = DialogueListeningStatus.Speaking(translated)
                ttsManager.speak(
                    text = translated,
                    langCode = targetLangCode,
                    onDone = {
                        if (shouldKeepListening) {
                            _status.value = DialogueListeningStatus.Listening
                            scheduleRestart(200)
                        } else {
                            _status.value = DialogueListeningStatus.Idle
                        }
                    }
                )
            } else {
                if (shouldKeepListening) {
                    _status.value = DialogueListeningStatus.Listening
                    scheduleRestart(200)
                } else {
                    _status.value = DialogueListeningStatus.Idle
                }
            }
        }
    }

    fun startListening() {
        shouldKeepListening = true
        _status.value = DialogueListeningStatus.Listening
        mainHandler.post {
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        try {
            if (speechRecognizer == null) {
                initRecognizer()
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting dialogue speech listening", e)
        }
    }

    fun stopListening() {
        shouldKeepListening = false
        cancelScheduledRestart()
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {}
            _status.value = DialogueListeningStatus.Idle
            _liveSpokenText.value = ""
        }
    }

    fun clearHistory() {
        _messages.value = emptyList()
        _liveSpokenText.value = ""
    }

    fun replayMessageAudio(msg: DialogueMessage) {
        ttsManager.speak(msg.translatedText, msg.targetLangCode)
    }

    private fun scheduleRestart(delayMs: Long) {
        cancelScheduledRestart()
        restartRunnable = Runnable {
            if (shouldKeepListening) {
                startListeningInternal()
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
}
