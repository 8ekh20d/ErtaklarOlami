package uz.haksenguz.ertaklar.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.haksenguz.ertaklar.core.holidays.Holidays
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.holidays.IcuLunarCalendar

/** The holidays, with the phone's own lunar calendars. */
@Singleton
class HolidayRepository @Inject constructor(private val content: ContentRepository) {
    private var holidays: Holidays? = null

    /** Read once; finding the lunar dates walks the calendar, so it runs off the main thread. */
    suspend fun holidays(): Holidays = holidays ?: withContext(Dispatchers.Default) {
        Holidays(content.holidays(), IcuLunarCalendar())
    }.also { holidays = it }
}
