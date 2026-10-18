package uz.haksenguz.ertaklar.ui.screens.bedtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.reading.Bedtime
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.models.BookRecord
import uz.haksenguz.ertaklar.data.repository.ProfileRepository
import uz.haksenguz.ertaklar.data.repository.ProgressRepository
import uz.haksenguz.ertaklar.data.repository.SettingsRepository

/** A calm book on the picker, with where the child is in it. */
data class CalmBook(val key: String, val title: String, val cover: String, val status: String)

data class BedtimeUiState(
    /** Pages tonight: 3, 5, or 0 for the whole book. */
    val pages: Int = 5,
    val books: List<CalmBook> = emptyList()
)

/** Uxlash vaqti: how many pages tonight, and which calm book. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BedtimeViewModel @Inject constructor(
    content: ContentRepository,
    profiles: ProfileRepository,
    progress: ProgressRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val records = profiles.active.filterNotNull().flatMapLatest { progress.records(it.id) }

    val state: StateFlow<BedtimeUiState> = combine(flow { emit(content.library()) }, records, settings.settings) { lib, recs, s ->
        fun locked(twin: String?) = twin != null && recs[twin]?.finished != true
        BedtimeUiState(
            pages = s.bedtimePages.takeIf { it in Bedtime.PAGES } ?: 5,
            books = Bedtime.books(lib.books, recs) { locked(it.twin) }.map { b -> CalmBook(b.key, b.title, b.cover, status(recs[b.key])) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BedtimeUiState())

    fun setPages(n: Int) {
        viewModelScope.launch { settings.setBedtimePages(n) }
    }

    /** "▶ 4-sahifadan", "✓ O'qildi" or "Yangi" (Uzbek; shown through the display). */
    private fun status(r: BookRecord?): String = when {
        r != null && r.page >= 1 -> "▶ ${r.page}-sahifadan"
        r != null && r.finished -> "✓ O'qildi"
        else -> "Yangi"
    }
}
