package uz.haksenguz.ertaklar.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The phone's own Korean voice, for a word's Korean meaning (almost every phone in Korea has one).
 * Nothing is recorded or sent anywhere; the system speaks the text.
 */
@Singleton
class KoreanTts @Inject constructor(@ApplicationContext private val context: Context) {
    private var tts: TextToSpeech? = null
    private val ready = CompletableDeferred<Boolean>()

    /** Whether the utterance being waited for started (true) or failed (false). */
    private var started: CompletableDeferred<Boolean>? = null
    private var utterance: String? = null

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            if (utteranceId == utterance) started?.complete(true)
        }

        override fun onDone(utteranceId: String?) = Unit

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            if (utteranceId == utterance) started?.complete(false)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            if (utteranceId == utterance) started?.complete(false)
        }
    }

    /** Says [text] in Korean; false when the phone has no Korean voice that can speak. */
    suspend fun speak(text: String, rate: Float = 0.9f): Boolean {
        val engine = tts ?: TextToSpeech(context) { status -> ready.complete(status == TextToSpeech.SUCCESS) }
            .also {
                it.setOnUtteranceProgressListener(listener)
                tts = it
            }
        if (withTimeoutOrNull(3_000) { ready.await() } != true) return false
        val result = engine.setLanguage(Locale.KOREAN)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return false
        engine.setSpeechRate(rate)
        val id = UUID.randomUUID().toString()
        val wait = CompletableDeferred<Boolean>()
        utterance = id
        started = wait
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) return false
        // A voice whose data isn't on the phone fails right away instead of speaking.
        return withTimeoutOrNull(4_000) { wait.await() } ?: false
    }

    fun stop() {
        tts?.stop()
    }
}
