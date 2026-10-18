package uz.haksenguz.ertaklar.data

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.haksenguz.ertaklar.core.levels.Levels
import uz.haksenguz.ertaklar.data.models.BooksFile
import uz.haksenguz.ertaklar.data.models.Category

/**
 * The bundled content is complete and well formed: every book
 * parses, every picture it names exists, questions point at real answers.
 * Unit tests run with the module directory (app/) as working directory.
 */
class ContentAssetsTest {

    private val assets = File("src/main/assets")
    private val books = Json { ignoreUnknownKeys = true }
        .decodeFromString<BooksFile>(File(assets, "data/books.json").readText()).books

    @Test
    fun allBooksAreThere() {
        assertEquals(34, books.size)
        assertEquals("zumrad", books.first().key)
        assertEquals("alifbo", books.last().key)
        assertEquals(books.size, books.map { it.key }.toSet().size)
    }

    @Test
    fun everyCategoryIsKnown() {
        val known = Category.entries.map { it.id }.toSet()
        books.forEach { assertTrue("${it.key}: category ${it.category}", it.category in known) }
    }

    @Test
    fun everyPictureExists() {
        val missing = books.flatMap { b ->
            listOf(b.cover, b.endImage) + b.pages.flatMap { p -> listOfNotNull(p.image, p.revealImage, p.lineArt, p.revealLineArt) }
        }.filterNot { File(assets, it).isFile }
        assertTrue("missing pictures: ${missing.take(10)}", missing.isEmpty())
    }

    @Test
    fun pagesAreWellFormed() {
        books.forEach { b ->
            assertTrue("${b.key}: at least 3 pages", b.pages.size >= 3)
            b.pages.forEachIndexed { i, p ->
                assertEquals("${b.key}: page numbers", i + 1, p.n)
                assertTrue("${b.key}:${p.n}: segments start with the title", p.segments.firstOrNull() == p.title)
                assertTrue("${b.key}:${p.n}: has sentences", p.segments.size >= 2)
                p.question?.let { q ->
                    assertTrue("${b.key}:${p.n}: right answer exists", q.ok == -1 || q.ok in q.a.indices)
                }
            }
            b.quiz?.let { q -> assertTrue("${b.key}: quiz answer exists", q.ok in q.a.indices) }
        }
    }

    @Test
    fun twinsPointAtRealBooks() {
        val keys = books.map { it.key }.toSet()
        books.mapNotNull { it.twin }.forEach { assertTrue("twin $it", it in keys) }
    }

    @Test
    fun everyShelfHasEnoughBooks() {
        Levels.SHELVES.forEach { s ->
            val n = books.count { Levels.fits(it.age, s.id) }
            assertTrue("the ${s.label} shelf has only $n books", n >= 5)
        }
    }
}
