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
import uz.haksenguz.ertaklar.data.audio.KoreanTts
import uz.haksenguz.ertaklar.data.audio.Sfx
import uz.haksenguz.ertaklar.data.repository.DictionaryRepository

data class ListenUiState(
    val rounds: Int = 0,
    val round: Int = 0,
    val options: List<Dictionary.Entry> = emptyList(),
    /** The word written out: always without a voice, else once picked (or after a miss). */
    val word: String = "",
    /** The phone can say the words. */
    val voice: Boolean = true,
    /** The picture found this round. */
    val right: String? = null,
    val shakes: Map<String, Int> = emptyMap(),
    val result: String = "",
    val won: Boolean = false
)

/** "🔊 Eshit va top": hear a word, tap its picture; five rounds. Without a voice the word is written to read. */
@HiltViewModel
class ListenGameViewModel @Inject constructor(dictionary: DictionaryRepository, sfx: Sfx, private val tts: KoreanTts) :
    WordGameViewModel(dictionary, DictionaryRepository.LISTEN, sfx) {

    private val _state = MutableStateFlow(ListenUiState())
    val state: StateFlow<ListenUiState> = _state.asStateFlow()

    private var entries: List<Dictionary.Entry> = emptyList()
    private var pool: List<Dictionary.Entry> = emptyList()

    init {
        viewModelScope.launch {
            entries = words()
            pool = Dictionary.pick(entries, minOf(ROUNDS, entries.size))
            _state.update { it.copy(rounds = pool.size, voice = tts.hasWordVoice()) }
            show()
        }
    }

    private fun show() {
        val target = pool.getOrNull(_state.value.round) ?: return
        val options = Dictionary.pick(listOf(target) + Dictionary.pick(entries - target, 3), 4)
        _state.update { it.copy(options = options, right = null, word = if (it.voice) "" else target.term) }
        if (_state.value.voice) say(target.term)
    }

    /** "🔊 Yana eshitish". */
    fun again() {
        pool.getOrNull(minOf(_state.value.round, pool.size - 1))?.let { say(it.term) }
    }

    private fun say(term: String) {
        viewModelScope.launch {
            // No voice after all: the word is written instead.
            if (!tts.sayWord(term)) _state.update { it.copy(voice = false, word = term) }
        }
    }

    fun pick(e: Dictionary.Entry) {
        val s = _state.value
        val target = pool.getOrNull(s.round) ?: return
        if (s.won || s.right != null) return
        if (e.key != target.key) {
            _state.update {
                it.copy(
                    shakes = it.shakes + (e.key to (it.shakes[e.key] ?: 0) + 1),
                    word = target.term,
                    result = "🤔 Yana bir bor tinglang!"
                )
            }
            if (s.voice) say(target.term)
            return
        }
        val round = s.round + 1
        val finished = round == pool.size
        _state.update {
            it.copy(
                right = e.key,
                word = target.term,
                round = round,
                won = finished,
                result = if (finished) "🎉 Barakalla! Hammasini topdingiz!" else RIGHT
            )
        }
        if (finished) {
            won()
            return
        }
        right()
        viewModelScope.launch {
            delay(PAUSE_MS)
            _state.update { it.copy(result = "") }
            show()
        }
    }

    override fun onCleared() {
        tts.stop()
    }

    private companion object {
        const val ROUNDS = 5
    }
}
