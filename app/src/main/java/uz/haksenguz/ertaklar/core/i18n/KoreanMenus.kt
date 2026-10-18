package uz.haksenguz.ertaklar.core.i18n

/**
 * The app's menus, buttons and messages in Korean, for Korean-speaking parents
 * and teachers. Stories stay Uzbek: only UI text goes through here.
 *
 * [exact] maps a whole Uzbek text to Korean; [patterns] handle texts with
 * numbers or names ("3 / 12 sahifa" -> "3 / 12쪽"). Unknown text gives null.
 */
class KoreanMenus(
    private val exact: Map<String, String>,
    private val patterns: List<Pattern>
) {
    /** One pattern: a regex over the Uzbek text, and either a Korean template ($1, $2…) or a named function. */
    class Pattern(val re: Regex, val ko: String?, val fn: String?)

    fun translate(text: String): String? {
        exact[text]?.let { return it }
        val trimmed = text.trim()
        if (trimmed != text) exact[trimmed]?.let { return text.replace(trimmed, it) }
        for (p in patterns) {
            val m = p.re.matchEntire(text) ?: continue
            return when {
                p.ko != null -> p.re.replace(text, p.ko)
                p.fn != null -> function(p.fn, m.groupValues) ?: continue
                else -> continue
            }
        }
        return null
    }

    /** Patterns whose Korean needs more than a template. */
    private fun function(name: String, g: List<String>): String? = when (name) {
        // "🎉 9-oktabr" -> "🎉 10월 9일"
        "monthDay" -> MONTHS.indexOf(g[2]).takeIf { it >= 0 }?.let { "🎉 ${it + 1}월 ${g[1]}일" }

        else -> null
    }

    companion object {
        val MONTHS = listOf("yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr")

        /** Android-only texts, not in the bundled table. */
        val EXTRA = mapOf(
            "Bu bo'lim tez orada qo'shiladi" to "곧 추가될 기능이에요",
            "Kitobni oching, sahifani surib varaqlang, rasmlarga bosib qahramonlarni jonlantiring va savollarga javob bering!" to
                "책을 펼치고, 페이지를 밀어서 넘겨 보세요. 그림을 누르면 주인공들이 움직이고, 질문에 답할 수도 있어요!",
            "Sahifani suring yoki ➜ tugmasini bosing" to "페이지를 밀거나 ➜ 버튼을 누르세요",
            "Tushunarli" to "알겠어요",
            "Ertaklar Olami — o'zbek ertaklari, rasmli va o'qib beriladigan kitoblar." to "Ertaklar Olami — 그림과 함께 읽어 주는 우즈베크 동화책",
            "Ha, o'chirish" to "네, 삭제할게요",
            "Bilimdon testi" to "퀴즈"
        )
    }
}
