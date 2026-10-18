package uz.haksenguz.ertaklar.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.reading.Reading
import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.BookRecord
import uz.haksenguz.ertaklar.data.repository.ProfileRepository
import uz.haksenguz.ertaklar.data.repository.ProgressRepository
import uz.haksenguz.ertaklar.navigation.ReaderRoute

/** A book opened for reading. */
data class ReaderUiState(
    val loading: Boolean = true,
    val book: Book? = null,
    val record: BookRecord = BookRecord(""),
    /** The view the book opens on: the cover, or the child's page when continuing. */
    val startView: Int = Reading.COVER,
    /** The Korean twin this tale opens once it is finished, as (key, title). */
    val twin: Pair<String, String>? = null,
    val points: Int = 0
) {
    val pages get() = book?.pages?.size ?: 0
    val lastView get() = Reading.lastView(pages)
    val resumePage get() = Reading.resumePage(record, pages)
    val score get() = book?.let { Scoring.score(it, record.answers) }
}

/** Short messages shown at the bottom of the screen. */
sealed interface ReaderEvent {
    data class Toast(val text: String) : ReaderEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val profiles: ProfileRepository,
    private val progress: ProgressRepository
) : ViewModel() {

    private val route = savedState.toRoute<ReaderRoute>()
    private val opened = MutableStateFlow<Opened?>(null)
    private val events = Channel<ReaderEvent>(Channel.BUFFERED)

    val eventFlow: Flow<ReaderEvent> = events.receiveAsFlow()

    private class Opened(val profileId: String, val book: Book, val startView: Int, val twin: Pair<String, String>?)

    val state: StateFlow<ReaderUiState> = opened.filterNotNull().flatMapLatest { o ->
        combine(progress.record(o.profileId, o.book.key), profiles.active.filterNotNull()) { record, profile ->
            ReaderUiState(
                loading = false,
                book = o.book,
                record = record,
                startView = o.startView,
                twin = o.twin,
                points = profile.points
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    init {
        viewModelScope.launch {
            val library = content.library()
            val book = library[route.key] ?: return@launch
            val profile = profiles.active.filterNotNull().first()
            val record = progress.record(profile.id, book.key).first()
            val start = if (route.resume && record.page >= 1) record.page else Reading.COVER
            val twin = library.books.firstOrNull { it.twin == book.key }?.let { it.key to it.title }
            opened.value = Opened(profile.id, book, start, twin)
        }
    }

    /** The child came to rest on [view]: remember it. */
    fun onPageSettled(view: Int) {
        val o = opened.value ?: return
        viewModelScope.launch {
            val settled = progress.settle(o.profileId, o.book, view) ?: return@launch
            if (settled.firstFinish && o.twin != null) events.send(ReaderEvent.Toast("🔓 Yangi ertak ochildi: «${o.twin.second}»!"))
        }
    }

    fun answer(view: Int, idx: Int) {
        val o = opened.value ?: return
        viewModelScope.launch {
            when (val outcome = progress.answer(o.profileId, o.book, view, idx)) {
                is Reading.Outcome.Right -> events.send(ReaderEvent.Toast("⭐ ${outcome.praise} +${Scoring.ANSWER} ball"))
                Reading.Outcome.Wrong -> events.send(ReaderEvent.Toast("🤔 Yana bir o'ylab ko'ring!"))
                Reading.Outcome.Ignored -> Unit
            }
        }
    }

    /** For parts that arrive in later phases. */
    fun comingSoon() {
        viewModelScope.launch { events.send(ReaderEvent.Toast("Bu bo'lim tez orada qo'shiladi")) }
    }
}
