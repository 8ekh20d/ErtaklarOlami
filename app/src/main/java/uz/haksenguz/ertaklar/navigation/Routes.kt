package uz.haksenguz.ertaklar.navigation

import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

/**
 * A book; [resume] opens it straight at the child's last page; [bedtime] reads
 * tonight's pages calmly; [page] opens it at that story page (a dictionary word's).
 */
@Serializable
data class ReaderRoute(val key: String, val resume: Boolean = false, val bedtime: Boolean = false, val page: Int = 0)

/** A book's final quiz ("Bilimdon testi"). */
@Serializable
data class QuizRoute(val key: String)

/** The recording studio for a book, opened at [page]. */
@Serializable
data class StudioRoute(val key: String, val page: Int = 1)

/** "🎨 Bo'yash": the picture of story page [view] (or of the ending) as a colouring page; [reveal] for a guessed riddle's picture. */
@Serializable
data class ColoringRoute(val key: String, val view: Int, val reveal: Boolean = false)

/** "🧩 Voqealar tartibi": four pictures of the book to put in order. */
@Serializable
data class OrderRoute(val key: String)

/** "🔍 Farqlarni toping": a twin tale against the Uzbek tale it pairs with. */
@Serializable
data class CompareRoute(val key: String)

/** "✍️ Harfni yozing": tracing the letter of Alifbo page [view]. */
@Serializable
data class TraceRoute(val key: String, val view: Int)

/** "📖 Mening lug'atim": the words the child has collected. */
@Serializable
data object DictionaryRoute

/** A game with the child's words: "listen", "match" or "spell". */
@Serializable
data class DictGameRoute(val game: String)

/** "📜 Litsenziyalar": the fonts', emoji's, icon's and map's licences. */
@Serializable
data object LicencesRoute

/** "🗺️ Mening pasportim"; [region] opens that place's card too (from the end of a book). */
@Serializable
data class PassportRoute(val region: String? = null)
