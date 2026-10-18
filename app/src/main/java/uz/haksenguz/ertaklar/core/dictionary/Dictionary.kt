package uz.haksenguz.ertaklar.core.dictionary

import kotlin.random.Random
import uz.haksenguz.ertaklar.core.text.Gloss
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.KoreanBook

/**
 * Mening lug'atim, the child's own dictionary: every "Yangi so'z" card they
 * have come across, with the picture of its page. A word is collected when its
 * page is opened; every word of a finished book counts too. Cards are in
 * Uzbek alphabet order.
 */
object Dictionary {
    /** The Uzbek Latin alphabet in order; o', g', sh, ch and ng are one letter each. */
    val ALPHABET = listOf(
        "a", "b", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "x", "y", "z",
        "o'", "g'", "sh", "ch", "ng"
    )

    /** Words needed before the games open. */
    const val MIN_WORDS = 4

    /** One card: the word, its meaning, its Korean (when the book has it) and where it was met. */
    data class Entry(
        /** "book:page". */
        val key: String,
        val book: String,
        val view: Int,
        val term: String,
        val meaning: String,
        val ko: String?,
        val image: String
    )

    /** A word as Uzbek letters: "o'rdak" → o', r, d, a, k. The tutuq (she'r) is a tile of its own. */
    fun letters(word: String): List<String> {
        val w = word.map { if (it in "ʻʼ‘’`") '\'' else it }.joinToString("")
        val out = mutableListOf<String>()
        var i = 0
        while (i < w.length) {
            val two = w.substring(i, minOf(i + 2, w.length)).lowercase()
            val pair = two == "o'" || two == "g'" || two == "sh" || two == "ch" || (two == "ng" && w.getOrNull(i + 2) != '\'')
            if (pair) {
                out += w.substring(i, i + 2)
                i += 2
            } else {
                out += w[i].toString()
                i++
            }
        }
        return out
    }

    /** Uzbek alphabet order, letter by letter; anything outside the alphabet sorts after. */
    val order: Comparator<String> = Comparator { a, b ->
        val x = letters(a.lowercase())
        val y = letters(b.lowercase())
        for (i in 0 until minOf(x.size, y.size)) {
            val p = ALPHABET.indexOf(x[i]).let { if (it < 0) 99 else it }
            val q = ALPHABET.indexOf(y[i]).let { if (it < 0) 99 else it }
            if (p != q) return@Comparator p - q
        }
        x.size - y.size
    }

    /** The Korean meaning of a book's word, from the book's Korean glossary. */
    fun koMeaning(korean: KoreanBook?, term: String): String? {
        val ko = korean ?: return null
        val i = Gloss(ko.forms).forTerm(term)
        return if (i >= 0) ko.word(i)?.second else null
    }

    /**
     * The child's dictionary: one card per word (the first page it was on, in
     * library order), sorted alphabetically. [seen] holds "book:page" keys of
     * word pages opened; every word of a [finished] book counts.
     */
    fun entries(books: List<Book>, korean: Map<String, KoreanBook>, seen: Set<String>, finished: Set<String>): List<Entry> {
        val terms = mutableSetOf<String>()
        val out = mutableListOf<Entry>()
        books.forEach { book ->
            book.pages.forEachIndexed { i, page ->
                val word = page.word ?: return@forEachIndexed
                val key = "${book.key}:${i + 1}"
                if (key !in seen && book.key !in finished) return@forEachIndexed
                if (!terms.add(word[0].lowercase())) return@forEachIndexed
                out += Entry(key, book.key, i + 1, word[0], word.getOrElse(1) { "" }, koMeaning(korean[book.key], word[0]), page.image)
            }
        }
        return out.sortedWith(compareBy(order) { it.term })
    }

    /** Fit for "So'zni yig'ing": one word of 3–7 letters. */
    fun spellable(term: String): Boolean = !term.any { it.isWhitespace() } && letters(term).size in 3..7

    /** [n] different items, shuffled. */
    fun <T> pick(list: List<T>, n: Int, random: Random = Random): List<T> = list.shuffled(random).take(n)

    /** A Korean meaning cut short for a button: "상자 — 물건을…" → "상자". */
    fun shortKo(ko: String): String = ko.split(" — ", "(", ",").first().trim()
}
