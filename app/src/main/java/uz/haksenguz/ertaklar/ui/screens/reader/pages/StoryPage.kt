package uz.haksenguz.ertaklar.ui.screens.reader.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import uz.haksenguz.ertaklar.core.text.Gloss
import uz.haksenguz.ertaklar.core.text.Hangul
import uz.haksenguz.ertaklar.data.models.AnswerState
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.data.models.KoreanBook
import uz.haksenguz.ertaklar.data.models.KoreanPage
import uz.haksenguz.ertaklar.data.models.Page
import uz.haksenguz.ertaklar.ui.components.dashedBorder
import uz.haksenguz.ertaklar.ui.text.Display
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.ArtHint
import uz.haksenguz.ertaklar.ui.theme.ArtRing
import uz.haksenguz.ertaklar.ui.theme.DropCap
import uz.haksenguz.ertaklar.ui.theme.GowunDodum
import uz.haksenguz.ertaklar.ui.theme.HeadingFont
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

/** Korean meaning lines (🇰🇷) and Hangul sound hints (가). */
val KoLineColor = Color(0xFF5B6472)
val ReadLineColor = Color(0xFF0F766E)
private val GlossText = Color(0xFF065F46)
private val KoWordColor = Color(0xFF1F4D33)
private val KoOnBorder = Color(0xFFCD2E3A)
private val KoOnBg = Color(0xFFFDECEE)
private val ToggleBorder = Color(0xFFF1D8A8)
private val ReadOnBg = Color(0xFFCCFBF1)

private val TraceText = Color(0xFF9A3412)
private val TraceBg = Color(0xFFFFF7ED)
private val TraceRing = Color(0xFFFDBA74)

/** The piece being read aloud. */
private val Reading = Color(0xFFFDE68A)

/** What the page shows under its Uzbek, and how a tap on an underlined word reaches the reader. */
class PageHelpers(
    val korean: KoreanBook?,
    val gloss: Gloss?,
    /** 🇰🇷 is on for this page. */
    val koShown: Boolean,
    /** 가 is on (and 🇰🇷 isn't showing here). */
    val readsOut: Boolean,
    val hasKorean: Boolean,
    val reading: Boolean,
    val onToggleKorean: () -> Unit,
    val onToggleReading: () -> Unit,
    val onGloss: (Int) -> Unit,
    /** Someone has read this page aloud: a tap on a piece plays it. */
    val onSay: ((Int) -> Unit)? = null,
    /** The piece being read aloud now (0 the title, then the sentences), or -1. */
    val highlight: Int = -1,
    /** Bedtime: the questions rest. */
    val calm: Boolean = false,
    /** 🎨: the page's picture as a colouring page. */
    val onColor: (() -> Unit)? = null,
    /** ✍️ Yozib ko'r: trace the Alifbo page's letter. */
    val onTrace: (() -> Unit)? = null
)

/**
 * Story page [view]: the picture and its note, then the title, the text and the
 * page's question. On a phone the two halves of the open book stack.
 */
