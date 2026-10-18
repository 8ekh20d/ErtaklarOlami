package uz.haksenguz.ertaklar.core.reading

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.haksenguz.ertaklar.core.narration.Narration
import uz.haksenguz.ertaklar.data.models.BookRecord
import uz.haksenguz.ertaklar.data.models.BooksFile

class BedtimeTest {

    private val books = Json { ignoreUnknownKeys = true }
        .decodeFromString<BooksFile>(File("src/main/assets/data/books.json").readText()).books

    @Test
    fun tonightStartsWhereTheBookWasLeft() {
        assertEquals(Bedtime.Tonight(1, 3), Bedtime.tonight(null, 8, 3))
        assertEquals(Bedtime.Tonight(4, 8), Bedtime.tonight(BookRecord("b", page = 4), 8, 5))
        assertEquals(Bedtime.Tonight(1, 8), Bedtime.tonight(BookRecord("b", page = 0), 8, 0))
        assertEquals(Bedtime.Tonight(1, 5), Bedtime.tonight(BookRecord("b", page = 12), 8, 5))
    }

    @Test
    fun progressCountsTonightsPages() {
        val t = Bedtime.Tonight(4, 8)
        assertEquals(1 to 5, Bedtime.progress(2, t))
        assertEquals(2 to 5, Bedtime.progress(5, t))
        assertEquals(5 to 5, Bedtime.progress(9, t))
    }

    @Test
    fun calmBooksInProgressFirstThenNewThenRead() {
        val calm = books.filter { it.bedtime }
        assertTrue("at least 8 calm books", calm.size >= 8)
        val (inProgress, read) = calm[2].key to calm[0].key
        val records = mapOf(inProgress to BookRecord(inProgress, page = 3), read to BookRecord(read, finished = true))
        val order = Bedtime.books(books, records) { false }.map { it.key }
        assertEquals(inProgress, order.first())
        assertEquals(read, order.last())
        assertTrue("twin tales open only later, so none is a calm book", calm.none { it.twin != null })
    }

    @Test
    fun aRecordingFitsOnlyTheTextItWasReadFrom() {
        val segs = listOf("Sarlavha", "Bir.", "Ikki.")
        assertTrue(Narration.fits(listOf(0.0, 1.0, 2.0), Narration.sig(segs), segs))
        assertTrue("no fingerprint: trust the marks", Narration.fits(listOf(0.0, 1.0, 2.0), "", segs))
        assertFalse("the text changed", Narration.fits(listOf(0.0, 1.0, 2.0), Narration.sig(segs), segs + "Uch."))
        assertFalse("other sentences", Narration.fits(listOf(0.0, 1.0, 2.0), Narration.sig(listOf("a", "b", "c")), segs))
    }
}
