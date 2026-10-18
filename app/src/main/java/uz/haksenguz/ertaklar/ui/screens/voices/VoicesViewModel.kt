package uz.haksenguz.ertaklar.ui.screens.voices

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.data.models.Voice
import uz.haksenguz.ertaklar.data.repository.VoiceRepository

/** The voice form: a new voice ([id] null) or one being changed. */
data class VoiceForm(
    val id: String?,
    val name: String = "",
    val avatar: String = Voice.AVATARS.first(),
    val confirmDelete: Boolean = false
)

data class VoicesUiState(
    val voices: List<Voice> = emptyList(),
    val form: VoiceForm? = null,
    /** What happened to the last pack file added (Uzbek; shown through the display). */
    val note: String? = null,
    val busy: Boolean = false
)

sealed interface VoicesEvent {
    /** A voice was chosen (or made) to record with. */
    data class Use(val voice: Voice) : VoicesEvent

    data class Share(val file: File, val text: String) : VoicesEvent
}

/** "Kimning ovozi?": who is reading, and their voice packs. */
@HiltViewModel
class VoicesViewModel @Inject constructor(private val repo: VoiceRepository) : ViewModel() {

    private val form = MutableStateFlow<VoiceForm?>(null)
    private val note = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)
    private val events = Channel<VoicesEvent>(Channel.BUFFERED)

    val eventFlow: Flow<VoicesEvent> = events.receiveAsFlow()

    val state: StateFlow<VoicesUiState> = combine(repo.voices, form, note, busy) { v, f, n, b -> VoicesUiState(v, f, n, b) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VoicesUiState())

    fun use(voice: Voice) {
        viewModelScope.launch { events.send(VoicesEvent.Use(voice)) }
    }

    fun edit(voice: Voice?) {
        note.value = null
        val taken = state.value.voices.map { it.avatar }.toSet()
        form.value = voice?.let { VoiceForm(it.id, it.name, it.avatar) }
            ?: VoiceForm(null, avatar = Voice.AVATARS.firstOrNull { it !in taken } ?: Voice.AVATARS.first())
    }

    fun setName(name: String) = form.update { it?.copy(name = name.take(24)) }

    fun setAvatar(avatar: String) = form.update { it?.copy(avatar = avatar) }

    fun cancel() {
        form.value = null
    }

    fun save() {
        val f = form.value ?: return
        if (f.name.isBlank()) return
        viewModelScope.launch {
            val v = if (f.id == null) repo.createVoice(f.name, f.avatar) else repo.updateVoice(f.id, f.name, f.avatar)
            form.value = null
            if (v != null) events.send(VoicesEvent.Use(v))
        }
    }

    fun askDelete(ask: Boolean) = form.update { it?.copy(confirmDelete = ask) }

    fun delete() {
        val id = form.value?.id ?: return
        viewModelScope.launch {
            repo.deleteVoice(id)
            form.value = null
        }
    }

    /** Every book this voice has recorded, in one file. */
    fun sendAll() {
        val id = form.value?.id ?: return
        viewModelScope.launch {
            busy.value = true
            val v = repo.voice(id)
            runCatching { repo.exportPack(id, null) }.onSuccess { file ->
                events.send(VoicesEvent.Share(file, "${v?.avatar} ${v?.name}"))
            }
            busy.value = false
        }
    }

    /** A pack someone sent, e.g. buvi through Telegram. */
    fun import(uri: Uri) {
        viewModelScope.launch {
            busy.value = true
            note.value = runCatching { repo.importPack(uri) }.fold(
                onSuccess = { r ->
                    "✓ ${r.voice.avatar} ${r.voice.name}: ${r.count} ta sahifa qo'shildi." +
                        if (r.unplayable) " Diqqat: bu telefon bu yozuvlarni o'qiy olmasligi mumkin." else ""
                },
                onFailure = { "Bu fayl ertak ovozi emas. Telegram'dagi .json faylni tanlang." }
            )
            busy.value = false
        }
    }
}
