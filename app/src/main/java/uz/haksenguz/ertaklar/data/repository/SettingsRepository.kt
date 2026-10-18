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
    suspend fun setLang(lang: String) = store.setLang(lang)
    suspend fun setSound(on: Boolean) = store.setSound(on)
    suspend fun setReading(on: Boolean) = store.setReading(on)
    suspend fun setBedtimePages(pages: Int) = store.setBedtimePages(pages)
}
