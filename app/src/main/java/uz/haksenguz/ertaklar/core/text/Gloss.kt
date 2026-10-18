package uz.haksenguz.ertaklar.core.text

/**
 * Finds a book's glossary words in its text, so a tap shows their Korean meaning.
 *
 * Each word has forms: "sandiq" matches words that start with it ("sandiqni"),
 * "=in" only the word "in". Case and apostrophe style don't matter.
 */
class Gloss(private val forms: List<List<String>>) {
    private data class Form(val exact: Boolean, val text: String)

    private val matchers: List<List<Form>> = forms.map { list ->
        list.map { f -> if (f.startsWith("=")) Form(true, norm(f.drop(1))) else Form(false, norm(f)) }
    }

    /** The glossary entry matching [word], or -1. */
    fun indexOf(word: String): Int {
        val w = norm(word)
        return matchers.indexOfFirst { list -> list.any { if (it.exact) w == it.text else w.startsWith(it.text) } }
    }

    /** The text cut into plain runs and glossary words: (text, entry index or -1). */
    fun split(text: String): List<Pair<String, Int>> {
        if (matchers.isEmpty()) return listOf(text to -1)
        val out = mutableListOf<Pair<String, Int>>()
        var last = 0
        for (m in WORD.findAll(text)) {
            val hit = indexOf(m.value)
            if (hit < 0) continue
            if (m.range.first > last) out += text.substring(last, m.range.first) to -1
            out += m.value to hit
            last = m.range.last + 1
        }
        if (last < text.length) out += text.substring(last) to -1
        return out
    }

    /** The entry for a word card's term (its first word). */
    fun forTerm(term: String): Int = indexOf(term.trim().split(Regex("\\s+")).first())

    companion object {
        private val WORD = Regex("[A-Za-z'ʻʼ‘’]+")

        private fun norm(w: String) = w.lowercase().map { if (it in "ʻʼ‘’`") '\'' else it }.joinToString("")
    }
}
