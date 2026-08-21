package com.example.audio

import android.content.Context
import android.content.SharedPreferences
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "TtsManager"
private const val PREFS_TTS = "tts_preferences"
private const val KEY_VOICE_PROFILE = "selected_voice_profile_id"

enum class VoiceGender {
    MASCULINE,
    FEMININE
}

data class VoiceProfile(
    val id: String,
    val name: String,
    val subtitle: String,
    val gender: VoiceGender,
    val pitch: Float,
    val speechRate: Float,
    val sampleText: String = "Olá! Esta é uma demonstração da minha voz no Olho do Dumbo."
)

object VoicePresets {
    val MASCULINE_1 = VoiceProfile(
        id = "masc_1",
        name = "Masculino 1",
        subtitle = "Grave, Firme & Autoritário",
        gender = VoiceGender.MASCULINE,
        pitch = 0.82f,
        speechRate = 0.96f
    )

    val MASCULINE_2 = VoiceProfile(
        id = "masc_2",
        name = "Masculino 2",
        subtitle = "Natural, Fluido & Dinâmico",
        gender = VoiceGender.MASCULINE,
        pitch = 1.05f,
        speechRate = 1.06f
    )

    val FEMININE_1 = VoiceProfile(
        id = "fem_1",
        name = "Feminino 1",
        subtitle = "Clara, Suave & Melódica",
        gender = VoiceGender.FEMININE,
        pitch = 1.25f,
        speechRate = 1.00f
    )

    val FEMININE_2 = VoiceProfile(
        id = "fem_2",
        name = "Feminino 2",
        subtitle = "Jovial, Expressiva & Rápida",
        gender = VoiceGender.FEMININE,
        pitch = 1.45f,
        speechRate = 1.15f
    )

    val ALL = listOf(MASCULINE_1, MASCULINE_2, FEMININE_1, FEMININE_2)

    fun findById(id: String): VoiceProfile {
        return ALL.firstOrNull { it.id == id } ?: FEMININE_1
    }
}

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = null
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_TTS, Context.MODE_PRIVATE)

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingText = MutableStateFlow<String?>(null)
    val speakingText: StateFlow<String?> = _speakingText.asStateFlow()

    private val _currentProfile = MutableStateFlow(VoicePresets.findById(prefs.getString(KEY_VOICE_PROFILE, "fem_1") ?: "fem_1"))
    val currentProfile: StateFlow<VoiceProfile> = _currentProfile.asStateFlow()

    private var onCompletionCallback: (() -> Unit)? = null

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
            applyVoiceProfile(_currentProfile.value)

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _speakingText.value = null
                    onCompletionCallback?.invoke()
                    onCompletionCallback = null
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _speakingText.value = null
                    onCompletionCallback?.invoke()
                    onCompletionCallback = null
                }
            })
            Log.d(TAG, "TextToSpeech successfully initialized with profile: ${_currentProfile.value.name}")
        } else {
            Log.e(TAG, "TextToSpeech init failed with status: $status")
        }
    }

    fun setVoiceProfile(profile: VoiceProfile) {
        _currentProfile.value = profile
        prefs.edit().putString(KEY_VOICE_PROFILE, profile.id).apply()
        applyVoiceProfile(profile)
    }

    private fun applyVoiceProfile(profile: VoiceProfile) {
        textToSpeech?.let { tts ->
            tts.setPitch(profile.pitch)
            tts.setSpeechRate(profile.speechRate)

            // Attempt to pick gender-matching system voice if available
            try {
                val availableVoices = tts.voices
                if (!availableVoices.isNullOrEmpty()) {
                    val matchingVoice = availableVoices.firstOrNull { voice ->
                        val vName = voice.name.lowercase()
                        when (profile.gender) {
                            VoiceGender.MASCULINE -> vName.contains("male") && !vName.contains("female")
                            VoiceGender.FEMININE -> vName.contains("female") || vName.contains("fem")
                        }
                    }
                    if (matchingVoice != null) {
                        tts.voice = matchingVoice
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "System voice selection fallback: ${e.message}")
            }
        }
    }

    fun playDemo(profile: VoiceProfile) {
        speak(
            text = profile.sampleText,
            langCode = "pt-PT",
            overrideProfile = profile
        )
    }

    fun speak(
        text: String,
        langCode: String,
        overrideProfile: VoiceProfile? = null,
        onDone: (() -> Unit)? = null
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            onDone?.invoke()
            return
        }

        if (!_isInitialized.value || textToSpeech == null) {
            Log.w(TAG, "TextToSpeech not yet initialized, re-trying initialization")
            textToSpeech = TextToSpeech(context.applicationContext, this)
            onDone?.invoke()
            return
        }

        this.onCompletionCallback = onDone

        val activeProfile = overrideProfile ?: _currentProfile.value
        applyVoiceProfile(activeProfile)

        val locale = mapCodeToLocale(langCode)
        try {
            val res = textToSpeech?.setLanguage(locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
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
        onCompletionCallback = null
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
