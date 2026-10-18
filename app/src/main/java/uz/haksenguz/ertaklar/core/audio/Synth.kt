package uz.haksenguz.ertaklar.core.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The app's little sounds, made from numbers rather than recordings: a page
 * swishing past, the cover's heavier thump, chimes for a right answer, and a
 * four-note lullaby at good night. Samples are mono floats in -1..1.
 */
object Synth {
    const val RATE = 44_100

    /**
     * Paper: noise fading out, through a band-pass filter whose centre sweeps
     * down like a sheet passing; [cover] is lower and heavier. [soft] scales it
     * down (a whisper of paper at bedtime).
     */
    fun page(cover: Boolean = false, soft: Float = 1f, random: Random = Random): FloatArray {
        val dur = if (cover) 0.5 else 0.38
        val n = (RATE * dur).toInt()
        val (f0, f1) = if (cover) 900.0 to 180.0 else 2600.0 to 700.0
        val peak = (if (cover) 0.45 else 0.3) * soft
        val q = 1.4
        val out = FloatArray(n)
        var x1 = 0.0
        var x2 = 0.0
        var y1 = 0.0
        var y2 = 0.0
        var b0 = 0.0
        var b2 = 0.0
        var a1 = 0.0
        var a2 = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            // The filter's coefficients follow the sweep, refreshed every 32 samples.
            if (i % 32 == 0) {
                val f = expRamp(f0, f1, t / dur)
                val w = 2 * PI * f / RATE
                val alpha = sin(w) / (2 * q)
                val a0 = 1 + alpha
                b0 = alpha / a0
                b2 = -alpha / a0
                a1 = -2 * cos(w) / a0
                a2 = (1 - alpha) / a0
            }
            val x = (random.nextDouble() * 2 - 1) * (1 - i.toDouble() / n)
            val y = b0 * x + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            out[i] = (y * envelope(t, attack = 0.06, end = dur, peak = peak)).toFloat()
        }
        return out
    }

    /** A right move: two quick bell notes, three going up when [happy]. */
    fun chime(happy: Boolean): FloatArray {
        val notes = if (happy) listOf(660.0, 880.0, 1320.0) else listOf(880.0, 1175.0)
        return tones(notes, step = 0.09, length = 0.32, attack = 0.02, end = 0.3, peak = 0.12, wave = ::triangle)
    }

    /** Good night: four soft, slow notes going down. */
    fun lullaby(): FloatArray =
        tones(listOf(659.25, 523.25, 440.0, 392.0), step = 0.6, length = 1.15, attack = 0.1, end = 1.1, peak = 0.06, wave = ::sin)

    /** 16-bit PCM for the speaker. */
    fun pcm(
        samples: FloatArray
    ): ShortArray = ShortArray(samples.size) { (max(-1f, min(1f, samples[it])) * Short.MAX_VALUE).toInt().toShort() }

    /** Notes [step] seconds apart, each [length] long, mixed together. */
    private fun tones(
        notes: List<Double>,
        step: Double,
        length: Double,
        attack: Double,
        end: Double,
        peak: Double,
        wave: (Double) -> Double
    ): FloatArray {
        val n = (RATE * (step * (notes.size - 1) + length)).toInt()
        val out = FloatArray(n)
        notes.forEachIndexed { k, f ->
            val start = (RATE * step * k).toInt()
            val len = (RATE * length).toInt()
            for (i in 0 until min(len, n - start)) {
                val t = i.toDouble() / RATE
                out[start + i] += (wave(2 * PI * f * t) * envelope(t, attack, end, peak)).toFloat()
            }
        }
        return out
    }

    /** A triangle wave with the period of sin. */
    private fun triangle(phase: Double): Double {
        val p = (phase / (2 * PI)) % 1.0
        return 4 * abs(p - 0.5) - 1
    }

    /** Rises exponentially from almost nothing to [peak] at [attack], then falls back by [end] seconds. */
    private fun envelope(t: Double, attack: Double, end: Double, peak: Double): Double = when {
        t <= attack -> expRamp(FLOOR, peak, t / attack)
        t <= end -> expRamp(peak, FLOOR, (t - attack) / (end - attack))
        else -> 0.0
    }

    /** From [from] to [to] along an exponential curve, [k] in 0..1. */
    private fun expRamp(from: Double, to: Double, k: Double): Double = from * exp(ln(to / from) * k.coerceIn(0.0, 1.0))

    private const val FLOOR = 0.0001
}
