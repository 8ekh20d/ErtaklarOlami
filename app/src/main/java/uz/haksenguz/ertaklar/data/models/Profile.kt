package uz.haksenguz.ertaklar.data.models

import kotlinx.serialization.Serializable

/** A child reading on this device. */
data class Profile(
    val id: String,
    val name: String,
    val avatar: String,
    /** Years, or null when not given; picks the child's shelf. */
    val age: Int?,
    val points: Int,
    /** The book read last, offered by "Davom ettirish". */
    val lastBook: String?,
    val created: Long
) {
    companion object {
        val AVATARS = listOf("🦊", "🐰", "🐻", "🦉", "🐱", "🐶", "🐴", "🐝")
        const val DEFAULT_NAME = "Bolajon"

        /** A believable age, or null. */
        fun cleanAge(age: Int?): Int? = age?.takeIf { it in 2..18 }
    }
}

/** One child's progress in one book. */
data class BookRecord(
    val book: String,
    /** Last story page they were on; 0 = nothing to continue. */
    val page: Int = 0,
    val finished: Boolean = false,
    val finishedAt: Long? = null,
    val quiz: Boolean = false,
    val order: Boolean = false,
    val compare: Boolean = false,
    /** Page number -> state of that page's question. */
    val answers: Map<String, AnswerState> = emptyMap(),
    /** Alifbo letters traced. */
    val traced: Set<String> = emptySet()
)

/** A page question's state. */
@Serializable
data class AnswerState(
    val wrong: List<Int> = emptyList(),
    val done: Boolean = false,
    val pick: Int? = null,
    val praise: String? = null
)
