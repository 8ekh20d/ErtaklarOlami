package uz.haksenguz.ertaklar.ui.screens.trace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
import uz.haksenguz.ertaklar.core.games.TraceGrid
import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.data.audio.Sfx
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.repository.ProfileRepository
import uz.haksenguz.ertaklar.data.repository.ProgressRepository
import uz.haksenguz.ertaklar.navigation.TraceRoute

data class TraceUiState(
    /** The capital, then the small letter (in Latin; the screen shows them in the chosen script). */
    val letters: List<String> = emptyList(),
    val step: Int = 0,
    /** Goes up each time the pad starts afresh (a new letter, or "Qaytadan"). */
    val round: Int = 0,
    val meter: Int = 0,
    val result: String = "",
    /** A letter is done and the next one isn't drawn yet. */
    val waiting: Boolean = false,
    val won: Boolean = false
) {
    fun done(i: Int): Boolean = i < step || ((won || waiting) && i == step)
}

/** "✍️ Harfni yozing": the Alifbo page's capital, then its small letter; 5 points the first time. */
@HiltViewModel
class TraceViewModel @Inject constructor(
    savedState: SavedStateHandle,
    content: ContentRepository,
    private val profiles: ProfileRepository,
    private val progress: ProgressRepository,
    private val sfx: Sfx
) : ViewModel() {

    private val route = savedState.toRoute<TraceRoute>()
    private val _state = MutableStateFlow(TraceUiState())
    val state: StateFlow<TraceUiState> = _state.asStateFlow()

    private val toasts = Channel<String>(Channel.BUFFERED)
    val toastEvents: Flow<String> = toasts.receiveAsFlow()

    /** The capital as written on the page: the key for its points. */
    private var big = ""

    init {
        viewModelScope.launch {
            val letter = content.library()[route.key]?.pages?.getOrNull(route.view - 1)?.letter ?: return@launch
            val parts = letter.split(' ')
            big = parts[0]
            _state.update { it.copy(letters = listOf(big, parts.getOrNull(1) ?: big.lowercase())) }
        }
    }

    /** A stroke ended; [score] is how the letter is covered so far. */
    fun check(score: TraceGrid.Score) {
        val s = _state.value
        if (s.won || s.waiting) return
        _state.update { it.copy(meter = score.meter) }
        when {
            score.passed && s.step + 1 < s.letters.size -> {
                _state.update { it.copy(result = "⭐ Barakalla! Endi kichik harf.", waiting = true) }
                sfx.chime()
                viewModelScope.launch {
                    delay(700)
                    _state.update { it.copy(step = it.step + 1, round = it.round + 1, meter = 0, waiting = false) }
                }
            }

            score.passed -> {
                _state.update { it.copy(result = "🎉 Barakalla! Harfni o'rgandingiz!", won = true) }
                sfx.chime(happy = true)
                viewModelScope.launch {
                    val profile = profiles.active.filterNotNull().first()
                    val first = progress.traced(profile.id, route.key, big)
                    toasts.send(if (first) "✍️ Barakalla! +${Scoring.TRACE} ball" else "✍️ Barakalla!")
                }
            }

            score.strayed -> _state.update { it.copy(result = "🤔 Harfdan chetga chiqib ketdi. Qaytadan urinib ko'ring!") }

            else -> _state.update { it.copy(result = "👍 Davom eting, harf ustidan yurgizing!") }
        }
    }

    /** "🔄 Qaytadan": the same letter afresh. */
    fun restart() {
        if (_state.value.won) return
        _state.update { it.copy(round = it.round + 1, meter = 0, result = "") }
    }
}
