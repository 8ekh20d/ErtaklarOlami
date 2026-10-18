package uz.haksenguz.ertaklar.data.models

import kotlinx.serialization.Serializable

/** assets/data/books.json. */
@Serializable
data class BooksFile(val version: Int, val books: List<Book>)

@Serializable
data class Book(
    val key: String,
    val title: String,
    val category: String,
    /** [youngest, oldest]: read with a grown-up .. read alone. */
    val age: List<Int> = emptyList(),
    /** Passport place, null for the short pieces and the Alifbo. */
    val region: String? = null,
    val tag: String = "",
    /** The part of [tag] before "•", shown as the card's chip. */
    val genre: String = "",
    val hue: String = "#f59e0b",
    val moral: String = "",
    val quiz: Question? = null,
    val bedtime: Boolean = false,
    /** The Uzbek tale this Korean twin pairs with; it opens once that one is finished. */
    val twin: String? = null,
    val compare: Compare? = null,
    val holidays: List<String> = emptyList(),
    val verse: Boolean = false,
    val order: Boolean = true,
    val isNew: Boolean = false,
    val cover: String,
    val endImage: String,
    val minutes: Int = 2,
    val pages: List<Page>
)

@Serializable
data class Page(
    val n: Int,
    val title: String,
    val text: String,
    /** The page title, then each sentence: the pieces read aloud and highlighted. */
    val segments: List<String> = emptyList(),
    val sig: String = "",
    val question: Question? = null,
    /** "Yangi so'z" card: [term, meaning]. */
    val word: List<String>? = null,
    val reveal: Reveal? = null,
    /** [Korean proverb, its literal Uzbek meaning]. */
    val proverbKo: List<String>? = null,
    val sounds: List<String>? = null,
    val letter: String? = null,
    val image: String,
    val revealImage: String? = null,
    val lineArt: String? = null,
    val revealLineArt: String? = null
)

/** A question with its answers; [ok] is the right answer's index, -1 when any answer counts. */
@Serializable
data class Question(val q: String, val a: List<String>, val ok: Int)

@Serializable
data class Reveal(val answer: String)

/** "Farqlarni toping": cards are [emoji, text, "uz" | "ko" | "both"]. */
@Serializable
data class Compare(val icons: List<String> = emptyList(), val cards: List<List<String>> = emptyList())
