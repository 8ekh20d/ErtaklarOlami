package uz.haksenguz.ertaklar.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import uz.haksenguz.ertaklar.data.local.SettingsStore
import uz.haksenguz.ertaklar.data.models.Settings

@Singleton
class SettingsRepository @Inject constructor(private val store: SettingsStore) {
    val settings: Flow<Settings> = store.settings

    suspend fun setScript(script: String) = store.setScript(script)

    /** The voice that reads books aloud, kept for next time. */
    suspend fun setVoice(id: String?) = store.setVoice(id)

    /** The voice the studio records with, kept for next time. */
    suspend fun setRecVoice(id: String?) = store.setRecVoice(id)

    /** "lat", "cyr" or "ko": Uzbek in Latin or Cyrillic, or the menus in Korean (Uzbek then in Latin). */
    suspend fun setChoice(choice: String) = store.setScriptAndLang(
        script = if (choice == "cyr") "cyr" else "lat",
        lang = if (choice == "ko") "ko" else "uz"
    )
    suspend fun setLang(lang: String) = store.setLang(lang)
    suspend fun setSound(on: Boolean) = store.setSound(on)
    suspend fun setReading(on: Boolean) = store.setReading(on)
    suspend fun setBedtimePages(pages: Int) = store.setBedtimePages(pages)
}
