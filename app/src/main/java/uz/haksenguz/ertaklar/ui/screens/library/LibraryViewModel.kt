package uz.haksenguz.ertaklar.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.levels.Levels
import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.content.Library
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.BookRecord
import uz.haksenguz.ertaklar.data.models.Category
import uz.haksenguz.ertaklar.data.models.Profile
import uz.haksenguz.ertaklar.data.repository.ProfileRepository
import uz.haksenguz.ertaklar.data.repository.ProgressRepository
import uz.haksenguz.ertaklar.data.repository.SettingsRepository

data class LibraryUiState(
    val loading: Boolean = true,
    val profile: Profile? = null,
    val hero: Hero? = null,
    val tabs: List<CategoryTab> = emptyList(),
    val category: Category = Category.All,
    /** The age shelf shown: "all", "4-6", "7-8" or "9+". */
    val shelf: String = Levels.ALL,
    /** The active child's own shelf, marked with their face. */
    val myShelf: String? = null,
    val books: List<BookCard> = emptyList(),
    /** Set while the "still locked" message is open: the Uzbek tale to finish first. */
    val lockedTwinOf: String? = null,
    /** 19:00–05:00: "Uxlash vaqti" glows. */
    val evening: Boolean = false
)

/** The banner's book: the one in progress ("Davom ettirish"), else a suggestion. */
data class Hero(
    val key: String,
    val title: String,
    val cover: String,
    val continueReading: Boolean,
    /** The page reached, while a book is in progress. */
    val resumePage: Int?,
    val pages: Int
)

data class CategoryTab(val category: Category, val count: Int)

data class BookCard(
    val key: String,
    /** The Korean title, shown under the Uzbek one with the Korean menus. */
    val koTitle: String?,
    val title: String,
    val genre: String,
    val ageLabel: String,
    val isNew: Boolean,
    val cover: String,
    val hue: String,
    val pageCount: Int,
    val locked: Boolean,
    /** For a locked twin: the Uzbek tale to finish first. */
    val twinTitle: String?,
    /** Half-read: the page reached. */
    val progressPage: Int?,
    /** Finished: its stars. */
    val stars: Int?
)

/** The library: which books show, the banner's book and each card's status. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val content: ContentRepository,
    private val profiles: ProfileRepository,
    progress: ProgressRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val library = MutableStateFlow<Library?>(null)
    private val category = MutableStateFlow(Category.All)

    /** A shelf picked by hand, for the child it was picked for; otherwise the child's own shelf shows. */
    private val shelfChoice = MutableStateFlow<Pair<String, String>?>(null)
    private val lockedTwinOf = MutableStateFlow<String?>(null)

    private val profileAndRecords = profiles.active.filterNotNull().flatMapLatest { p ->
        progress.records(p.id).map { p to it }
    }

    val state: StateFlow<LibraryUiState> = combine(
        library.filterNotNull(),
        profileAndRecords,
        category,
        shelfChoice,
        lockedTwinOf
    ) { lib, (profile, records), cat, choice, locked ->
        build(lib, profile, records, cat, choice, locked)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        viewModelScope.launch {
            profiles.ensureActive()
            library.value = content.library()
        }
    }

    /** Lot / Кир / 한. */
    fun setChoice(choice: String) {
        viewModelScope.launch { settings.setChoice(choice) }
    }

    fun selectCategory(c: Category) {
        category.value = c
    }

    fun selectShelf(shelf: String) {
        val id = state.value.profile?.id ?: return
        shelfChoice.value = id to shelf
    }

    /** Tapping a locked twin explains what to read first. */
    fun showLocked(key: String) {
        lockedTwinOf.update { _ -> library.value?.get(key)?.twin?.let { library.value?.get(it)?.title } }
    }

    fun dismissLocked() {
        lockedTwinOf.value = null
    }

    private fun build(
        lib: Library,
        profile: Profile,
        records: Map<String, BookRecord>,
        cat: Category,
        choice: Pair<String, String>?,
        locked: String?
    ): LibraryUiState {
        val myShelf = Levels.shelfFor(profile.age)
        val shelf = choice?.takeIf { it.first == profile.id }?.second ?: myShelf ?: Levels.ALL

        fun isLocked(book: Book): Boolean = book.twin != null && records[book.twin]?.finished != true
        fun onShelf(book: Book) = Levels.fits(book.age, shelf)

        val shown = lib.books.filter { cat.matches(it) && onShelf(it) }
        val tabs = Category.entries.map { c -> CategoryTab(c, lib.books.count { c.matches(it) && onShelf(it) }) }

        val cards = shown.map { book ->
            val rec = records[book.key]
            val total = book.pages.size
            BookCard(
                key = book.key,
                koTitle = lib.korean[book.key]?.title,
                title = book.title,
                genre = book.genre,
                ageLabel = Levels.label(book.age),
                isNew = book.isNew,
                cover = book.cover,
                hue = book.hue,
                pageCount = total,
                locked = isLocked(book),
                twinTitle = book.twin?.let { lib[it]?.title },
                progressPage = rec?.page?.takeIf { it >= 1 },
                stars = if (rec != null && rec.page < 1 && rec.finished) Scoring.score(book, rec.answers).stars else null
            )
        }

        return LibraryUiState(
            loading = false,
            profile = profile,
            hero = hero(lib, profile, records, myShelf, ::isLocked),
            tabs = tabs,
            category = cat,
            shelf = shelf,
            myShelf = myShelf,
            books = cards,
            lockedTwinOf = locked,
            evening = LocalTime.now().hour.let { it >= 19 || it < 5 }
        )
    }

    private fun hero(
        lib: Library,
        profile: Profile,
        records: Map<String, BookRecord>,
        myShelf: String?,
        isLocked: (Book) -> Boolean
    ): Hero? {
        val lastRec = profile.lastBook?.let { records[it] }
        val inProgress = profile.lastBook?.takeIf { lib[it] != null && (lastRec?.page ?: 0) >= 1 }
        val key = inProgress ?: suggestedBook(lib, records, myShelf, isLocked) ?: return null
        val book = lib[key] ?: return null
        return Hero(
            key = key,
            title = book.title,
            cover = book.cover,
            continueReading = inProgress != null,
            resumePage = if (inProgress != null) lastRec?.page else null,
            pages = book.pages.size
        )
    }

    /** The first book on the child's shelf they haven't finished (holidays come with the passport phase). */
    private fun suggestedBook(
        lib: Library,
        records: Map<String, BookRecord>,
        shelf: String?,
        isLocked: (Book) -> Boolean
    ): String? {
        val open = lib.books.filterNot(isLocked)
        val fits = { b: Book -> shelf == null || Levels.fits(b.age, shelf) }
        val unread = { b: Book -> records[b.key]?.finished != true }
        return (open.firstOrNull { fits(it) && unread(it) } ?: open.firstOrNull(fits) ?: open.firstOrNull())?.key
    }
}
