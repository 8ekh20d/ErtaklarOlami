package uz.haksenguz.ertaklar.core.games

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamesTest {

    @Test
    fun orderPicksFirstThirdsAndLast() {
        assertEquals(listOf(0, 4, 7, 11), Games.orderPicks(12))
        assertEquals(listOf(0, 3, 6, 9), Games.orderPicks(10))
        assertEquals(listOf(0, 1, 2), Games.orderPicks(3))
        assertEquals(listOf(0), Games.orderPicks(1))
    }

    @Test
    fun aShuffleNeverLeavesEverythingInPlace() {
        val list = listOf(1, 2, 3, 4)
        repeat(500) { seed ->
            val s = Games.shuffled(list, Random(seed))
            assertEquals(list.sorted(), s.sorted())
            assertNotEquals(list, s)
        }
        repeat(20) { seed -> assertEquals(listOf(2, 1), Games.shuffled(listOf(1, 2), Random(seed))) }
        assertEquals(listOf(7), Games.shuffled(listOf(7)))
    }

    /** A 6 × 6 picture: a box drawn in dark ink inside a white page, with a grey soft edge on its left side. */
    private fun page(): Regions {
        val w = 6
        val lum = IntArray(36) { 255 }
        for (i in 1..4) {
            lum[1 * w + i] = 40
            lum[4 * w + i] = 40
            lum[i * w + 1] = 40
            lum[i * w + 4] = 40
        }
        lum[2 * w + 0] = 150 // soft edge outside the box
        return Regions.of(w, 6, lum)
    }

    @Test
    fun linesSeparateTheAreas() {
        val r = page()
        assertEquals(2, r.count)
        val outside = r.at(0, 0)
        val inside = r.at(2, 2)
        assertNotEquals(outside, inside)
        assertEquals(inside, r.at(3, 3))
        assertEquals(-1, r.at(1, 1))
        assertEquals("the soft edge joins the area beside it", outside, r.at(0, 2))
        assertEquals(-1, r.at(-1, 0))
    }

    @Test
    fun aTapOnALineFillsTheNearestArea() {
        val r = page()
        assertTrue(r.near(1, 2, 1) >= 0)
        assertEquals(-1, r.near(1, 1, 0))
        val colors = IntArray(r.count) { 0x10 + it }
        val out = IntArray(36)
        r.paint(colors, line = 0, out = out)
        assertEquals(colors[r.at(2, 2)], out[2 * 6 + 2])
        assertEquals(0, out[1 * 6 + 1])
    }

    @Test
    fun luminanceWeighsGreenMost() {
        assertEquals(255, Regions.luminance(0xFFFFFFFF.toInt()))
        assertEquals(0, Regions.luminance(0xFF000000.toInt()))
        assertTrue(Regions.luminance(0xFF00FF00.toInt()) > Regions.luminance(0xFFFF0000.toInt()))
    }

    /** A letter "L" on a 120-pixel pad: a bar down the left and a bar along the bottom. */
    private fun letterL(): TraceGrid = TraceGrid.of(120) { x, y -> (x in 20..40 && y in 10..110) || (x in 20..100 && y in 90..110) }

    @Test
    fun goingOverTheWholeLetterPasses() {
        val g = letterL()
        g.line(30f, 10f, 30f, 110f, 22f)
        g.line(20f, 100f, 100f, 100f, 22f)
        val s = g.score()
        assertTrue("$s", s.passed)
        assertEquals(100, s.meter)
    }

    @Test
    fun onlyOnePartOfTheLetterIsNotEnough() {
        val g = letterL()
        g.line(30f, 10f, 30f, 110f, 22f)
        val s = g.score()
        assertFalse("$s", s.passed)
        assertTrue(s.part < TraceGrid.PART)
        assertTrue(s.meter < 100)
    }

    @Test
    fun scribblingBesideTheLetterStrays() {
        val g = letterL()
        g.line(30f, 10f, 30f, 110f, 22f)
        g.line(20f, 100f, 100f, 100f, 22f)
        for (y in 0..80 step 8) g.line(50f, y.toFloat(), 118f, y.toFloat(), 22f)
        val s = g.score()
        assertTrue("$s", s.strayed)
        assertFalse(s.passed)
        g.clear()
        assertEquals(0.0, g.score().cover, 0.0)
    }

    @Test
    fun aboutAHundredAndTwentyCellsAcross() {
        assertEquals(5, TraceGrid.cellFor(600))
        assertEquals(2, TraceGrid.cellFor(100))
    }
}
