package uz.haksenguz.ertaklar.data.holidays

import android.icu.util.Calendar
import android.icu.util.ULocale
import java.time.LocalDate
import java.time.ZoneId
import uz.haksenguz.ertaklar.core.holidays.LunarCalendar

/**
 * The phone's own calendars: the Korean lunar one ("dangi") and the Islamic
 * Umm al-Qura one, as Android's ICU knows them.
 */
class IcuLunarCalendar : LunarCalendar {
    private val calendars = mutableMapOf<String, Calendar?>()

    override fun monthDay(cal: String, date: LocalDate): Pair<Int, Int>? {
        val c = synchronized(calendars) { calendars.getOrPut(cal) { open(cal) } } ?: return null
        synchronized(c) {
            // Noon, safe from clock changes.
            c.timeInMillis = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            // A leap month repeats a month's number; the holidays are never in one.
            if (c.get(Calendar.IS_LEAP_MONTH) == 1) return 0 to 0
            return c.get(Calendar.MONTH) + 1 to c.get(Calendar.DAY_OF_MONTH)
        }
    }

    /** The calendar, if the phone really has it (rather than falling back to Gregorian). */
    private fun open(cal: String): Calendar? = runCatching {
        Calendar.getInstance(ULocale("en@calendar=$cal")).takeIf { it.type == cal }
    }.getOrNull()
}
