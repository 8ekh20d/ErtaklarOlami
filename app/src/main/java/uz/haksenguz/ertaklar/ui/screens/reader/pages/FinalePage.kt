package uz.haksenguz.ertaklar.ui.screens.reader.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.Amber500
import uz.haksenguz.ertaklar.ui.theme.ArtHint
import uz.haksenguz.ertaklar.ui.theme.Brand700
import uz.haksenguz.ertaklar.ui.theme.ChapterChipBg
import uz.haksenguz.ertaklar.ui.theme.ChoiceText
import uz.haksenguz.ertaklar.ui.theme.FairyPurple
import uz.haksenguz.ertaklar.ui.theme.FairySky
import uz.haksenguz.ertaklar.ui.theme.Gold
import uz.haksenguz.ertaklar.ui.theme.GoldSoft
import uz.haksenguz.ertaklar.ui.theme.Gray300
import uz.haksenguz.ertaklar.ui.theme.Ink
import uz.haksenguz.ertaklar.ui.theme.InkSoft
import uz.haksenguz.ertaklar.ui.theme.Rust
import uz.haksenguz.ertaklar.ui.theme.TamomFrom
import uz.haksenguz.ertaklar.ui.theme.TamomTo
import uz.haksenguz.ertaklar.ui.theme.WordCardTo

/** What the end page can do. */
data class FinaleActions(
    val onTwin: () -> Unit,
    val onCompare: () -> Unit,
    val onQuiz: () -> Unit,
    val onOrder: () -> Unit,
    val onRestart: () -> Unit,
    val onShare: () -> Unit,
    /** 🎨: the last picture as a colouring page. */
    val onColor: (() -> Unit)? = null
)

/** "Tamom!": the last picture with confetti, the stars, the moral and what to do next. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinalePage(
    book: Book,
    score: Scoring.Score,
    twinTitle: String?,
    actions: FinaleActions,
    modifier: Modifier = Modifier,
    /** The moral in Korean, when the book has it; [koShown] while 🇰🇷 is on. */
    koMoral: String? = null,
    koShown: Boolean = false,
    onToggleKorean: () -> Unit = {}
) {
    BookSheet(modifier) {
        PageHead(chip = null, running = storyText(book.title), chipFirst = false)
        PageArt(book.endImage, contentDescription = uiText("Tamom!"), onColor = actions.onColor, colorAt = Alignment.BottomEnd) {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.TopCenter) {
                Text(
                    uiText("Tamom!"),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    color = Color.White,
                    modifier = Modifier
                        .rotate(-3f)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(TamomFrom, TamomTo)))
                        .padding(horizontal = 22.dp, vertical = 4.dp)
                )
            }
        }
        Text(
            uiText("✨ Ertak shu yerda tugadi ✨"),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ArtHint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(FLEURON, color = Gold, fontSize = 26.sp)
            Text(uiText("Ertak tugadi!"), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp), color = Rust)
            Text(
                buildAnnotatedString {
                    withStyle(TextStyle(color = Amber500).toSpanStyle()) { append("★".repeat(score.stars)) }
                    withStyle(TextStyle(color = Gray300).toSpanStyle()) { append("★".repeat(3 - score.stars)) }
                },
                fontSize = 40.sp,
                letterSpacing = 6.sp,
                style = TextStyle(shadow = Shadow(Brand700, androidx.compose.ui.geometry.Offset(0f, 6f), 0f))
            )
            Text(
                uiText(if (score.asked > 0) "Savollarga javoblar: ${score.right} / ${score.asked}" else "Ajoyib o'qidingiz!"),
                style = MaterialTheme.typography.labelLarge,
                color = InkSoft
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WordCardTo)
                    .border(2.dp, GoldSoft, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        uiText("Ertakdan saboq:"),
                        fontWeight = FontWeight.ExtraBold,
                        color = Rust,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (koMoral != null) KoreanToggle(koShown, onToggleKorean)
                }
                Text(
                    storyText(book.moral.ifEmpty { "Yaxshilik va ezgulik har doim g'alaba qiladi." }),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink
                )
                if (koShown && koMoral != null) KoLine(koMoral)
            }
            when {
                twinTitle != null -> WideButton(
                    "${uiText("🇰🇷 Egizak ertak ochildi!")} «${storyText(twinTitle)}»",
                    FairySky,
                    onClick = actions.onTwin
                )

                book.compare != null -> WideButton(uiText("🔍 Farqlarni toping"), FairySky, onClick = actions.onCompare)
            }
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(uiText("🏆 Bilimdon testi"), Brush.linearGradient(listOf(Amber500, TamomTo)), Color.White, actions.onQuiz)
                if (book.order) {
                    ActionButton(
                        uiText("🧩 Voqealar tartibi"),
                        Brush.linearGradient(listOf(FairyPurple, FairyPurple)),
                        Color.White,
                        actions.onOrder
                    )
                }
                ActionButton(
                    uiText("↺ Boshidan o'qish"),
                    Brush.linearGradient(listOf(ChapterChipBg, ChapterChipBg)),
                    ChoiceText,
                    actions.onRestart
                )
                ActionButton(uiText("📤 Ulashish"), Brush.linearGradient(listOf(ChapterChipBg, ChapterChipBg)), ChoiceText, actions.onShare)
            }
        }
    }
}

@Composable
private fun WideButton(text: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ActionButton(text: String, background: Brush, color: Color, onClick: () -> Unit) {
    Text(
        text,
        color = color,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp)
    )
}
