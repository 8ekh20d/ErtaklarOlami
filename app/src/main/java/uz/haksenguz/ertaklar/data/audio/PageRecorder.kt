package uz.haksenguz.ertaklar.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.math.min

/**
 * Records one page from the microphone: AAC in MP4, which plays on every phone
 * (and in shared voice packs). The file stays on this device.
 */
class PageRecorder @Inject constructor(@ApplicationContext private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var startedAt = 0L

    /** The recording being made. */
    var file: File? = null
        private set

    /** Starts recording into [file]; [onLimit] is called when a page reaches [MAX_SECONDS]. */
    fun start(file: File, onLimit: () -> Unit) {
        cancel()
        file.parentFile?.mkdirs()
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(44_100)
            r.setAudioEncodingBitRate(48_000)
            r.setMaxDuration(MAX_SECONDS * 1000)
            r.setOnInfoListener { _, what, _ -> if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) onLimit() }
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
        } catch (e: Exception) {
            r.release()
            throw e
        }
        recorder = r
        this.file = file
        startedAt = System.currentTimeMillis()
    }

    /** How loud the voice is now, 0..1, for the ring around the record button. */
    fun level(): Float = runCatching { min(1f, (recorder?.maxAmplitude ?: 0) / 32_767f * 2.5f) }.getOrDefault(0f)

    /** Seconds since recording started. */
    fun seconds(): Double = (System.currentTimeMillis() - startedAt) / 1000.0

    /** Ends the recording; returns its file, or null when nothing usable was recorded. */
    fun stop(): File? {
        val r = recorder ?: return null
        recorder = null
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        return file.takeIf { ok }
    }

    /** Stops without keeping anything. */
    fun cancel() {
        recorder?.let { r ->
            runCatching { r.stop() }
            r.release()
        }
        recorder = null
        file?.delete()
        file = null
    }

    companion object {
        /** A page is limited to three minutes. */
        const val MAX_SECONDS = 180

        /** AAC in MP4, as browsers name it, so packs play in every app. */
        const val MIME = "audio/mp4;codecs=mp4a.40.2"
    }
}
