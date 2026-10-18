package uz.haksenguz.ertaklar.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import uz.haksenguz.ertaklar.core.i18n.KoreanMenus
import uz.haksenguz.ertaklar.core.text.Translit

/**
 * How text is shown: Uzbek in Latin or Cyrillic, and the menus in Uzbek or Korean.
 *
 * Everything is written in Latin Uzbek. [ui] is for menus, buttons and messages:
 * Korean when the menus are Korean, else Uzbek in the chosen script. [content] is
 * for story text (titles, sentences, questions, names): never translated, only
 * shown in the chosen script. With Korean menus, Uzbek stays in Latin.
 */
@Immutable
class Display(
    val script: String = "lat",
    val lang: String = "uz",
    private val menus: KoreanMenus? = null
) {
    val korean get() = lang == "ko"
    val cyrillic get() = script == "cyr" && !korean

    /** Which of Lot / Кир / 한 is on. */
    val choice get() = if (korean) "ko" else script

    fun ui(text: String): String = when {
        korean -> menus?.translate(text) ?: text
        cyrillic -> Translit.toCyrillic(text)
        else -> text
    }

    fun content(text: String): String = if (cyrillic) Translit.toCyrillic(text) else text
}

val LocalDisplay = staticCompositionLocalOf { Display() }

/** A menu, button or message text, as the reader wants it shown. */
@Composable
@ReadOnlyComposable
fun uiText(text: String): String = LocalDisplay.current.ui(text)

/** Story text in the chosen script. */
@Composable
@ReadOnlyComposable
fun storyText(text: String): String = LocalDisplay.current.content(text)
