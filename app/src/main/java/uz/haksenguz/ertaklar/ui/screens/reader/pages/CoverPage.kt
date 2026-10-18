package uz.haksenguz.ertaklar.ui.screens.reader.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.ui.components.AssetImage
import uz.haksenguz.ertaklar.ui.components.parseHue
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.CoverTitle
import uz.haksenguz.ertaklar.ui.theme.GoldSoft

/** The closed book: a cloth cover in the book's colour; tapping it opens the book. */
@Composable
fun CoverPage(
    book: Book,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    /** On a tablet the cover is the book's front board itself, edge to edge. */
    inBook: Boolean = false
) {
    val hue = parseHue(book.hue)
    val shape = if (inBook) RoundedCornerShape(4.dp, 16.dp, 16.dp, 4.dp) else RoundedCornerShape(10.dp, 22.dp, 22.dp, 10.dp)
    Box(
        modifier
            .fillMaxSize()
            .padding(horizontal = if (inBook) 0.dp else 20.dp, vertical = if (inBook) 0.dp else 12.dp)
            .shadow(if (inBook) 0.dp else 14.dp, shape)
            .clip(shape)
            .background(hue)
            .clothTexture()
            .clickable(onClick = onOpen)
            .padding(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .border(3.dp, GoldSoft.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .padding(6.dp)
                .border(2.dp, GoldSoft.copy(alpha = 0.45f), RoundedCornerShape(9.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                "ERTAKLAR OLAMI",
                color = GoldSoft,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 3.sp
            )
            Text(
                storyText(book.title),
                color = CoverTitle,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Box(
                Modifier
                    .fillMaxWidth(0.72f)
                    .aspectRatio(1f)
                    .shadow(10.dp, CircleShape)
                    .border(5.dp, GoldSoft, CircleShape)
                    .clip(CircleShape)
            ) {
                AssetImage(book.cover, Modifier.fillMaxSize(), contentDescription = book.title)
            }
            Text(
                storyText(book.tag),
                color = GoldSoft,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
            Text(
                uiText("👆 Kitobni ochish uchun bosing"),
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

/** A soft highlight and fine diagonal threads, like book cloth. */
private fun Modifier.clothTexture(): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width * 0.3f, size.height * 0.18f),
            radius = size.maxDimension * 0.55f
        )
    )
    clipRect {
        val step = 5.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(Color.Black.copy(alpha = 0.06f), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 2.dp.toPx() / 2)
            x += step
        }
    }
}
