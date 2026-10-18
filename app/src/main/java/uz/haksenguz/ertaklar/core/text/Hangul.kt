package uz.haksenguz.ertaklar.core.text

/**
 * Korean-letter sound hints (가): Uzbek written in Hangul the way it sounds,
 * for children who read Korean letters first.
 *
 *   read("Assalomu alaykum") == "앗살로무 알라이쿰"
 *   read("Toshkent")         == "토시켄트"
 *
 * Uzbek spelling is regular, so a few rules do it:
 *  - each sound has its letter (q and k are ㅋ, x and h are ㅎ, v is ㅂ, o and
 *    o' are ㅗ); sh, ch and y before a vowel fold into it (sha 샤, ya 야);
 *  - between two vowels one consonant starts the next syllable, except l,
 *    which is doubled as Korean writes it (lola 롤라, alaykum 알라이쿰);
 *  - a doubled consonant closes the syllable before it (Assalomu 앗살로무);
 *  - n, m, l and ng can end a syllable; any other consonant without a vowel
 *    gets ㅡ (or ㅣ after sh, ch, j): anor 아노르, kitob 키토브, Toshkent 토시켄트.
 * Anything that isn't an Uzbek letter (spaces, punctuation, numbers) is kept.
 */
object Hangul {
    // Sounds: sh -> S, ch -> C, ng -> N, o' -> O, g' -> G; other letters as they are.

    /** Initial consonants (index into ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ); no consonant is ㅇ (11). */
    private val ONSET = mapOf(
        'g' to 0, 'G' to 0, 'n' to 2, 'd' to 3, 'l' to 5, 'r' to 5, 'm' to 6, 'b' to 7, 'v' to 7, 's' to 9, 'S' to 9,
        'j' to 12, 'z' to 12, 'C' to 14, 'k' to 15, 'q' to 15, 't' to 16, 'f' to 17, 'p' to 17, 'h' to 18, 'x' to 18
    )
    private const val NO_ONSET = 11

    /** Vowels (index into ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ); [GLIDE] after y or sh. */
    private val VOWEL = mapOf('a' to 0, 'e' to 5, 'i' to 20, 'o' to 8, 'O' to 8, 'u' to 13)
    private val GLIDE = mapOf('a' to 2, 'e' to 7, 'i' to 20, 'o' to 12, 'O' to 12, 'u' to 17)
    private const val EU = 18 // ㅡ
    private const val I = 20 // ㅣ

    /** Final consonants a syllable may end with (index into the 28 finals). */
    private val CODA = mapOf(
        'k' to 1, 'q' to 1, 'g' to 1, 'n' to 4, 'l' to 8, 'r' to 8, 'm' to 16, 'b' to 17, 'p' to 17,
        's' to 19, 't' to 19, 'd' to 19, 'z' to 19, 'N' to 21
    )

    /** May end a syllable without being doubled. */
    private val SOFT_END = mapOf('n' to 4, 'm' to 16, 'l' to 8, 'N' to 21)

    private val WORD = Regex("[A-Za-z]+(?:['ʻʼ‘’`][A-Za-z]+)*")

    private fun block(l: Int, v: Int, t: Int = 0): String = (0xAC00 + (l * 21 + v) * 28 + t).toChar().toString()

    private fun isVowel(c: Char) = c in VOWEL

    /** Each run of letters (with o', g' and the tutuq) is read as a word; everything else stays. */
    fun read(text: String): String = WORD.replace(text) { readWord(it.value) }

    /** The Uzbek letters of one word as sounds. */
    fun sounds(word: String): List<Char> {
        val w = word.lowercase().map { if (it in "ʻʼ‘’`") '\'' else it }.joinToString("")
        val out = mutableListOf<Char>()
        var i = 0
        while (i < w.length) {
            val two = w.substring(i, minOf(i + 2, w.length))
            val sound = when {
                two == "sh" -> 'S'

                two == "ch" -> 'C'

                two == "ng" && w.getOrNull(i + 2) != '\'' -> 'N'

                // ng' is n + g'
                two == "o'" -> 'O'

                two == "g'" -> 'G'

                else -> null
            }
            if (sound != null) {
                out += sound
                i += 2
            } else {
                val c = w[i]
                if (c != '\'' && (isVowel(c) || c in ONSET || c == 'y')) out += c
                i += 1
            }
        }
        return out
    }

    /** A consonant with no vowel: its own syllable with ㅡ (ㅣ after sh, ch, j; y is 이). */
    private fun alone(c: Char): String = when (c) {
        'y' -> block(NO_ONSET, I)
        'N' -> block(NO_ONSET, EU, 21)
        else -> block(ONSET.getValue(c), if (c == 'S' || c == 'C' || c == 'j') I else EU)
    }

    private class Syl(val l: Int = 0, val v: Int = 0, var t: Int = 0, val alone: String? = null)

    fun readWord(word: String): String {
        val ph = sounds(word)
        if (ph.isEmpty()) return word
        val syl = mutableListOf<Syl>()

        fun closeWith(code: Int): Boolean {
            val last = syl.lastOrNull() ?: return false
            if (last.alone != null || last.t != 0) return false
            last.t = code
            return true
        }

        // The consonants between the last vowel and the next one (atEnd: none follows).
        // Returns the next vowel's onset (null for none) and whether it glides (y, sh).
        fun flush(cluster: List<Char>, atEnd: Boolean): Pair<Char?, Boolean> {
            var onset: Char? = null
            var glide = false
            val n = cluster.size
            // the last consonant (and a y after it) start the next syllable
            var lead = n
            if (!atEnd && n > 0) {
                if (cluster[n - 1] == 'y') {
                    glide = true
                    lead = n - 1
                    if (lead > 0 && cluster[lead - 1] != 'y') {
                        onset = cluster[lead - 1]
                        lead--
                    }
                } else {
                    onset = cluster[n - 1]
                    lead = n - 1
                }
            }
            for (k in 0 until lead) {
                val c = cluster[k]
                val next: Char? = if (k + 1 < lead) cluster[k + 1] else onset
                val coda = CODA[c]
                // doubled: ss -> ㅅ + ㅅ
                if (k == 0 && syl.isNotEmpty() && next != null && c == next && coda != null && closeWith(coda)) continue
                // n before g, g', k, q is said ng
                val soft = SOFT_END[c]
                if (k == 0 && syl.isNotEmpty() && soft != null &&
                    closeWith(if (c == 'n' && next != null && next in "gGkq") 21 else soft)
                ) {
                    continue
                }
                syl += Syl(alone = alone(c))
            }
            // l between vowels is written twice; ng before a vowel is ㄴ + ㄱ
            if (!atEnd && onset == 'l' && lead == 0 && syl.isNotEmpty() && syl.last().alone == null) closeWith(8)
            if (onset == 'N') {
                if (!(syl.isNotEmpty() && syl.last().alone == null && closeWith(4))) syl += Syl(alone = block(2, EU))
                onset = 'g'
            }
            if (onset == 'S' && !glide) glide = true
            return onset to glide
        }

        val pending = mutableListOf<Char>()
        for (c in ph) {
            if (isVowel(c)) {
                val (onset, glide) = flush(pending, atEnd = false)
                pending.clear()
                val l = onset?.let { ONSET.getValue(it) } ?: NO_ONSET
                syl += Syl(l = l, v = if (glide) GLIDE.getValue(c) else VOWEL.getValue(c))
            } else {
                pending += c
            }
        }
        if (pending.isNotEmpty()) flush(pending, atEnd = true)
        return syl.joinToString("") { it.alone ?: block(it.l, it.v, it.t) }
    }
}
