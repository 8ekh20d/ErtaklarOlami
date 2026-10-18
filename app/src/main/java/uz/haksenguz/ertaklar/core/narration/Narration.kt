package uz.haksenguz.ertaklar.core.narration

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * Reading a page aloud, with each piece lit up as it is read.
 *
 * A recording covers one story page: its title, then its sentences (the page's
 * segments). Nobody marks where each piece starts: [marks] finds it from the
 * pauses in the recording.
 */
object Narration {
    /** Seconds per loudness frame. */
    const val FRAME = 0.02

    /** Shortest silence that counts as a pause. */
    private const val MIN_GAP = 0.12

    /** How strongly a sentence break must stay near its expected place. */
    private const val DRIFT = 40.0

    private fun round2(n: Double) = floor(n * 100 + 0.5) / 100

    /** Fingerprint of the text a clip was read from; highlighting is only trusted while the page text still matches. */
    fun sig(segments: List<String>): String {
        val s = segments.joinToString("\n")
        var h = 2166136261L.toInt()
        for (ch in s) {
            h = h xor ch.code
            h *= 16777619
        }
        return (h.toLong() and 0xFFFFFFFFL).toString(36)
    }

    /** Loudness (dB) of one frame from the sum of its squared samples. */
    fun frameDb(sumOfSquares: Double, size: Int): Float = (10 * log10(sumOfSquares / size + 1e-10)).toFloat()

    /**
     * Start time (s) of each piece, given per-frame loudness [db] and the text
     * length of each piece. Breaks go at pauses: long ones near where an evenly
     * paced reader would reach the next piece win.
     */
    fun marks(db: FloatArray, lengths: List<Int>, frame: Double = FRAME): List<Double> {
        val n = lengths.size
        if (n == 0) return emptyList()
        val duration = db.size * frame
        val total = lengths.sum().takeIf { it > 0 } ?: 1
        fun spread(t0: Double, t1: Double): List<Double> {
            var acc = 0
            return lengths.dropLast(1).map { len ->
                acc += len
                t0 + (t1 - t0) * (acc.toDouble() / total)
            }
        }

        val sorted = db.sorted()
        fun at(p: Double): Double = sorted[min(sorted.size - 1, floor(p * sorted.size).toInt())].toDouble()
        val quiet = if (sorted.isEmpty()) 0.0 else at(0.05)
        val loud = if (sorted.isEmpty()) 0.0 else at(0.95)
        // Voice is well above the quietest moments and not far below the loudest:
        // room noise that gets through before the phone's noise filter settles stays out.
        val floorDb = max(quiet + max(6.0, (loud - quiet) * 0.3), loud - 22)
        val voiced = BooleanArray(db.size) { db[it] > floorDb }

        // Speech starts and ends with a real stretch of voice, not a tap on the screen.
        fun run(i: Int, step: Int): Int {
            var count = 0
            var j = i
            while (j >= 0 && j < voiced.size && voiced[j]) {
                count++
                j += step
            }
            return count
        }
        var first = -1
        var last = -1
        for (i in voiced.indices) {
            if (voiced[i] && run(i, 1) * frame >= 0.1) {
                first = i
                break
            }
        }
        for (i in voiced.indices.reversed()) {
            if (voiced[i] && run(i, -1) * frame >= 0.1) {
                last = i
                break
            }
        }
        if (first < 0 || loud - quiet < 6) return (listOf(0.0) + spread(0.0, duration)).map(::round2)

        val t0 = first * frame
        val t1 = (last + 1) * frame
        val gaps = mutableListOf<Gap>()
        var i = first
        while (i <= last) {
            if (voiced[i]) {
                i++
                continue
            }
            var j = i
            while (j <= last && !voiced[j]) j++
            if ((j - i) * frame >= MIN_GAP) gaps += Gap(i * frame, j * frame, (j - i) * frame)
            i = j
        }

        val expect = spread(t0, t1)
        val chosen = align(gaps, expect, max(t1 - t0, 1.0))
        // The title lights up as soon as the page starts playing.
        val marks = mutableListOf(0.0)
        chosen.forEachIndexed { k, g -> marks += if (g != null) max(g.to - 0.12, (g.from + g.to) / 2) else expect[k] }
        // Keep them in order and apart, whatever happened above.
        for (k in 1 until n) marks[k] = min(max(marks[k], marks[k - 1] + 0.3), duration)
        return marks.map(::round2)
    }

    /**
     * Whether a recording was read from the text now on the page: one mark per
     * piece, and the same fingerprint. Otherwise it still plays, unlit.
     */
    fun fits(marks: List<Double>, clipSig: String, segments: List<String>): Boolean =
        marks.size == segments.size && (clipSig.isEmpty() || clipSig == sig(segments))

    /** Index of the piece being read at time [t]. */
    fun segAt(marks: List<Double>, t: Double): Int {
        var i = 0
        while (i + 1 < marks.size && marks[i + 1] <= t + 0.05) i++
        return i
    }

    /** A pause in the speech. */
    data class Gap(val from: Double, val to: Double, val len: Double)

    /**
     * Picks, for each expected break (in order), a later pause than the one
     * before, or none; maximises pause length minus distance from expectation.
     */
    fun align(gaps: List<Gap>, expect: List<Double>, span: Double): List<Gap?> {
        val m = expect.size
        val g = gaps.size
        if (m == 0) return emptyList()
        fun score(k: Int, j: Int): Double {
            val gap = gaps[j]
            val off = ((gap.from + gap.to) / 2 - expect[k]) / span
            return ln(gap.len / MIN_GAP) / ln(2.0) + 1 - DRIFT * off * off
        }

        // state s = index of the last pause used + 1 (0: none yet)
        var best = DoubleArray(g + 1) { Double.NEGATIVE_INFINITY }
        best[0] = 0.0
        val back = mutableListOf<Array<Step?>>()
        for (k in 0 until m) {
            val next = DoubleArray(g + 1) { Double.NEGATIVE_INFINITY }
            val from = arrayOfNulls<Step>(g + 1)
            for (s in 0..g) {
                if (best[s] == Double.NEGATIVE_INFINITY) continue
                if (best[s] > next[s]) {
                    next[s] = best[s]
                    from[s] = Step(s, -1)
                }
                for (j in s until g) {
                    val v = best[s] + score(k, j)
                    if (v > next[j + 1]) {
                        next[j + 1] = v
                        from[j + 1] = Step(s, j)
                    }
                }
            }
            back += from
            best = next
        }
        val top = best.max()
        var s = best.indexOfFirst { it == top }
        val out = arrayOfNulls<Gap>(m)
        for (k in m - 1 downTo 0) {
            val f = back[k][s]!!
            out[k] = if (f.use < 0) null else gaps[f.use]
            s = f.s
        }
        return out.toList()
    }

    private class Step(val s: Int, val use: Int)
}
