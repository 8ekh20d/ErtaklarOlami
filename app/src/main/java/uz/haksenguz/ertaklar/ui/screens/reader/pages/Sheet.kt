package uz.haksenguz.ertaklar.ui.screens.reader.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.haksenguz.ertaklar.ui.components.PokeableArt
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.ArtRing
import uz.haksenguz.ertaklar.ui.theme.ChapterChipBg
import uz.haksenguz.ertaklar.ui.theme.ChapterChipText
import uz.haksenguz.ertaklar.ui.theme.InkSoft
import uz.haksenguz.ertaklar.ui.theme.Paper
import uz.haksenguz.ertaklar.ui.theme.Paper2

/** The book's ornament, U+2766 with the text-style selector U+FE0E, so it draws as text, not as a colour emoji. */
val FLEURON: String = String(intArrayOf(0x2766, 0xFE0E), 0, 2)

/** A sheet of paper: one page of the book, scrolling when its text is long. */
@Composable
fun BookSheet(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Paper, Paper2)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

/** The line at the top of a page: a chip on one side, the running head on the other. */
@Composable
fun PageHead(chip: String?, running: String, modifier: Modifier = Modifier, chipFirst: Boolean = true) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val head = @Composable {
            Text(
                running,
                style = MaterialTheme.typography.labelMedium,
                fontStyle = FontStyle.Italic,
                color = InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        val chipView = @Composable {
            if (chip != null) {
                Text(
                    chip,
                    style = MaterialTheme.typography.labelMedium,
                    color = ChapterChipText,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ChapterChipBg)
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
        if (chipFirst) {
            chipView()
            head()
        } else {
            head()
            chipView()
        }
    }
}

/**
 * A page picture in its white frame with a gold ring; tapping it makes it hop.
 * With [onColor], a 🎨 button (at [colorAt]) opens it as a colouring page.
 */
@Composable
fun PageArt(
    path: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onColor: (() -> Unit)? = null,
    colorAt: Alignment = Alignment.TopEnd,
    onPoke: () -> Unit = {},
    overlay: @Composable () -> Unit = {}
) {
    val shape = RoundedCornerShape(18.dp)
    PokeableArt(
        path = path,
        contentDescription = contentDescription,
        onPoke = onPoke,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(400f / 384f)
            .shadow(8.dp, shape)
            .border(2.dp, ArtRing, shape)
            .padding(2.dp)
            .border(4.dp, Color.White, shape)
            .clip(shape),
        overlay = {
            overlay()
            if (onColor != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentAlignment = colorAt
                ) { ColorButton(onColor) }
            }
        }
    )
}

/** "🎨": opens the picture as a colouring page. */
@Composable
private fun ColorButton(onClick: () -> Unit) {
    val label = uiText("Rasmni bo'yash")
    Box(
        Modifier
            .size(44.dp)
            .shadow(5.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f))
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text("🎨", fontSize = 20.sp) }
}
