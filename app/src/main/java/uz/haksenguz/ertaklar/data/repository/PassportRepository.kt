package uz.haksenguz.ertaklar.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.haksenguz.ertaklar.core.passport.Passport
import uz.haksenguz.ertaklar.core.passport.Passport.StickerState
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.local.dao.ProfileDao
import uz.haksenguz.ertaklar.data.local.dao.ProgressDao
import uz.haksenguz.ertaklar.data.local.entities.StickerEntity
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.BookRecord
import uz.haksenguz.ertaklar.data.models.MapFile
import uz.haksenguz.ertaklar.data.models.Profile

/** The active child's passport: their stamps, their stickers, and buying more. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PassportRepository @Inject constructor(
    private val content: ContentRepository,
    private val profiles: ProfileRepository,
    private val progress: ProgressRepository,
    private val progressDao: ProgressDao,
    private val profileDao: ProfileDao
) {
    private val mutex = Mutex()
    private var passport: Passport? = null

    suspend fun passport(): Passport = passport ?: Passport(content.passport()).also { passport = it }

    suspend fun map(): MapFile = content.map()

    /** Everything the passport screens show for the active child. */
    data class Snapshot(
        val passport: Passport,
        val books: List<Book>,
        val profile: Profile,
        val records: Map<String, BookRecord>,
        val stamps: Map<String, Long>,
        val stickers: Set<String>
    )

    val snapshot: Flow<Snapshot> = profiles.active.filterNotNull().flatMapLatest { p ->
        combine(
            flow { emit(passport() to content.library().books) },
            progress.records(p.id),
            progressDao.observeStickers(p.id).map { list -> list.map { it.stickerId }.toSet() }
        ) { (passport, books), records, stickers ->
            Snapshot(passport, books, p, records, passport.stamps(books, records), stickers)
        }
    }

    /** How many places the active child has stamped ("Pasportim (3/15)"). */
    val stampCount: Flow<Int> = snapshot.map { it.stamps.size }

    /** Buys a sticker with points; returns what stood in the way, or OK when it is theirs now. */
    suspend fun buy(id: String): StickerState = mutex.withLock {
        val s = snapshot.first()
        val state = s.passport.stickerState(id, s.stamps, s.stickers, s.profile.points)
        if (state != StickerState.OK) return state
        if (profileDao.spendPoints(s.profile.id, s.passport.price) == 0) return StickerState.POINTS
        progressDao.addSticker(StickerEntity(s.profile.id, id, System.currentTimeMillis()))
        StickerState.OK
    }
}
