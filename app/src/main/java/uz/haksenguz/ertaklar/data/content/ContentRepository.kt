package uz.haksenguz.ertaklar.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import uz.haksenguz.ertaklar.core.i18n.KoreanMenus
import uz.haksenguz.ertaklar.core.passport.Passport
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.BooksFile
import uz.haksenguz.ertaklar.data.models.HolidaysFile
import uz.haksenguz.ertaklar.data.models.I18nFile
import uz.haksenguz.ertaklar.data.models.KoreanBook
import uz.haksenguz.ertaklar.data.models.MapFile
import uz.haksenguz.ertaklar.data.models.PassportFile

/** The books in assets/data, in library order, with their Korean helpers. */
class Library(val books: List<Book>, val korean: Map<String, KoreanBook>) {
    private val byKey = books.associateBy { it.key }
    operator fun get(key: String): Book? = byKey[key]
}

@Singleton
class ContentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val mutex = Mutex()
    private var library: Library? = null
    private var menus: KoreanMenus? = null
    private var passport: PassportFile? = null
    private var map: MapFile? = null
    private var holidays: HolidaysFile? = null

    /** Read once, off the main thread. */
    suspend fun library(): Library = mutex.withLock {
        library ?: withContext(Dispatchers.IO) {
            Library(
                books = json.decodeFromString<BooksFile>(read("data/books.json")).books,
                korean = json.decodeFromString<Map<String, KoreanBook>>(read("data/korean.json"))
            )
        }.also { library = it }
    }

    /** The menus in Korean. */
    suspend fun koreanMenus(): KoreanMenus {
        val places = passport()
        return mutex.withLock {
            menus ?: withContext(Dispatchers.IO) {
                koreanMenus(json.decodeFromString<I18nFile>(read("data/i18n.json")), places)
            }.also { menus = it }
        }
    }

    /** The culture passport's places and stickers. */
    suspend fun passport(): PassportFile = mutex.withLock {
        passport ?: withContext(Dispatchers.IO) { json.decodeFromString<PassportFile>(read("data/passport.json")) }.also { passport = it }
    }

    /** The map of Uzbekistan. */
    suspend fun map(): MapFile = mutex.withLock {
        map ?: withContext(Dispatchers.IO) { json.decodeFromString<MapFile>(read("data/map.json")) }.also { map = it }
    }

    /** The holidays and their dates. */
    suspend fun holidays(): HolidaysFile = mutex.withLock {
        holidays ?: withContext(Dispatchers.IO) { json.decodeFromString<HolidaysFile>(read("data/holidays.json")) }.also { holidays = it }
    }

    private fun read(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }

    companion object {
        fun koreanMenus(file: I18nFile, passport: PassportFile? = null) = KoreanMenus(
            exact = KoreanMenus.EXTRA + file.exact,
            patterns = file.patterns.map {
                val options = if ('i' in it.flags) setOf(RegexOption.IGNORE_CASE) else emptySet()
                KoreanMenus.Pattern(Regex(it.re, options), it.ko, it.fn)
            },
            places = KoreanMenus.Places(
                byName = passport?.regions?.associate { it.name to it.ko }.orEmpty(),
                byDative = passport?.regions?.associate { Passport.dative(it.name) to it.ko }.orEmpty(),
                stickers = passport?.regions?.flatMap { it.stickers }?.associate { it.name to it.ko }.orEmpty()
            )
        )
    }
}

/** Coil model for a file under assets/, e.g. "img/covers/zumrad.webp". */
fun assetUri(path: String) = "file:///android_asset/$path"
