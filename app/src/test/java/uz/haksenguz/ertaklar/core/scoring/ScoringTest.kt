package uz.haksenguz.ertaklar.core.scoring

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.haksenguz.ertaklar.data.models.AnswerState
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.Page
import uz.haksenguz.ertaklar.data.models.Question

class ScoringTest {

    private fun book(questions: Int, plain: Int) = Book(
        key = "t",
        title = "T",
        category = "folk",
        cover = "c",
        endImage = "e",
        pages = List(questions) { Page(it + 1, "p", "x", image = "i", question = Question("q", listOf("a", "b"), 0)) } +
            List(plain) { Page(questions + it + 1, "p", "x", image = "i") }
    )

    private fun done(vararg pages: Int) = pages.associate { it.toString() to AnswerState(done = true) }

    @Test
    fun noQuestionsGivesThreeStars() {
        assertEquals(3, Scoring.score(book(0, 5), emptyMap()).stars)
    }

    @Test
    fun starsFollowRightAnswers() {
        val b = book(4, 4)
        assertEquals(1, Scoring.score(b, emptyMap()).stars) // never fewer than one
        assertEquals(1, Scoring.score(b, done(1)).stars) // 0.75 -> 1
        assertEquals(2, Scoring.score(b, done(1, 2)).stars) // 1.5 -> 2
        assertEquals(2, Scoring.score(b, done(1, 2, 3)).stars) // 2.25 -> 2
        assertEquals(3, Scoring.score(b, done(1, 2, 3, 4)).stars)
        assertEquals(Scoring.Score(4, 2, 2), Scoring.score(b, done(1, 2)))
    }
}
