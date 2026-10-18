package uz.haksenguz.ertaklar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import uz.haksenguz.ertaklar.data.local.dao.ProfileDao
import uz.haksenguz.ertaklar.data.local.dao.ProgressDao
import uz.haksenguz.ertaklar.data.local.dao.VoiceDao
import uz.haksenguz.ertaklar.data.local.entities.BookRecordEntity
import uz.haksenguz.ertaklar.data.local.entities.ClipEntity
import uz.haksenguz.ertaklar.data.local.entities.DictWinEntity
import uz.haksenguz.ertaklar.data.local.entities.ProfileEntity
import uz.haksenguz.ertaklar.data.local.entities.StickerEntity
import uz.haksenguz.ertaklar.data.local.entities.VoiceEntity
import uz.haksenguz.ertaklar.data.local.entities.WordSeenEntity

/** Everything the app remembers. It never leaves the device. */
@Database(
    entities = [
        ProfileEntity::class,
        BookRecordEntity::class,
        WordSeenEntity::class,
        StickerEntity::class,
        DictWinEntity::class,
        VoiceEntity::class,
        ClipEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun progressDao(): ProgressDao
    abstract fun voiceDao(): VoiceDao
}
