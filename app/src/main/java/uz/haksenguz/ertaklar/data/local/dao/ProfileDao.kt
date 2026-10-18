package uz.haksenguz.ertaklar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import uz.haksenguz.ertaklar.data.local.entities.ProfileEntity

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY created")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY created")
    suspend fun all(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun get(id: String): ProfileEntity?

    @Insert
    suspend fun insert(profile: ProfileEntity)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE profiles SET points = points + :n WHERE id = :id")
    suspend fun addPoints(id: String, n: Int)

    /** Takes [n] points only if there are that many; returns the rows changed (0 when not enough). */
    @Query("UPDATE profiles SET points = points - :n WHERE id = :id AND points >= :n AND :n > 0")
    suspend fun spendPoints(id: String, n: Int): Int

    @Query("UPDATE profiles SET last_book = :book WHERE id = :id")
    suspend fun setLastBook(id: String, book: String?)
}
