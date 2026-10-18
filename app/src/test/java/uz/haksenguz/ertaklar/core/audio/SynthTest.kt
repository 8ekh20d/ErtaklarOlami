package uz.haksenguz.ertaklar.core.audio

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SynthTest {

    private fun peak(s: FloatArray) = s.maxOf { abs(it) }

    @Test
    fun lengthsMatchTheSounds() {
        assertEquals((Synth.RATE * 0.38).toInt(), Synth.page().size)
        assertEquals((Synth.RATE * 0.5).toInt(), Synth.page(cover = true).size)
        assertEquals((Synth.RATE * (0.09 + 0.32)).toInt(), Synth.chime(false).size)
        assertEquals((Synth.RATE * (0.18 + 0.32)).toInt(), Synth.chime(true).size)
        assertEquals(Synth.RATE * 2.95, Synth.lullaby().size.toDouble(), 2.0)
    }

    @Test
    fun soundsAreAudibleButNeverClip() {
        listOf(
            Synth.page(random = Random(1)),
            Synth.page(cover = true, random = Random(1)),
            Synth.chime(true),
            Synth.chime(false),
            Synth.lullaby()
        )
            .forEach { s ->
                assertTrue("not silent: ${peak(s)}", peak(s) > 0.01f)
                assertTrue("not clipping: ${peak(s)}", peak(s) < 1f)
            }
        assertTrue("the lullaby is soft", peak(Synth.lullaby()) <= 0.07f)
    }

    @Test
    fun bedtimePaperIsAWhisper() {
        val loud = peak(Synth.page(random = Random(7)))
        val soft = peak(Synth.page(soft = 0.35f, random = Random(7)))
        assertEquals(0.35f, soft / loud, 0.01f)
    }

    @Test
    fun soundsFadeOutAtTheEnd() {
        listOf(Synth.page(random = Random(3)), Synth.chime(true), Synth.lullaby()).forEach { s ->
            assertTrue(s.takeLast(200).all { abs(it) < 0.005f })
        }
    }

    @Test
    fun pcmScalesToSixteenBits() {
        val pcm = Synth.pcm(floatArrayOf(0f, 1f, -1f, 2f, 0.5f))
        assertEquals(listOf<Short>(0, 32767, -32767, 32767, 16383), pcm.toList())
    }
}
