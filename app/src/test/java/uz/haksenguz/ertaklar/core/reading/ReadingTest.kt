package uz.haksenguz.ertaklar.core.reading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.data.models.AnswerState
import uz.haksenguz.ertaklar.data.models.BookRecord
import uz.haksenguz.ertaklar.data.models.Question

class ReadingTest {

    private val q = Question("Bu nima?", listOf("Piyoz", "Karam", "Olma"), 0)

    @Test
    fun rightAnswerIsDoneWithPraise() {
        val (state, outcome) = Reading.answer(null, q, view = 2, idx = 0)
        assertTrue(state.done)
        assertEquals(0, state.pick)
        val praise = Scoring.PRAISE[2 % Scoring.PRAISE.size]
        assertEquals(praise, state.praise)
        assertEquals(Reading.Outcome.Right(praise), outcome)
    }

    @Test
    fun wrongAnswerIsRememberedOnce() {
        val (first, o1) = Reading.answer(null, q, view = 1, idx = 2)
        assertEquals(listOf(2), first.wrong)
        assertEquals(Reading.Outcome.Wrong, o1)
        val (again, o2) = Reading.answer(first, q, view = 1, idx = 2)
        assertEquals(listOf(2), again.wrong)
        assertEquals(Reading.Outcome.Ignored, o2)
        val (right, o3) = Reading.answer(again, q, view = 1, idx = 0)
        assertTrue(right.done)
        assertEquals(listOf(2), right.wrong)
        assertTrue(o3 is Reading.Outcome.Right)
    }

    @Test
    fun answeredQuestionIgnoresMoreTaps() {
        val done = AnswerState(done = true, pick = 0, praise = "Ofarin!")
        val (state, outcome) = Reading.answer(done, q, view = 1, idx = 1)
        assertEquals(done, state)
        assertEquals(Reading.Outcome.Ignored, outcome)
    }

    @Test
    fun anyAnswerCountsWhenOkIsNegative() {
        val open = q.copy(ok = -1)
        val (state, outcome) = Reading.answer(null, open, view = 3, idx = 2)
        assertTrue(state.done)
        assertTrue(outcome is Reading.Outcome.Right)
    }

    @Test
    fun settlingRemembersStoryPages() {
        val rec = BookRecord("zumrad")
        assertNull(Reading.settle(rec, pages = 12, view = Reading.COVER, now = 1))
        assertNull(Reading.settle(rec, pages = 12, view = Reading.TITLE, now = 1))
        val s = Reading.settle(rec, pages = 12, view = 5, now = 1)!!
        assertEquals(5, s.record.page)
        assertFalse(s.firstFinish)
    }

    @Test
    fun reachingTheEndFinishesOnceAndClearsThePage() {
        val reading = BookRecord("zumrad", page = 12)
        val first = Reading.settle(reading, pages = 12, view = 13, now = 100)!!
        assertTrue(first.firstFinish)
        assertTrue(first.record.finished)
        assertEquals(0, first.record.page)
        assertEquals(100L, first.record.finishedAt)
        val again = Reading.settle(first.record.copy(page = 4), pages = 12, view = 13, now = 200)!!
        assertFalse(again.firstFinish)
        assertEquals("the first finish dates the book", 100L, again.record.finishedAt)
    }

    @Test
    fun resumeIsOfferedFromPageTwo() {
        assertEquals(0, Reading.resumePage(BookRecord("b", page = 0), 10))
        assertEquals(0, Reading.resumePage(BookRecord("b", page = 1), 10))
        assertEquals(4, Reading.resumePage(BookRecord("b", page = 4), 10))
        assertEquals(0, Reading.resumePage(BookRecord("b", page = 11), 10))
    }
}
