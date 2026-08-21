package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "TtsManager"

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = null
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingText = MutableStateFlow<String?>(null)
    val speakingText: StateFlow<String?> = _speakingText.asStateFlow()

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error instantiating TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _isInitialized.value = true
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _speakingText.value = null
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _speakingText.value = null
                }
            })
            Log.d(TAG, "TextToSpeech successfully initialized")
        } else {
            Log.e(TAG, "TextToSpeech init failed with status: $status")
        }
    }

    fun speak(text: String, langCode: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        if (!_isInitialized.value || textToSpeech == null) {
            Log.w(TAG, "TextToSpeech not yet initialized, re-trying initialization")
            textToSpeech = TextToSpeech(context.applicationContext, this)
            return
        }

        val locale = mapCodeToLocale(langCode)
        try {
            val res = textToSpeech?.setLanguage(locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default or English/Portuguese if unavailable
                textToSpeech?.language = Locale.getDefault()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting language $langCode: ${e.message}")
        }

        _speakingText.value = cleanText
        val utteranceId = "tts_${System.currentTimeMillis()}"
        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        _isSpeaking.value = false
        _speakingText.value = null
    }

    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
        textToSpeech = null
        _isInitialized.value = false
    }

    private fun mapCodeToLocale(code: String): Locale {
        val clean = code.trim().lowercase().split("-").first()
        return when (clean) {
            "pt" -> Locale("pt", "PT")
            "es" -> Locale("es", "ES")
            "fr" -> Locale("fr", "FR")
            "en" -> Locale("en", "US")
            "de" -> Locale("de", "DE")
            "it" -> Locale("it", "IT")
            "nl" -> Locale("nl", "NL")
            "hr" -> Locale("hr", "HR")
            "sq" -> Locale("sq", "AL")
            "da" -> Locale("da", "DK")
            "fi" -> Locale("fi", "FI")
            "ber" -> Locale("ar", "MA")
            "ur" -> Locale("ur", "PK")
            "hi" -> Locale("hi", "IN")
            "bn" -> Locale("bn", "BD")
            "ar" -> Locale("ar", "SA")
            "uk" -> Locale("uk", "UA")
            "mo", "ro" -> Locale("ro", "RO")
            "kmb" -> Locale("pt", "AO")
            "ko" -> Locale("ko", "KR")
            "ja" -> Locale("ja", "JP")
            "th" -> Locale("th", "TH")
            "vi" -> Locale("vi", "VN")
            "zh" -> Locale("zh", "CN")
            "ru" -> Locale("ru", "RU")
            else -> Locale(clean)
        }
    }
}
