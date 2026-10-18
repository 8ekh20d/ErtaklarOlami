package uz.haksenguz.ertaklar.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.audio.Synth
import uz.haksenguz.ertaklar.data.repository.SettingsRepository

/**
 * Plays the app's little sounds (the page turning, chimes, the lullaby) unless
 * the sound is switched off. At bedtime the paper only whispers and the chimes
 * stay quiet.
 */
@Singleton
class Sfx @Inject constructor(settings: SettingsRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var on = true

    private val chime by lazy { Synth.pcm(Synth.chime(false)) }
    private val happy by lazy { Synth.pcm(Synth.chime(true)) }
    private val lullaby by lazy { Synth.pcm(Synth.lullaby()) }

    init {
        scope.launch { settings.settings.collect { on = it.sound } }
    }

    /** A page swishing past; the [cover] is heavier. */
    fun page(cover: Boolean = false, bedtime: Boolean = false) {
        if (on) scope.launch { play(Synth.pcm(Synth.page(cover, soft = if (bedtime) 0.35f else 1f))) }
    }

    /** A right move; [happy] for a right answer or a game won. */
    fun chime(happy: Boolean = false, bedtime: Boolean = false) {
        if (on && !bedtime) scope.launch { play(if (happy) this@Sfx.happy else chime) }
    }

    /** Good night. */
    fun lullaby() {
        if (on) scope.launch { play(lullaby) }
    }

    private suspend fun play(pcm: ShortArray) {
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(Synth.RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
        }.getOrNull() ?: return
        // The sounds are a nicety: if the phone won't play one, the app carries on.
        runCatching {
            track.write(pcm, 0, pcm.size)
            track.play()
            delay(pcm.size * 1000L / Synth.RATE + 100)
        }
        track.release()
    }
}
