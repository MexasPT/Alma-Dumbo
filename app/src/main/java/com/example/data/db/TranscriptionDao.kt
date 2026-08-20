package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcriptions ORDER BY createdAt DESC")
    fun getAllTranscriptions(): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteTranscriptions(): Flow<List<TranscriptionEntity>>

    @Query("""
        SELECT * FROM transcriptions 
        WHERE title LIKE '%' || :query || '%' 
           OR transcription LIKE '%' || :query || '%' 
           OR translationPt LIKE '%' || :query || '%' 
           OR detectedLanguage LIKE '%' || :query || '%'
           OR locationAddress LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchTranscriptions(query: String): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE languageCode = :languageCode ORDER BY createdAt DESC")
    fun filterByLanguage(languageCode: String): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE id = :id LIMIT 1")
    fun getTranscriptionById(id: Long): Flow<TranscriptionEntity?>

    @Query("SELECT * FROM transcriptions WHERE id = :id LIMIT 1")
    suspend fun getTranscriptionByIdSync(id: Long): TranscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TranscriptionEntity): Long

    @Update
    suspend fun update(record: TranscriptionEntity)

    @Delete
    suspend fun delete(record: TranscriptionEntity)

    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transcriptions")
    suspend fun deleteAll()

    @Query("UPDATE transcriptions SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE transcriptions SET title = :title, notes = :notes WHERE id = :id")
    suspend fun updateTitleAndNotes(id: Long, title: String, notes: String)

    @Query("SELECT COUNT(*) FROM transcriptions")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT DISTINCT detectedLanguage, languageCode, flagEmoji FROM transcriptions")
    fun getDistinctLanguages(): Flow<List<LanguageInfoResult>>
}

data class LanguageInfoResult(
    val detectedLanguage: String,
    val languageCode: String,
    val flagEmoji: String
)
