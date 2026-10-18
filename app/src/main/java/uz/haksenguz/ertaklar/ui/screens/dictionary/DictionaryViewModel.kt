package uz.haksenguz.ertaklar.ui.screens.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.dictionary.Dictionary
import uz.haksenguz.ertaklar.data.audio.KoreanTts
import uz.haksenguz.ertaklar.data.repository.DictionaryRepository

data class DictionaryUiState(
    val loading: Boolean = true,
    val entries: List<Dictionary.Entry> = emptyList()
) {
    /** Fewer words than the games need. */
    val short: Boolean get() = entries.size < Dictionary.MIN_WORDS

    /** Whether each game has enough words: listening needs four, the others three fitting words. */
    fun opens(game: String): Boolean = when (game) {
        DictionaryRepository.MATCH -> entries.count { it.ko != null } >= 3
        DictionaryRepository.SPELL -> entries.count { Dictionary.spellable(it.term) } >= 3
        else -> entries.size >= Dictionary.MIN_WORDS
    }
}

/** "📖 Mening lug'atim": picture cards of the words the child has met, and three games with them. */
@HiltViewModel
class DictionaryViewModel @Inject constructor(dictionary: DictionaryRepository, private val tts: KoreanTts) : ViewModel() {

    val state: StateFlow<DictionaryUiState> = dictionary.entries
        .map { DictionaryUiState(loading = false, entries = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DictionaryUiState())

    private val toasts = Channel<String>(Channel.BUFFERED)
    val toastEvents: Flow<String> = toasts.receiveAsFlow()

    /** 🔊: the word in the phone's voice. */
    fun say(term: String) {
        viewModelScope.launch {
            if (!tts.sayWord(term)) toasts.send("Bu qurilmada ovoz topilmadi")
        }
    }

    override fun onCleared() {
        tts.stop()
    }
}
