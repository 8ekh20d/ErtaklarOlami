package uz.haksenguz.ertaklar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import coil3.compose.AsyncImage
import uz.haksenguz.ertaklar.core.passport.Passport
import uz.haksenguz.ertaklar.data.content.assetUri
import uz.haksenguz.ertaklar.data.models.Region
import uz.haksenguz.ertaklar.ui.theme.Nunito

private val EmptyRing = Color(0xFFD9C7A3)
private val EmptyMark = Color(0xFFE2D3B4)

/** "#b45309" -> its colour. */
fun inkColor(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

/**
 * A place's rubber stamp in its ink, tilted; with [date] (millis) the day it
 * was stamped is printed on it too. The stamp is drawn in a 120-unit box.
 */
@Composable
fun StampImage(region: Region, modifier: Modifier = Modifier, date: Long? = null) {
    val measurer = rememberTextMeasurer()
    val label = date?.let { Passport.dateLabel(it) }.orEmpty()
    Box(modifier) {
        AsyncImage(model = assetUri(region.stamp), contentDescription = null, modifier = Modifier.fillMaxSize())
        if (label.isNotEmpty()) {
            Canvas(Modifier.fillMaxSize()) {
                val unit = size.minDimension / 120f
                val layout = measurer.measure(
                    label,
                    TextStyle(
                        color = inkColor(region.ink).copy(alpha = 0.9f),
                        fontFamily = Nunito,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (8.6f * unit).toSp(),
                        letterSpacing = (0.4f * unit).toSp()
                    )
                )
                // Between the picture and the bottom words, on the stamp's tilt.
                val baseline = (if (region.capital) 25f else 31f) * unit
                rotate(region.rot.toFloat()) {
                    translate(center.x - layout.size.width / 2f, center.y + baseline - layout.firstBaseline) {
                        drawText(layout)
                    }
                }
            }
        }
    }
}

/** An empty place for a stamp still to come: a dashed ring (a square for Korea) and a "?". */
@Composable
fun EmptyStamp(region: Region, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val unit = size.minDimension / 120f
        val stroke = Stroke(width = 2.6f * unit, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f * unit, 6f * unit)))
        if (region.capital) {
            drawRoundRect(
                EmptyRing,
                topLeft = Offset(center.x - 54 * unit, center.y - 46 * unit),
                size = Size(108 * unit, 92 * unit),
                cornerRadius = CornerRadius(10 * unit),
                style = stroke
            )
        } else {
            drawCircle(EmptyRing, radius = 54 * unit, style = stroke)
        }
        val q = measurer.measure(
            "?",
            TextStyle(
                color = EmptyMark,
                fontFamily = Nunito,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (
                    44 *
                        unit
                    ).toSp()
            )
        )
        drawText(q, topLeft = Offset(center.x - q.size.width / 2f, center.y + 15 * unit - q.firstBaseline))
    }
}

/** A sticker picture; [faded] for one not yet the child's (grey and pale). */
@Composable
fun StickerImage(path: String, modifier: Modifier = Modifier, faded: Float = 0f) {
    val filter = if (faded > 0f) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1f - faded) }) else null
    AsyncImage(
        model = assetUri(path),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = filter,
        alpha = if (faded > 0f) 1f - faded * 0.7f else 1f,
        modifier = modifier
    )
}
