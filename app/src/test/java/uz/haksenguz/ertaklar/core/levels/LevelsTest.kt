package uz.haksenguz.ertaklar.core.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which shelf a child belongs on, and which shelves a book stands on. */
class LevelsTest {

    @Test
    fun noAgeNoShelf() {
        assertNull(Levels.shelfFor(null))
    }

    @Test
    fun shelfForAge() {
        listOf(3 to "4-6", 4 to "4-6", 6 to "4-6", 7 to "7-8", 8 to "7-8", 9 to "9+", 11 to "9+", 15 to "9+")
            .forEach { (age, shelf) -> assertEquals("a $age-year-old", shelf, Levels.shelfFor(age)) }
        Levels.AGES.forEach { assertNotNull("age $it has a shelf", Levels.shelfFor(it)) }
    }

    @Test
    fun booksStandOnEveryShelfTheirAgesTouch() {
        assertTrue(Levels.fits(listOf(5, 8), "4-6"))
        assertTrue(Levels.fits(listOf(5, 8), "7-8"))
        assertFalse(Levels.fits(listOf(5, 8), "9+"))
        assertFalse(Levels.fits(listOf(9, 12), "4-6"))
        assertFalse(Levels.fits(listOf(9, 12), "7-8"))
        assertTrue(Levels.fits(listOf(9, 12), "9+"))
        assertTrue(Levels.fits(listOf(9, 12), Levels.ALL))
        assertTrue("a book without ages is everywhere", Levels.fits(emptyList(), "4-6"))
    }

    @Test
    fun labels() {
        assertEquals("5–8 yosh", Levels.label(listOf(5, 8)))
        assertEquals("6 yosh", Levels.ageLabel(6))
        assertEquals("11+ yosh", Levels.ageLabel(11))
    }
}
