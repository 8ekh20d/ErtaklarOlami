package uz.haksenguz.ertaklar.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatar: String,
    val age: Int?,
    val points: Int = 0,
    @ColumnInfo(name = "last_book") val lastBook: String? = null,
    val created: Long
)

@Entity(
    tableName = "book_records",
    primaryKeys = ["profile_id", "book"],
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profile_id"], onDelete = ForeignKey.CASCADE)]
)
data class BookRecordEntity(
    @ColumnInfo(name = "profile_id") val profileId: String,
    val book: String,
    val page: Int = 0,
    val finished: Boolean = false,
    @ColumnInfo(name = "finished_at") val finishedAt: Long? = null,
    val quiz: Boolean = false,
    @ColumnInfo(name = "order_won") val orderWon: Boolean = false,
    val compare: Boolean = false,
    /** JSON object: page number -> AnswerState. */
    @ColumnInfo(name = "answers_json") val answersJson: String = "{}",
    /** JSON array of traced Alifbo letters. */
    @ColumnInfo(name = "traced_json") val tracedJson: String = "[]"
)

/** A word card the child has met (Mening lug'atim). */
@Entity(
    tableName = "words_seen",
    primaryKeys = ["profile_id", "book", "page"],
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profile_id"], onDelete = ForeignKey.CASCADE)]
)
data class WordSeenEntity(
    @ColumnInfo(name = "profile_id") val profileId: String,
    val book: String,
    val page: Int
)

/** A passport sticker bought with points. */
@Entity(
    tableName = "stickers",
    primaryKeys = ["profile_id", "sticker_id"],
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profile_id"], onDelete = ForeignKey.CASCADE)]
)
data class StickerEntity(
    @ColumnInfo(name = "profile_id") val profileId: String,
    @ColumnInfo(name = "sticker_id") val stickerId: String,
    @ColumnInfo(name = "bought_at") val boughtAt: Long
)

/** The day a dictionary game was last won (its points come once a day). */
@Entity(
    tableName = "dict_wins",
    primaryKeys = ["profile_id", "game"],
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profile_id"], onDelete = ForeignKey.CASCADE)]
)
data class DictWinEntity(
    @ColumnInfo(name = "profile_id") val profileId: String,
    val game: String,
    /** yyyy-MM-dd */
    val day: String
)

/** A family voice that reads books aloud (recording studio). */
@Entity(tableName = "voices")
data class VoiceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatar: String,
    val created: Long
)

/** One recorded page; the audio file lives under filesDir/voices/. */
@Entity(
    tableName = "clips",
    primaryKeys = ["voice_id", "book", "page"],
    foreignKeys = [ForeignKey(VoiceEntity::class, ["id"], ["voice_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("book"), Index("voice_id")]
)
data class ClipEntity(
    @ColumnInfo(name = "voice_id") val voiceId: String,
    val book: String,
    val page: Int,
    val file: String,
    val mime: String,
    val duration: Double,
    /** JSON array of seconds where each segment starts. */
    @ColumnInfo(name = "marks_json") val marksJson: String,
    val sig: String,
    val created: Long
)
