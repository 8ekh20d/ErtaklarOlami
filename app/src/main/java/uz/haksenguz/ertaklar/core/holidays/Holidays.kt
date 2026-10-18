package uz.haksenguz.ertaklar.core.holidays

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import uz.haksenguz.ertaklar.data.models.HolidayDef
import uz.haksenguz.ertaklar.data.models.HolidaysFile

/** The month and day a date has in another calendar. */
fun interface LunarCalendar {
    /** [date] as (month, day) in calendar [cal] ("dangi", "islamic-umalqura"); null when the phone doesn't have it. */
    fun monthDay(cal: String, date: LocalDate): Pair<Int, Int>?
}

/**
 * When each holiday comes round, so the library can show a holiday book's date
 * and the banner can offer it in the weeks before. Most days are fixed;
 * Seollal and Chusok follow the Korean lunar calendar and the two Hayits the
 * Islamic one, found through [calendar]. Without those calendars they have no
 * date rather than a wrong one.
 */
class Holidays(private val file: HolidaysFile, private val calendar: LunarCalendar) {
    val days: Map<String, HolidayDef> = file.days

    /** The banner offers a holiday's book from this many days before it. */
    val soonDays: Int = file.soon

    /** A holiday near or on: its id and name, its date, and the days from today (negative on its later days). */
    data class Upcoming(val id: String, val name: String, val date: LocalDate, val days: Int)

    private val found = mutableMapOf<String, LocalDate?>()

    /** The holiday's next date, counting from [back] days before [today]. */
    fun next(id: String, today: LocalDate, back: Int = 0): LocalDate? {
        val h = days[id] ?: return null
        val from = today.minusDays(back.toLong())
        val (m, d) = h.date
        val cal = h.cal ?: return LocalDate.of(from.year, m, d).let { if (it < from) LocalDate.of(from.year + 1, m, d) else it }
        return synchronized(found) {
            found.getOrPut("$cal $m/$d $from") {
                (0 until SEARCH).asSequence().map { from.plusDays(it.toLong()) }.firstOrNull { calendar.monthDay(cal, it) == (m to d) }
            }
        }
    }

    /** The book's holiday that comes first, including one that is on now. */
    fun upcoming(holidays: List<String>, today: LocalDate): Upcoming? = holidays.mapNotNull { id ->
        val h = days[id] ?: return@mapNotNull null
        val date = next(id, today, h.long) ?: return@mapNotNull null
        Upcoming(id, h.name, date, daysTo(date, today))
    }.minByOrNull { it.days }

    /** That holiday, if it is near enough for the banner. */
    fun soon(holidays: List<String>, today: LocalDate): Upcoming? = upcoming(holidays, today)?.takeIf { it.days <= soonDays }

    /** "9-oktabr". */
    fun dateLabel(date: LocalDate): String = "${date.dayOfMonth}-${file.months[date.monthValue - 1]}"

    companion object {
        /** A lunar holiday is looked for this many days ahead. */
        private const val SEARCH = 400

        /** Whole days from [today] to [date]; negative once it has passed. */
        fun daysTo(date: LocalDate, today: LocalDate): Int = ChronoUnit.DAYS.between(today, date).toInt()

        /** The banner's words before the holiday's name. */
        fun whenText(days: Int): String = when {
            days < 0 -> "🎉 Bayram kunlari:"
            days == 0 -> "🎉 Bugun bayram:"
            days == 1 -> "🎉 Ertaga bayram:"
            else -> "🎉 $days kundan keyin bayram:"
        }
    }
}
