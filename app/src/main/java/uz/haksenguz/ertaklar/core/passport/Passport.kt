package uz.haksenguz.ertaklar.core.passport

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.BookRecord
import uz.haksenguz.ertaklar.data.models.PassportFile
import uz.haksenguz.ertaklar.data.models.Region
import uz.haksenguz.ertaklar.data.models.Sticker

/**
 * Madaniyat pasporti, the culture passport. Every book takes the child
 * somewhere: to one of Uzbekistan's 14 regions, or to Korea. Finishing a book
 * stamps that place, and points buy its stickers.
 */
class Passport(val file: PassportFile) {
    val regions: List<Region> = file.regions
    val price: Int = file.price

    /** Every sticker, with the place it belongs to. */
    val stickers: List<Pair<Sticker, Region>> = regions.flatMap { r -> r.stickers.map { it to r } }

    private val byId = regions.associateBy { it.id }

    fun region(id: String?): Region? = id?.let { byId[it] }

    fun regionOf(book: Book?): Region? = region(book?.region)

    fun sticker(id: String): Pair<Sticker, Region>? = stickers.firstOrNull { it.first.id == id }

    /** The place named in "... seni Xorazmga olib bordi". */
    fun byDative(to: String): Region? = regions.firstOrNull { dative(it.name) == to }

    fun byName(name: String): Region? = regions.firstOrNull { it.name == name }

    /** The books that take the child to a place, in library order. */
    fun booksOf(books: List<Book>, id: String): List<Book> = books.filter { it.region == id }

    /**
     * The child's stamps: place -> when the first of its books was finished
     * (0 when not known, for books finished before dates were kept).
     */
    fun stamps(books: List<Book>, records: Map<String, BookRecord>): Map<String, Long> {
        val out = mutableMapOf<String, Long>()
        books.forEach { book ->
            val rec = records[book.key]?.takeIf { it.finished } ?: return@forEach
            val r = regionOf(book) ?: return@forEach
            val at = rec.finishedAt ?: 0L
            val had = out[r.id]
            if (had == null || (at != 0L && (had == 0L || at < had))) out[r.id] = at
        }
        return out
    }

    /** Whether the child can have a sticker. */
    enum class StickerState { HAVE, STAMP, POINTS, OK, NONE }

    fun stickerState(id: String, stamps: Map<String, Long>, owned: Set<String>, points: Int): StickerState {
        val (_, region) = sticker(id) ?: return StickerState.NONE
        return when {
            id in owned -> StickerState.HAVE
            region.id !in stamps -> StickerState.STAMP
            points < price -> StickerState.POINTS
            else -> StickerState.OK
        }
    }

    companion object {
        /** "Xorazm" -> "Xorazmga" (to Xorazm); after k and q the suffix is -ka, -qa. */
        fun dative(name: String): String = name + when {
            name.endsWith("k") -> "ka"
            name.endsWith("q") -> "qa"
            else -> "ga"
        }

        private val DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        /** A stamp's date, "01.10.2026"; empty when not known. */
        fun dateLabel(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
            if (millis <= 0) "" else DATE.format(Instant.ofEpochMilli(millis).atZone(zone))
    }
}
