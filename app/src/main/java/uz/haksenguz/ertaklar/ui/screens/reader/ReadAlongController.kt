package uz.haksenguz.ertaklar.ui.screens.reader

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.narration.Narration
import uz.haksenguz.ertaklar.data.audio.ClipPlayer
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.repository.VoiceRepository

/** Listening: whether it's on, the page waiting for its question, and the piece lit up. */
data class ReadAlongState(val on: Boolean = false, val waiting: Int = 0, val highlight: Int = -1)

/**
 * Tinglash: plays the page on screen in a family voice, lights up each piece
 * as it is read, turns the page when it ends, and at an unanswered question
 * waits for the child's answer before going on. Tapping a sentence plays just
 * that one.
 */
class ReadAlongController(
    private val scope: CoroutineScope,
    private val player: ClipPlayer,
    private val voices: VoiceRepository,
    private val book: Book,
    private val host: Host
) {
    /** What the reader does for the read-along. */
    interface Host {
        val view: Int
        fun next()
        fun turnTo(view: Int)
        fun answered(view: Int): Boolean
        val calm: Boolean
        fun toast(text: String)
    }

    private val _state = MutableStateFlow(ReadAlongState())
    val state: StateFlow<ReadAlongState> = _state.asStateFlow()

    /** The voice reading; null when nobody has read this book. */
    var voiceId: String? = null
        set(value) {
            if (value != field) stop()
            field = value
        }

    private var token = 0
    private var timer: Job? = null

    /** The page being read. */
    private var reading: Int? = null

    private val pages get() = book.pages.size

    fun toggle() = if (_state.value.on) stop() else start()

    fun start() {
        if (voiceId == null) return
        _state.update { it.copy(on = true, waiting = 0) }
        val v = host.view
        when {
            v in 1..pages -> playView(v)
            v > pages -> host.turnTo(1)
            else -> host.next()
        }
    }

    fun stop() {
        token++
        timer?.cancel()
        reading = null
        player.stop()
        _state.value = ReadAlongState()
    }

    /** The book came to rest on [view]. */
    fun onPageSettled(view: Int) {
        val s = _state.value
        if (!s.on) {
            if (player.playing) stopSay()
            return
        }
        // The same page: carry on as we were.
        if (view == reading && (player.playing || s.waiting == view)) return
        timer?.cancel()
        _state.update { it.copy(waiting = 0) }
        when {
            view in 1..pages -> playView(view)
            view <= 0 -> later(500)
            else -> stop()
        }
    }

    /** The child answered; a right answer lets the reading go on. */
    fun onAnswer(right: Boolean) {
        val s = _state.value
        if (s.on && right && s.waiting != 0) {
            _state.update { it.copy(waiting = 0) }
            later(1300)
        }
    }

    /** A tap on piece [seg]: read just that one (or jump to it while reading). */
    fun say(seg: Int) {
        val voice = voiceId ?: return
        val view = host.view
        if (view < 1 || view > pages) return
        if (_state.value.on) {
            if (player.playing && reading == view) currentMarks?.getOrNull(seg)?.let(player::seek)
            return
        }
        val t = ++token
        scope.launch {
            val clip = voices.clip(voice, book.key, view) ?: return@launch
            val page = book.pages[view - 1]
            if (t != token || !Narration.fits(clip.marks, clip.sig, page.segments)) return@launch
            val from = clip.marks.getOrNull(seg) ?: return@launch
            player.play(
                scope = scope,
                uri = Uri.fromFile(clip.file),
                marks = clip.marks,
                from = from,
                to = clip.marks.getOrNull(seg + 1),
                onSeg = { _state.update { it.copy(highlight = seg) } },
                onEnd = { _state.update { it.copy(highlight = -1) } }
            )
        }
    }

    private var currentMarks: List<Double>? = null

    private fun stopSay() {
        player.stop()
        _state.update { it.copy(highlight = -1) }
    }

    private fun playView(view: Int) {
        val t = ++token
        player.stop()
        _state.update { it.copy(highlight = -1) }
        reading = view
        val voice = voiceId ?: return
        scope.launch {
            val clip = voices.clip(voice, book.key, view)
            if (t != token || !_state.value.on) return@launch
            if (clip == null) {
                stop()
                host.toast("🎙️ Bu sahifa hali o'qib berilmagan")
                return@launch
            }
            val fits = Narration.fits(clip.marks, clip.sig, book.pages[view - 1].segments)
            currentMarks = clip.marks
            player.play(
                scope = scope,
                uri = Uri.fromFile(clip.file),
                marks = clip.marks,
                onSeg = { i -> if (fits) _state.update { it.copy(highlight = i) } },
                onEnd = { err -> pageDone(view, t, err) }
            )
        }
    }

    private fun pageDone(view: Int, t: Int, err: Throwable?) {
        if (t != token || !_state.value.on) return
        _state.update { it.copy(highlight = -1) }
        if (err != null) {
            stop()
            return
        }
        // At bedtime the questions rest, so there is nothing to wait for.
        val question = book.pages[view - 1].question
        if (question != null && !host.calm && !host.answered(view)) {
            _state.update { it.copy(waiting = view) }
            host.toast("💡 Endi savolga javob bering!")
            return
        }
        later(900)
    }

    private fun later(ms: Long) {
        timer?.cancel()
        timer = scope.launch {
            delay(ms)
            if (_state.value.on) host.next()
        }
    }
}
