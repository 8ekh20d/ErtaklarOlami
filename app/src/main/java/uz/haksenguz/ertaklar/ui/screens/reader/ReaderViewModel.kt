package uz.haksenguz.ertaklar.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.reading.Bedtime
import uz.haksenguz.ertaklar.core.reading.Reading
import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.core.text.Gloss
import uz.haksenguz.ertaklar.data.audio.ClipPlayer
import uz.haksenguz.ertaklar.data.audio.KoreanTts
import uz.haksenguz.ertaklar.data.audio.Sfx
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.BookRecord
import uz.haksenguz.ertaklar.data.models.KoreanBook
import uz.haksenguz.ertaklar.data.models.Region
import uz.haksenguz.ertaklar.data.models.Voice
import uz.haksenguz.ertaklar.data.repository.GateRepository
import uz.haksenguz.ertaklar.data.repository.PassportRepository
import uz.haksenguz.ertaklar.data.repository.ProfileRepository
import uz.haksenguz.ertaklar.data.repository.ProgressRepository
import uz.haksenguz.ertaklar.data.repository.SettingsRepository
import uz.haksenguz.ertaklar.data.repository.VoiceRepository
import uz.haksenguz.ertaklar.navigation.ColoringRoute
import uz.haksenguz.ertaklar.navigation.CompareRoute
import uz.haksenguz.ertaklar.navigation.OrderRoute
import uz.haksenguz.ertaklar.navigation.PassportRoute
import uz.haksenguz.ertaklar.navigation.ReaderRoute
import uz.haksenguz.ertaklar.navigation.TraceRoute

/**
 * "Xayrli tun": the good-night screen after tonight's last page. [next] says
 * where tomorrow begins; [stamp] is a place the finished book has just stamped.
 */
data class Goodnight(val name: String, val next: String, val stamp: Region? = null, val stampedAt: Long = 0)

/** A book opened for reading. */
data class ReaderUiState(
    val loading: Boolean = true,
    val book: Book? = null,
    val record: BookRecord = BookRecord(""),
    /** The view the book opens on: the cover, or the child's page when continuing. */
    val startView: Int = Reading.COVER,
    /** The Korean twin this tale opens once it is finished, as (key, title). */
    val twin: Pair<String, String>? = null,
    val points: Int = 0,
    /** The book's Korean helper, if it has one. */
    val korean: KoreanBook? = null,
    val gloss: Gloss? = null,
    /** 🇰🇷: the one view whose Korean is shown; it turns off when the page changes. */
    val koView: Int? = null,
    /** 가: Hangul sound hints, kept from page to page. */
    val reading: Boolean = false,
    /** The open word card: (term, Korean meaning). */
    val glossCard: Pair<String, String>? = null,
    /** Who has read this book aloud, and who is reading now. */
    val voices: List<Voice> = emptyList(),
    val voiceId: String? = null,
    val readAlong: ReadAlongState = ReadAlongState(),
    /** At bedtime: tonight's pages. The questions rest, and past the last one it is good night. */
    val tonight: Bedtime.Tonight? = null,
    val goodnight: Goodnight? = null,
    /** The page sound and the chimes are on. */
    val sound: Boolean = true,
    /** The passport place this book takes the child to. */
    val place: Region? = null
) {
    val pages get() = book?.pages?.size ?: 0
    val lastView get() = Reading.lastView(pages)
    val resumePage get() = Reading.resumePage(record, pages)
    val score get() = book?.let { Scoring.score(it, record.answers) }
    val calm get() = tonight != null
    val voice get() = voices.firstOrNull { it.id == voiceId }

    /** The last view the pages reach: the end, or tonight's last page. */
    val reachable get() = tonight?.limit ?: lastView

    /** Whether 🇰🇷 can be offered on [view]. */
    fun hasKorean(view: Int): Boolean =
        korean != null && ((view == lastView && korean.moral.isNotEmpty()) || korean.pages.getOrNull(view - 1) != null)
}

/** Short messages shown at the bottom of the screen, and where to go next. */
sealed interface ReaderEvent {
    data class Toast(val text: String) : ReaderEvent
    data object OpenQuiz : ReaderEvent

    /** The recording studio, at this story page. */
    data class OpenStudio(val page: Int) : ReaderEvent

    /** Turn to [view] (read-along and bedtime turn pages by themselves). */
    data class Turn(val view: Int) : ReaderEvent

