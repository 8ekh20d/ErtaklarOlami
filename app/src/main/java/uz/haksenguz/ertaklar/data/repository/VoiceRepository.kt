package uz.haksenguz.ertaklar.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStreamReader
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONObject
import uz.haksenguz.ertaklar.data.local.dao.VoiceDao
import uz.haksenguz.ertaklar.data.local.entities.ClipEntity
import uz.haksenguz.ertaklar.data.local.entities.VoiceEntity
import uz.haksenguz.ertaklar.data.models.Clip
import uz.haksenguz.ertaklar.data.models.ImportResult
import uz.haksenguz.ertaklar.data.models.Voice

/**
 * Family voices and their recordings, kept on this device: rows in the
 * database, audio files under filesDir/voices/<voice>/<book>/<page>.
 *
 * A voice pack carries one voice's recordings between phones as a single
 * ".json" file (the share sheet and Telegram accept it):
 *   { format: "ertaklar-ovoz", version: 1, voice: { id, name, avatar }, created,
 *     clips: [ { book, page, mime, duration, marks, sig, audio: base64 } ] }
 */
@Singleton
class VoiceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: VoiceDao,
    private val json: Json
) {
    val voices: Flow<List<Voice>> = dao.observeVoices().map { list -> list.map { it.toModel() } }

    /** Pages of [book] that [voiceId] has recorded. */
    fun pages(voiceId: String, book: String): Flow<Set<Int>> = dao.observePages(voiceId, book).map { it.toSet() }

    suspend fun voice(id: String): Voice? = dao.voice(id)?.toModel()

    suspend fun createVoice(name: String, avatar: String): Voice {
        val v = VoiceEntity(newId(), name.trim().ifEmpty { "Ovoz" }.take(24), avatar, System.currentTimeMillis())
        dao.upsertVoice(v)
        return v.toModel()
    }

    suspend fun updateVoice(id: String, name: String, avatar: String): Voice? {
        val v = dao.voice(id) ?: return null
        val updated = v.copy(name = name.trim().ifEmpty { v.name }.take(24), avatar = avatar)
        dao.upsertVoice(updated)
        return updated.toModel()
    }

    /** Removes a voice with all its recordings. */
    suspend fun deleteVoice(id: String) {
        dao.deleteVoice(id)
        withContext(Dispatchers.IO) { File(root, safe(id)).deleteRecursively() }
    }

    suspend fun clip(voiceId: String, book: String, page: Int): Clip? = dao.clip(voiceId, book, page)?.toModel()

    /** Keeps a page just recorded in [recording]. */
    suspend fun saveClip(
        voiceId: String,
        book: String,
        page: Int,
        recording: File,
        mime: String,
        duration: Double,
        marks: List<Double>,
        sig: String
    ) {
        val path = clipPath(voiceId, book, page, mime)
        withContext(Dispatchers.IO) {
            val target = File(context.filesDir, path)
            target.parentFile?.mkdirs()
            if (!recording.renameTo(target)) {
                recording.copyTo(target, overwrite = true)
                recording.delete()
            }
        }
        dao.upsertClip(ClipEntity(voiceId, book, page, path, mime, duration, json.encodeToString(marks), sig, System.currentTimeMillis()))
    }

    suspend fun deleteClip(voiceId: String, book: String, page: Int) {
        val c = dao.clip(voiceId, book, page) ?: return
        dao.deleteClip(voiceId, book, page)
        withContext(Dispatchers.IO) { File(context.filesDir, c.file).delete() }
    }

    /** A fresh file to record into, outside the voice folders until it is kept. */
    fun recordingFile(): File = File(context.cacheDir, "recording/${System.currentTimeMillis()}.m4a").also { it.parentFile?.mkdirs() }

    // ---------- voice packs ----------

    /** One file with this voice's recordings (of [book], or of every book), ready to share. */
    suspend fun exportPack(voiceId: String, book: String?): File = withContext(Dispatchers.IO) {
        val voice = dao.voice(voiceId) ?: error("no such voice")
        val clips = dao.clipsOfVoice(voiceId).filter { book == null || it.book == book }
        val slug = voice.name.replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').ifEmpty { "ovoz" }
        val out = File(context.cacheDir, "share/ovoz-$slug-${book ?: "hammasi"}.json")
        out.parentFile?.mkdirs()
        out.bufferedWriter().use { w ->
            w.write("{\"format\":${q(PACK_FORMAT)},\"version\":1,")
            w.write("\"voice\":{\"id\":${q(voice.id)},\"name\":${q(voice.name)},\"avatar\":${q(voice.avatar)}},")
            w.write("\"created\":${q(Instant.now().toString())},\"clips\":[")
            clips.forEachIndexed { i, c ->
                if (i > 0) w.write(",")
                val marks = json.decodeFromString<List<Double>>(c.marksJson).joinToString(",") { num(it) }
                w.write("{\"book\":${q(c.book)},\"page\":${c.page},\"mime\":${q(c.mime)},\"duration\":${num(c.duration)},")
                w.write("\"marks\":[$marks],\"sig\":${q(c.sig)},\"audio\":\"")
                // One page at a time: a 3-minute page is about a megabyte.
                w.write(Base64.encodeToString(File(context.filesDir, c.file).readBytes(), Base64.NO_WRAP))
                w.write("\"}")
            }
            w.write("]}")
        }
        out
    }

    /** Adds (or updates) the voice in a pack file; throws [NotAPackException] for anything else. */
    suspend fun importPack(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: throw NotAPackException()
        val staged = mutableListOf<StagedClip>()
        var format: String? = null
        var version = 0
        var voiceId: String? = null
        var name = "Ovoz"
        var avatar = "🎙️"
        var sawClips = false
        try {
            JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { r ->
                r.beginObject()
                while (r.hasNext()) {
                    when (r.nextName()) {
                        "format" -> format = r.stringOrNull()

                        "version" -> version = runCatching { r.nextInt() }.getOrElse {
                            r.skipValue()
                            0
                        }

                        "voice" -> {
                            r.beginObject()
                            while (r.hasNext()) {
                                when (r.nextName()) {
                                    "id" -> voiceId = r.stringOrNull()
                                    "name" -> name = r.stringOrNull()?.take(30) ?: name
                                    "avatar" -> avatar = r.stringOrNull()?.take(8) ?: avatar
                                    else -> r.skipValue()
                                }
                            }
                            r.endObject()
                        }

                        "clips" -> {
                            sawClips = true
                            r.beginArray()
                            while (r.hasNext()) readClip(r)?.let { staged += it }
                            r.endArray()
                        }

                        else -> r.skipValue()
                    }
                }
                r.endObject()
            }
        } catch (e: Exception) {
            staged.forEach { it.file.delete() }
            throw NotAPackException()
        }
        if (format != PACK_FORMAT || version != 1 || !sawClips) {
            staged.forEach { it.file.delete() }
            throw NotAPackException()
        }
        val id = voiceId?.takeIf { ID.matches(it) } ?: newId()
        val voice = (dao.voice(id) ?: VoiceEntity(id, name, avatar, System.currentTimeMillis())).copy(name = name, avatar = avatar)
        dao.upsertVoice(voice)
        staged.forEach { c -> saveClip(id, c.book, c.page, c.file, c.mime, c.duration, c.marks, c.sig) }
        ImportResult(voice.toModel(), staged.size, unplayable = staged.isNotEmpty() && !canPlay(staged.first().mime))
    }

    private class StagedClip(
        val book: String,
        val page: Int,
        val mime: String,
        val duration: Double,
        val marks: List<Double>,
        val sig: String,
        val file: File
    )

    /** One clip of a pack, its audio written to a staging file; null when it isn't a usable page. */
    private fun readClip(r: JsonReader): StagedClip? {
        var book: String? = null
        var page = 0
        var mime: String? = null
        var duration = 0.0
        var marks: List<Double>? = null
        var sig = ""
        var file: File? = null
        r.beginObject()
        while (r.hasNext()) {
            when (r.nextName()) {
                "book" -> book = r.stringOrNull()

                "page" -> page = runCatching { r.nextInt() }.getOrElse {
                    r.skipValue()
                    0
                }

                "mime" -> mime = r.stringOrNull()

                "duration" -> duration = runCatching { r.nextDouble() }.getOrElse {
                    r.skipValue()
                    0.0
                }

                "marks" -> marks = runCatching {
                    val list = mutableListOf<Double>()
                    r.beginArray()
                    while (r.hasNext()) list += r.nextDouble()
                    r.endArray()
                    list
                }.getOrNull()

                "sig" -> sig = r.stringOrNull().orEmpty()

                "audio" -> r.stringOrNull()?.let { b64 ->
                    file = File(context.cacheDir, "import/${System.nanoTime()}.bin").also {
                        it.parentFile?.mkdirs()
                        it.writeBytes(Base64.decode(b64, Base64.DEFAULT))
                    }
                }

                else -> r.skipValue()
            }
        }
        r.endObject()
        val ok = book != null && BOOK.matches(book!!) && page > 0 && mime?.startsWith("audio/") == true && marks != null && file != null
        if (!ok) {
            file?.delete()
            return null
        }
        return StagedClip(book!!, page, mime!!, duration, marks!!, sig, file!!)
    }

    private fun JsonReader.stringOrNull(): String? = if (peek() == JsonToken.STRING) {
        nextString()
    } else {
        skipValue()
        null
    }

    private val root get() = File(context.filesDir, "voices")

    private fun clipPath(voiceId: String, book: String, page: Int, mime: String) = "voices/${safe(
        voiceId
    )}/${safe(book)}/$page.${extension(mime)}"

    private fun VoiceEntity.toModel() = Voice(id, name, avatar, created)

    private fun ClipEntity.toModel() = Clip(
        voiceId = voiceId,
        book = book,
        page = page,
        file = File(context.filesDir, file),
        mime = mime,
        duration = duration,
        marks = runCatching { json.decodeFromString<List<Double>>(marksJson) }.getOrDefault(listOf(0.0)),
        sig = sig
    )

    /** A voice pack file that isn't one. */
    class NotAPackException : Exception("not a voice pack")

    companion object {
        const val PACK_FORMAT = "ertaklar-ovoz"
        private val ID = Regex("^v[a-z0-9]{6,24}$")
        private val BOOK = Regex("^[a-z0-9_]{1,40}$")

        fun newId(): String = "v" + System.currentTimeMillis().toString(36) + Random.nextLong(2_176_782_336L).toString(36)

        private fun safe(part: String) = part.replace(Regex("[^A-Za-z0-9_-]"), "_")

        private fun q(s: String) = JSONObject.quote(s)

        private fun num(d: Double) = if (d == Math.floor(d) && !d.isInfinite()) d.toLong().toString() else d.toString()

        /** File extension for a recording's type. */
        fun extension(mime: String) = when (mime.substringBefore(';').trim()) {
            "audio/mp4", "audio/m4a", "audio/x-m4a" -> "m4a"
            "audio/webm" -> "webm"
            "audio/ogg" -> "ogg"
            "audio/mpeg" -> "mp3"
            "audio/aac" -> "aac"
            "audio/wav", "audio/x-wav" -> "wav"
            else -> "bin"
        }

        /** Whether this phone can play recordings of this type. */
        fun canPlay(mime: String) = extension(mime) != "bin"
    }
}
