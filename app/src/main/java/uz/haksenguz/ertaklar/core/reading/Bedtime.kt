package uz.haksenguz.ertaklar.core.reading

import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.BookRecord

/**
 * Uxlash vaqti: a calm way to end the day with a book. A grown-up picks one
 * of the calm books and how many pages to read tonight; the questions rest,
 * a recorded voice reads on by itself, and after tonight's last page it is
 * good night. Tomorrow the book opens on the next page.
 */
object Bedtime {
    /** Pages a night; 0 is to the end of the book. */
    val PAGES = listOf(3, 5, 0)

    /** Tonight's first and last story page. */
    data class Tonight(val from: Int, val limit: Int)

    /** Tonight starts where the book was left (or at page 1) and reads [pagesTonight] pages. */
    fun tonight(record: BookRecord?, pages: Int, pagesTonight: Int): Tonight {
        val from = record?.page?.takeIf { it in 1..pages } ?: 1
        val limit = if (pagesTonight > 0) minOf(from + pagesTonight - 1, pages) else pages
        return Tonight(from, limit)
    }

    /** "🌙 2 / 5": tonight's page out of tonight's pages. */
    fun progress(view: Int, tonight: Tonight): Pair<Int, Int> =
        maxOf(1, minOf(view, tonight.limit) - tonight.from + 1) to (tonight.limit - tonight.from + 1)

    /** The calm books, the one under way first, then new ones, then those read before. */
    fun books(all: List<Book>, records: Map<String, BookRecord>, locked: (Book) -> Boolean): List<Book> {
        fun rank(b: Book): Int {
            val r = records[b.key]
            return when {
                r != null && r.page >= 1 -> 0
                r != null && r.finished -> 2
                else -> 1
            }
        }
        return all.filter { it.bedtime && !locked(it) }.sortedBy(::rank)
    }
}
