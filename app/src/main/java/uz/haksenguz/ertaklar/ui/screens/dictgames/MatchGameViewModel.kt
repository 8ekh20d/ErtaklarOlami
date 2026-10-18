package uz.haksenguz.ertaklar.ui.screens.dictgames

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.dictionary.Dictionary
import uz.haksenguz.ertaklar.data.repository.DictionaryRepository

/** A side of the board: the Uzbek words, or their Korean meanings. */
enum class Side { UZ, KO }

data class MatchUiState(
    /** The Uzbek words, in one order... */
    val uz: List<Dictionary.Entry> = emptyList(),
    /** ...and their Korean meanings, in another. */
    val ko: List<Dictionary.Entry> = emptyList(),
    /** The word tapped first, waiting for its pair. */
    val chosen: Pair<Side, String>? = null,
    /** Keys of the pairs found. */
    val matched: Set<String> = emptySet(),
    val shakes: Map<Pair<Side, String>, Int> = emptyMap(),
    val result: String = "",
    val won: Boolean = false
)

/** "🇰🇷 Juftini top": tap an Uzbek word, then its Korean meaning; five pairs. */
@HiltViewModel
class MatchGameViewModel @Inject constructor(dictionary: DictionaryRepository) :
    WordGameViewModel(dictionary, DictionaryRepository.MATCH) {

    private val _state = MutableStateFlow(MatchUiState())
    val state: StateFlow<MatchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val list = Dictionary.pick(words().filter { it.ko != null }, PAIRS)
            _state.value = MatchUiState(uz = list, ko = Dictionary.pick(list, list.size))
        }
    }

    fun tap(side: Side, key: String) {
        val s = _state.value
        if (s.won || key in s.matched) return
        val chosen = s.chosen
        // The first of a pair, or a change of mind on the same side.
        if (chosen == null || chosen.first == side) {
            _state.update { it.copy(chosen = side to key) }
            return
        }
        if (chosen.second != key) {
            _state.update {
                it.copy(
                    chosen = null,
                    shakes = it.shakes + (chosen to (it.shakes[chosen] ?: 0) + 1) + ((side to key) to (it.shakes[side to key] ?: 0) + 1),
                    result = "🤔 Bu juft emas. Yana urinib ko'ring!"
                )
            }
            return
        }
        val matched = s.matched + key
        val finished = matched.size == s.uz.size
        _state.update {
            it.copy(
                chosen = null,
                matched = matched,
                won = finished,
                result = if (finished) "🎉 Barakalla! Hamma juftlar topildi!" else RIGHT
            )
        }
        if (finished) won()
    }

    private companion object {
        const val PAIRS = 5
    }
}
