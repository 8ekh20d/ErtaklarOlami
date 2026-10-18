package uz.haksenguz.ertaklar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.haksenguz.ertaklar.data.local.entities.BookRecordEntity
import uz.haksenguz.ertaklar.data.local.entities.DictWinEntity
import uz.haksenguz.ertaklar.data.local.entities.StickerEntity
import uz.haksenguz.ertaklar.data.local.entities.WordSeenEntity

@Dao
interface ProgressDao {
    @Query("SELECT * FROM book_records WHERE profile_id = :profileId")
    fun observeRecords(profileId: String): Flow<List<BookRecordEntity>>

    @Query("SELECT * FROM book_records WHERE profile_id = :profileId AND book = :book")
    suspend fun record(profileId: String, book: String): BookRecordEntity?

    @Upsert
    suspend fun upsert(record: BookRecordEntity)

    @Query("SELECT * FROM words_seen WHERE profile_id = :profileId")
    fun observeWords(profileId: String): Flow<List<WordSeenEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addWord(word: WordSeenEntity)

    @Query("SELECT * FROM stickers WHERE profile_id = :profileId")
    fun observeStickers(profileId: String): Flow<List<StickerEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSticker(sticker: StickerEntity)

    @Query("SELECT * FROM dict_wins WHERE profile_id = :profileId")
    fun observeDictWins(profileId: String): Flow<List<DictWinEntity>>

    @Query("SELECT * FROM dict_wins WHERE profile_id = :profileId AND game = :game")
    suspend fun dictWin(profileId: String, game: String): DictWinEntity?

    @Upsert
    suspend fun upsertDictWin(win: DictWinEntity)
}
