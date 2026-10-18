package uz.haksenguz.ertaklar.core.scoring

import kotlin.math.max
import kotlin.math.roundToInt
import uz.haksenguz.ertaklar.data.models.AnswerState
import uz.haksenguz.ertaklar.data.models.Book

/** Points for each activity, and a finished book's stars. */
object Scoring {
    const val ANSWER = 10
    const val QUIZ = 50
    const val ORDER = 30
    const val COMPARE = 30
    const val DICT = 10
    const val TRACE = 5

    /** Encouragement after a right answer; children learn these words, so they stay Uzbek. */
    val PRAISE = listOf("Barakalla!", "Ofarin!", "Juda to'g'ri!", "Qoyil!", "Zo'r!")

    data class Score(val asked: Int, val right: Int, val stars: Int)

    /** Questions answered right out of those asked, as 1–3 stars (3 when a book asks none). */
    fun score(book: Book, answers: Map<String, AnswerState>): Score {
        val asked = book.pages.count { it.question != null }
        val right = answers.values.count { it.done }
        val stars = if (asked > 0) max(1, (right.toDouble() / asked * 3).roundToInt()) else 3
        return Score(asked, right, stars)
    }
}
