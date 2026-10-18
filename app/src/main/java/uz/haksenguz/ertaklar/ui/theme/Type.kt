package uz.haksenguz.ertaklar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uz.haksenguz.ertaklar.R

/** Headings: Fredoka (Latin only; Cyrillic headings switch to Nunito with the script switch). */
val Fredoka = FontFamily(
    Font(R.font.fredoka_regular, FontWeight.Normal),
    Font(R.font.fredoka_bold, FontWeight.Bold)
)

/** Body text: Nunito, which has the Uzbek Cyrillic letters (ў қ ғ ҳ). */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold)
)

/** Korean text: Gowun Dodum, a rounded Hangul font. */
val GowunDodum = FontFamily(Font(R.font.gowun_dodum, FontWeight.Normal))

val Typography = Typography(
    headlineLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp)
)

/** Fredoka has no Cyrillic letters, so in Cyrillic the headings are set in Nunito. */
val CyrillicTypography = Typography.run {
    fun TextStyle.nunito() = copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold)
    copy(
        headlineLarge = headlineLarge.nunito(),
        headlineMedium = headlineMedium.nunito(),
        headlineSmall = headlineSmall.nunito(),
        titleLarge = titleLarge.nunito(),
        titleMedium = titleMedium.nunito()
    )
}
