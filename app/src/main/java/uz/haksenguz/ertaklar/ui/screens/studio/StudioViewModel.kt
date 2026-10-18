package uz.haksenguz.ertaklar.ui.screens.studio

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.haksenguz.ertaklar.core.narration.Narration
import uz.haksenguz.ertaklar.data.audio.ClipPlayer
import uz.haksenguz.ertaklar.data.audio.PageRecorder
import uz.haksenguz.ertaklar.data.audio.PcmEnergies
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.Voice
import uz.haksenguz.ertaklar.data.repository.SettingsRepository
import uz.haksenguz.ertaklar.data.repository.VoiceRepository
import uz.haksenguz.ertaklar.navigation.StudioRoute

/** A question the studio asks before going on. */
enum class StudioConfirm { Rerecord, Delete }

data class StudioUiState(
    val book: Book? = null,
    val page: Int = 1,
    val voice: Voice? = null,
    /** Pages this voice has recorded in this book. */
    val recorded: Set<Int> = emptySet(),
    val recording: Boolean = false,
    /** Busy opening the microphone or saving: the button waits. */
    val busy: Boolean = false,
    /** Loudness now, 0..1, for the ring around the record button. */
    val level: Float = 0f,
    val seconds: Int = 0,
    /** A message for the grown-up (Uzbek; shown through the display). */
    val status: String? = null,
    val playing: Boolean = false,
    /** The piece lit up while playing back, or -1. */
    val highlight: Int = -1,
    val confirm: StudioConfirm? = null,
    val voicesOpen: Boolean = false
) {
    val pages get() = book?.pages?.size ?: 0
    val segments: List<String> get() = book?.pages?.getOrNull(page - 1)?.segments.orEmpty()
    val hasRecording get() = page in recorded
}

sealed interface StudioEvent {
    data class Share(val file: File, val text: String) : StudioEvent
}