@Composable
fun StoryPage(
    book: Book,
    view: Int,
    answer: AnswerState?,
    helpers: PageHelpers,
    onAnswer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val page = book.pages[view - 1]
    val solved = answer?.done == true
    val ko: KoreanPage? = if (helpers.koShown) helpers.korean?.pages?.getOrNull(view - 1) else null
    val display = LocalDisplay.current
    val headingFont = HeadingFont
    BookSheet(modifier) {
        PageHead(chip = uiText("$view-sahifa"), running = storyText(book.title))
        PageArt(
            if (solved && page.revealImage != null) page.revealImage else page.image,
            contentDescription = storyText(page.title),
            onColor = helpers.onColor
        )
        PageNote(page, solved, helpers, ko != null)
        HorizontalDivider(color = ArtRing.copy(alpha = 0.6f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PageHead(chip = null, running = uiText(book.genre), modifier = Modifier.weight(1f))
            ReadToggle(helpers.reading, helpers.onToggleReading)
            if (helpers.hasKorean) KoreanToggle(helpers.koShown, helpers.onToggleKorean)
            PageHead(chip = "$view / ${book.pages.size}", running = "", modifier = Modifier.widthIn(max = 80.dp))
        }
        Column {
            Text(
                storyText(page.title),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp, lineHeight = 30.sp),
                color = PageTitle,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (helpers.highlight == 0) Reading else Color.Transparent)
                    .then(helpers.onSay?.let { say -> Modifier.clickable { say(0) } } ?: Modifier)
            )
            when {
                ko != null -> KoLine(ko.s.getOrElse(0) { "" })
                helpers.readsOut -> ReadLine(Hangul.read(page.title))
            }
        }
        page.letter?.let { letter ->
            // The tutuq belgisi (ʼ) is a sign, not a letter to trace.
            AlifboLetter(storyText(letter), onTrace = helpers.onTrace.takeIf { letter.any { it in 'A'..'Z' || it in 'a'..'z' } })
        }
        val sentences = page.segments.drop(1).ifEmpty { listOf(page.text) }
        val lines: List<Pair<String, Boolean>>? = when {
            ko != null -> sentences.indices.map { (ko.s.getOrElse(it + 1) { "" }) to true }
            helpers.readsOut -> sentences.map { Hangul.read(it) to false }
            else -> null
        }
        val text = pageText(sentences, lines, book.verse, helpers, display, headingFont)
        var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 31.sp),
            color = Ink,
            textAlign = if (book.verse) TextAlign.Center else TextAlign.Start,
            onTextLayout = { layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val l = layout ?: return@drawBehind
                    text.getStringAnnotations(READING, 0, text.length).firstOrNull()?.let { drawReading(l, it.start, it.end) }
                }
        )
        Text(
            "$FLEURON $FLEURON $FLEURON",
            color = MedallionRing,
            fontSize = 20.sp,
            letterSpacing = 8.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        if (!helpers.calm) page.question?.let { QuestionBlock(page, it, answer, ko?.q, helpers.readsOut, onAnswer) }
    }
}

/**
 * The text: glossary words underlined (a tap shows their Korean), a big first letter,
 * and with 🇰🇷 or 가 on, each sentence on its own line with its Korean or its reading under it.
 */
private fun pageText(
    sentences: List<String>,
    lines: List<Pair<String, Boolean>>?,
    verse: Boolean,
    helpers: PageHelpers,
    display: Display,
    headingFont: FontFamily?
): AnnotatedString = buildAnnotatedString {
    val gloss = helpers.gloss
    val firstText = display.content(sentences.firstOrNull().orEmpty())
    // A page that opens with a number keeps it with its word; verse and the lined layout have no big letter.
    var dropCap = lines == null && !verse && firstText.isNotEmpty() && !firstText.first().isDigit()
    sentences.forEachIndexed { i, sentence ->
        if (i > 0) append(if (lines != null || verse) "\n" else " ")
        val parts = gloss?.split(sentence) ?: listOf(sentence to -1)
        val seg = i + 1
        val reading = helpers.highlight == seg
        val start = length
        parts.forEach { (text, entry) ->
            var shown = display.content(text)
            if (dropCap && shown.isNotEmpty()) {
                withStyle(SpanStyle(fontFamily = headingFont, fontWeight = FontWeight.Bold, fontSize = 2.1.em, color = DropCap)) {
                    append(shown.first())
                }
                shown = shown.substring(1)
                dropCap = false
            }
            val say = helpers.onSay
            when {
                entry >= 0 -> {
                    val style = SpanStyle(textDecoration = TextDecoration.Underline, color = GlossText)
                    withLink(LinkAnnotation.Clickable("gloss:$entry", TextLinkStyles(style)) { helpers.onGloss(entry) }) { append(shown) }
                }

                // A tap on the sentence plays it (its glossary words keep their own tap).
                say != null -> withLink(LinkAnnotation.Clickable("say:$seg") { say(seg) }) { append(shown) }

                else -> append(shown)
            }
        }
        if (reading) addStringAnnotation(READING, "", start, length)
        lines?.getOrNull(i)?.let { (line, korean) ->
            append("\n")
            val style = if (korean) {
                SpanStyle(fontFamily = GowunDodum, fontSize = 0.82.em, fontWeight = FontWeight.SemiBold, color = KoLineColor)
            } else {
                SpanStyle(fontFamily = FontFamily.Default, fontSize = 0.8.em, fontWeight = FontWeight.Medium, color = ReadLineColor)
            }
            withStyle(style) { append(line) }
        }
    }
}

/** Marks the piece being read; its marker is drawn behind the text. */
private const val READING = "reading"

/**
 * A marker behind [start] until [end], line by line, hugging the letters. The line
 * boxes carry the extra line height above the text, so it is sized from the baseline.
 */
