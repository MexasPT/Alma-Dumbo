package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcriptions")
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val audioFilePath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val detectedLanguage: String,
    val languageCode: String,
    val languageScript: String = "",
    val flagEmoji: String = "🌐",
    val confidence: Float = 0.95f,
    val transcription: String,
    val translationPt: String = "",
    val translationEn: String = "",
    val summary: String = "",
    val keywords: String = "",
    val segmentsJson: String = "[]",
    val isFavorite: Boolean = false,
    val notes: String = ""
)
