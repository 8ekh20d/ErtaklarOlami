package uz.haksenguz.ertaklar.ui.screens.passport

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.passport.Passport.StickerState
import uz.haksenguz.ertaklar.data.audio.Sfx
import uz.haksenguz.ertaklar.data.models.MapFile
import uz.haksenguz.ertaklar.data.repository.PassportRepository
import uz.haksenguz.ertaklar.navigation.PassportRoute

data class PassportUiState(
    val snapshot: PassportRepository.Snapshot? = null,
    val map: MapFile? = null,
    /** The place whose card is open. */
    val selected: String? = null,
    /** A sticker just bought, to pop in. */
    val fresh: String? = null,
    /** The Uzbek tale to finish first, while the "still locked" message is open. */
    val lockedTwinOf: String? = null
)

/** "🗺️ Mening pasportim": the map, the stamps, the sticker album, and each place's card. */
@HiltViewModel
class PassportViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: PassportRepository,
    private val sfx: Sfx
) : ViewModel() {

    private val selected = MutableStateFlow(savedState.toRoute<PassportRoute>().region)
    private val fresh = MutableStateFlow<String?>(null)
    private val locked = MutableStateFlow<String?>(null)

    val state: StateFlow<PassportUiState> = combine(
        repo.snapshot,
        flow { emit(repo.map()) },
        selected,
        fresh,
        locked
    ) { snapshot, map, sel, new, lock -> PassportUiState(snapshot, map, sel, new, lock) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PassportUiState())

    private val events = Channel<Event>(Channel.BUFFERED)
    val eventFlow: Flow<Event> = events.receiveAsFlow()

    sealed interface Event {
        data class Toast(val text: String) : Event

        data class OpenBook(val key: String) : Event
    }

    fun select(id: String?) {
        selected.value = id
        fresh.value = null
    }

    fun buy(stickerId: String) {
        viewModelScope.launch {
            val s = state.value.snapshot ?: return@launch
            val text = when (repo.buy(stickerId)) {
                StickerState.STAMP -> "🔒 Avval bu yerga sayohat qiling: kitobini oxirigacha o'qing!"

                StickerState.POINTS -> "⭐ Ball yetmaydi: yana ${s.passport.price - s.profile.points} ball kerak"

                StickerState.OK -> {
                    fresh.value = stickerId
                    sfx.chime(happy = true)
                    "🎁 Yangi stiker: ${s.passport.sticker(stickerId)?.first?.name}!"
                }

                else -> return@launch
            }
            events.send(Event.Toast(text))
        }
    }

    /** A book on a place's card: open it where the child left off, unless it is a twin tale still locked. */
    fun openBook(key: String) {
        val s = state.value.snapshot ?: return
        val twin = s.books.firstOrNull { it.key == key }?.twin
        if (twin != null && s.records[twin]?.finished != true) {
            locked.value = s.books.firstOrNull { it.key == twin }?.title
            return
        }
        viewModelScope.launch { events.send(Event.OpenBook(key)) }
    }

    fun dismissLocked() {
        locked.value = null
    }
}
