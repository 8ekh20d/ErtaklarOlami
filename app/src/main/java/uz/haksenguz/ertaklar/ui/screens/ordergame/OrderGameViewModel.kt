package uz.haksenguz.ertaklar.ui.screens.ordergame

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
import uz.haksenguz.ertaklar.navigation.OrderRoute

/** One picture of the story; [order] is its place in the tale (0 first). */
data class OrderCard(val order: Int, val image: String, val title: String)

data class OrderUiState(
    val cards: List<OrderCard> = emptyList(),
    /** How many are in place. */
    val step: Int = 0,
    /** Each card's shakes so far (a wrong pick shakes it). */
    val shakes: Map<Int, Int> = emptyMap(),
    /** The card to pick next, pulsing after two misses. */
    val hint: Int? = null,
    val result: String = "",
    val won: Boolean = false
)

/** "🧩 Voqealar tartibi": tap the four pictures in the order they happen; 30 points once per book. */
@HiltViewModel
class OrderGameViewModel @Inject constructor(
    savedState: SavedStateHandle,
    content: ContentRepository,
    private val profiles: ProfileRepository,
    private val progress: ProgressRepository,
    private val sfx: Sfx
) : ViewModel() {

    private val key = savedState.toRoute<OrderRoute>().key
    private val _state = MutableStateFlow(OrderUiState())
    val state: StateFlow<OrderUiState> = _state.asStateFlow()

    private val toasts = Channel<String>(Channel.BUFFERED)
    val toastEvents: Flow<String> = toasts.receiveAsFlow()

    private var wrong = 0
    private var hintJob: Job? = null

    init {
        viewModelScope.launch {
            val book = content.library()[key] ?: return@launch
            val cards = Games.orderPicks(book.pages.size).mapIndexed { i, idx ->
                OrderCard(i, book.pages[idx].image, book.pages[idx].title)
            }
            _state.update { it.copy(cards = Games.shuffled(cards)) }
        }
    }

    fun pick(card: OrderCard) {
        val s = _state.value
        if (s.won || card.order < s.step) return
        if (card.order == s.step) {
            wrong = 0
            hintJob?.cancel()
            val step = s.step + 1
            val won = step == s.cards.size
            _state.update {
                it.copy(
                    step = step,
                    hint = null,
                    won = won,
                    result = if (won) "🎉 Barakalla! Hammasi to'g'ri tartibda!" else ""
                )
            }
            sfx.chime(happy = true)
            if (won) win()
            return
        }
        _state.update { it.copy(shakes = it.shakes + (card.order to (it.shakes[card.order] ?: 0) + 1), result = "🤔 Yana o'ylab ko'ring!") }
        toasts.trySend("🤔 Yana o'ylab ko'ring!")
        if (++wrong >= 2) {
            hintJob?.cancel()
            hintJob = viewModelScope.launch {
                _state.update { it.copy(hint = it.step) }
                delay(1_600)
                _state.update { it.copy(hint = null) }
            }
        }
    }

    private fun win() {
        viewModelScope.launch {
            val profile = profiles.active.filterNotNull().first()
            val first = progress.winOrder(profile.id, key)
            toasts.send(if (first) "🧩 Barakalla! +${Scoring.ORDER} ball" else "🧩 Barakalla!")
        }
    }
}
