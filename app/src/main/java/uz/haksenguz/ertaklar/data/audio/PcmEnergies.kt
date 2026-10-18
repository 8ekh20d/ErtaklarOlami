package uz.haksenguz.ertaklar.data.audio

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder
import kotlin.math.max
import uz.haksenguz.ertaklar.core.narration.Narration

/** A decoded recording: how long it is, and its loudness frame by frame. */
class Loudness(val duration: Double, val db: FloatArray)

/**
 * Decodes a recording and measures its loudness in [Narration.FRAME]-long
 * frames (channels averaged), streaming, so a long page never sits in memory.
 */
object PcmEnergies {

    fun measure(file: File): Loudness {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)
        val track = (0 until extractor.trackCount).first {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") ==
                true
        }
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var floatPcm = false
        var frameSize = max(1, (sampleRate * Narration.FRAME).toInt())
        val db = ArrayList<Float>()
        var sum = 0.0
        var inFrame = 0
        var samples = 0L

        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            if (!inputDone) {
                val inIndex = codec.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    val buffer = codec.getInputBuffer(inIndex)!!
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = codec.dequeueOutputBuffer(info, 10_000)
            when {
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val out = codec.outputFormat
                    sampleRate = out.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = out.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    floatPcm = out.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        out.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                    frameSize = max(1, (sampleRate * Narration.FRAME).toInt())
                }

                outIndex >= 0 -> {
                    val buffer = codec.getOutputBuffer(outIndex)!!.order(ByteOrder.LITTLE_ENDIAN)
                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    if (floatPcm) {
                        val f = buffer.asFloatBuffer()
                        while (f.remaining() >= channels) {
                            var v = 0.0
                            repeat(channels) { v += f.get() }
                            v /= channels
                            sum += v * v
                            samples++
                            if (++inFrame == frameSize) {
                                db += Narration.frameDb(sum, frameSize)
                                sum = 0.0
                                inFrame = 0
                            }
                        }
                    } else {
                        val s = buffer.asShortBuffer()
                        while (s.remaining() >= channels) {
                            var v = 0.0
                            repeat(channels) { v += s.get() / 32_768.0 }
                            v /= channels
                            sum += v * v
                            samples++
                            if (++inFrame == frameSize) {
                                db += Narration.frameDb(sum, frameSize)
                                sum = 0.0
                                inFrame = 0
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        }
        codec.stop()
        codec.release()
        extractor.release()
        return Loudness(duration = samples.toDouble() / sampleRate, db = db.toFloatArray())
    }
}
