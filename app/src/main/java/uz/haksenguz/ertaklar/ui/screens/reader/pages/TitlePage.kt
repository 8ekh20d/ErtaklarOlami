package uz.haksenguz.ertaklar.ui.screens.reader.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.haksenguz.ertaklar.core.levels.Levels
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.ui.components.PokeableArt
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.DropCap
import uz.haksenguz.ertaklar.ui.theme.Exlibris
import uz.haksenguz.ertaklar.ui.theme.ExlibrisBorder
import uz.haksenguz.ertaklar.ui.theme.Gold
import uz.haksenguz.ertaklar.ui.theme.InkSoft
import uz.haksenguz.ertaklar.ui.theme.MedallionRing
import uz.haksenguz.ertaklar.ui.theme.ResumeFrom
import uz.haksenguz.ertaklar.ui.theme.ResumeTo
import uz.haksenguz.ertaklar.ui.theme.Rust
import uz.haksenguz.ertaklar.ui.theme.TitleTag

/** The book's name page, with the ex-libris above it and "continue" when a reading is under way. */
@Composable
fun TitlePage(
    book: Book,
    resumePage: Int,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
    onPoke: () -> Unit = {},
    /** On a tablet the ex-libris is on the endpaper opposite, so the right page shows only the title. */
    half: PageHalf = PageHalf.BOTH
) {
    BookSheet(modifier, half) {
        if (half == PageHalf.BOTH) ExLibris(book, Modifier.fillMaxWidth())
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(FLEURON, color = Gold, fontSize = 26.sp)
            Text(storyText(book.title), style = MaterialTheme.typography.headlineLarge, color = Rust, textAlign = TextAlign.Center)
            Text(storyText(book.tag), style = MaterialTheme.typography.labelLarge, color = TitleTag, textAlign = TextAlign.Center)
            PokeableArt(
                book.cover,
                Modifier
                    .fillMaxWidth(0.62f)
                    .aspectRatio(1f)
                    .shadow(10.dp, CircleShape)
                    .border(3.dp, MedallionRing, CircleShape)
                    .padding(3.dp)
                    .border(6.dp, Color.White, CircleShape)
                    .clip(CircleShape),
                contentDescription = storyText(book.title),
                onPoke = onPoke
            )
            val ages = if (book.age.size >= 2) " · ${uiText(Levels.label(book.age))}" else ""
            Text(
                uiText("📄 ${book.pages.size} sahifa · ⏱ ~${book.minutes} daqiqa") + ages,
                style = MaterialTheme.typography.labelLarge,
                color = InkSoft,
                textAlign = TextAlign.Center
            )
            Text(storyText("«Bir bor ekan, bir yo'q ekan...»"), style = MaterialTheme.typography.titleMedium, color = DropCap)
            if (resumePage > 0) {
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Box(
                        Modifier
                            .background(Brush.linearGradient(listOf(ResumeFrom, ResumeTo)))
                            .padding(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Text(
                            uiText("▶ Davom ettirish · $resumePage-sahifa"),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                Text(uiText("Sahifani suring yoki ➜ tugmasini bosing"), style = MaterialTheme.typography.bodySmall, color = InkSoft)
            }
        }
    }
}

/** "Ushbu kitob ... Ertaklar Olami kutubxonasidan": the owner's label inside the cover. */
@Composable
private fun ExLibris(book: Book, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Exlibris)
            .border(3.dp, ExlibrisBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(uiText("Ushbu kitob"), style = MaterialTheme.typography.labelMedium, color = InkSoft)
        Text(storyText(book.title), style = MaterialTheme.typography.titleMedium, color = Rust, textAlign = TextAlign.Center)
        Text(uiText("Ertaklar Olami kutubxonasidan"), style = MaterialTheme.typography.labelMedium, color = InkSoft)
    }
}

/**
 * The endpaper inside the cover: a patterned paper with the ex-libris in the
 * middle. On a tablet it faces the title page, and it is the back of the cover.
 */
@Composable
fun EndpaperPage(book: Book, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp, 0.dp, 0.dp, 6.dp))
            .background(EndpaperBg)
            .drawBehind {
                // Diagonal gold lines and small dots, every 44dp.
                val step = 44.dp.toPx()
                val line = 1.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(EndpaperLine, Offset(x, 0f), Offset(x + size.height, size.height), line)
                    drawLine(EndpaperLine, Offset(x + size.height, 0f), Offset(x, size.height), line)
                    x += step / 2
                }
                var y = 0f
                while (y < size.height) {
                    var cx = 0f
                    while (cx < size.width) {
                        drawCircle(EndpaperDotRed, 6.dp.toPx(), Offset(cx + step * 0.25f, y + step * 0.25f))
                        drawCircle(EndpaperDotRed, 6.dp.toPx(), Offset(cx + step * 0.75f, y + step * 0.75f))
                        drawCircle(EndpaperDotTeal, 3.dp.toPx(), Offset(cx + step * 0.75f, y + step * 0.25f))
                        drawCircle(EndpaperDotTeal, 3.dp.toPx(), Offset(cx + step * 0.25f, y + step * 0.75f))
                        cx += step
                    }
                    y += step
                }
            },
        contentAlignment = Alignment.Center
    ) {
        ExLibris(
            book,
            Modifier
                .fillMaxWidth(0.72f)
                .shadow(8.dp, RoundedCornerShape(18.dp))
        )
    }
}

private val EndpaperBg = Color(0xFFF8ECD2)
private val EndpaperLine = Color(0x1FD4A017)
private val EndpaperDotRed = Color(0x38BE4628)
private val EndpaperDotTeal = Color(0x2E1E6E6E)
