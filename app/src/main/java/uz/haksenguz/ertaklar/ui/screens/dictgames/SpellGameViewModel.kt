package uz.haksenguz.ertaklar.ui.screens.dictgames

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.dictionary.Dictionary
import uz.haksenguz.ertaklar.data.repository.DictionaryRepository

/** A letter tile: its letter and its place in the word. */
data class Tile(val letter: String, val index: Int)

data class SpellUiState(
    val rounds: Int = 0,
    val round: Int = 0,
    val word: Dictionary.Entry? = null,
    /** The word's letters, in order (lower case). */
    val letters: List<String> = emptyList(),
    /** The same letters, shuffled, to tap. */
    val tiles: List<Tile> = emptyList(),
    /** How many letters are in place. */
    val next: Int = 0,
    /** Tiles already used (by index in the word). */
    val used: Set<Int> = emptySet(),
    val shakes: Map<Int, Int> = emptyMap(),
    val result: String = "",
    val won: Boolean = false
)

/** "🔤 So'zni yig'ing": under the word's picture, tap its letters in order; three words. */
@HiltViewModel
class SpellGameViewModel @Inject constructor(dictionary: DictionaryRepository) :
    WordGameViewModel(dictionary, DictionaryRepository.SPELL) {

    private val _state = MutableStateFlow(SpellUiState())
    val state: StateFlow<SpellUiState> = _state.asStateFlow()

    private var pool: List<Dictionary.Entry> = emptyList()

    init {
        viewModelScope.launch {
            pool = Dictionary.pick(words().filter { Dictionary.spellable(it.term) }, ROUNDS)
            _state.update { it.copy(rounds = pool.size) }
            show()
        }
    }

    private fun show() {
        val e = pool.getOrNull(_state.value.round) ?: return
        val letters = Dictionary.letters(e.term).map { it.lowercase() }
        _state.update {
            it.copy(
                word = e,
                letters = letters,
                tiles = Dictionary.pick(letters.mapIndexed { i, l -> Tile(l, i) }, letters.size),
                next = 0,
                used = emptySet()
            )
        }
    }

    fun tap(tile: Tile) {
        val s = _state.value
        if (s.won || tile.index in s.used || s.next >= s.letters.size) return
        // Any tile with the right letter will do (a word can have the same letter twice).
        if (tile.letter != s.letters[s.next]) {
            _state.update {
                it.copy(
                    shakes = it.shakes + (tile.index to (it.shakes[tile.index] ?: 0) + 1),
                    result = "🤔 Bu harf emas. Qaysi harf keladi?"
                )
            }
            return
        }
        val next = s.next + 1
        if (next < s.letters.size) {
            _state.update { it.copy(next = next, used = it.used + tile.index, result = "") }
            return
        }
        val round = s.round + 1
        val finished = round == pool.size
        _state.update {
            it.copy(
                next = next,
                used = it.used + tile.index,
                round = round,
                won = finished,
                result = if (finished) "🎉 Barakalla! So'zlarni yig'dingiz!" else RIGHT
            )
        }
        if (finished) {
            won()
            return
        }
        viewModelScope.launch {
            delay(PAUSE_MS)
            _state.update { it.copy(result = "") }
            show()
        }
    }

    private companion object {
        const val ROUNDS = 3
    }
}
