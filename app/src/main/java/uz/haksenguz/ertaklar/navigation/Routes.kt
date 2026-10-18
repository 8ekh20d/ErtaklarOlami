package uz.haksenguz.ertaklar.navigation

import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

/** A book; [resume] opens it straight at the child's last page; [bedtime] reads tonight's pages calmly. */
@Serializable
data class ReaderRoute(val key: String, val resume: Boolean = false, val bedtime: Boolean = false)

/** A book's final quiz ("Bilimdon testi"). */
@Serializable
data class QuizRoute(val key: String)

/** The recording studio for a book, opened at [page]. */
@Serializable
data class StudioRoute(val key: String, val page: Int = 1)
