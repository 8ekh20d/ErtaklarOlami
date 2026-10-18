package uz.haksenguz.ertaklar.ui.screens.reader.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import uz.haksenguz.ertaklar.data.models.AnswerState
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.Page
import uz.haksenguz.ertaklar.ui.components.dashedBorder
import uz.haksenguz.ertaklar.ui.theme.ArtHint
import uz.haksenguz.ertaklar.ui.theme.ArtRing
import uz.haksenguz.ertaklar.ui.theme.DropCap
import uz.haksenguz.ertaklar.ui.theme.Fredoka
import uz.haksenguz.ertaklar.ui.theme.GowunDodum
import uz.haksenguz.ertaklar.ui.theme.Ink
import uz.haksenguz.ertaklar.ui.theme.InkSoft
import uz.haksenguz.ertaklar.ui.theme.MedallionRing
import uz.haksenguz.ertaklar.ui.theme.PageTitle
import uz.haksenguz.ertaklar.ui.theme.ProverbKo
import uz.haksenguz.ertaklar.ui.theme.TwisterSounds
import uz.haksenguz.ertaklar.ui.theme.WordCardBorder
import uz.haksenguz.ertaklar.ui.theme.WordCardFrom
import uz.haksenguz.ertaklar.ui.theme.WordCardTo
import uz.haksenguz.ertaklar.ui.theme.WordLabel
import uz.haksenguz.ertaklar.ui.theme.WordTerm

/**
 * Story page [view]: the picture and its note, then the title, the text and the
 * page's question. On a phone the two halves of the open book stack.
 */
@Composable
fun StoryPage(
    book: Book,
    view: Int,
    answer: AnswerState?,
    onAnswer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val page = book.pages[view - 1]
    val solved = answer?.done == true
    BookSheet(modifier) {
        PageHead(chip = "$view-sahifa", running = book.title)
        PageArt(if (solved && page.revealImage != null) page.revealImage else page.image, contentDescription = page.title)
        PageNote(page, solved)
        HorizontalDivider(color = ArtRing.copy(alpha = 0.6f))
        PageHead(chip = "$view / ${book.pages.size}", running = book.genre, chipFirst = false)
        Text(page.title, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp, lineHeight = 30.sp), color = PageTitle)
        page.letter?.let { AlifboLetter(it) }
        val sentences = page.segments.drop(1).ifEmpty { listOf(page.text) }
        Text(
            pageText(sentences, verse = book.verse),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 31.sp),
            color = Ink,
            textAlign = if (book.verse) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "$FLEURON $FLEURON $FLEURON",
            color = MedallionRing,
            fontSize = 20.sp,
            letterSpacing = 8.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        page.question?.let { QuestionBlock(page, it, answer, onAnswer) }
    }
}

/** The text with a big first letter; verse books set each line on its own. */
private fun pageText(sentences: List<String>, verse: Boolean): AnnotatedString = buildAnnotatedString {
    val text = sentences.joinToString(if (verse) "\n" else " ")
    // A page that opens with a number keeps it with its word, and verse has no big letter.
    if (verse || text.isEmpty() || text.first().isDigit()) {
        append(text)
        return@buildAnnotatedString
    }
    withStyle(SpanStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 2.1.em, color = DropCap)) {
        append(text.first())
    }
    append(text.substring(1))
}

/** Under the picture: the new word, a proverb's Korean twin, a tongue twister's sounds, or a hint. */
@Composable
private fun PageNote(page: Page, solved: Boolean) {
    when {
        page.proverbKo != null -> NoteCard("🇰🇷 Koreyada ham shunday deyishadi:") {
            Text(
                page.proverbKo[0],
                fontFamily = GowunDodum,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = ProverbKo,
                lineHeight = 25.sp
            )
            Text("«${page.proverbKo.getOrElse(1) { "" }}»", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        }

        page.sounds != null -> NoteCard("🔁 Uch marta, tez-tez ayting!") {
            Text(
                page.sounds.joinToString(" · "),
                fontFamily = Fredoka,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                letterSpacing = 2.sp,
                color = TwisterSounds
            )
        }

        page.reveal != null && !solved -> Hint("🤔 Javobini toping — rasm ochiladi!")

        page.word != null -> NoteCard("📖 Yangi so'z") {
            Text(page.word[0], fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = WordTerm)
            Text(page.word.getOrElse(1) { "" }, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        }

        else -> Hint("👆 Rasmga bosing — qahramonlar jonlanadi!")
    }
}

@Composable
private fun NoteCard(label: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(WordCardFrom, WordCardTo)))
            .dashedBorder(2.dp, WordCardBorder, 14.dp, 6.dp, 4.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, color = WordLabel)
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = ArtHint,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

/** An Alifbo page's letter, capital and small, written big. */
@Composable
private fun AlifboLetter(letter: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(
            letter,
            fontFamily = Fredoka,
            fontWeight = FontWeight.Bold,
            fontSize = 72.sp,
            color = DropCap,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(WordCardTo)
                .border(3.dp, MedallionRing, RoundedCornerShape(24.dp))
                .padding(horizontal = 28.dp, vertical = 4.dp)
        )
    }
}
