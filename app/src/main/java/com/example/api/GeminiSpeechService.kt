package com.example.api

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.audio.SupportedLanguages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "GeminiSpeechService"

data class SpeechAnalysisResult(
    val detectedLanguage: String,
    val languageCode: String,
    val languageScript: String,
    val flagEmoji: String,
    val confidence: Float,
    val transcription: String,
    val translationPt: String,
    val translationEn: String,
    val summary: String,
    val keywords: List<String>,
    val rawJson: String = ""
)

class GeminiSpeechService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeAudio(
        audioFile: File,
        apiKeyOverride: String? = null
    ): Result<SpeechAnalysisResult> = withContext(Dispatchers.IO) {
        val apiKey = if (!apiKeyOverride.isNullOrBlank()) {
            apiKeyOverride.trim()
        } else {
            BuildConfig.GEMINI_API_KEY
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Chave da API Gemini não configurada. Insira a sua GEMINI_API_KEY nas Configurações da app ou no painel Secrets.")
            )
        }

        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(
                IllegalArgumentException("Ficheiro de áudio vazio ou inexistente.")
            )
        }

        try {
            val audioBytes = audioFile.readBytes()
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            // Primary model: gemini-2.5-flash (specialized for audio & multimodal understanding)
            val primaryModel = "gemini-2.5-flash"
            val fallbackModel = "gemini-3.5-flash"

            var responseJson = executeRequest(apiKey, primaryModel, audioBase64)
            if (responseJson == null) {
                Log.w(TAG, "Failed with $primaryModel, trying fallback $fallbackModel")
                responseJson = executeRequest(apiKey, fallbackModel, audioBase64)
            }

            if (responseJson == null) {
                return@withContext Result.failure(
                    Exception("Falha ao obter resposta do serviço de IA da Gemini.")
                )
            }

            val parsed = parseGeminiResponse(responseJson)
            Result.success(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing speech audio", e)
            Result.failure(e)
        }
    }

    private fun executeRequest(apiKey: String, model: String, audioBase64: String): String? {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val promptText = """
                You are a world-class linguistic phonetics expert, multilingual speech recognition engine, and translator.
                Analyze the provided audio recording with high precision:
                1. Detect the exact language and dialect spoken.
                   You MUST accurately detect all languages including:
                   - Português (Portugal, Brasil, Angola, etc.)
                   - Castelhano / Espanhol (Español, Castellano)
                   - Francês (Français)
                   - Inglês (English)
                   - Italiano (Italiano)
                   - Alemão (Deutsch)
                   - Holandês / Neerlandês (Nederlands)
                   - Croata (Hrvatski)
                   - Albanês (Shqip)
                   - Dinamarquês (Dansk)
                   - Finlandês (Suomi)
                   - Tamazight / Berbere (Tamaziɣt / ⵜⴰⵎⴰⵣⵉⵖⵜ)
                   - Paquistanês (Urdu, Punjabi, Pashto, Sindhi)
                   - Indiano (Hindi, Tamil, Telugu, Bengali, Marathi, Gujarati, Kannada, Malayalam)
                   - Bangladesh (Bengali / Bangla, Sylheti)
                   - Árabe (العربية - Modern Standard Arabic or regional dialects)
                   - Ucraniano (Українська)
                   - Moldavo (Moldovenească) / Romeno (Română)
                   - Kimbundu (Quimbundo - Angola) / Kikongo / Umbundu
                   - Coreano (한국어)
                   - Japonês (日本語)
                   - Tailandês (ภาษาไทย)
                   - Vietnamita (Tiếng Việt)
                   - Chinês (Mandarim, Cantonês, etc.)
                   - Russo (Русский) e qualquer outro idioma do mundo.
                   If multiple languages or code-switching is present, identify the dominant one and note it.
                2. Transcribe verbatim all spoken text in its original native script and orthography (e.g. Arabic script for Urdu/Arabic, Devanagari for Hindi, Bengali script for Bengali, Cyrillic for Ukrainian, Hangul for Korean, Kanji/Kana for Japanese, Thai script for Thai, Chinese characters for Chinese, Latin for Portuguese/Kimbundu/Romanian/Moldavian/etc.).
                3. Provide an accurate and natural translation into Portuguese (Português).
                4. Provide an English translation.
                5. Provide a 1-2 sentence summary of what was spoken (in Portuguese).
                6. Output strictly valid JSON matching this schema:
                {
                  "detectedLanguage": "Name of language in Portuguese (e.g. 'Português', 'Urdu (Paquistão)', 'Hindi (Índia)', 'Bengali (Bangladesh)', 'Árabe', 'Ucraniano', 'Moldavo', 'Romeno', 'Kimbundu (Angola)', 'Coreano', 'Japonês', 'Tailandês', 'Vietnamita', 'Mandarim (Chinês)', etc.)",
                  "languageCode": "ISO code (e.g. 'pt', 'ur', 'hi', 'bn', 'ar', 'uk', 'mo', 'ro', 'kmb', 'ko', 'ja', 'th', 'vi', 'zh', 'en', 'es', 'fr', 'de', 'it', 'ru')",
                  "languageScript": "Script name (e.g. 'Latino', 'Perso-Árabe', 'Devanagari', 'Bengali', 'Árabe', 'Cirílico', 'Hangul', 'Kanji/Kana', 'Tailandês', 'Hanzi')",
                  "flagEmoji": "Flag emoji corresponding to the country/culture (e.g. 🇵🇹, 🇵🇰, 🇮🇳, 🇧🇩, 🇸🇦, 🇺🇦, 🇲🇩, 🇷🇴, 🇦🇴, 🇰🇷, 🇯🇵, 🇹🇭, 🇻🇳, 🇨🇳, 🇬🇧, 🇪🇸)",
                  "confidence": 0.98,
                  "transcription": "Full verbatim transcription in original language and native script",
                  "translationPt": "Complete translation into Portuguese",
                  "translationEn": "Complete translation into English",
                  "summary": "Resumo claro e conciso em português do que foi dito",
                  "keywords": ["palavra1", "palavra2", "palavra3"]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "audio/mp4")
                                    put("data", audioBase64)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string()
                Log.e(TAG, "Gemini API HTTP ${response.code}: $errBody")
                return null
            }

            response.body?.string()
        } catch (e: Exception) {
            Log.e(TAG, "Request to $model failed", e)
            null
        }
    }

    private fun parseGeminiResponse(rawResponseBody: String): SpeechAnalysisResult {
        var rawText = ""
        try {
            val root = JSONObject(rawResponseBody)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    rawText = parts.getJSONObject(0).optString("text", "")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing root response: $rawResponseBody", e)
        }

        if (rawText.isBlank()) {
            rawText = rawResponseBody
        }

        // Clean any markdown fences ```json ... ```
        val cleanedJson = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        try {
            val json = JSONObject(cleanedJson)
            val detectedLanguage = json.optString("detectedLanguage", "Desconhecido")
            var languageCode = json.optString("languageCode", "und").lowercase()
            val languageScript = json.optString("languageScript", "Desconhecido")
            var flagEmoji = json.optString("flagEmoji", "🌐")
            val confidence = json.optDouble("confidence", 0.92).toFloat()
            val transcription = json.optString("transcription", "").trim()
            val translationPt = json.optString("translationPt", "").trim()
            val translationEn = json.optString("translationEn", "").trim()
            val summary = json.optString("summary", "").trim()

            val keywordsList = mutableListOf<String>()
            val kwArray = json.optJSONArray("keywords")
            if (kwArray != null) {
                for (i in 0 until kwArray.length()) {
                    keywordsList.add(kwArray.optString(i))
                }
            }

            // Cross-verify with known supported catalog
            if (flagEmoji == "🌐" || flagEmoji.isBlank()) {
                val matched = SupportedLanguages.findByCode(languageCode) 
                    ?: SupportedLanguages.findByNameOrCode(detectedLanguage)
                if (matched != null) {
                    flagEmoji = matched.flag
                    if (languageCode == "und") languageCode = matched.code
                }
            }

            return SpeechAnalysisResult(
                detectedLanguage = detectedLanguage,
                languageCode = languageCode,
                languageScript = languageScript,
                flagEmoji = flagEmoji,
                confidence = confidence,
                transcription = if (transcription.isNotBlank()) transcription else "(Sem fala detectada ou áudio inaudível)",
                translationPt = translationPt,
                translationEn = translationEn,
                summary = summary,
                keywords = keywordsList,
                rawJson = cleanedJson
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse structured JSON, returning fallback extraction", e)
            return SpeechAnalysisResult(
                detectedLanguage = "Detectado",
                languageCode = "und",
                languageScript = "Latino",
                flagEmoji = "🌐",
                confidence = 0.85f,
                transcription = cleanedJson,
                translationPt = cleanedJson,
                translationEn = cleanedJson,
                summary = "Transcrição automática de áudio.",
                keywords = emptyList(),
                rawJson = cleanedJson
            )
        }
    }
}
