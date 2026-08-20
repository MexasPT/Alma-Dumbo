package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiSpeechService
import com.example.audio.AudioPlaybackManager
import com.example.audio.AudioRecorderManager
import com.example.audio.LiveSpeechManager
import com.example.audio.PlaybackInfo
import com.example.audio.SupportedLanguages
import com.example.data.db.AppDatabase
import com.example.data.db.TranscriptionEntity
import com.example.location.LocationHelper
import com.example.location.LocationInfo
import com.example.smtp.SmtpClient
import com.example.smtp.SmtpConfig
import com.example.smtp.SmtpPreferencesManager
import com.example.smtp.SmtpResult
import com.example.smtp.SmtpSendRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "TranscriberViewModel"
private const val PREFS_NAME = "vozlingua_prefs"
private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

sealed interface RecordingUiState {
    object Idle : RecordingUiState
    data class Recording(val durationMs: Long, val currentAmp: Float) : RecordingUiState
    data class Processing(val message: String, val durationMs: Long) : RecordingUiState
    data class Success(val record: TranscriptionEntity) : RecordingUiState
    data class Error(val errorMessage: String, val canRetryFile: File? = null) : RecordingUiState
}

sealed interface SmtpOperationState {
    object Idle : SmtpOperationState
    data class Sending(val message: String) : SmtpOperationState
    data class Success(val message: String) : SmtpOperationState
    data class Error(val errorMessage: String) : SmtpOperationState
}

class TranscriberViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.transcriptionDao()
    private val speechService = GeminiSpeechService()
    private val smtpClient = SmtpClient()
    private val smtpPrefsManager = SmtpPreferencesManager(application)
    private val locationHelper = LocationHelper(application)

    val recorderManager = AudioRecorderManager(application)
    val playbackManager = AudioPlaybackManager(application)
    val liveSpeechManager = LiveSpeechManager(application, viewModelScope)

    private val sharedPrefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // UI States
    private val _recordingState = MutableStateFlow<RecordingUiState>(RecordingUiState.Idle)
    val recordingState: StateFlow<RecordingUiState> = _recordingState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLanguageFilter = MutableStateFlow<String?>(null)
    val selectedLanguageFilter: StateFlow<String?> = _selectedLanguageFilter.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    private val _customApiKey = MutableStateFlow(sharedPrefs.getString(KEY_CUSTOM_API_KEY, "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    // SMTP Config State
    private val _smtpConfig = MutableStateFlow(smtpPrefsManager.getSmtpConfig())
    val smtpConfig: StateFlow<SmtpConfig> = _smtpConfig.asStateFlow()

    private val _smtpState = MutableStateFlow<SmtpOperationState>(SmtpOperationState.Idle)
    val smtpState: StateFlow<SmtpOperationState> = _smtpState.asStateFlow()

    // Live Portuguese Translation State
    private val _livePortugueseTranslation = MutableStateFlow("")
    val livePortugueseTranslation: StateFlow<String> = _livePortugueseTranslation.asStateFlow()

    private val _isLiveTranslating = MutableStateFlow(false)
    val isLiveTranslating: StateFlow<Boolean> = _isLiveTranslating.asStateFlow()

    init {
        // Automatically translate live transcription to Portuguese when text is received
        viewModelScope.launch {
            liveSpeechManager.fullTranscript.collect { transcript ->
                if (transcript.isNotBlank()) {
                    val lang = liveSpeechManager.activeLanguage.value
                    if (!lang.startsWith("pt", ignoreCase = true)) {
                        translateLiveTranscript(transcript, lang)
                    } else {
                        _livePortugueseTranslation.value = transcript
                    }
                } else {
                    _livePortugueseTranslation.value = ""
                }
            }
        }
    }

    fun translateLiveTranscript(text: String, langCode: String? = null) {
        if (text.isBlank()) {
            _livePortugueseTranslation.value = ""
            return
        }
        val lang = langCode ?: liveSpeechManager.activeLanguage.value
        if (lang.startsWith("pt", ignoreCase = true)) {
            _livePortugueseTranslation.value = text
            return
        }

        viewModelScope.launch {
            _isLiveTranslating.value = true
            val res = speechService.translateTextToPortuguese(
                text = text,
                sourceLang = lang,
                apiKeyOverride = _customApiKey.value.ifBlank { null }
            )
            res.fold(
                onSuccess = { translated ->
                    _livePortugueseTranslation.value = translated
                },
                onFailure = {
                    Log.w(TAG, "Live translation failed", it)
                }
            )
            _isLiveTranslating.value = false
        }
    }

    // Combined Flow for Recordings History
    val recordings: StateFlow<List<TranscriptionEntity>> = combine(
        _searchQuery,
        _selectedLanguageFilter,
        _showFavoritesOnly
    ) { query, langFilter, favOnly ->
        Triple(query, langFilter, favOnly)
    }.flatMapLatest { (query, langFilter, favOnly) ->
        when {
            query.isNotBlank() -> dao.searchTranscriptions(query.trim())
            favOnly -> dao.getFavoriteTranscriptions()
            langFilter != null -> dao.filterByLanguage(langFilter)
            else -> dao.getAllTranscriptions()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalRecordingsCount: StateFlow<Int> = dao.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val playbackState: StateFlow<PlaybackInfo> = playbackManager.playbackState

    fun startRecording() {
        playbackManager.stop()
        val file = recorderManager.startRecording()
        if (file != null) {
            _recordingState.value = RecordingUiState.Recording(0L, 0f)
        } else {
            _recordingState.value = RecordingUiState.Error("Não foi possível iniciar a gravação de áudio.")
        }
    }

    fun stopAndTranscribe() {
        val result = recorderManager.stopRecording()
        if (result == null || result.durationMs < 400L) {
            _recordingState.value = RecordingUiState.Error("Gravação demasiado curta. Fale durante pelo menos 1 segundo.")
            return
        }

        val audioFile = result.file
        val durationMs = result.durationMs
        val fileSizeBytes = result.fileSizeBytes

        _recordingState.value = RecordingUiState.Processing(
            message = "A analisar áudio, a detetar idioma e localização...",
            durationMs = durationMs
        )

        viewModelScope.launch {
            val locInfo: LocationInfo? = locationHelper.getCurrentLocationInfo()

            val analysisResult = speechService.analyzeAudio(
                audioFile = audioFile,
                apiKeyOverride = _customApiKey.value.ifBlank { null }
            )

            analysisResult.fold(
                onSuccess = { res ->
                    val defaultTitle = "${res.detectedLanguage} (${formatTimestamp(System.currentTimeMillis())})"
                    val entity = TranscriptionEntity(
                        title = defaultTitle,
                        audioFilePath = audioFile.absolutePath,
                        durationMs = durationMs,
                        fileSizeBytes = fileSizeBytes,
                        createdAt = System.currentTimeMillis(),
                        detectedLanguage = res.detectedLanguage,
                        languageCode = res.languageCode,
                        languageScript = res.languageScript,
                        flagEmoji = res.flagEmoji,
                        confidence = res.confidence,
                        transcription = res.transcription,
                        translationPt = res.translationPt,
                        translationEn = res.translationEn,
                        summary = res.summary,
                        keywords = res.keywords.joinToString(", "),
                        isFavorite = false,
                        latitude = locInfo?.latitude,
                        longitude = locInfo?.longitude,
                        locationAddress = locInfo?.address
                    )

                    val newId = dao.insert(entity)
                    val savedEntity = entity.copy(id = newId)
                    _recordingState.value = RecordingUiState.Success(savedEntity)
                },
                onFailure = { error ->
                    Log.e(TAG, "Analysis failed", error)
                    _recordingState.value = RecordingUiState.Error(
                        errorMessage = error.localizedMessage ?: "Erro ao analisar o áudio.",
                        canRetryFile = audioFile
                    )
                }
            )
        }
    }

    fun cancelRecording() {
        recorderManager.cancelRecording()
        _recordingState.value = RecordingUiState.Idle
    }

    fun resetRecordingState() {
        _recordingState.value = RecordingUiState.Idle
    }

    fun retryAnalysis(file: File) {
        if (!file.exists()) {
            _recordingState.value = RecordingUiState.Error("O ficheiro de áudio já não existe.")
            return
        }

        _recordingState.value = RecordingUiState.Processing(
            message = "A tentar nova análise de IA...",
            durationMs = 0L
        )

        viewModelScope.launch {
            val locInfo = locationHelper.getCurrentLocationInfo()
            val analysisResult = speechService.analyzeAudio(
                audioFile = file,
                apiKeyOverride = _customApiKey.value.ifBlank { null }
            )

            analysisResult.fold(
                onSuccess = { res ->
                    val defaultTitle = "${res.detectedLanguage} (${formatTimestamp(System.currentTimeMillis())})"
                    val entity = TranscriptionEntity(
                        title = defaultTitle,
                        audioFilePath = file.absolutePath,
                        durationMs = 1000L,
                        fileSizeBytes = file.length(),
                        createdAt = System.currentTimeMillis(),
                        detectedLanguage = res.detectedLanguage,
                        languageCode = res.languageCode,
                        languageScript = res.languageScript,
                        flagEmoji = res.flagEmoji,
                        confidence = res.confidence,
                        transcription = res.transcription,
                        translationPt = res.translationPt,
                        translationEn = res.translationEn,
                        summary = res.summary,
                        keywords = res.keywords.joinToString(", "),
                        isFavorite = false,
                        latitude = locInfo?.latitude,
                        longitude = locInfo?.longitude,
                        locationAddress = locInfo?.address
                    )

                    val newId = dao.insert(entity)
                    val savedEntity = entity.copy(id = newId)
                    _recordingState.value = RecordingUiState.Success(savedEntity)
                },
                onFailure = { error ->
                    _recordingState.value = RecordingUiState.Error(
                        errorMessage = error.localizedMessage ?: "Erro ao reprocessar.",
                        canRetryFile = file
                    )
                }
            )
        }
    }

    fun saveLiveSession(transcriptText: String, langCode: String) {
        if (transcriptText.isBlank()) return

        viewModelScope.launch {
            val locInfo = locationHelper.getCurrentLocationInfo()
            val meta = SupportedLanguages.findByCode(langCode)
                ?: SupportedLanguages.findByNameOrCode(langCode)
                ?: SupportedLanguages.ALL.first()

            var ptTranslation = _livePortugueseTranslation.value
            if (ptTranslation.isBlank() && !langCode.startsWith("pt", ignoreCase = true)) {
                val res = speechService.translateTextToPortuguese(
                    text = transcriptText,
                    sourceLang = langCode,
                    apiKeyOverride = _customApiKey.value.ifBlank { null }
                )
                ptTranslation = res.getOrDefault("")
            } else if (langCode.startsWith("pt", ignoreCase = true)) {
                ptTranslation = transcriptText
            }

            val entity = TranscriptionEntity(
                title = "Sessão Live - ${meta.namePt} (${formatTimestamp(System.currentTimeMillis())})",
                audioFilePath = "", // Live text session
                durationMs = 0L,
                fileSizeBytes = 0L,
                createdAt = System.currentTimeMillis(),
                detectedLanguage = meta.namePt,
                languageCode = meta.code,
                languageScript = meta.script,
                flagEmoji = meta.flag,
                confidence = 0.98f,
                transcription = transcriptText,
                translationPt = ptTranslation,
                translationEn = "",
                summary = "Transcrição gravada em modo Live (Tempo Real).",
                keywords = "live, transcrição",
                isFavorite = false,
                latitude = locInfo?.latitude,
                longitude = locInfo?.longitude,
                locationAddress = locInfo?.address
            )
            dao.insert(entity)
        }
    }

    fun toggleFavorite(record: TranscriptionEntity) {
        viewModelScope.launch {
            dao.updateFavorite(record.id, !record.isFavorite)
        }
    }

    fun updateTitleAndNotes(id: Long, newTitle: String, newNotes: String) {
        viewModelScope.launch {
            dao.updateTitleAndNotes(id, newTitle, newNotes)
        }
    }

    fun deleteRecord(record: TranscriptionEntity) {
        viewModelScope.launch {
            if (playbackManager.playbackState.value.recordId == record.id) {
                playbackManager.stop()
            }
            dao.delete(record)
            withContext(Dispatchers.IO) {
                try {
                    if (record.audioFilePath.isNotBlank()) {
                        val file = File(record.audioFilePath)
                        if (file.exists()) file.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting audio file", e)
                }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            playbackManager.stop()
            val all = recordings.value
            dao.deleteAll()
            withContext(Dispatchers.IO) {
                all.forEach { record ->
                    try {
                        if (record.audioFilePath.isNotBlank()) {
                            val file = File(record.audioFilePath)
                            if (file.exists()) file.delete()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting audio file", e)
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLanguageFilter(langCode: String?) {
        _selectedLanguageFilter.value = langCode
    }

    fun toggleShowFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
        sharedPrefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    // SMTP Methods
    fun updateSmtpConfig(newConfig: SmtpConfig) {
        _smtpConfig.value = newConfig
        smtpPrefsManager.saveSmtpConfig(newConfig)
    }

    fun testSmtpConnection(testRecipient: String, onResult: (SmtpResult) -> Unit) {
        _smtpState.value = SmtpOperationState.Sending("A testar ligação com o servidor SMTP...")
        viewModelScope.launch {
            val result = smtpClient.testConnection(_smtpConfig.value, testRecipient)
            when (result) {
                is SmtpResult.Success -> {
                    _smtpState.value = SmtpOperationState.Success(result.message)
                }
                is SmtpResult.Failure -> {
                    _smtpState.value = SmtpOperationState.Error(result.errorMessage)
                }
            }
            onResult(result)
        }
    }

    fun sendRecordViaSmtp(
        record: TranscriptionEntity,
        recipientEmail: String,
        includeAudio: Boolean = true,
        includeLocation: Boolean = true,
        onComplete: (SmtpResult) -> Unit
    ) {
        _smtpState.value = SmtpOperationState.Sending("A preparar email e a enviar via SMTP...")
        viewModelScope.launch {
            val config = _smtpConfig.value
            val targetEmail = recipientEmail.ifBlank { config.defaultRecipient }
            if (targetEmail.isBlank()) {
                val err = SmtpResult.Failure("Indique um endereço de email válido para o destinatário.")
                _smtpState.value = SmtpOperationState.Error(err.errorMessage)
                onComplete(err)
                return@launch
            }

            // Location
            var locationHtml = ""
            var locationPlain = ""
            if (includeLocation) {
                val address = record.locationAddress ?: "Localização não disponível"
                val mapLink = if (record.latitude != null && record.longitude != null) {
                    "https://www.google.com/maps/search/?api=1&query=${record.latitude},${record.longitude}"
                } else null

                locationPlain = "\n--- LOCALIZAÇÃO DO ÁUDIO ---\nMorada: $address\n" +
                        (if (mapLink != null) "Mapa: $mapLink\n" else "")

                locationHtml = """
                    <div style="background-color: #f8f9fa; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; margin-top: 18px;">
                        <h4 style="margin: 0 0 8px 0; color: #1e293b; font-size: 14px;">📍 Localização do Registo</h4>
                        <p style="margin: 0 0 8px 0; font-size: 13px; color: #475569;"><strong>Morada:</strong> $address</p>
                        ${if (mapLink != null) "<p style=\"margin: 0;\"><a href=\"$mapLink\" target=\"_blank\" style=\"display: inline-block; background-color: #6750a4; color: #ffffff; text-decoration: none; padding: 6px 14px; border-radius: 6px; font-size: 12px; font-weight: bold;\">🗺️ Ver no Google Maps</a></p>" else ""}
                    </div>
                """.trimIndent()
            }

            val subject = "[Alma Dumbo] Transcrição: ${record.detectedLanguage} - ${formatTimestamp(record.createdAt)}"

            val plainText = """
                ALMA DUMBO - RELATÓRIO DE TRANSCRIÇÃO & ANÁLISE DE VOZ
                Entidade: ITerp - Tecnologias de Informação Lda (Aka Fábrica de Software)
                Desenvolvido por: Rodolfo Valentim by ITerp
                -----------------------------------------------------------------
                Título: ${record.title}
                Data/Hora: ${formatTimestamp(record.createdAt)}
                Língua Detetada: ${record.detectedLanguage} (${record.languageCode.uppercase()})
                Confiança do Modelo: ${(record.confidence * 100).toInt()}%
                Sistema de Escrita: ${record.languageScript}
                
                --- TRANSCRIÇÃO ORIGINAL ---
                ${record.transcription}
                
                --- TRADUÇÃO EM PORTUGUÊS ---
                ${record.translationPt.ifBlank { "(Mesmo idioma ou não aplicável)" }}
                
                --- RESUMO EXECUTIVO ---
                ${record.summary.ifBlank { "Sem resumo disponível" }}
                $locationPlain
                -----------------------------------------------------------------
                Alma Dumbo • Inteligência Acústica Multimodal
            """.trimIndent()

            val htmlBody = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, Helvetica, sans-serif; background-color: #f1f5f9; padding: 20px; color: #0f172a; margin: 0;">
                    <div style="max-width: 650px; margin: 0 auto; background: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 6px 18px rgba(0,0,0,0.06); border: 1px solid #cbd5e1;">
                        <!-- Header -->
                        <div style="background: linear-gradient(135deg, #1c1b1f 0%, #2b2930 100%); padding: 22px 26px; color: #ffffff;">
                            <div style="display: flex; align-items: center; justify-content: space-between;">
                                <h2 style="margin: 0; color: #d0bcff; font-size: 22px; letter-spacing: 0.5px;">Alma Dumbo AI</h2>
                                <span style="background: rgba(208, 188, 255, 0.2); color: #d0bcff; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: bold; border: 1px solid rgba(208,188,255,0.4);">
                                    ${record.flagEmoji} ${record.detectedLanguage}
                                </span>
                            </div>
                            <p style="margin: 6px 0 0 0; color: #cac4d0; font-size: 12px;">
                                ITerp - Tecnologias de Informação Lda &bull; Aka Fábrica de Software
                            </p>
                        </div>
                        
                        <!-- Content -->
                        <div style="padding: 24px;">
                            <!-- Info Badges -->
                            <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px; background: #f8fafc; padding: 12px; border-radius: 8px; border: 1px solid #e2e8f0; font-size: 13px;">
                                <div style="margin-right: 14px;"><strong>Data:</strong> ${formatTimestamp(record.createdAt)}</div>
                                <div style="margin-right: 14px;"><strong>Confiança:</strong> ${(record.confidence * 100).toInt()}%</div>
                                <div><strong>Escrita:</strong> ${record.languageScript}</div>
                            </div>
                            
                            <!-- Transcription -->
                            <div style="margin-bottom: 20px;">
                                <h3 style="margin: 0 0 8px 0; color: #6750a4; font-size: 15px; text-transform: uppercase; letter-spacing: 1px;">Transcrição Fonética Original</h3>
                                <div style="background-color: #faf5ff; border-left: 4px solid #6750a4; padding: 14px 16px; border-radius: 0 8px 8px 0; font-size: 15px; line-height: 1.6; color: #1e1b4b; white-space: pre-wrap;">
                                    ${record.transcription}
                                </div>
                            </div>
                            
                            <!-- Translation -->
                            ${if (record.translationPt.isNotBlank() && record.translationPt != record.transcription) """
                            <div style="margin-bottom: 20px;">
                                <h3 style="margin: 0 0 8px 0; color: #0284c7; font-size: 15px; text-transform: uppercase; letter-spacing: 1px;">Tradução em Português</h3>
                                <div style="background-color: #f0f9ff; border-left: 4px solid #0284c7; padding: 14px 16px; border-radius: 0 8px 8px 0; font-size: 15px; line-height: 1.6; color: #082f49; white-space: pre-wrap;">
                                    ${record.translationPt}
                                </div>
                            </div>
                            """ else ""}
                            
                            <!-- Summary -->
                            ${if (record.summary.isNotBlank()) """
                            <div style="margin-bottom: 20px;">
                                <h3 style="margin: 0 0 8px 0; color: #475569; font-size: 14px; text-transform: uppercase; letter-spacing: 1px;">Resumo Executivo</h3>
                                <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; padding: 12px 14px; border-radius: 8px; font-size: 13px; line-height: 1.5; color: #334155;">
                                    ${record.summary}
                                </div>
                            </div>
                            """ else ""}
                            
                            $locationHtml
                        </div>
                        
                        <!-- Footer -->
                        <div style="background: #f1f5f9; padding: 16px 24px; border-top: 1px solid #e2e8f0; font-size: 11px; color: #64748b; text-align: center;">
                            <p style="margin: 0 0 4px 0;"><strong>Alma Dumbo</strong> &bull; Desenvolvido por <strong>Rodolfo Valentim by ITerp</strong></p>
                            <p style="margin: 0;">ITerp - Tecnologias de Informação Lda &bull; Aka Fábrica de Software</p>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val attachment = if (includeAudio && record.audioFilePath.isNotBlank()) {
                val f = File(record.audioFilePath)
                if (f.exists()) f else null
            } else null

            val result = smtpClient.sendMail(
                config = config,
                request = SmtpSendRequest(
                    recipientEmail = targetEmail,
                    subject = subject,
                    htmlBody = htmlBody,
                    plainTextBody = plainText,
                    attachmentFile = attachment,
                    attachmentMimeType = "audio/mp4"
                )
            )

            when (result) {
                is SmtpResult.Success -> {
                    _smtpState.value = SmtpOperationState.Success(result.message)
                }
                is SmtpResult.Failure -> {
                    _smtpState.value = SmtpOperationState.Error(result.errorMessage)
                }
            }
            onComplete(result)
        }
    }

    fun playAudio(record: TranscriptionEntity) {
        if (record.audioFilePath.isNotBlank()) {
            playbackManager.play(record.id, record.audioFilePath)
        }
    }

    fun pauseAudio() {
        playbackManager.pause()
    }

    fun seekAudio(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackManager.setSpeed(speed)
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.cancelRecording()
        playbackManager.stop()
        liveSpeechManager.destroy()
    }

    private fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