private fun DrawScope.drawReading(layout: TextLayoutResult, start: Int, end: Int) {
    if (end <= start) return
    val first = layout.getLineForOffset(start)
    val last = layout.getLineForOffset(end - 1)
    val pad = 3.dp.toPx()
    val em = 19.sp.toPx()
    for (line in first..last) {
        val left = if (line == first) layout.getHorizontalPosition(start, true) else layout.getLineLeft(line)
        val right = if (line == last) layout.getHorizontalPosition(end, true) else layout.getLineRight(line)
        val baseline = layout.getLineBaseline(line)
        val top = baseline - 1.05f * em
        val bottom = baseline + 0.35f * em
        drawRoundRect(
            Reading,
            topLeft = Offset(minOf(left, right) - pad, top),
            size = Size(abs(right - left) + 2 * pad, bottom - top),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
    }
}

/** Korean under a piece of Uzbek. */
@Composable
fun KoLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontFamily = GowunDodum,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = KoLineColor,
        modifier = modifier
    )
}

/** A piece of Uzbek read out in Hangul. */
@Composable
fun ReadLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp,
        color = ReadLineColor,
        modifier = modifier
    )
}

/** 가: Hangul sound hints, kept from page to page. */
@Composable
fun ReadToggle(on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PillToggle(
        "가",
        if (on) ReadLineColor else ToggleBorder,
        if (on) ReadOnBg else Color.White,
        ReadLineColor,
        uiText("Koreys harflarida o'qilishi"),
        onClick,
        modifier
    )
}

/** 🇰🇷: the Korean of this page. */
@Composable
fun KoreanToggle(on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PillToggle(
        "🇰🇷",
        if (on) KoOnBorder else ToggleBorder,
        if (on) KoOnBg else Color.White,
        Color.Unspecified,
        uiText("Koreyscha tarjima"),
        onClick,
        modifier
    )
}

@Composable
private fun PillToggle(
    label: String,
    border: Color,
    background: Color,
    color: Color,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        label,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .height(32.dp)
            .widthIn(min = 40.dp)
            .clip(CircleShape)
            .background(background)
            .border(2.dp, border, CircleShape)
            .clickable(role = Role.Switch, onClickLabel = description, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

/** Under the picture: the new word, a proverb's Korean twin, a tongue twister's sounds, or a hint. */
@Composable
private fun PageNote(page: Page, solved: Boolean, helpers: PageHelpers, koShown: Boolean) {
    val word = page.word
    when {
        page.proverbKo != null -> NoteCard(uiText("🇰🇷 Koreyada ham shunday deyishadi:")) {
            Text(
                page.proverbKo[0],
                fontFamily = GowunDodum,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = ProverbKo,
                lineHeight = 25.sp
            )
            Text("«${storyText(page.proverbKo.getOrElse(1) { "" })}»", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        }

        page.sounds != null -> NoteCard(uiText("🔁 Uch marta, tez-tez ayting!")) {
            Text(
                page.sounds.joinToString(" · "),
                fontFamily = HeadingFont,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                letterSpacing = 2.sp,
                color = TwisterSounds
            )
        }

        page.reveal != null && !solved -> Hint(uiText("🤔 Javobini toping — rasm ochiladi!"))

        word != null -> NoteCard(uiText("📖 Yangi so'z")) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(storyText(word[0]), fontFamily = HeadingFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = WordTerm)
                if (helpers.readsOut) Text(Hangul.read(word[0]), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ReadLineColor)
            }
            Text(storyText(word.getOrElse(1) { "" }), style = MaterialTheme.typography.bodyMedium, color = InkSoft)
            if (koShown) {
                val entry = helpers.gloss?.forTerm(word[0]) ?: -1
                helpers.korean?.word(entry)?.let { (_, meaning) ->
                    Text("🇰🇷 $meaning", fontFamily = GowunDodum, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = KoWordColor)
                }
            }
        }

        else -> Hint(uiText("👆 Rasmga bosing — qahramonlar jonlanadi!"))
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlifboLetter(letter: String, onTrace: (() -> Unit)?) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            letter,
            fontFamily = HeadingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 72.sp,
            color = DropCap,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(WordCardTo)
                .border(3.dp, MedallionRing, RoundedCornerShape(24.dp))
                .padding(horizontal = 28.dp, vertical = 4.dp)
        )
        if (onTrace != null) {
            Text(
                uiText("✍️ Yozib ko'r"),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = TraceText,
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TraceBg)
                    .border(2.dp, TraceRing, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClick = onTrace)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
    }
}
