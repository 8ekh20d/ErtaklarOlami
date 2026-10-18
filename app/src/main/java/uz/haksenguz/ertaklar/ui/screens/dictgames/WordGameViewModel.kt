package uz.haksenguz.ertaklar.ui.screens.dictgames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.dictionary.Dictionary
import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.data.audio.Sfx
import uz.haksenguz.ertaklar.data.repository.DictionaryRepository

/** What the three word games share: the child's words, the toasts, and the daily points. */
abstract class WordGameViewModel(
    private val dictionary: DictionaryRepository,
    private val game: String,
    private val sfx: Sfx
) : ViewModel() {

    private val toasts = Channel<String>(Channel.BUFFERED)
    val toastEvents: Flow<String> = toasts.receiveAsFlow()

    /** The child's words, once. */
    protected suspend fun words(): List<Dictionary.Entry> = dictionary.entries.first()

    protected fun toast(text: String) {
        toasts.trySend(text)
    }

    /** A right move. */
    protected fun right() = sfx.chime()

    /** The game is won: the first win of the day is worth points. */
    protected fun won() {
        sfx.chime(happy = true)
        viewModelScope.launch {
            if (dictionary.win(game)) toasts.send("📖 Barakalla! +${Scoring.DICT} ball")
        }
    }

    companion object {
        /** A right move: shown a moment before the next round. */
        const val RIGHT = "⭐ Barakalla!"

        /** How long a right answer shows before the next round. */
        const val PAUSE_MS = 900L
    }
}
