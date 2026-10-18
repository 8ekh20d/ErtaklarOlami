package uz.haksenguz.ertaklar.navigation

import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

/** A book; [resume] opens it straight at the child's last page. */
@Serializable
data class ReaderRoute(val key: String, val resume: Boolean = false)

/** A book's final quiz ("Bilimdon testi"). */
@Serializable
data class QuizRoute(val key: String)
