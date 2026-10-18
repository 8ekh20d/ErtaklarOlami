package uz.haksenguz.ertaklar.core.reading

import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.data.models.AnswerState
import uz.haksenguz.ertaklar.data.models.BookRecord
import uz.haksenguz.ertaklar.data.models.Question

/**
 * How a book is read, page by page.
 *
 * A book is a row of views: -1 the closed cover, 0 the title page, 1..N the
 * story pages, N + 1 the end ("Tamom!").
 */
object Reading {
    const val COVER = -1
    const val TITLE = 0

    fun lastView(pages: Int) = pages + 1

    /** What tapping an answer did. */
    sealed interface Outcome {
        data class Right(val praise: String) : Outcome
        data object Wrong : Outcome

        /** Already answered, or the same wrong answer again: nothing happens. */
        data object Ignored : Outcome
    }

    /** The page question's new state after tapping answer [idx] on story page [view]. */
    fun answer(state: AnswerState?, question: Question, view: Int, idx: Int): Pair<AnswerState, Outcome> {
        val st = state ?: AnswerState()
        if (st.done) return st to Outcome.Ignored
        val right = question.ok < 0 || idx == question.ok
        return when {
            right -> {
                val praise = Scoring.PRAISE[(view + idx) % Scoring.PRAISE.size]
                st.copy(done = true, pick = idx, praise = praise) to Outcome.Right(praise)
            }

            idx !in st.wrong -> st.copy(wrong = st.wrong + idx) to Outcome.Wrong

            else -> st to Outcome.Ignored
        }
    }

    /** A record after the child settles on [view]; [firstFinish] is true the first time the end is reached. */
    data class Settled(val record: BookRecord, val firstFinish: Boolean)

    /**
     * Remembers the page the child is on; reaching the end marks the book read
     * and leaves nothing to continue. The cover and title page change nothing (null).
     */
    fun settle(record: BookRecord, pages: Int, view: Int, now: Long): Settled? = when {
        view == lastView(pages) -> Settled(
            record.copy(finished = true, finishedAt = record.finishedAt ?: now, page = 0),
            firstFinish = !record.finished
        )

        view in 1..pages -> Settled(record.copy(page = view), firstFinish = false)

        else -> null
    }

    /** The story page the title page offers to continue from (page 1 is just the next turn), or 0. */
    fun resumePage(record: BookRecord, pages: Int): Int = record.page.takeIf { it in 2..pages } ?: 0
}
