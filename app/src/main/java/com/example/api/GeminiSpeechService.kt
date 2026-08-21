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

    suspend fun translateText(
        text: String,
        sourceLang: String?,
        targetLang: String = "pt",
        apiKeyOverride: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return@withContext Result.success("")

        val cleanSource = sourceLang?.take(2)?.lowercase() ?: "auto"
        val cleanTarget = targetLang.take(2).lowercase()

        if (cleanSource == cleanTarget && cleanSource != "auto") {
            return@withContext Result.success(trimmed)
        }

        // TIER 1: Try Gemini API if API key is provided
        val apiKey = if (!apiKeyOverride.isNullOrBlank()) {
            apiKeyOverride.trim()
        } else {
            BuildConfig.GEMINI_API_KEY
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val geminiResult = tryGeminiGenericTranslation(trimmed, cleanTarget, apiKey)
            if (geminiResult != null && geminiResult.isNotBlank()) {
                return@withContext Result.success(geminiResult)
            }
        }

        // TIER 2: Seamless Google Translate Free Web Endpoint
        val googleResult = tryGoogleTranslateGenericEndpoint(trimmed, cleanSource, cleanTarget)
        if (googleResult != null && googleResult.isNotBlank()) {
            return@withContext Result.success(googleResult)
        }

        // TIER 3: MyMemory Translation API fallback
        val myMemoryResult = tryMyMemoryGenericTranslate(trimmed, cleanSource, cleanTarget)
        if (myMemoryResult != null && myMemoryResult.isNotBlank()) {
            return@withContext Result.success(myMemoryResult)
        }

        Result.success(trimmed)
    }

    private fun tryGeminiGenericTranslation(text: String, targetLang: String, apiKey: String): String? {
        val targetName = when (targetLang) {
            "pt" -> "Português (pt-PT)"
            "en" -> "Inglês (English)"
            "es" -> "Espanhol / Castelhano (Español)"
            "fr" -> "Francês (Français)"
            "de" -> "Alemão (Deutsch)"
            "it" -> "Italiano"
            "zh" -> "Chinês Mandarim"
            "ja" -> "Japonês"
            "ar" -> "Árabe"
            "ru" -> "Russo"
            "uk" -> "Ucraniano"
            "hi" -> "Hindi"
            "ur" -> "Urdu"
            else -> targetLang
        }

        val modelsToTry = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-flash-latest")
        for (model in modelsToTry) {
            try {
                val prompt = """
                    Traduza com máxima fidelidade e naturalidade o seguinte texto para $targetName:
                    Texto original:
                    $text

                    Responda APENAS com a tradução, sem notas, sem introduções e sem aspas.
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.1)
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = requestJson.toString().toRequestBody(mediaType)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder().url(url).post(body).build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val rawResponse = response.body?.string() ?: ""
                    val root = JSONObject(rawResponse)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val translatedText = parts.getJSONObject(0).optString("text", "").trim()
                            if (translatedText.isNotBlank()) {
                                return translatedText
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Generic translation with $model failed: ${e.message}")
            }
        }
        return null
    }

    private fun tryGoogleTranslateGenericEndpoint(text: String, sourceLang: String, targetLang: String): String? {
        return try {
            val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
            val sl = if (sourceLang.isNotBlank()) sourceLang else "auto"
            val tl = if (targetLang.isNotBlank()) targetLang else "pt"
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sl&tl=$tl&dt=t&q=$encodedText"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0)")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string() ?: ""
                val jsonArray = JSONArray(responseStr)
                val sentencesArray = jsonArray.optJSONArray(0)
                if (sentencesArray != null && sentencesArray.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until sentencesArray.length()) {
                        val sentence = sentencesArray.optJSONArray(i)
                        if (sentence != null && sentence.length() > 0) {
                            val translatedSegment = sentence.optString(0, "")
                            sb.append(translatedSegment)
                        }
                    }
                    val result = sb.toString().trim()
                    if (result.isNotBlank()) return result
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Google Translate generic endpoint failed: ${e.message}")
            null
        }
    }

    private fun tryMyMemoryGenericTranslate(text: String, sourceLang: String, targetLang: String): String? {
        return try {
            val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
            val sl = if (sourceLang.isNotBlank()) sourceLang else "autodetect"
            val tl = if (targetLang.isNotBlank()) targetLang else "pt"
            val url = "https://api.mymemory.translated.net/get?q=$encodedText&langpair=$sl|$tl"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string() ?: ""
                val root = JSONObject(responseStr)
                val responseData = root.optJSONObject("responseData")
                val translated = responseData?.optString("translatedText", "")?.trim()
                if (!translated.isNullOrBlank() && !translated.contains("MYMEMORY WARNING", ignoreCase = true)) {
                    return translated
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "MyMemory generic translate failed: ${e.message}")
            null
        }
    }

    suspend fun translateTextToPortuguese(
        text: String,
        sourceLang: String? = null,
        apiKeyOverride: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return@withContext Result.success("")

        // If source language is already Portuguese, return original text
        val lang = sourceLang?.lowercase() ?: ""
        if (lang == "pt" || lang.startsWith("pt-") || lang.startsWith("pt_")) {
            return@withContext Result.success(trimmed)
        }

        // TIER 1: Try Gemini API if API key is provided
        val apiKey = if (!apiKeyOverride.isNullOrBlank()) {
            apiKeyOverride.trim()
        } else {
            BuildConfig.GEMINI_API_KEY
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val geminiResult = tryGeminiTranslation(trimmed, apiKey)
            if (geminiResult != null && geminiResult.isNotBlank()) {
                Log.d(TAG, "Translation succeeded via Gemini AI")
                return@withContext Result.success(geminiResult)
            }
        }

        // TIER 2: Seamless Google Translate Free Web Endpoint (Zero-config fallback)
        val googleResult = tryGoogleTranslateEndpoint(trimmed, sourceLang)
        if (googleResult != null && googleResult.isNotBlank()) {
            Log.d(TAG, "Translation succeeded via Google Translate endpoint")
            return@withContext Result.success(googleResult)
        }

        // TIER 3: MyMemory Translation API fallback
        val myMemoryResult = tryMyMemoryTranslate(trimmed, sourceLang)
        if (myMemoryResult != null && myMemoryResult.isNotBlank()) {
            Log.d(TAG, "Translation succeeded via MyMemory endpoint")
            return@withContext Result.success(myMemoryResult)
        }

        // Final fallback: return original text if all networks fail
        Log.w(TAG, "All translation endpoints failed, returning original text")
        Result.success(trimmed)
    }

    private fun tryGeminiTranslation(text: String, apiKey: String): String? {
        val modelsToTry = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-flash-latest")

        for (model in modelsToTry) {
            try {
                val prompt = """
                    Traduza com máxima fidelidade e naturalidade o seguinte texto para Português de Portugal (pt-PT):
                    Texto original:
                    $text

                    Responda APENAS com a tradução em português, sem notas, introduções ou aspas.
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.1)
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = requestJson.toString().toRequestBody(mediaType)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder().url(url).post(body).build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val rawResponse = response.body?.string() ?: ""
                    val root = JSONObject(rawResponse)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val translatedText = parts.getJSONObject(0).optString("text", "").trim()
                            if (translatedText.isNotBlank()) {
                                return translatedText
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini translation attempt with $model failed: ${e.message}")
            }
        }
        return null
    }

    private fun tryGoogleTranslateEndpoint(text: String, sourceLang: String?): String? {
        return try {
            val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
            val sl = if (!sourceLang.isNullOrBlank() && sourceLang.length >= 2) sourceLang.substring(0, 2).lowercase() else "auto"
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sl&tl=pt&dt=t&q=$encodedText"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0)")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string() ?: ""
                val jsonArray = JSONArray(responseStr)
                val sentencesArray = jsonArray.optJSONArray(0)
                if (sentencesArray != null && sentencesArray.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until sentencesArray.length()) {
                        val sentence = sentencesArray.optJSONArray(i)
                        if (sentence != null && sentence.length() > 0) {
                            val translatedSegment = sentence.optString(0, "")
                            sb.append(translatedSegment)
                        }
                    }
                    val result = sb.toString().trim()
                    if (result.isNotBlank()) return result
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Google Translate endpoint failed: ${e.message}")
            null
        }
    }

    private fun tryMyMemoryTranslate(text: String, sourceLang: String?): String? {
        return try {
            val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
            val sl = if (!sourceLang.isNullOrBlank() && sourceLang.length >= 2) sourceLang.substring(0, 2).lowercase() else "autodetect"
            val url = "https://api.mymemory.translated.net/get?q=$encodedText&langpair=$sl|pt"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string() ?: ""
                val root = JSONObject(responseStr)
                val responseData = root.optJSONObject("responseData")
                val translated = responseData?.optString("translatedText", "")?.trim()
                if (!translated.isNullOrBlank() && !translated.contains("MYMEMORY WARNING", ignoreCase = true)) {
                    return translated
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "MyMemory translate failed: ${e.message}")
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
