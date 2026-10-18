package uz.haksenguz.ertaklar.data.holidays

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uz.haksenguz.ertaklar.core.holidays.Holidays
import uz.haksenguz.ertaklar.data.models.HolidaysFile

/** The lunar holidays with the phone's own calendars, against the dates published in Korea and Uzbekistan. */
@RunWith(AndroidJUnit4::class)
class IcuHolidaysTest {

    private val holidays = Holidays(
        Json { ignoreUnknownKeys = true }.decodeFromString<HolidaysFile>(
            InstrumentationRegistry.getInstrumentation().targetContext.assets.open("data/holidays.json").bufferedReader().use {
                it.readText()
            }
        ),
        IcuLunarCalendar()
    )

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun koreanLunarHolidays() {
        assertEquals(d("2026-02-17"), holidays.next("seollal", d("2026-01-01")))
        assertEquals(d("2027-02-07"), holidays.next("seollal", d("2026-10-01")))
        assertEquals(d("2028-01-27"), holidays.next("seollal", d("2027-03-01")))
        assertEquals(d("2026-09-25"), holidays.next("chuseok", d("2026-09-01")))
        assertEquals(d("2027-09-15"), holidays.next("chuseok", d("2026-10-01")))
        assertEquals(d("2028-10-03"), holidays.next("chuseok", d("2027-10-01")))
    }

    /** Uzbekistan announces Hayit each year; it can fall a day either side of Umm al-Qura. */
    @Test
    fun hayitWithinADay() {
        listOf(
            Triple("ramazon", "2026-01-01", "2026-03-20"),
            Triple("ramazon", "2026-10-01", "2027-03-09"),
            Triple("qurbon", "2026-01-01", "2026-05-27"),
            Triple("qurbon", "2026-10-01", "2027-05-16")
        ).forEach { (id, from, want) ->
            val got = holidays.next(id, d(from))
            assertNotNull("$id after $from", got)
            assertTrue("$id after $from: about $want, got $got", abs(Holidays.daysTo(got!!, d(want))) <= 1)
        }
    }
}
