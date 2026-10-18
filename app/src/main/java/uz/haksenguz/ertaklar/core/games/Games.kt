package uz.haksenguz.ertaklar.core.games

import kotlin.math.floor
import kotlin.random.Random

/** Small rules shared by the play corner. */
object Games {
    /** "Voqealar tartibi": the first page, one a third in, one two thirds in, and the last (as page indices). */
    fun orderPicks(pages: Int): List<Int> = listOf(0, round((pages - 1) / 3.0), round(2 * (pages - 1) / 3.0), pages - 1).distinct()

    /** A shuffle that never leaves everything where it was, so there is always something to do. */
    fun <T> shuffled(list: List<T>, random: Random = Random): List<T> {
        val order = list.indices.toMutableList()
        for (i in order.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            order[i] = order[j].also { order[j] = order[i] }
        }
        if (order.size > 1 && order.withIndex().all { (i, v) -> i == v }) order[0] = order[1].also { order[1] = order[0] }
        return order.map { list[it] }
    }

    /** Rounds halves up. */
    private fun round(x: Double): Int = floor(x + 0.5).toInt()
}
