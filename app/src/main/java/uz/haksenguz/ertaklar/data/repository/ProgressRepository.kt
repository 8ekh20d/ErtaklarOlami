package uz.haksenguz.ertaklar.data.repository

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import uz.haksenguz.ertaklar.core.reading.Reading
import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.data.local.dao.ProfileDao
import uz.haksenguz.ertaklar.data.local.dao.ProgressDao
import uz.haksenguz.ertaklar.data.local.entities.BookRecordEntity
import uz.haksenguz.ertaklar.data.local.entities.DictWinEntity
import uz.haksenguz.ertaklar.data.local.entities.WordSeenEntity
import uz.haksenguz.ertaklar.data.models.AnswerState
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.BookRecord

/** Reading progress per child and book. */
@Singleton
class ProgressRepository @Inject constructor(
    private val dao: ProgressDao,
    private val profileDao: ProfileDao,
    private val json: Json
) {
    /** Answers and page changes both rewrite a book's record; one at a time. */
    private val mutex = Mutex()

    /** The child's records by book key. */
    fun records(profileId: String): Flow<Map<String, BookRecord>> =
        dao.observeRecords(profileId).map { list -> list.associate { it.book to it.toModel() } }

    /** The child's record for one book (an empty one before they open it). */
    fun record(profileId: String, key: String): Flow<BookRecord> = records(profileId).map { it[key] ?: BookRecord(key) }

    /** The child settled on [view]: remember the page, collect its word card, or mark the book read. */
    suspend fun settle(profileId: String, book: Book, view: Int): Reading.Settled? = mutex.withLock {
        val current = load(profileId, book.key)
        val settled = Reading.settle(current, book.pages.size, view, System.currentTimeMillis()) ?: return null
        dao.upsert(settled.record.toEntity(profileId))
        if (view in 1..book.pages.size) {
            profileDao.setLastBook(profileId, book.key)
            if (book.pages[view - 1].word != null) dao.addWord(WordSeenEntity(profileId, book.key, view))
        }
        settled
    }

    /** Bedtime ended before the book did: tomorrow it opens on [page]. */
    suspend fun rememberPage(profileId: String, book: Book, page: Int) = mutex.withLock {
        val current = load(profileId, book.key)
        dao.upsert(current.copy(page = page.coerceIn(1, book.pages.size)).toEntity(profileId))
        profileDao.setLastBook(profileId, book.key)
    }

    /** Answer [idx] to story page [view]'s question; a right answer earns its points. */
    suspend fun answer(profileId: String, book: Book, view: Int, idx: Int): Reading.Outcome = mutex.withLock {
        val question = book.pages.getOrNull(view - 1)?.question ?: return Reading.Outcome.Ignored
        val current = load(profileId, book.key)
        val (state, outcome) = Reading.answer(current.answers[view.toString()], question, view, idx)
        if (outcome != Reading.Outcome.Ignored) {
            dao.upsert(current.copy(answers = current.answers + (view.toString() to state)).toEntity(profileId))
        }
        if (outcome is Reading.Outcome.Right) profileDao.addPoints(profileId, Scoring.ANSWER)
        outcome
    }

    /** The final quiz was answered right; its points come only the first time. Returns whether it was the first. */
    suspend fun passQuiz(profileId: String, key: String): Boolean = mutex.withLock {
        val current = load(profileId, key)
        if (current.quiz) return false
        dao.upsert(current.copy(quiz = true).toEntity(profileId))
        profileDao.addPoints(profileId, Scoring.QUIZ)
        true
    }

    /** "Voqealar tartibi" won; its points come only the first time. Returns whether it was the first. */
    suspend fun winOrder(
        profileId: String,
        key: String
    ): Boolean = once(profileId, key, Scoring.ORDER, { it.order }) { it.copy(order = true) }

    /** "Farqlarni toping" won; its points come only the first time. */
    suspend fun winCompare(profileId: String, key: String): Boolean =
        once(profileId, key, Scoring.COMPARE, { it.compare }) { it.copy(compare = true) }

    /** An Alifbo [letter] traced; its points come only the first time. */
    suspend fun traced(profileId: String, key: String, letter: String): Boolean =
        once(profileId, key, Scoring.TRACE, { letter in it.traced }) { it.copy(traced = it.traced + letter) }

    /** The "book:page" keys of the word pages the child has opened. */
    fun words(
        profileId: String
    ): Flow<Set<String>> = dao.observeWords(profileId).map { list -> list.map { "${it.book}:${it.page}" }.toSet() }

    /** A dictionary game won; its points come once a day. Returns whether they came now. */
    suspend fun winDictGame(profileId: String, game: String): Boolean = mutex.withLock {
        val today = LocalDate.now().toString()
        if (dao.dictWin(profileId, game)?.day == today) return false
        dao.upsertDictWin(DictWinEntity(profileId, game, today))
        profileDao.addPoints(profileId, Scoring.DICT)
        true
    }

    private suspend fun once(
        profileId: String,
        key: String,
        points: Int,
        done: (BookRecord) -> Boolean,
        mark: (BookRecord) -> BookRecord
    ): Boolean = mutex.withLock {
        val current = load(profileId, key)
        if (done(current)) return false
        dao.upsert(mark(current).toEntity(profileId))
        profileDao.addPoints(profileId, points)
        true
    }

    private suspend fun load(profileId: String, key: String): BookRecord = dao.record(profileId, key)?.toModel() ?: BookRecord(key)

    private fun BookRecordEntity.toModel() = BookRecord(
        book = book,
        page = page,
        finished = finished,
        finishedAt = finishedAt,
        quiz = quiz,
        order = orderWon,
        compare = compare,
        answers = runCatching { json.decodeFromString<Map<String, AnswerState>>(answersJson) }.getOrDefault(emptyMap()),
        traced = runCatching { json.decodeFromString<List<String>>(tracedJson).toSet() }.getOrDefault(emptySet())
    )

    private fun BookRecord.toEntity(profileId: String) = BookRecordEntity(
        profileId = profileId,
        book = book,
        page = page,
        finished = finished,
        finishedAt = finishedAt,
        quiz = quiz,
        orderWon = order,
        compare = compare,
        answersJson = json.encodeToString(answers),
        tracedJson = json.encodeToString(traced.toList())
    )
}
