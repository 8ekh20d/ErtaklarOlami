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
    private val patterns: List<Pattern>,
    private val places: Places = Places()
) {
    /** Passport names in Korean: places by name and by their "to" form ("Xorazmga"), and stickers. */
    class Places(
        val byName: Map<String, String> = emptyMap(),
        val byDative: Map<String, String> = emptyMap(),
        val stickers: Map<String, String> = emptyMap()
    )

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

        // "✓ 👵 Buvijon: 12 ta sahifa qo'shildi." (and a warning when the phone may not play them)
        "voicePackImported" -> "✓ ${g[1]}: ${g[2]}쪽을 추가했어요." +
            if (g.getOrElse(3) { "" }.isNotEmpty()) " 주의: 이 휴대폰에서는 이 녹음이 재생되지 않을 수도 있어요." else ""

        "placeByDative" -> "📍 이 이야기를 따라 ${places.byDative[g[1]] ?: g[1]}에 다녀왔어요!"

        "placeByName" -> "🗺️ 새 도장: ${places.byName[g[1]] ?: g[1]}!"

        "stickerByName" -> "🎁 새 스티커: ${places.stickers[g[1]] ?: g[1]}!"

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
            "💾 Rasm galereyaga saqlandi" to "💾 그림을 갤러리에 저장했어요",
            "Rasmni saqlab bo'lmadi" to "그림을 저장하지 못했어요",
            "Rang" to "색",
            "Harf yozish maydoni" to "글자 쓰는 곳",
            "Ertaklar Olami — o'zbek ertaklari, rasmli va o'qib beriladigan kitoblar." to "Ertaklar Olami — 그림과 함께 읽어 주는 우즈베크 동화책",
            "Ha, o'chirish" to "네, 삭제할게요",
            "Ha" to "네",
            "Bilimdon testi" to "퀴즈"
        )
    }
}
