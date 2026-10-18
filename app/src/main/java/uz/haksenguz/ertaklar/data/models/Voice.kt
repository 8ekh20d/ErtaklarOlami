package uz.haksenguz.ertaklar.data.models

import java.io.File

/** Someone who reads books aloud in the app (buvi, dada...). */
data class Voice(val id: String, val name: String, val avatar: String, val created: Long) {
    companion object {
        val AVATARS = listOf("👵", "👴", "👩", "👨", "🧕", "👧", "👦", "🎙️")
    }
}

/**
 * One story page read aloud. [marks] holds the second where each piece (the
 * title, then each sentence) starts; [sig] fingerprints the text it was read from.
 */
data class Clip(
    val voiceId: String,
    val book: String,
    val page: Int,
    val file: File,
    val mime: String,
    val duration: Double,
    val marks: List<Double>,
    val sig: String
)

/** A voice pack that was added: who, how many pages, and whether this phone may not play them. */
data class ImportResult(val voice: Voice, val count: Int, val unplayable: Boolean)