/**
 * Ovoz yozish: a grown-up reads the book aloud page by page — the title, then
 * the text, pausing a little between sentences — and sends it as one file.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StudioViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val voices: VoiceRepository,
    private val settings: SettingsRepository,
    private val recorder: PageRecorder,
    private val player: ClipPlayer
) : ViewModel() {

    private val route = savedState.toRoute<StudioRoute>()
    private val _state = MutableStateFlow(StudioUiState(page = route.page))
    val state: StateFlow<StudioUiState> = _state.asStateFlow()
    private val events = Channel<StudioEvent>(Channel.BUFFERED)
    val eventFlow: Flow<StudioEvent> = events.receiveAsFlow()
    private val voice = MutableStateFlow<Voice?>(null)
    private var meter: Job? = null

    init {
        viewModelScope.launch {
            val book = content.library()[route.key] ?: return@launch
            _state.update { it.copy(book = book, page = route.page.coerceIn(1, book.pages.size)) }
            val last = settings.settings.first().recVoice?.let { voices.voice(it) }
            if (last != null) voice.value = last else _state.update { it.copy(voicesOpen = true) }
        }
        viewModelScope.launch {
            voice.collectLatest { v ->
                _state.update { it.copy(voice = v) }
                (v?.let { voices.pages(it.id, route.key) } ?: flowOf(emptySet())).collect { pages ->
                    _state.update { it.copy(recorded = pages) }
                }
            }
        }
    }

    fun openVoices(open: Boolean) {
        if (_state.value.recording || _state.value.busy) return
        _state.update { it.copy(voicesOpen = open) }
    }

    /** Record with [v] from now on. */
    fun useVoice(v: Voice) {
        stopPlayback()
        voice.value = v
        _state.update { it.copy(voicesOpen = false, status = null) }
        viewModelScope.launch { settings.setRecVoice(v.id) }
    }

    fun goTo(page: Int) {
        val s = _state.value
        if (s.recording || s.busy) return
        stopPlayback()
        _state.update { it.copy(page = page.coerceIn(1, s.pages), status = null) }
    }

    /** The red button: start, or stop and keep the recording. */
    fun toggleRecord() {
        val s = _state.value
        when {
            s.busy -> Unit
            s.voice == null -> openVoices(true)
            s.recording -> finish()
            s.hasRecording -> _state.update { it.copy(confirm = StudioConfirm.Rerecord) }
            else -> start()
        }
    }

    fun micDenied() {
        _state.update { it.copy(status = "Mikrofonga ruxsat bering va qaytadan bosing.") }
    }

    fun confirm(yes: Boolean) {
        val what = _state.value.confirm
        _state.update { it.copy(confirm = null) }
        if (!yes) return
        when (what) {
            StudioConfirm.Rerecord -> start()
            StudioConfirm.Delete -> remove()
            null -> Unit
        }
    }

    private fun start() {
        stopPlayback()
        try {
            recorder.start(voices.recordingFile()) { viewModelScope.launch { finish() } }
        } catch (e: Exception) {
            _state.update { it.copy(status = "Mikrofonga ruxsat bering va qaytadan bosing.") }
            return
        }
        _state.update { it.copy(recording = true, status = null, seconds = 0, level = 0f) }
        meter = viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(level = recorder.level(), seconds = recorder.seconds().toInt()) }
                delay(60)
            }
        }
    }

    private fun finish() {
        val s = _state.value
        if (!s.recording) return
        meter?.cancel()
        val v = s.voice ?: return
        val book = s.book ?: return
        val page = s.page
        val segments = s.segments
        _state.update { it.copy(recording = false, busy = true, level = 0f, status = "Saqlanmoqda...") }
        viewModelScope.launch {
            val status = try {
                val file = recorder.stop() ?: error("nothing recorded")
                val loud = withContext(Dispatchers.IO) { PcmEnergies.measure(file) }
                if (loud.duration < 1) {
                    file.delete()
                    "Juda qisqa chiqdi. Qaytadan yozing."
                } else {
                    val marks = Narration.marks(loud.db, segments.map { it.length })
                    val duration = (loud.duration * 100).roundToInt() / 100.0
                    voices.saveClip(v.id, book.key, page, file, PageRecorder.MIME, duration, marks, Narration.sig(segments))
                    if (page < book.pages.size) {
                        "✓ Saqlandi. Tinglab ko'ring yoki keyingi sahifaga o'ting."
                    } else {
                        "✓ Saqlandi. Kitob tugadi — endi uni yuboring!"
                    }
                }
            } catch (e: Exception) {
                "Yozib bo'lmadi. Qaytadan urinib ko'ring."
            }
            _state.update { it.copy(busy = false, status = status) }
        }
    }

    /** Plays this page back with its sentences lit up, as the child will hear it. */
    fun preview() {
        if (_state.value.playing) {
            stopPlayback()
            return
        }
        val s = _state.value
        val v = s.voice ?: return
        viewModelScope.launch {
            val clip = voices.clip(v.id, route.key, s.page) ?: return@launch
            _state.update { it.copy(playing = true) }
            player.play(
                scope = viewModelScope,
                uri = Uri.fromFile(clip.file),
                marks = clip.marks,
                onSeg = { i -> _state.update { it.copy(highlight = i) } },
                onEnd = { _state.update { it.copy(playing = false, highlight = -1) } }
            )
        }
    }

    fun askDelete() {
        _state.update { it.copy(confirm = StudioConfirm.Delete) }
    }

    private fun remove() {
        val s = _state.value
        val v = s.voice ?: return
        stopPlayback()
        viewModelScope.launch {
            voices.deleteClip(v.id, route.key, s.page)
            _state.update { it.copy(status = null) }
        }
    }

    /** The book's recordings as one file, for the share sheet. */
    fun send() {
        val s = _state.value
        val v = s.voice ?: return
        if (s.recorded.isEmpty() || s.recording) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            runCatching { voices.exportPack(v.id, route.key) }.onSuccess { file ->
                events.send(StudioEvent.Share(file, "${v.avatar} ${v.name}: «${s.book?.title}»"))
            }
            _state.update { it.copy(busy = false) }
        }
    }

    private fun stopPlayback() {
        player.stop()
        _state.update { it.copy(playing = false, highlight = -1) }
    }

    /** Leaving the studio: nothing keeps recording or playing. */
    override fun onCleared() {
        meter?.cancel()
        recorder.cancel()
        player.release()
    }
}
