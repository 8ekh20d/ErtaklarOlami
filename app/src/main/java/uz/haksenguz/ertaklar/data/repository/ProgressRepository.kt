package uz.haksenguz.ertaklar.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import uz.haksenguz.ertaklar.data.local.dao.ProgressDao
import uz.haksenguz.ertaklar.data.local.entities.BookRecordEntity
import uz.haksenguz.ertaklar.data.models.AnswerState
import uz.haksenguz.ertaklar.data.models.BookRecord

/** Reading progress per child and book. */
@Singleton
class ProgressRepository @Inject constructor(
    private val dao: ProgressDao,
    private val json: Json
) {
    /** The child's records by book key. */
    fun records(profileId: String): Flow<Map<String, BookRecord>> =
        dao.observeRecords(profileId).map { list -> list.associate { it.book to it.toModel() } }

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
}
