package uz.haksenguz.ertaklar.data.models

/** Device settings, kept in DataStore. */
data class Settings(
    /** "lat" or "cyr": how Uzbek is written. */
    val script: String = "lat",
    /** "uz" or "ko": the language of the menus. */
    val lang: String = "uz",
    val sound: Boolean = true,
    /** 가: Hangul sound hints under the text. */
    val reading: Boolean = false,
    /** Pages read at bedtime: 3, 5, or 0 for the whole book. */
    val bedtimePages: Int = 5,
    val voice: String? = null,
    val recVoice: String? = null,
    val activeProfile: String? = null
)
