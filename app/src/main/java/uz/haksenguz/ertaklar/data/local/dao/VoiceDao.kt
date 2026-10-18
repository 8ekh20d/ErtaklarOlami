package uz.haksenguz.ertaklar.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.haksenguz.ertaklar.data.local.entities.ClipEntity
import uz.haksenguz.ertaklar.data.local.entities.VoiceEntity

@Dao
interface VoiceDao {
    @Query("SELECT * FROM voices ORDER BY created")
    fun observeVoices(): Flow<List<VoiceEntity>>

    @Upsert
    suspend fun upsertVoice(voice: VoiceEntity)

    @Query("DELETE FROM voices WHERE id = :id")
    suspend fun deleteVoice(id: String)

    @Query("SELECT * FROM clips WHERE book = :book")
    fun observeClipsForBook(book: String): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE voice_id = :voiceId AND book = :book AND page = :page")
    suspend fun clip(voiceId: String, book: String, page: Int): ClipEntity?

    @Upsert
    suspend fun upsertClip(clip: ClipEntity)

    @Query("DELETE FROM clips WHERE voice_id = :voiceId AND book = :book AND page = :page")
    suspend fun deleteClip(voiceId: String, book: String, page: Int)
}
