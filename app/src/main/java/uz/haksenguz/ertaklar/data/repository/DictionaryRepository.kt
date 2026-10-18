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
import uz.haksenguz.ertaklar.core.dictionary.Dictionary
import uz.haksenguz.ertaklar.data.content.ContentRepository

/** The active child's "Mening lug'atim" and the points of its games. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DictionaryRepository @Inject constructor(
    private val content: ContentRepository,
    private val profiles: ProfileRepository,
    private val progress: ProgressRepository
) {
    /** The child's cards in alphabet order, kept up to date as pages are read. */
    val entries: Flow<List<Dictionary.Entry>> = profiles.active.filterNotNull().flatMapLatest { p ->
        combine(flow { emit(content.library()) }, progress.words(p.id), progress.records(p.id)) { lib, seen, records ->
            Dictionary.entries(lib.books, lib.korean, seen, records.filterValues { it.finished }.keys)
        }
    }

    /** A game won; returns whether today's points for it came now. */
    suspend fun win(game: String): Boolean = progress.winDictGame(profiles.active.filterNotNull().first().id, game)

    companion object {
        const val LISTEN = "listen"
        const val MATCH = "match"
        const val SPELL = "spell"
    }
}
