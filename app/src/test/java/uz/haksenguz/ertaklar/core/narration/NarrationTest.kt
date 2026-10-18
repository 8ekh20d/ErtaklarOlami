package uz.haksenguz.ertaklar.core.narration

import java.io.File
import kotlin.math.abs
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.haksenguz.ertaklar.data.models.BooksFile

/** Finding where each sentence starts in a recording of a page. */
class NarrationTest {

    /**
     * A fake loudness track: (kind, seconds) with kind 's' speech, 'p' pause,
     * 'n' room noise before the phone's noise filter settles, 'c' a tap on the screen.
     */
    private fun track(parts: List<Pair<Char, Double>>, speech: Double = -22.0, silence: Double = -75.0, jitter: Double = 3.0): FloatArray {
        val out = mutableListOf<Float>()
        var seed = 7L
        fun rnd(): Double {
            seed = (seed * 16807) % 2147483647
            return (seed.toDouble() / 2147483647 - 0.5) * 2
        }
        parts.forEach { (kind, sec) ->
            val n = Math.round(sec / Narration.FRAME).toInt()
            val level = when (kind) {
                's' -> speech
                'p' -> silence
                'n' -> -45.0
                else -> -10.0
            }
            repeat(n) { out += (level + rnd() * jitter).toFloat() }
        }
        return out.toFloatArray()
    }

    private fun near(got: List<Double>, want: List<Double>, tol: Double = 0.25) =
        got.size == want.size && got.indices.all { abs(got[it] - want[it]) <= tol }

    @Test
    fun clearPauses() {
        val t = track(
            listOf(
                'p' to 0.6, 's' to 1.2, 'p' to 0.7, 's' to 2.0, 'p' to 0.15, 's' to 1.5, 'p' to 0.6, 's' to 3.0, 'p' to 0.5,
                's' to 2.2, 'p' to 0.8
            )
        )
        val m = Narration.marks(t, listOf(18, 90, 80, 55))
        assertTrue("clear pauses: $m", near(m, listOf(0.0, 2.38, 6.63, 10.13), 0.05))
    }

    @Test
    fun longCommaPauseFarFromABreakIsIgnored() {
        val t = track(listOf('p' to 0.5, 's' to 1.0, 'p' to 0.6, 's' to 0.8, 'p' to 0.9, 's' to 4.0, 'p' to 0.5, 's' to 3.0, 'p' to 0.5))
        val m = Narration.marks(t, listOf(12, 100, 60))
        assertTrue("comma pause ignored: $m", near(m, listOf(0.0, 1.98, 8.18), 0.05))
    }

    @Test
    fun readerWhoBarelyPauses() {
        val t = track(listOf('p' to 0.5, 's' to 10.0, 'p' to 0.5))
        val m = Narration.marks(t, listOf(10, 50, 40))
        assertTrue("no pauses: $m", m.size == 3 && m[0] < m[1] && m[1] < m[2] && abs(m[1] - 1.5) < 0.3 && abs(m[2] - 6.5) < 0.3)
    }

    @Test
    fun silenceSpreadsEvenly() {
        val m = Narration.marks(track(listOf('p' to 6.0)), listOf(10, 20, 30))
        assertTrue("silence: $m", near(m, listOf(0.0, 1.0, 3.0)))
    }

    @Test
    fun onePieceStartsAtZero() {
        val m = Narration.marks(track(listOf('p' to 1.0, 's' to 3.0, 'p' to 1.0)), listOf(40))
        assertTrue("single piece: $m", near(m, listOf(0.0)))
    }

    @Test
    fun roomNoiseAndTapsAreNotSpeech() {
        val t = track(
            listOf(
                'c' to 0.04, 'n' to 0.6, 'p' to 0.6, 's' to 1.2, 'p' to 0.45, 's' to 3.5, 'p' to 0.7,
                's' to 2.2, 'p' to 0.7, 's' to 3.2, 'p' to 0.8, 'c' to 0.04, 'p' to 0.2
            )
        )
        val m = Narration.marks(t, listOf(17, 79, 47, 68))
        assertTrue("noise and taps: $m", near(m, listOf(0.0, 2.77, 6.97, 9.87), 0.05))
    }

    @Test
    fun segAt() {
        val marks = listOf(0.4, 2.4, 7.3)
        assertEquals(0, Narration.segAt(marks, 0.0))
        assertEquals(1, Narration.segAt(marks, 2.39))
        assertEquals(2, Narration.segAt(marks, 9.0))
    }

    @Test
    fun sigFollowsTheText() {
        assertNotEquals(Narration.sig(listOf("a", "b")), Narration.sig(listOf("a", "c")))
        assertEquals(Narration.sig(listOf("a", "b")), Narration.sig(listOf("a", "b")))
    }

    /** Recordings made in the app and in shared voice packs carry the same fingerprint for the same page. */
    @Test
    fun sigMatchesTheBundledPages() {
        val books = Json { ignoreUnknownKeys = true }
            .decodeFromString<BooksFile>(File("src/main/assets/data/books.json").readText()).books
        val wrong = books.flatMap { b -> b.pages.filter { Narration.sig(it.segments) != it.sig }.map { "${b.key}:${it.n}" } }
        assertEquals(emptyList<String>(), wrong)
    }
}
