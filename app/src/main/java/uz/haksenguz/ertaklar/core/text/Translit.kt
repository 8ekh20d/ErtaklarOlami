package uz.haksenguz.ertaklar.core.text

/**
 * Uzbek Latin -> Cyrillic.
 *
 * Content is written once, in Latin, and shown in Cyrillic on request:
 *   toCyrillic("Bir bor ekan, bir yo'q ekan") == "Бир бор экан, бир йўқ экан"
 * Words containing a digit ("3D") and anything that is not a Latin letter are left alone.
 */
object Translit {
    /** O'/g' letters and the tutuq belgisi are typed with any of these. */
    private const val APOS = "'ʻʼ‘’`"

    private val LETTERS = mapOf(
        'a' to "а", 'b' to "б", 'c' to "с", 'd' to "д", 'e' to "е", 'f' to "ф", 'g' to "г", 'h' to "ҳ", 'i' to "и",
        'j' to "ж", 'k' to "к", 'l' to "л", 'm' to "м", 'n' to "н", 'o' to "о", 'p' to "п", 'q' to "қ", 'r' to "р",
        's' to "с", 't' to "т", 'u' to "у", 'v' to "в", 'w' to "в", 'x' to "х", 'y' to "й", 'z' to "з"
    )

    /** Loanwords whose Cyrillic spelling the rules can't guess (whole words only). */
    private val EXCEPTIONS = mapOf(
        "kompyuter" to "компьютер", "sirk" to "цирк", "konsert" to "концерт",
        "yanvar" to "январь", "fevral" to "февраль", "aprel" to "апрель", "iyun" to "июнь", "iyul" to "июль",
        "sentabr" to "сентябрь", "oktabr" to "октябрь", "noyabr" to "ноябрь", "dekabr" to "декабрь"
    )

    private val WORD = Regex("[A-Za-z0-9'ʻʼ‘’`]+")
    private val ALL_CAPS = Regex("[A-Z].*[A-Z]")

    private fun isApos(ch: Char?) = ch != null && ch in APOS
    private fun isLetter(ch: Char?) = ch != null && (ch in 'a'..'z' || ch in 'A'..'Z')
    private fun isVowel(ch: Char) = ch in "aeiou"

    fun toCyrillic(text: String): String = WORD.replace(text) { convertWord(it.value) }

    fun convertWord(w: String): String {
        if (w.any { it.isDigit() }) return w
        EXCEPTIONS[w.lowercase()]?.let { exc ->
            return if (w[0].isUpperCase()) exc.replaceFirstChar { it.uppercaseChar() } else exc
        }
        val allCaps = ALL_CAPS.containsMatchIn(w) && w == w.uppercase()
        val out = StringBuilder()
        var prevLetter = false
        var prevVowel = false

        // Case follows the first Latin letter of the unit: "Sh" -> "Ш", "SHOH" -> "ШОҲ".
        fun emit(cyr: String, src: Char, vowel: Boolean) {
            val upper = src.isUpperCase()
            out.append(
                when {
                    !upper -> cyr
                    allCaps -> cyr.uppercase()
                    else -> cyr.replaceFirstChar { it.uppercaseChar() }
                }
            )
            prevLetter = true
            prevVowel = vowel
        }

        var i = 0
        while (i < w.length) {
            val ch = w[i]
            val lo = ch.lowercaseChar()
            val next = w.getOrNull(i + 1)?.lowercaseChar()
            val start = !prevLetter

            if (isApos(ch)) {
                // tutuq belgisi between letters: ma'no -> маъно, mo''jiza -> мўъжиза
                if (prevLetter && isLetter(w.getOrNull(i + 1))) out.append(if (allCaps) "Ъ" else "ъ") else out.append(ch)
                i += 1
                continue
            }
            if (!isLetter(ch)) {
                out.append(ch)
                prevLetter = false
                prevVowel = false
                i += 1
                continue
            }
            when {
                lo == 's' && isApos(w.getOrNull(i + 1)) && w.getOrNull(i + 2)?.lowercaseChar() == 'h' -> {
                    emit("сҳ", ch, false) // Is'hoq -> Исҳоқ
                    i += 3
                }

                lo == 'o' && isApos(w.getOrNull(i + 1)) -> {
                    emit("ў", ch, true)
                    i += 2
                }

                lo == 'g' && isApos(w.getOrNull(i + 1)) -> {
                    emit("ғ", ch, false)
                    i += 2
                }

                lo == 's' && next == 'h' -> {
                    emit("ш", ch, false)
                    i += 2
                }

                lo == 'c' && next == 'h' -> {
                    emit("ч", ch, false)
                    i += 2
                }

                lo == 'y' && next == 'o' && isApos(w.getOrNull(i + 2)) -> {
                    emit("й", ch, false) // yo'l -> йўл: the oʻ is its own letter
                    i += 1
                }

                lo == 'y' && (next == 'o' || next == 'u' || next == 'a') -> {
                    emit(
                        when (next) {
                            'o' -> "ё"
                            'u' -> "ю"
                            else -> "я"
                        },
                        ch,
                        true
                    )
                    i += 2
                }

                lo == 'y' && next == 'e' -> {
                    // yer -> ер, poyezd -> поезд; after a consonant only in loanwords: obyekt -> объект
                    emit(if (start || prevVowel) "е" else "ъе", ch, true)
                    i += 2
                }

                lo == 'e' -> {
                    emit(if (start || prevVowel) "э" else "е", ch, true) // eshik -> эшик, poeziya -> поэзия
                    i += 1
                }

                else -> {
                    emit(LETTERS.getValue(lo), ch, isVowel(lo))
                    i += 1
                }
            }
        }
        return out.toString()
    }
}
