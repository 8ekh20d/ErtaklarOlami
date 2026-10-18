package uz.haksenguz.ertaklar.core.dictionary

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.haksenguz.ertaklar.data.models.BooksFile
import uz.haksenguz.ertaklar.data.models.KoreanBook

class DictionaryTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val data = File("src/main/assets/data")
    private val books = json.decodeFromString<BooksFile>(File(data, "books.json").readText()).books
    private val korean = json.decodeFromString<Map<String, KoreanBook>>(File(data, "korean.json").readText())

    @Test
    fun digraphsAreOneTile() {
        assertEquals(listOf("o'", "r", "d", "a", "k"), Dictionary.letters("o'rdak"))
        assertEquals(listOf("ch", "o", "y", "n", "a", "k"), Dictionary.letters("choynak"))
        assertEquals(listOf("q", "o'", "n", "g'", "i", "r", "o", "q"), Dictionary.letters("qo'ng'iroq"))
        assertEquals(listOf("d", "e", "ng", "i", "z"), Dictionary.letters("dengiz"))
        assertEquals("the tutuq is a tile", listOf("sh", "e", "'", "r"), Dictionary.letters("she'r"))
        assertEquals("capitals stay", listOf("Sh", "a", "r", "q"), Dictionary.letters("Sharq"))
        assertEquals("curly apostrophes count", listOf("o'", "t"), Dictionary.letters("oʻt"))
    }

    @Test
    fun uzbekAlphabetOrder() {
        val words = listOf("shar", "o'rdak", "anor", "zamburug'", "g'oz", "choynak", "olma", "xo'roz", "sabzi", "baliq")
        assertEquals(
            "anor baliq olma sabzi xo'roz zamburug' o'rdak g'oz shar choynak",
            words.sortedWith(Dictionary.order).joinToString(" ")
        )
    }

    @Test
    fun koreanMeaningsComeFromTheBooksGlossary() {
        assertTrue(Dictionary.koMeaning(korean["zumrad"], "sandiq").orEmpty().contains("상자"))
        assertEquals("코끼리", Dictionary.koMeaning(korean["alifbo"], "fil"))
        assertNull("a book without Korean has none", Dictionary.koMeaning(korean["sariq_dev"], "anything"))
        assertEquals("상자", Dictionary.shortKo("상자 — 물건을 넣는 것"))
    }

    @Test
    fun whatAChildHasCollected() {
        assertTrue("a new child has none", Dictionary.entries(books, korean, emptySet(), emptySet()).isEmpty())

        val some = Dictionary.entries(books, korean, setOf("alifbo:1", "alifbo:5", "zumrad:2", "zumrad:3"), emptySet())
        assertEquals("three word pages opened (one without a word): ${some.map { it.term }}", 3, some.size)
        assertTrue(some.all { it.term.isNotEmpty() && it.meaning.isNotEmpty() && it.image.isNotEmpty() && it.view > 0 })
        assertEquals("in alphabet order", "anor", some[0].term)

        val alifbo = books.first { it.key == "alifbo" }
        assertEquals(
            "a finished book gives all its words",
            alifbo.pages.count { it.word != null },
            Dictionary.entries(books, korean, emptySet(), setOf("alifbo")).size
        )

        val twice = Dictionary.entries(books, korean, emptySet(), setOf("sirli_sandiq", "zumrad"))
        assertEquals("a word met in two books is one card", 1, twice.count { it.term.lowercase() == "sandiq" })

        val everything = Dictionary.entries(books, korean, emptySet(), books.map { it.key }.toSet())
        assertTrue("every book read: ${everything.size} words", everything.size > 100)
        assertTrue("with Korean: ${everything.count { it.ko != null }}", everything.count { it.ko != null } >= 40)
        assertTrue(
            "to build: ${everything.count { Dictionary.spellable(it.term) }}",
            everything.count { Dictionary.spellable(it.term) } >= 40
        )
    }

    @Test
    fun spellableIsOneWordOfThreeToSevenLetters() {
        assertFalse(Dictionary.spellable("davlat tili"))
        assertTrue(Dictionary.spellable("anor"))
        assertFalse(Dictionary.spellable("karvonsaroy"))
        assertFalse(Dictionary.spellable("ot"))
    }

    @Test
    fun pickGivesDifferentItemsFromTheList() {
        val p = Dictionary.pick(listOf(1, 2, 3, 4, 5, 6), 4)
        assertEquals(4, p.size)
        assertEquals(4, p.toSet().size)
        assertTrue(p.all { it in 1..6 })
    }
}
