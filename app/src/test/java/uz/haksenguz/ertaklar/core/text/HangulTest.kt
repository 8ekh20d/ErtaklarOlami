package uz.haksenguz.ertaklar.core.text

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import uz.haksenguz.ertaklar.data.models.BooksFile

class HangulTest {

    private val cases = listOf(
        // greetings and place names
        "Assalomu alaykum" to "앗살로무 알라이쿰",
        "Salom!" to "살롬!",
        "Rahmat" to "라흐마트",
        "Toshkent" to "토시켄트",
        "O'zbekiston" to "오즈베키스톤",
        "Samarqand" to "사마르칸드",
        "Buxoro" to "부호로",
        // one consonant between vowels starts the next syllable; l is written twice
        "anor" to "아노르",
        "lola" to "롤라",
        "oila" to "오일라",
        "Alla" to "알라",
        // sh, ch, y and the letters with an apostrophe
        "shar" to "샤르",
        "she'r" to "셰르",
        "choynak" to "초이나크",
        "yulduz" to "율두즈",
        "o'rdak" to "오르다크",
        "G'oz" to "고즈",
        "xo'roz" to "호로즈",
        "ma'no" to "마노",
        // ng: ㅇ at the end, ㄴ + ㄱ before a vowel; n before g' is said ng too
        "ming" to "밍",
        "dengiz" to "덴기즈",
        "qo'ng'iroq" to "콩기로크",
        // consonants with no vowel get ㅡ (ㅣ after sh, ch, j)
        "kitob" to "키토브",
        "baliq" to "발리크",
        "maktab" to "마크타브",
        // letters on their own, as on the Alifbo pages
        "Ng" to "응",
        "Sh sh" to "시 시",
        "Ch" to "치",
        // everything that isn't a letter stays
        "21-oktabr" to "21-오크타브르",
        "qip-qizil" to "키프-키질",
        "Mening ismim — Asal" to "메닝 이스밈 — 아살",
        "«Kkul» — asal" to "«크쿨» — 아살"
    )

    @Test
    fun readings() {
        val wrong = cases.mapNotNull { (uz, ko) -> Hangul.read(uz).takeIf { it != ko }?.let { "$uz -> $it (want $ko)" } }
        assertEquals(emptyList<String>(), wrong)
    }

    /** Every sentence of every book reads out in Hangul, with no Latin letter left over. */
    @Test
    fun everySentenceOfEveryBook() {
        val books = Json { ignoreUnknownKeys = true }
            .decodeFromString<BooksFile>(File("src/main/assets/data/books.json").readText()).books
        val latin = Regex("[A-Za-z]")
        val hangul = Regex("[가-힣]")
        val bad = books.flatMap { b ->
            b.pages.flatMap { p ->
                p.segments.mapNotNull { t ->
                    val k = Hangul.read(t)
                    if (latin.containsMatchIn(k) || !hangul.containsMatchIn(k)) "${b.key} p${p.n}: \"$t\" -> \"$k\"" else null
                }
            }
        }
        assertEquals(emptyList<String>(), bad.take(10))
    }
}
