package uz.haksenguz.ertaklar.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.haksenguz.ertaklar.data.models.Settings

/** Device settings in DataStore. */
@Singleton
class SettingsStore @Inject constructor(private val store: DataStore<Preferences>) {

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            script = p[SCRIPT] ?: "lat",
            lang = p[LANG] ?: "uz",
            sound = p[SOUND] ?: true,
            reading = p[READING] ?: false,
            bedtimePages = p[BEDTIME_PAGES] ?: 5,
            voice = p[VOICE],
            recVoice = p[REC_VOICE],
            activeProfile = p[ACTIVE_PROFILE]
        )
    }

    /** Lot / Кир / 한 at once, so no screen sees half a switch. */
    suspend fun setScriptAndLang(script: String, lang: String) {
        store.edit {
            it[SCRIPT] = script
            it[LANG] = lang
        }
    }

    suspend fun setScript(value: String) {
        store.edit { it[SCRIPT] = value }
    }
    suspend fun setLang(value: String) {
        store.edit { it[LANG] = value }
    }
    suspend fun setSound(value: Boolean) {
        store.edit { it[SOUND] = value }
    }
    suspend fun setReading(value: Boolean) {
        store.edit { it[READING] = value }
    }
    suspend fun setBedtimePages(value: Int) {
        store.edit { it[BEDTIME_PAGES] = value }
    }
    suspend fun setVoice(value: String?) {
        store.edit { it.put(VOICE, value) }
    }
    suspend fun setRecVoice(value: String?) {
        store.edit { it.put(REC_VOICE, value) }
    }
    suspend fun setActiveProfile(value: String?) {
        store.edit { it.put(ACTIVE_PROFILE, value) }
    }

    private fun MutablePreferences.put(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        val SCRIPT = stringPreferencesKey("script")
        val LANG = stringPreferencesKey("lang")
        val SOUND = booleanPreferencesKey("sound")
        val READING = booleanPreferencesKey("reading")
        val BEDTIME_PAGES = intPreferencesKey("bedtime_pages")
        val VOICE = stringPreferencesKey("voice")
        val REC_VOICE = stringPreferencesKey("rec_voice")
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
    }
}
