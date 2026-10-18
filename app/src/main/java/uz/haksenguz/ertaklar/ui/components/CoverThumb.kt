package uz.haksenguz.ertaklar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A book's cover picture in a rounded frame ringed with its colour; locked twins are veiled. */
@Composable
fun CoverThumb(
    path: String,
    hue: Color,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    locked: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, shape)
            .border(3.dp, hue, shape)
            .clip(shape)
    ) {
        AssetImage(path, Modifier.fillMaxSize())
        if (locked) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔒", fontSize = 30.sp)
            }
        }
    }
}

/** "#1f7a58" -> Color; falls back to the brand orange. */
fun parseHue(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFFF59E0B))
