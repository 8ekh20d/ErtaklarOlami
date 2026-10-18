package uz.haksenguz.ertaklar.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslitTest {

    private val cases = listOf(
        // letters and digraphs
        "O'zbek" to "Ўзбек",
        "g'alaba" to "ғалаба",
        "Shirin" to "Ширин",
        "choy" to "чой",
        "Hoshimjon" to "Ҳошимжон",
        "Buxoro" to "Бухоро",
        "Samarqand" to "Самарқанд",
        "yangi tong" to "янги тонг",
        // e / э
        "ertak" to "эртак",
        "Bir bor ekan, bir yo'q ekan" to "Бир бор экан, бир йўқ экан",
        "keldi" to "келди",
        "poeziya" to "поэзия",
        "bahor elchisi" to "баҳор элчиси",
        // y + vowel
        "yo'l" to "йўл",
        "Yo'q" to "Йўқ",
        "yoz" to "ёз",
        "ayol" to "аёл",
        "Sayohati" to "Саёҳати",
        "daryo" to "дарё",
        "ayyor" to "айёр",
        "yulduz" to "юлдуз",
        "kelyapti" to "келяпти",
        "yer" to "ер",
        "Yetti" to "Етти",
        "poyezd" to "поезд",
        "qayerga" to "қаерга",
        "To'xtaboyev" to "Тўхтабоев",
        "obyekt" to "объект",
        "Keyingi" to "Кейинги",
        "yigit" to "йигит",
        // tutuq belgisi
        "ma'no" to "маъно",
        "San'at" to "Санъат",
        "E'tibor" to "Эътибор",
        "qat'iy" to "қатъий",
        "mo''jiza" to "мўъжиза",
        "Is'hoq" to "Исҳоқ",
        // words ending in g'
        "tog' ortida" to "тоғ ортида",
        "urug'" to "уруғ",
        // t + s stays two letters
        "ketsa" to "кетса",
        "Mehnatsevarlik" to "Меҳнатсеварлик",
        // case
        "SHOH" to "ШОҲ",
        "O'ZBEK" to "ЎЗБЕК",
        "Zo'r!" to "Зўр!",
        // loanword exceptions
        "kompyuter" to "компьютер",
        "Oktabr" to "Октябрь",
        // left alone
        "3D" to "3D",
        "12-sahifa" to "12-саҳифа",
        "«Assalomu alaykum!»" to "«Ассалому алайкум!»",
        "Кирилл 한국어 ⭐" to "Кирилл 한국어 ⭐"
    )

    @Test
    fun spelling() {
        val wrong = cases.mapNotNull { (lat, want) ->
            Translit.toCyrillic(lat).takeIf { it != want }?.let { "$lat -> $it (want $want)" }
        }
        assertEquals(emptyList<String>(), wrong)
    }
}
