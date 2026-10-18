package uz.haksenguz.ertaklar.ui.screens.compare

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.games.Games
import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.data.audio.Sfx
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.repository.ProfileRepository
import uz.haksenguz.ertaklar.data.repository.ProgressRepository
import uz.haksenguz.ertaklar.navigation.CompareRoute

/** Where a card belongs. */
enum class Where { UZ, BOTH, KO }

/** Something from the tales: in the first, the second, or both. */
data class CompareCard(val emoji: String, val text: String, val where: Where)

/** One of the two tales: its icon, title and cover. */
data class Tale(val icon: String, val title: String, val cover: String)

data class CompareUiState(
    val left: Tale? = null,
    val right: Tale? = null,
    val deck: List<CompareCard> = emptyList(),
    /** The card being asked about. */
    val index: Int = 0,
    /** Shakes so far (a wrong answer shakes the card). */
    val shakes: Int = 0,
    val result: String = "",
    val won: Boolean = false
) {
    val card: CompareCard? get() = deck.getOrNull(index)

    /** The cards sorted so far, by column. */
    fun sorted(where: Where): List<CompareCard> = deck.take(index).filter { it.where == where }
}

/**
 * "🔍 Farqlarni toping": one card at a time, is it only in the Uzbek tale,
 * only in its Korean twin, or in both? 30 points once per book.
 */
@HiltViewModel
class CompareViewModel @Inject constructor(
    savedState: SavedStateHandle,
    content: ContentRepository,
    private val profiles: ProfileRepository,
    private val progress: ProgressRepository,
    private val sfx: Sfx
) : ViewModel() {

    private val key = savedState.toRoute<CompareRoute>().key
    private val _state = MutableStateFlow(CompareUiState())
    val state: StateFlow<CompareUiState> = _state.asStateFlow()

    private val toasts = Channel<String>(Channel.BUFFERED)
    val toastEvents: Flow<String> = toasts.receiveAsFlow()

    init {
        viewModelScope.launch {
            val library = content.library()
            val book = library[key] ?: return@launch
            val compare = book.compare ?: return@launch
            val uz = book.twin?.let { library[it] } ?: return@launch
            val icons = compare.icons.takeIf { it.size >= 2 } ?: listOf("📕", "📗")
            val cards = compare.cards.mapNotNull { c ->
                val where = when (c.getOrNull(2)) {
                    "uz" -> Where.UZ
                    "ko" -> Where.KO
                    "both" -> Where.BOTH
                    else -> null
                } ?: return@mapNotNull null
                CompareCard(c[0], c[1], where)
            }
            _state.value = CompareUiState(
                left = Tale(icons[0], uz.title, uz.cover),
                right = Tale(icons[1], book.title, book.cover),
                deck = Games.shuffled(cards)
            )
        }
    }

    fun answer(where: Where) {
        val s = _state.value
        val card = s.card ?: return
        if (where != card.where) {
            _state.update { it.copy(shakes = it.shakes + 1, result = "🤔 Yana o'ylab ko'ring!") }
            toasts.trySend("🤔 Yana o'ylab ko'ring!")
            return
        }
        sfx.chime(happy = true)
        val next = s.index + 1
        val won = next == s.deck.size
        _state.update {
            it.copy(
                index = next,
                won = won,
                result = if (won) "🎉 Barakalla! Ikki ertakning o'xshash va farqli tomonlarini topdingiz!" else ""
            )
        }
        if (won) {
            viewModelScope.launch {
                val profile = profiles.active.filterNotNull().first()
                val first = progress.winCompare(profile.id, key)
                toasts.send(if (first) "🔍 Barakalla! +${Scoring.COMPARE} ball" else "🔍 Barakalla!")
            }
        }
    }
}
