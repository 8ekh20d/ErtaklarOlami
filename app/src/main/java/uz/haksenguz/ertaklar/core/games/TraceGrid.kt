package uz.haksenguz.ertaklar.core.games

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * "Harfni yozing": the letter's shape as a mask on a coarse grid, and the cells
 * the child's finger has gone over. A trace counts once it covers most of the
 * letter, every part of it, without wandering far off it.
 */
class TraceGrid(
    /** The pad's side in pixels. */
    val size: Int,
    /** Grid cell side in pixels. */
    val cell: Int,
    /** Whether each cell (row by row, [cols] a row) lies inside the letter. */
    private val mask: BooleanArray
) {
    val cols = ceil(size.toDouble() / cell).toInt()
    private val drawn = BooleanArray(cols * cols)

    init {
        require(mask.size == cols * cols)
    }

    fun clear() = drawn.fill(false)

    /** A round brush of diameter [brush] touched ([x], [y]). */
    fun mark(x: Float, y: Float, brush: Float) {
        val r = brush / 2
        val g0 = max(0, floor((x - r) / cell).toInt())
        val g1 = min(cols - 1, floor((x + r) / cell).toInt())
        val h0 = max(0, floor((y - r) / cell).toInt())
        val h1 = min(cols - 1, floor((y + r) / cell).toInt())
        for (gy in h0..h1) {
            for (gx in g0..g1) {
                val cx = gx * cell + cell / 2f - x
                val cy = gy * cell + cell / 2f - y
                if (cx * cx + cy * cy <= r * r) drawn[gy * cols + gx] = true
            }
        }
    }

    /** The brush went from ([ax], [ay]) to ([bx], [by]). */
    fun line(ax: Float, ay: Float, bx: Float, by: Float, brush: Float) {
        val n = max(1, ceil(hypot(bx - ax, by - ay) / (cell * 0.8f)).toInt())
        for (i in 0..n) mark(ax + (bx - ax) * i / n, ay + (by - ay) * i / n, brush)
    }

    /**
     * How much of the letter is gone over, overall and in its weakest part
     * (so the legs of an A count, not only its top), and how much ink is off it.
     */
    fun score(): Score {
        var inLetter = 0
        var covered = 0
        var ink = 0
        var off = 0
        var x0 = cols
        var x1 = 0
        var y0 = cols
        var y1 = 0
        for (i in mask.indices) {
            if (mask[i]) {
                inLetter++
                val gx = i % cols
                val gy = i / cols
                x0 = min(x0, gx)
                x1 = max(x1, gx)
                y0 = min(y0, gy)
                y1 = max(y1, gy)
            }
            if (drawn[i]) {
                ink++
                if (mask[i]) covered++ else off++
            }
        }
        // The letter cut in a 3 × 3 grid; parts holding almost none of it are left out.
        val parts = Array(9) { IntArray(2) }
        for (i in mask.indices) {
            if (!mask[i]) continue
            val gx = i % cols
            val gy = i / cols
            val px = min(2, floor((gx - x0).toDouble() / (x1 - x0 + 1) * 3).toInt())
            val py = min(2, floor((gy - y0).toDouble() / (y1 - y0 + 1) * 3).toInt())
            val p = parts[px + 3 * py]
            p[0]++
            if (drawn[i]) p[1]++
        }
        val weakest = parts.filter { it[0] > 0 && it[0] >= inLetter * 0.03 }.minOfOrNull { it[1].toDouble() / it[0] } ?: 1.0
        return Score(
            cover = if (inLetter > 0) covered.toDouble() / inLetter else 0.0,
            part = min(1.0, weakest),
            off = if (ink > 0) off.toDouble() / ink else 0.0
        )
    }

    data class Score(val cover: Double, val part: Double, val off: Double) {
        val passed: Boolean get() = cover >= COVER && part >= PART && off <= OFF
        val strayed: Boolean get() = off > OFF

        /** The meter under the pad, 0..100. */
        val meter: Int get() = min(100, (min(cover / COVER, part / PART) * 100).roundToInt())
    }

    companion object {
        /** Share of the letter that must be gone over... */
        const val COVER = 0.6

        /** ...and of each part of it. */
        const val PART = 0.35

        /** Share of the drawing allowed outside the letter. */
        const val OFF = 0.5

        /** About 120 cells across the pad. */
        fun cellFor(size: Int): Int = max(2, floor(size / 120.0 + 0.5).toInt())

        /** The grid for a pad of [size] pixels, sampling [inside] at each cell's centre. */
        fun of(size: Int, inside: (x: Int, y: Int) -> Boolean): TraceGrid {
            val cell = cellFor(size)
            val cols = ceil(size.toDouble() / cell).toInt()
            val mask = BooleanArray(cols * cols) { i ->
                val gx = i % cols
                val gy = i / cols
                inside(min(size - 1, gx * cell + cell / 2), min(size - 1, gy * cell + cell / 2))
            }
            return TraceGrid(size, cell, mask)
        }
    }
}
