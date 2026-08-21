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
    // 8 VOZES MASCULINAS DINÂMICAS E REAIS
    val MASCULINE_1 = VoiceProfile(
        id = "masc_1",
        name = "Masculino 1 (Rodrigo)",
        subtitle = "Barítono Noticioso • Firme & Confiante",
        gender = VoiceGender.MASCULINE,
        pitch = 0.85f,
        speechRate = 0.98f,
        sampleText = "Olá! Eu sou o Rodrigo. Esta é a minha voz masculina de barítono noticioso no Olho do Dumbo."
    )

    val MASCULINE_2 = VoiceProfile(
        id = "masc_2",
        name = "Masculino 2 (Tiago)",
        subtitle = "Conversacional Fluido • Natural & Dinâmico",
        gender = VoiceGender.MASCULINE,
        pitch = 0.92f,
        speechRate = 1.04f,
        sampleText = "Olá! Eu sou o Tiago. Esta é a minha voz masculina natural e dinâmica para conversação diária."
    )

    val MASCULINE_3 = VoiceProfile(
        id = "masc_3",
        name = "Masculino 3 (Afonso)",
        subtitle = "Narrador Solene • Muito Grave & Profundo",
        gender = VoiceGender.MASCULINE,
        pitch = 0.72f,
        speechRate = 0.88f,
        sampleText = "Olá! Eu sou o Afonso. Esta é a minha voz masculina solene, pausada e de tom profundo."
    )

    val MASCULINE_4 = VoiceProfile(
        id = "masc_4",
        name = "Masculino 4 (Diogo)",
        subtitle = "Executivo Ágil • Enérgico & Rápido",
        gender = VoiceGender.MASCULINE,
        pitch = 0.98f,
        speechRate = 1.14f,
        sampleText = "Olá! Eu sou o Diogo. Esta é a minha voz executiva, ágil, direta e confiante."
    )

    val MASCULINE_5 = VoiceProfile(
        id = "masc_5",
        name = "Masculino 5 (Miguel)",
        subtitle = "Caloroso & Empático • Tom Quente & Suave",
        gender = VoiceGender.MASCULINE,
        pitch = 0.88f,
        speechRate = 0.94f,
        sampleText = "Olá! Eu sou o Miguel. Esta é a minha voz masculina acolhedora, tranquila e empática."
    )

    val MASCULINE_6 = VoiceProfile(
        id = "masc_6",
        name = "Masculino 6 (Bernardo)",
        subtitle = "Locutor de Rádio & Podcast • Ressonante",
        gender = VoiceGender.MASCULINE,
        pitch = 0.78f,
        speechRate = 1.02f,
        sampleText = "Olá! Eu sou o Bernardo. Esta é a minha voz encorpada de rádio e locução profissional."
    )

    val MASCULINE_7 = VoiceProfile(
        id = "masc_7",
        name = "Masculino 7 (Gabriel)",
        subtitle = "Jovem & Espontâneo • Leve & Coloquial",
        gender = VoiceGender.MASCULINE,
        pitch = 1.04f,
        speechRate = 1.10f,
        sampleText = "Olá! Eu sou o Gabriel. Esta é a minha voz jovem, leve, descontraída e moderna."
    )

    val MASCULINE_8 = VoiceProfile(
        id = "masc_8",
        name = "Masculino 8 (Vasco)",
        subtitle = "Clássico Autoritário • Sério & Imponente",
        gender = VoiceGender.MASCULINE,
        pitch = 0.66f,
        speechRate = 0.85f,
        sampleText = "Olá! Eu sou o Vasco. Esta é a minha voz clássica de grande autoridade e solidez."
    )

    // 4 VOZES FEMININAS
    val FEMININE_1 = VoiceProfile(
        id = "fem_1",
        name = "Feminino 1 (Sofia)",
        subtitle = "Clara, Suave & Melódica",
        gender = VoiceGender.FEMININE,
        pitch = 1.15f,
        speechRate = 1.00f,
        sampleText = "Olá! Eu sou a Sofia. Esta é a minha voz feminina clara e melodiosa no Olho do Dumbo."
    )

    val FEMININE_2 = VoiceProfile(
        id = "fem_2",
        name = "Feminino 2 (Inês)",
        subtitle = "Jovial, Expressiva & Dinâmica",
        gender = VoiceGender.FEMININE,
        pitch = 1.35f,
        speechRate = 1.08f,
        sampleText = "Olá! Eu sou a Inês. Esta é a minha voz feminina jovial e enérgica."
    )

    val FEMININE_3 = VoiceProfile(
        id = "fem_3",
        name = "Feminino 3 (Beatriz)",
        subtitle = "Serena, Elegante & Pausada",
        gender = VoiceGender.FEMININE,
        pitch = 1.18f,
        speechRate = 0.92f,
        sampleText = "Olá! Eu sou a Beatriz. Esta é a minha voz feminina serena e elegante."
    )

    val FEMININE_4 = VoiceProfile(
        id = "fem_4",
        name = "Feminino 4 (Matilde)",
        subtitle = "Vibrante, Calorosa & Luminosa",
        gender = VoiceGender.FEMININE,
        pitch = 1.45f,
        speechRate = 1.04f,
        sampleText = "Olá! Eu sou a Matilde. Esta é a minha voz feminina vibrante e luminosa."
    )

    val MASCULINE_VOICES = listOf(
        MASCULINE_1, MASCULINE_2, MASCULINE_3, MASCULINE_4,
        MASCULINE_5, MASCULINE_6, MASCULINE_7, MASCULINE_8
    )
    val FEMININE_VOICES = listOf(FEMININE_1, FEMININE_2, FEMININE_3, FEMININE_4)
    val ALL = MASCULINE_VOICES + FEMININE_VOICES

    fun findById(id: String): VoiceProfile {
        return ALL.firstOrNull { it.id == id } ?: MASCULINE_1
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
                    val matchingVoices = availableVoices.filter { voice ->
                        val vName = voice.name.lowercase()
                        when (profile.gender) {
                            VoiceGender.MASCULINE -> (
                                vName.contains("male") ||
                                vName.contains("ptm") ||
                                vName.contains("-m-") ||
                                vName.contains("man") ||
                                vName.contains("jfs") ||
                                vName.contains("afs") ||
                                vName.contains("male-") ||
                                vName.contains("-male") ||
                                vName.contains("#male")
                            ) && !vName.contains("female") && !vName.contains("woman") && !vName.contains("fem")
                            VoiceGender.FEMININE -> (
                                vName.contains("female") ||
                                vName.contains("fem") ||
                                vName.contains("ptf") ||
                                vName.contains("-f-") ||
                                vName.contains("woman") ||
                                vName.contains("lady") ||
                                vName.contains("#female")
                            )
                        }
                    }
                    if (matchingVoices.isNotEmpty()) {
                        // Prioritize matching the current locale first (e.g. pt)
                        val localeMatching = matchingVoices.filter {
                            it.locale.language.equals("pt", ignoreCase = true)
                        }
                        val candidates = if (localeMatching.isNotEmpty()) localeMatching else matchingVoices
                        
                        val index = when (profile.id) {
                            "fem_1", "masc_1" -> 0
                            "fem_2", "masc_2" -> 1
                            "fem_3", "masc_3" -> 2
                            "fem_4", "masc_4" -> 3
                            "masc_5" -> 4
                            "masc_6" -> 5
                            "masc_7" -> 6
                            "masc_8" -> 7
                            else -> 0
                        } % candidates.size
                        tts.voice = candidates[index]
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
