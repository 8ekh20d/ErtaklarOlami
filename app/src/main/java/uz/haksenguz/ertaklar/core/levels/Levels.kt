package uz.haksenguz.ertaklar.core.levels

/**
 * Reading levels.
 *
 * Every book has `age: [youngest, oldest]`. The library has three shelves; a
 * book stands on every shelf its ages touch. A child's age picks their shelf.
 */
object Levels {
    data class Shelf(val id: String, val label: String, val from: Int, val to: Int)

    const val ALL = "all"

    val SHELVES = listOf(
        Shelf("4-6", "4–6 yosh", 0, 6),
        Shelf("7-8", "7–8 yosh", 7, 8),
        Shelf("9+", "9+ yosh", 9, Int.MAX_VALUE)
    )

    /** Ages offered in the profile form; 11 stands for "11 and older". */
    val AGES = listOf(4, 5, 6, 7, 8, 9, 10, 11)

    /** The shelf for a child of this age, or null when the age isn't set. */
    fun shelfFor(age: Int?): String? {
        if (age == null || age == 0) return null
        return SHELVES.first { age >= it.from && age <= it.to }.id
    }

    /** Whether a book with these ages stands on a shelf ("all" holds every book). */
    fun fits(age: List<Int>, shelf: String?): Boolean {
        if (shelf == null || shelf == ALL) return true
        val s = SHELVES.firstOrNull { it.id == shelf } ?: return true
        val lo = age.getOrNull(0) ?: 0
        val hi = age.getOrNull(1) ?: Int.MAX_VALUE
        return lo <= s.to && hi >= s.from
    }

    /** "5–8 yosh", as shown on library cards and title pages. */
    fun label(age: List<Int>): String = if (age.size >= 2) "${age[0]}–${age[1]} yosh" else ""

    fun ageLabel(age: Int): String = if (age >= 11) "11+ yosh" else "$age yosh"
}