    /** A game from the play corner. */
    data class OpenGame(val route: Any) : ReaderEvent
}

private data class Helpers(
    val koView: Int?,
    val reading: Boolean,
    val glossCard: Pair<String, String>?,
    val voiceSetting: String?,
    val sound: Boolean
)

private data class Listening(val voices: List<Voice>, val readAlong: ReadAlongState, val goodnight: Goodnight?)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val profiles: ProfileRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val tts: KoreanTts,
    private val gate: GateRepository,
    private val voiceRepo: VoiceRepository,
    private val player: ClipPlayer,
    private val passport: PassportRepository,
    private val sfx: Sfx
) : ViewModel() {

    private val route = savedState.toRoute<ReaderRoute>()
    private val opened = MutableStateFlow<Opened?>(null)
    private val koView = MutableStateFlow<Int?>(null)
    private val glossCard = MutableStateFlow<Pair<String, String>?>(null)
    private val goodnight = MutableStateFlow<Goodnight?>(null)
    private val events = Channel<ReaderEvent>(Channel.BUFFERED)
    private var view = Reading.COVER
    private var readAlong: ReadAlongController? = null
    private val readAlongState = MutableStateFlow(ReadAlongState())

    val eventFlow: Flow<ReaderEvent> = events.receiveAsFlow()

    /** Set while the grown-ups gate is up. */
    val gateOpen = MutableStateFlow(false)

    private class Opened(
        val profileId: String,
        val book: Book,
        val startView: Int,
        val twin: Pair<String, String>?,
        val korean: KoreanBook?,
        val tonight: Bedtime.Tonight?,
        val place: Region?
    ) {
        val gloss = korean?.let { Gloss(it.forms) }
    }

    private val helpers = combine(koView, settings.settings, glossCard) { k, s, g -> Helpers(k, s.reading, g, s.voice, s.sound) }

    private val bookVoices = opened.flatMapLatest { o -> o?.let { voiceRepo.voicesForBook(it.book.key) } ?: flowOf(emptyList()) }

    private val listening = combine(bookVoices, readAlongState, goodnight) { v, r, g -> Listening(v, r, g) }

    val state: StateFlow<ReaderUiState> = opened.filterNotNull().flatMapLatest { o ->
        combine(progress.record(o.profileId, o.book.key), profiles.active.filterNotNull(), helpers, listening) { record, profile, h, l ->
            ReaderUiState(
                loading = false,
                book = o.book,
                record = record,
                startView = o.startView,
                twin = o.twin,
                points = profile.points,
                korean = o.korean,
                gloss = o.gloss,
                koView = h.koView,
                reading = h.reading,
                glossCard = h.glossCard,
                voices = l.voices,
                voiceId = (l.voices.firstOrNull { it.id == h.voiceSetting } ?: l.voices.firstOrNull())?.id,
                sound = h.sound,
                readAlong = l.readAlong,
                tonight = o.tonight,
                goodnight = l.goodnight,
                place = o.place
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    private val host = object : ReadAlongController.Host {
        override val view: Int get() = this@ReaderViewModel.view
        override val calm: Boolean get() = opened.value?.tonight != null

        override fun next() = this@ReaderViewModel.next()

        override fun turnTo(view: Int) {
            viewModelScope.launch { events.send(ReaderEvent.Turn(view)) }
        }

        override fun answered(view: Int) = state.value.record.answers[view.toString()]?.done == true

        override fun toast(text: String) {
            viewModelScope.launch { events.send(ReaderEvent.Toast(text)) }
        }
    }

    init {
        viewModelScope.launch {
            val library = content.library()
            val book = library[route.key] ?: return@launch
            val profile = profiles.active.filterNotNull().first()
            val record = progress.record(profile.id, book.key).first()
            val tonight = if (route.bedtime) Bedtime.tonight(record, book.pages.size, settings.settings.first().bedtimePages) else null
            val start = when {
                tonight != null -> tonight.from
                route.page in 1..book.pages.size -> route.page
                route.resume && record.page >= 1 -> record.page
                else -> Reading.COVER
            }
            val twin = library.books.firstOrNull { it.twin == book.key }?.let { it.key to it.title }
            readAlong = ReadAlongController(viewModelScope, player, voiceRepo, book, host).also { r ->
                viewModelScope.launch { r.state.collect { readAlongState.value = it } }
            }
            val place = passport.passport().regionOf(book)
            // The book opens here: listening that starts by itself must begin on this page,
            // even before the screen has reported it.
            view = start
            opened.value = Opened(profile.id, book, start, twin, library.korean[book.key], tonight, place)
            // Someone has read this book aloud: say so, and at bedtime their voice reads on by itself.
            val first = state.filterNotNull().first { !it.loading }
            val voice = first.voice
            readAlong?.voiceId = voice?.id
            if (voice != null) {
                if (tonight != null) {
                    readAlong?.start()
                } else {
                    events.send(ReaderEvent.Toast("${voice.avatar} ${voice.name} bu ertakni o'qib bergan — 🎧 bosing!"))
                }
            }
        }
        // The voice may change (another one chosen, or one recorded in the studio).
        viewModelScope.launch {
            state.collect { s -> if (!s.loading) readAlong?.voiceId = s.voiceId }
        }
    }

    /** The child came to rest on [view]: remember it; the Korean and the word card are for one page only. */
    fun onPageSettled(view: Int) {
        val o = opened.value ?: return
        if (view != this.view) {
            koView.value = null
            closeGloss()
        }
        this.view = view
        readAlong?.onPageSettled(view)
        viewModelScope.launch {
            val fresh = if (view == Reading.lastView(o.book.pages.size)) freshPlace(o) else null
            val settled = progress.settle(o.profileId, o.book, view) ?: return@launch
            if (!settled.firstFinish) return@launch
            if (o.twin != null) events.send(ReaderEvent.Toast("🔓 Yangi ertak ochildi: «${o.twin.second}»!"))
            // The first book finished there stamps its place in the passport.
            if (fresh != null) {
                if (o.twin != null) delay(2_000)
                events.send(ReaderEvent.Toast("🗺️ Yangi muhr: ${fresh.name}!"))
            }
        }
    }

    /** The next page; at bedtime, past tonight's last one, good night. */
    fun next() {
        val o = opened.value ?: return
        val tonight = o.tonight
        if (tonight != null && view >= tonight.limit) {
            goodnight()
            return
        }
        viewModelScope.launch { events.send(ReaderEvent.Turn(view + 1)) }
    }

    fun answer(view: Int, idx: Int) {
        val o = opened.value ?: return
        viewModelScope.launch {
            when (val outcome = progress.answer(o.profileId, o.book, view, idx)) {
                is Reading.Outcome.Right -> {
                    events.send(ReaderEvent.Toast("⭐ ${outcome.praise} +${Scoring.ANSWER} ball"))
                    sfx.chime(happy = true)
                    readAlong?.onAnswer(true)
                }

                Reading.Outcome.Wrong -> events.send(ReaderEvent.Toast("🤔 Yana bir o'ylab ko'ring!"))

                Reading.Outcome.Ignored -> Unit
            }
        }
    }

    // ---------- listening ----------

    /** Tinglash / To'xtatish. */
    fun toggleListen() {
        readAlong?.toggle()
    }

    /** A tap on piece [seg] of the page: hear just that one. */
    fun say(seg: Int) {
        readAlong?.say(seg)
    }

    /** The next voice that has read this book (the face next to "Tinglash"). */
    fun nextVoice() {
        val s = state.value
        if (s.voices.size < 2) return
        val i = s.voices.indexOfFirst { it.id == s.voiceId }
        val v = s.voices[(i + 1) % s.voices.size]
        val wasOn = s.readAlong.on
        readAlong?.voiceId = v.id
        viewModelScope.launch {
            settings.setVoice(v.id)
            events.send(ReaderEvent.Toast("${v.avatar} ${v.name} o'qib beradi"))
            if (wasOn) readAlong?.start()
        }
    }

    // ---------- bedtime ----------

    /** Tonight's last page is behind us: remember where tomorrow begins, and say good night. */
    private fun goodnight() {
        val o = opened.value ?: return
        val tonight = o.tonight ?: return
        readAlong?.stop()
        viewModelScope.launch {
            val name = profiles.active.filterNotNull().first().name
            val pages = o.book.pages.size
            var stamp: Region? = null
            val next = if (tonight.limit >= pages) {
                // The whole book is read: it counts as finished, and may stamp the passport.
                stamp = freshPlace(o)
                progress.settle(o.profileId, o.book, Reading.lastView(pages))
                "Ertak tugadi! Ertaga yangisini o'qiymiz."
            } else {
                progress.rememberPage(o.profileId, o.book, tonight.limit + 1)
                "Ertaga shu yerdan davom etamiz: «${o.book.title}», ${tonight.limit + 1}-sahifa."
            }
            goodnight.value = Goodnight(name, next, stamp, System.currentTimeMillis())
            sfx.lullaby()
        }
    }

    /** The book's place, if finishing the book now would give it its first stamp. */
    private suspend fun freshPlace(o: Opened): Region? {
        val place = o.place ?: return null
        val records = progress.records(o.profileId).first()
        if (records[o.book.key]?.finished == true) return null
        val stamps = passport.passport().stamps(content.library().books, records)
        return place.takeIf { it.id !in stamps }
    }

    // ---------- sounds ----------

    /** A page starts to turn. */
    fun turning(cover: Boolean) = sfx.page(cover, bedtime = opened.value?.tonight != null)

    /** The picture was tapped. */
    fun poke() = sfx.chime(bedtime = opened.value?.tonight != null)

    /** 🔊 on and off: the page sound and the chimes. */
    fun toggleSound() {
        viewModelScope.launch { settings.setSound(!state.value.sound) }
    }

    /** The stamp on the last page: the passport, at this book's place. */
    fun openPassport() {
        val place = opened.value?.place ?: return
        readAlong?.stop()
        viewModelScope.launch { events.send(ReaderEvent.OpenGame(PassportRoute(place.id))) }
    }

    // ---------- Korean helper ----------

    /** 🇰🇷 on [view]: the Korean under each sentence (it takes the place of 가 there). */
    fun toggleKorean(view: Int) {
        koView.value = if (koView.value == view) null else view
    }

    /** 가: turning it on hides the Korean so the sound hints show. */
    fun toggleReading() {
        val on = !state.value.reading
        if (on) koView.value = null
        viewModelScope.launch { settings.setReading(on) }
    }

    /** A tap on an underlined word: its Korean meaning. */
    fun openGloss(i: Int) {
        glossCard.value = opened.value?.korean?.word(i)
    }

    fun closeGloss() {
        if (glossCard.value != null) tts.stop()
        glossCard.value = null
    }

    fun sayGloss() {
        val meaning = glossCard.value?.second ?: return
        viewModelScope.launch {
            if (!tts.speak(meaning)) events.send(ReaderEvent.Toast("Bu qurilmada koreyscha ovoz topilmadi"))
        }
    }

    // ---------- studio and quiz ----------

    /** Ovoz yozish: for grown-ups, behind a small sum. */
    fun openStudio() {
        if (gate.isOpen) studioAt() else gateOpen.value = true
    }

    fun gatePassed() {
        gate.open()
        gateOpen.value = false
        studioAt()
    }

    private fun studioAt() {
        val pages = opened.value?.book?.pages?.size ?: return
        tts.stop()
        readAlong?.stop()
        viewModelScope.launch { events.send(ReaderEvent.OpenStudio(view.coerceIn(1, pages))) }
    }

    fun openQuiz() {
        readAlong?.stop()
        viewModelScope.launch { events.send(ReaderEvent.OpenQuiz) }
    }

    /** 🎨 on story page [view] (or the end picture): a guessed riddle colours its uncovered picture. */
    fun openColoring(view: Int) {
        val s = state.value
        val book = s.book ?: return
        val page = book.pages.getOrNull(view.coerceIn(1, book.pages.size) - 1)
        val solved = view <= book.pages.size && s.record.answers[view.toString()]?.done == true
        play(ColoringRoute(book.key, view, reveal = solved && page?.revealLineArt != null))
    }

    fun openOrder() {
        state.value.book?.let { play(OrderRoute(it.key)) }
    }

    fun openCompare() {
        state.value.book?.let { play(CompareRoute(it.key)) }
    }

    fun openTrace(view: Int) {
        state.value.book?.let { play(TraceRoute(it.key, view)) }
    }

    private fun play(route: Any) {
        readAlong?.stop()
        viewModelScope.launch { events.send(ReaderEvent.OpenGame(route)) }
    }

    /** Leaving the screen for a while (the studio, the quiz): nothing keeps playing. */
    fun pause() {
        readAlong?.stop()
    }

    override fun onCleared() {
        tts.stop()
        readAlong?.stop()
        player.release()
    }
}
