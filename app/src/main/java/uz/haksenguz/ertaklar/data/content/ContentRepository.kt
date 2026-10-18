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
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.BooksFile

/** The books in assets/data, in library order. */
class Library(val books: List<Book>) {
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

    /** Read once, off the main thread. */
    suspend fun library(): Library = mutex.withLock {
        library ?: withContext(Dispatchers.IO) {
            val text = context.assets.open("data/books.json").bufferedReader().use { it.readText() }
            Library(json.decodeFromString<BooksFile>(text).books)
        }.also { library = it }
    }
}

/** Coil model for a file under assets/, e.g. "img/covers/zumrad.webp". */
fun assetUri(path: String) = "file:///android_asset/$path"
