package uz.haksenguz.ertaklar.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * A book's Korean helper (assets/data/korean.json): the Korean under each Uzbek
 * sentence, its glossary and its quiz. Story text itself always stays Uzbek.
 */
@Serializable
data class KoreanBook(
    val reviewed: Boolean = false,
    val title: String = "",
    val tag: String = "",
    val moral: String = "",
    /** The question, then each answer, in the Uzbek order. */
    val quiz: List<String>? = null,
    /** [term, Korean meaning, [forms]]. */
    val words: List<JsonArray> = emptyList(),
    val pages: List<KoreanPage> = emptyList()
) {
    /** (term, Korean meaning) of glossary entry [i]. */
    fun word(i: Int): Pair<String, String>? = words.getOrNull(i)?.let { it[0].jsonPrimitive.content to it[1].jsonPrimitive.content }

    /** Each entry's forms, for matching words in the text. */
    val forms: List<List<String>>
        get() = words.map { w ->
            (w.getOrNull(2) as? JsonArray)?.map { (it as JsonPrimitive).content } ?: listOf(w[0].jsonPrimitive.content)
        }
}

/** A page's Korean: [s] is the title then each sentence; [q] the question then each answer. */
@Serializable
data class KoreanPage(val s: List<String> = emptyList(), val q: List<String>? = null)

/** assets/data/i18n.json: the menus in Korean. */
@Serializable
data class I18nFile(val exact: Map<String, String> = emptyMap(), val patterns: List<I18nPattern> = emptyList())

@Serializable
data class I18nPattern(
    val re: String,
    val ko: String? = null,
    val fn: String? = null,
    val flags: String = "",
    val example: String = "",
    val exampleKo: String = ""
)
