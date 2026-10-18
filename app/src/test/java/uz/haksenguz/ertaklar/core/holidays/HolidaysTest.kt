package uz.haksenguz.ertaklar.core.holidays

import java.io.File
import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.haksenguz.ertaklar.data.models.HolidaysFile

class HolidaysTest {

    private val file = Json {
        ignoreUnknownKeys = true
    }.decodeFromString<HolidaysFile>(File("src/main/assets/data/holidays.json").readText())

    /** The lunar dates as published, for the years the tests look at (the phone's ICU gives them in the app). */
    private val table: Map<LocalDate, Pair<String, Pair<Int, Int>>> = mapOf(
        LocalDate.of(2026, 2, 17) to ("dangi" to (1 to 1)),
        LocalDate.of(2027, 2, 7) to ("dangi" to (1 to 1)),
        LocalDate.of(2028, 1, 27) to ("dangi" to (1 to 1)),
        LocalDate.of(2026, 9, 25) to ("dangi" to (8 to 15)),
        LocalDate.of(2027, 9, 15) to ("dangi" to (8 to 15)),
        LocalDate.of(2028, 10, 3) to ("dangi" to (8 to 15)),
        LocalDate.of(2026, 3, 20) to ("islamic-umalqura" to (10 to 1)),
        LocalDate.of(2027, 3, 9) to ("islamic-umalqura" to (10 to 1)),
        LocalDate.of(2026, 5, 27) to ("islamic-umalqura" to (12 to 10)),
        LocalDate.of(2027, 5, 16) to ("islamic-umalqura" to (12 to 10))
    )
    private val calendar = LunarCalendar { cal, date -> table[date]?.takeIf { it.first == cal }?.second ?: (0 to 0) }
    private val h = Holidays(file, calendar)

    private fun d(s: String) = LocalDate.parse(s)

    private fun next(id: String, today: String) = h.next(id, d(today))?.toString() ?: "none"

    @Test
    fun fixedDaysThisYearOrNext() {
        assertEquals("2026-10-09", next("hangul", "2026-10-01"))
        assertEquals("2026-10-09", next("hangul", "2026-10-09"))
        assertEquals("2027-10-09", next("hangul", "2026-10-10"))
        assertEquals("2026-10-21", next("uztili", "2026-10-01"))
        assertEquals("2027-03-21", next("navruz", "2026-10-01"))
        assertEquals("2027-09-01", next("mustaqillik", "2026-10-01"))
        assertEquals("2027-05-05", next("eorini", "2026-10-01"))
        assertEquals("2027-06-01", next("bolalar", "2026-10-01"))
    }

    @Test
    fun lunarDaysFromTheCalendar() {
        assertEquals("2026-02-17", next("seollal", "2026-01-01"))
        assertEquals("2027-02-07", next("seollal", "2026-10-01"))
        assertEquals("2028-01-27", next("seollal", "2027-03-01"))
        assertEquals("2026-09-25", next("chuseok", "2026-09-01"))
        assertEquals("2027-09-15", next("chuseok", "2026-10-01"))
        assertEquals("2028-10-03", next("chuseok", "2027-10-01"))
        assertEquals("2027-03-09", next("ramazon", "2026-10-01"))
        assertEquals("2027-05-16", next("qurbon", "2026-10-01"))
        h.days.keys.forEach { id ->
            val date = h.next(id, d("2026-10-01"))
            assertTrue("$id comes within a year", date != null && Holidays.daysTo(date, d("2026-10-01")) in 0..366)
        }
        assertNull(h.next("nonsense", d("2026-10-01")))
    }

    @Test
    fun withoutTheLunarCalendarsNoDate() {
        val none = Holidays(file) { _, _ -> null }
        assertNull("no date rather than a wrong one", none.next("seollal", d("2030-06-01")))
        assertNull(none.next("ramazon", d("2030-06-01")))
        assertEquals(d("2030-10-09"), none.next("hangul", d("2030-06-01")))
    }

    @Test
    fun distancesAndWords() {
        assertEquals(8, Holidays.daysTo(d("2026-10-09"), d("2026-10-01")))
        assertEquals(-2, Holidays.daysTo(d("2026-10-01"), d("2026-10-03")))
        assertEquals("9-oktabr", h.dateLabel(d("2026-10-09")))
        assertEquals("7-fevral", h.dateLabel(d("2027-02-07")))
        assertEquals("31-dekabr", h.dateLabel(d("2027-12-31")))
        assertEquals("🎉 8 kundan keyin bayram:", Holidays.whenText(8))
        assertEquals("🎉 Ertaga bayram:", Holidays.whenText(1))
        assertEquals("🎉 Bugun bayram:", Holidays.whenText(0))
        assertEquals("🎉 Bayram kunlari:", Holidays.whenText(-1))
    }

    @Test
    fun aBooksNextHoliday() {
        val letters = listOf("hangul", "uztili")
        val newYears = listOf("seollal", "navruz")
        h.upcoming(letters, d("2026-10-01"))!!.let { assertEquals("hangul" to 8, it.id to it.days) }
        h.upcoming(letters, d("2026-10-10"))!!.let { assertEquals("uztili" to 11, it.id to it.days) }
        h.upcoming(letters, d("2026-10-22"))!!.let { assertEquals("hangul" to d("2027-10-09"), it.id to it.date) }
        h.upcoming(newYears, d("2026-10-01"))!!.let { assertEquals("seollal" to d("2027-02-07"), it.id to it.date) }
        h.upcoming(newYears, d("2027-02-08"))!!.let { assertEquals("Seollal is three days long", "seollal" to -1, it.id to it.days) }
        assertEquals("navruz", h.upcoming(newYears, d("2027-02-09"))?.id)
        assertNull(h.upcoming(emptyList(), d("2026-10-01")))
        assertNull(h.upcoming(listOf("nonsense"), d("2026-10-01")))
    }

    @Test
    fun theBannerOffersTheBookThreeWeeksBefore() {
        val letters = listOf("hangul", "uztili")
        assertNull(h.soon(letters, d("2026-09-17")))
        assertEquals(21, h.soon(letters, d("2026-09-18"))?.days)
        assertEquals(0, h.soon(letters, d("2026-10-09"))?.days)
        assertEquals("uztili", h.soon(letters, d("2026-10-10"))?.id)
        assertEquals(-1, h.soon(listOf("seollal", "navruz"), d("2027-02-08"))?.days)
    }
}
