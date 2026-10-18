package uz.haksenguz.ertaklar.core.i18n

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.haksenguz.ertaklar.core.text.Gloss
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.models.BooksFile
import uz.haksenguz.ertaklar.data.models.I18nFile
import uz.haksenguz.ertaklar.data.models.KoreanBook

/** The Korean menus, and each book's Korean helper lining up with its Uzbek. */
class KoreanTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val data = File("src/main/assets/data")
    private val i18n = json.decodeFromString<I18nFile>(File(data, "i18n.json").readText())
    private val menus = ContentRepository.koreanMenus(i18n)
    private val books = json.decodeFromString<BooksFile>(File(data, "books.json").readText()).books.associateBy { it.key }
    private val korean = json.decodeFromString<Map<String, KoreanBook>>(File(data, "korean.json").readText())

    @Test
    fun everyPatternTranslatesItsExample() {
        val handled = setOf("monthDay", "voicePackImported")
        val wrong = i18n.patterns.filter { it.ko != null || it.fn in handled }.mapNotNull { p ->
            menus.translate(p.example).takeIf { it != p.exampleKo }?.let { "${p.example} -> $it (want ${p.exampleKo})" }
        }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun exactTextsAndUnknownText() {
        assertEquals("전체", menus.translate("Barchasi"))
        assertEquals("곧 추가될 기능이에요", menus.translate("Bu bo'lim tez orada qo'shiladi"))
        assertEquals("5~8세", menus.translate("5–8 yosh"))
        assertEquals("10점", menus.translate("10 ball"))
        assertNull(menus.translate("Zumrad va Qimmat"))
    }

    @Test
    fun koreanLinesUpWithEveryPage() {
        korean.forEach { (key, ko) ->
            val book = books.getValue(key)
            assertEquals("$key: pages", book.pages.size, ko.pages.size)
            book.pages.forEachIndexed { i, p ->
                assertEquals("$key p${i + 1}: one Korean line per sentence", p.segments.size, ko.pages[i].s.size)
                p.question?.let { q -> assertEquals("$key p${i + 1}: question and answers", q.a.size + 1, ko.pages[i].q?.size) }
            }
            book.quiz?.let { q -> ko.quiz?.let { assertEquals("$key: quiz", q.a.size + 1, it.size) } }
        }
    }

    @Test
    fun everyGlossaryWordAppearsInItsBook() {
        korean.forEach { (key, ko) ->
            val gloss = Gloss(ko.forms)
            val text = books.getValue(key).pages.flatMap { it.segments + listOfNotNull(it.word?.first()) }
            val found = text.flatMap { t -> gloss.split(t).map { it.second } }.filter { it >= 0 }.toSet()
            ko.words.indices.forEach { i -> assertTrue("$key: «${ko.word(i)?.first}» never appears", i in found) }
        }
    }
}
