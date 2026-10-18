package uz.haksenguz.ertaklar.core.passport

import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.haksenguz.ertaklar.core.passport.Passport.StickerState
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.models.BookRecord
import uz.haksenguz.ertaklar.data.models.BooksFile
import uz.haksenguz.ertaklar.data.models.I18nFile
import uz.haksenguz.ertaklar.data.models.MapFile
import uz.haksenguz.ertaklar.data.models.PassportFile

class PassportTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val assets = File("src/main/assets")
    private val data = File(assets, "data")
    private val file = json.decodeFromString<PassportFile>(File(data, "passport.json").readText())
    private val map = json.decodeFromString<MapFile>(File(data, "map.json").readText())
    private val books = json.decodeFromString<BooksFile>(File(data, "books.json").readText()).books
    private val p = Passport(file)

    private fun hangul(s: String?) = s != null && s.any { it in '가'..'힣' }

    @Test
    fun fourteenRegionsAndKorea() {
        val ids = p.regions.map { it.id }
        assertEquals(15, ids.toSet().size)
        assertEquals("the map has exactly the 14 regions", ids.filter { it != "koreya" }.sorted(), map.regions.keys.sorted())
        p.regions.forEach { r ->
            assertTrue("${r.id}: names", r.name.isNotBlank() && hangul(r.ko))
            assertTrue("${r.id}: fact in Uzbek", r.fact.matches(Regex(".*[.!]$")) && !hangul(r.fact))
            assertTrue("${r.id}: fact in Korean", hangul(r.koFact))
            assertTrue("${r.id}: city in Korean", r.city == null || hangul(r.koCity))
            assertTrue("${r.id}: colours", r.ink.matches(Regex("#[0-9a-f]{6}")) && r.fill.matches(Regex("#[0-9a-f]{6}")))
            assertEquals("${r.id}: two stickers", 2, r.stickers.size)
            assertTrue("${r.id}: a book goes there", p.booksOf(books, r.id).isNotEmpty())
            assertEquals("${r.id}: found by its 'to' form", r, p.byDative(Passport.dative(r.name)))
            assertEquals("${r.id}: found by name", r, p.byName(r.name))
            assertTrue("${r.id}: stamp picture", File(assets, r.stamp).isFile)
            r.stickers.forEach { s -> assertTrue("${s.id}: picture", File(assets, s.image).isFile && hangul(s.ko)) }
        }
        assertEquals(30, p.stickers.map { it.first.id }.toSet().size)
        assertEquals(50, p.price)
    }

    @Test
    fun theToFormOfAPlace() {
        assertEquals("Xorazmga", Passport.dative("Xorazm"))
        assertEquals("Toshkent viloyatiga", Passport.dative("Toshkent viloyati"))
        assertEquals("Ipakka", Passport.dative("Ipak"))
        assertEquals("Iroqqa", Passport.dative("Iroq"))
    }

    @Test
    fun everyStoryGoesSomewhere() {
        books.filter { it.category != "alifbo" && it.category != "kichik" }.forEach { b ->
            assertNotNull("${b.key}: has a place", p.regionOf(b))
            if (b.category in setOf("korea", "twins") && b.key != "buvijon_qongiroq") assertEquals(b.key, "koreya", b.region)
        }
        assertEquals("buxoro", p.regionOf(books.first { it.key == "afandi_1" })?.id)
        assertEquals("xorazm", p.regionOf(books.first { it.key == "oltin_tarvuz" })?.id)
        assertEquals("samarqand", p.regionOf(books.first { it.key == "buvijon_qongiroq" })?.id)
    }

    @Test
    fun finishedBooksStampTheirPlaces() {
        assertTrue(p.stamps(books, emptyMap()).isEmpty())
        val records = mapOf(
            "oltin_tarvuz" to BookRecord("oltin_tarvuz", finished = true, finishedAt = 300),
            "hakim_qizi" to BookRecord("hakim_qizi", finished = true, finishedAt = 100),
            "sehrli_gilam" to BookRecord("sehrli_gilam", finished = true),
            "afandi_1" to BookRecord("afandi_1", page = 4),
            "alifbo" to BookRecord("alifbo", finished = true, finishedAt = 50),
            "asal_ismi" to BookRecord("asal_ismi", finished = true),
            "nosuchbook" to BookRecord("nosuchbook", finished = true)
        )
        val s = p.stamps(books, records)
        assertEquals(setOf("koreya", "xorazm"), s.keys)
        assertEquals("dated by the first book finished there", 100L, s["xorazm"])
        assertEquals("finished before dates were kept", 0L, s["koreya"])
        val oct1 = LocalDateTime.of(2026, 10, 1, 12, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()
        assertEquals("01.10.2026", Passport.dateLabel(oct1, ZoneId.of("Asia/Seoul")))
        assertEquals("", Passport.dateLabel(0))
    }

    @Test
    fun whatAChildMayDoWithASticker() {
        val stamps = mapOf("xorazm" to 0L)
        assertEquals(StickerState.HAVE, p.stickerState("kaltaminor", stamps, setOf("kaltaminor"), 70))
        assertEquals(StickerState.OK, p.stickerState("qovun", stamps, setOf("kaltaminor"), 70))
        assertEquals(StickerState.STAMP, p.stickerState("registon", stamps, emptySet(), 70))
        assertEquals(StickerState.POINTS, p.stickerState("qovun", stamps, emptySet(), 49))
        assertEquals(StickerState.NONE, p.stickerState("nothing", stamps, emptySet(), 70))
    }

    @Test
    fun placesInKorean() {
        val menus = ContentRepository.koreanMenus(json.decodeFromString<I18nFile>(File(data, "i18n.json").readText()), file)
        assertEquals("📍 이 이야기를 따라 호라즘에 다녀왔어요!", menus.translate("📍 Bu ertak seni Xorazmga olib bordi!"))
        assertTrue(menus.translate("📍 Bu ertak seni Toshkent viloyatiga olib bordi!").orEmpty().contains("타슈켄트주에"))
        assertEquals("🗺️ 새 도장: 카슈카다리야!", menus.translate("🗺️ Yangi muhr: Qashqadaryo!"))
        assertEquals("🎁 새 스티커: 칼타 미노르!", menus.translate("🎁 Yangi stiker: Kalta minor!"))
        assertEquals("도장 3 / 15개", menus.translate("3 / 15 muhr"))
        assertEquals("20점 더 필요해요", menus.translate("Yana 20 ball kerak"))
    }

    @Test
    fun theMapHasItsParts() {
        assertEquals(1000, map.width)
        assertTrue(map.height in 601..719)
        assertEquals(4900, map.seoulKm)
        map.regions.values.forEach { assertEquals(2, it.label.size) }
        assertEquals(4, map.korea.box.size)
        assertTrue(file.names.keys.containsAll(map.names.keys))
    }
}
