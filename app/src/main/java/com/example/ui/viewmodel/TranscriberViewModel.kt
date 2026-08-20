package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiSpeechService
import com.example.api.SpeechAnalysisResult
import com.example.audio.AudioPlaybackManager
import com.example.audio.AudioRecorderManager
import com.example.audio.PlaybackInfo
import com.example.audio.SupportedLanguages
import com.example.data.db.AppDatabase
import com.example.data.db.TranscriptionEntity
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

class TranscriberViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.transcriptionDao()
    private val speechService = GeminiSpeechService()

    val recorderManager = AudioRecorderManager(application)
    val playbackManager = AudioPlaybackManager(application)

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
            message = "A analisar áudio e a detetar idioma...",
            durationMs = durationMs
        )

        viewModelScope.launch {
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
                        isFavorite = false
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
                        isFavorite = false
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
                    val file = File(record.audioFilePath)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting audio file", e)
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

    fun playAudio(record: TranscriptionEntity) {
        playbackManager.play(record.id, record.audioFilePath)
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
    }

    private fun formatTimestamp(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }
}
