package uz.haksenguz.ertaklar.core.games

/**
 * A colouring page's areas: the light places between the lines, each one
 * filled in a single tap. Found once per picture from its pixels' brightness.
 */
class Regions private constructor(
    val width: Int,
    val height: Int,
    /** The area of each pixel, or -1 on a line. */
    private val labels: IntArray,
    val count: Int
) {
    fun at(x: Int, y: Int): Int = if (x in 0 until width && y in 0 until height) labels[y * width + x] else -1

    /** The area under a finger: the one tapped, or the nearest within [radius] pixels when the tap lands on a line. */
    fun near(x: Int, y: Int, radius: Int): Int {
        at(x, y).let { if (it >= 0) return it }
        var best = -1
        var bestD = Int.MAX_VALUE
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val d = dx * dx + dy * dy
                if (d >= bestD || d > radius * radius) continue
                val r = at(x + dx, y + dy)
                if (r >= 0) {
                    best = r
                    bestD = d
                }
            }
        }
        return best
    }

    /** Paints [colors] (one per area) into [out]; pixels on the lines get [line]. */
    fun paint(colors: IntArray, line: Int, out: IntArray) {
        for (i in labels.indices) {
            val r = labels[i]
            out[i] = if (r >= 0) colors[r] else line
        }
    }

    companion object {
        /** Darker than this is a line. */
        const val INK = 230

        /** The soft edge of a line (lighter than this) takes the colour of the area beside it. */
        const val SOFT_INK = 120

        /** How many pixels deep the soft edge is. */
        private const val SOFT_DEPTH = 2

        /** Brightness 0..255 of an ARGB pixel. */
        fun luminance(argb: Int): Int {
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            return (299 * r + 587 * g + 114 * b) / 1000
        }

        fun of(width: Int, height: Int, luminance: IntArray): Regions {
            val n = width * height
            require(luminance.size == n)
            var labels = IntArray(n) { -1 }
            val stack = IntArray(n)
            var count = 0
            for (start in 0 until n) {
                if (labels[start] != -1 || luminance[start] < INK) continue
                val id = count++
                var top = 0
                labels[start] = id
                stack[top++] = start
                while (top > 0) {
                    val p = stack[--top]
                    val x = p % width
                    if (x > 0 && labels[p - 1] == -1 && luminance[p - 1] >= INK) {
                        labels[p - 1] = id
                        stack[top++] = p - 1
                    }
                    if (x < width - 1 && labels[p + 1] == -1 && luminance[p + 1] >= INK) {
                        labels[p + 1] = id
                        stack[top++] = p + 1
                    }
                    if (p >= width && labels[p - width] == -1 && luminance[p - width] >= INK) {
                        labels[p - width] = id
                        stack[top++] = p - width
                    }
                    if (p < n - width && labels[p + width] == -1 && luminance[p + width] >= INK) {
                        labels[p + width] = id
                        stack[top++] = p + width
                    }
                }
            }
            repeat(SOFT_DEPTH) {
                val next = labels.copyOf()
                for (p in 0 until n) {
                    if (labels[p] != -1 || luminance[p] < SOFT_INK) continue
                    val x = p % width
                    next[p] = when {
                        x > 0 && labels[p - 1] >= 0 -> labels[p - 1]
                        x < width - 1 && labels[p + 1] >= 0 -> labels[p + 1]
                        p >= width && labels[p - width] >= 0 -> labels[p - width]
                        p < n - width && labels[p + width] >= 0 -> labels[p + width]
                        else -> -1
                    }
                }
                labels = next
            }
            return Regions(width, height, labels, count)
        }
    }
}
