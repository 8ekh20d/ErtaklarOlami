package uz.haksenguz.ertaklar.ui.screens.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.data.audio.Sfx
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.models.Question
import uz.haksenguz.ertaklar.data.repository.ProfileRepository
import uz.haksenguz.ertaklar.data.repository.ProgressRepository
import uz.haksenguz.ertaklar.navigation.QuizRoute

/** What the last answer did. */
sealed interface QuizResult {
    data object Wrong : QuizResult

    /** [first]: the book's quiz points were just earned (they come only once). */
    data class Right(val first: Boolean) : QuizResult
}

data class QuizUiState(
    val question: Question? = null,
    /** The answers' indices in the order shown (shuffled once). */
    val order: List<Int> = emptyList(),
    /** The quiz in Korean (question, then each answer), when the book has it. */
    val korean: List<String>? = null,
    val koShown: Boolean = false,
    val result: QuizResult? = null
)

/** "Bilimdon testi": the book's lesson as a question; the right answer earns 50 points once. */
@HiltViewModel
class QuizViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val profiles: ProfileRepository,
    private val progress: ProgressRepository,
    private val sfx: Sfx
) : ViewModel() {

    private val key = savedState.toRoute<QuizRoute>().key
    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private val done = Channel<Unit>(Channel.CONFLATED)

    /** Back to the library, a moment after a right answer. */
    val doneEvents: Flow<Unit> = done.receiveAsFlow()

    init {
        viewModelScope.launch {
            val library = content.library()
            val book = library[key]
            val quiz = book?.quiz ?: Scoring.DEFAULT_QUIZ
            // The Korean quiz matches only the book's own quiz.
            val korean = if (book?.quiz != null) library.korean[key]?.quiz else null
            _state.value = QuizUiState(question = quiz, order = quiz.a.indices.shuffled(), korean = korean)
        }
    }

    fun toggleKorean() = _state.update { it.copy(koShown = !it.koShown) }

    fun answer(i: Int) {
        val q = _state.value.question ?: return
        if (_state.value.result is QuizResult.Right) return
        if (i != q.ok) {
            _state.update { it.copy(result = QuizResult.Wrong) }
            return
        }
        viewModelScope.launch {
            val profile = profiles.active.filterNotNull().first()
            val first = progress.passQuiz(profile.id, key)
            sfx.chime(happy = true)
            _state.update { it.copy(result = QuizResult.Right(first)) }
            delay(2_200)
            done.send(Unit)
        }
    }

    fun dismissResult() {
        if (_state.value.result == QuizResult.Wrong) _state.update { it.copy(result = null) }
    }
}
