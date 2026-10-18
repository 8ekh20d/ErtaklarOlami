package uz.haksenguz.ertaklar.data.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.narration.Narration

/**
 * Plays one recorded page (or a stretch of it) and reports which piece is
 * being read, so the page can light it up. Used from the main thread.
 */
class ClipPlayer @Inject constructor(@ApplicationContext private val context: Context) {
    private var player: ExoPlayer? = null
    private var ticker: Job? = null
    private var onEnd: ((Throwable?) -> Unit)? = null

    val playing: Boolean get() = onEnd != null

    /**
     * Plays [uri] from [from] seconds, until [to] or its end; [onSeg] gets the
     * piece being read (by [marks]) as it changes, and [onEnd] is called once.
     */
    fun play(
        scope: CoroutineScope,
        uri: Uri,
        marks: List<Double>,
        from: Double = 0.0,
        to: Double? = null,
        onSeg: (Int) -> Unit = {},
        onEnd: (Throwable?) -> Unit = {}
    ) {
        stop()
        val p = player ?: ExoPlayer.Builder(context).build().also { player = it }
        this.onEnd = onEnd
        p.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) finish(null)
                }

                override fun onPlayerError(error: PlaybackException) = finish(error)
            }.also { listener = it }
        )
        p.setMediaItem(MediaItem.fromUri(uri))
        p.prepare()
        if (from > 0) p.seekTo((from * 1000).toLong())
        p.play()
        var last = -1
        ticker = scope.launch {
            while (isActive) {
                val t = p.currentPosition / 1000.0
                val seg = Narration.segAt(marks, t)
                if (seg != last) {
                    last = seg
                    onSeg(seg)
                }
                if (to != null && t >= to) {
                    finish(null)
                    break
                }
                delay(50)
            }
        }
    }

    private var listener: Player.Listener? = null

    private fun finish(error: Throwable?) {
        val done = onEnd ?: return
        stop()
        done(error)
    }

    /** Stops without calling the end callback. */
    fun stop() {
        ticker?.cancel()
        ticker = null
        onEnd = null
        player?.let { p ->
            listener?.let { p.removeListener(it) }
            p.stop()
            p.clearMediaItems()
        }
        listener = null
    }

    fun release() {
        stop()
        player?.release()
        player = null
    }
}
