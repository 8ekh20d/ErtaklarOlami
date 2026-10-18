package uz.haksenguz.ertaklar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.Amber500
import uz.haksenguz.ertaklar.ui.theme.Brand700
import uz.haksenguz.ertaklar.ui.theme.Gray300
import uz.haksenguz.ertaklar.ui.theme.Orange100

/** A small coloured label on a library card: genre, ages, "Yangi". */
@Composable
fun TagChip(text: String, background: Color, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** "★★☆": a finished book's stars out of three. */
@Composable
fun StarRow(stars: Int, modifier: Modifier = Modifier) {
    Row(modifier) {
        Text("★".repeat(stars), color = Amber500, fontSize = 15.sp, letterSpacing = 1.sp)
        Text("★".repeat((3 - stars).coerceAtLeast(0)), color = Gray300, fontSize = 15.sp, letterSpacing = 1.sp)
    }
}

/** The child's points in the header. */
@Composable
fun PointsChip(points: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Orange100.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = Amber500, modifier = Modifier.padding(0.dp))
        Text(uiText("$points ball"), color = Brand700, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
