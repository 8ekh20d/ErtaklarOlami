package uz.haksenguz.ertaklar.ui.screens.passport

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import uz.haksenguz.ertaklar.data.models.MapFile
import uz.haksenguz.ertaklar.data.models.Region
import uz.haksenguz.ertaklar.ui.components.inkColor
import uz.haksenguz.ertaklar.ui.theme.Nunito

private val Sea = Color(0xFFCFE7F2)
private val Land = Color(0xFFEBE5D8)
private val LandEdge = Color(0xFFCFC3AA)
private val Shadow = Color(0x245B3A1A)
private val Border = Color(0xFFB07A3C)
private val RegionFill = Color(0xFFFFFAF0)
private val RegionEdge = Color(0xFFDCC49C)
private val Selected = Color(0xFF7C2D12)
private val Star = Color(0xFFE63946)
private val Name = Color(0xFF5B3A1A)
private val NameOn = Color(0xFF9A3412)
private val Country = Color(0xFFA39577)
private val SeaName = Color(0xFF4F86A8)
private val Inset = Color(0xFFEEF6FB)
private val InsetEdge = Color(0xFF8FB8EA)
private val Flight = Color(0xFF1E40AF)
private val CityText = Color(0xFF1E3A8A)

private const val STAR = "M0 -10 L2.9 -3.1 L10 -3.1 L4.2 1.4 L6.2 8.5 L0 4.3 L-6.2 8.5 L-4.2 1.4 L-10 -3.1 L-2.9 -3.1Z"

/** The map's shapes, parsed once, and each region's area for finding where a finger lands. */
private class MapShapes(map: MapFile) {
    private fun path(d: String): Path = PathParser().parsePathString(d).toPath()

    val regions: Map<String, Path> = map.regions.mapValues { path(it.value.d) }
    val neighbours: List<Path> = map.neighbours.values.map(::path)
    val outline: Path = path(map.outline)
    val south: Path = path(map.korea.south)
    val north: Path = path(map.korea.north)
    val star: Path = path(STAR)

    private val areas: Map<String, android.graphics.Region> = regions.mapValues { (_, p) ->
        android.graphics.Region().apply { setPath(p.asAndroidPath(), android.graphics.Region(0, 0, map.width, map.height)) }
    }

    /** The region at map point ([x], [y]), if any. */
    fun at(x: Float, y: Float): String? = areas.entries.firstOrNull { it.value.contains(x.toInt(), y.toInt()) }?.key
}

/**
 * The map of Uzbekistan: stamped places in their colour with a tick, the open
 * place outlined, Tashkent's star, and Korea in its corner with the way there.
 * On a phone only the open place keeps its name, as the map is small.
 */
@Composable
fun UzMap(
    map: MapFile,
    regions: List<Region>,
    countries: Map<String, List<String>>,
    stamped: Set<String>,
    selected: String?,
    /** Korean menus: the map's names in Korean. */
    ko: Boolean,
    /** Uzbek text as shown (Latin or Cyrillic). */
    text: (String) -> String,
    onRegion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shapes = remember(map) { MapShapes(map) }
    val measurer = rememberTextMeasurer()
    val tap by rememberUpdatedState(onRegion)
    val byId = remember(regions) { regions.associateBy { it.id } }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val wide = maxWidth >= 640.dp
        // The names, resolved here so the drawing below needs no composition.
        val names = regions.associate { it.id to if (ko) it.ko else text(it.label ?: it.name) }
        val countryNames = countries.mapValues { (_, n) -> if (ko) n.getOrElse(1) { "" } else text(n.getOrElse(0) { "" }) }
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(map.width.toFloat() / map.height)
                .clip(RoundedCornerShape(20.dp))
                .pointerInput(map) {
                    detectTapGestures { p ->
                        val s = size.width / map.width.toFloat()
                        val x = p.x / s
                        val y = p.y / s
                        val (bx, by, bw, bh) = map.korea.box
                        val hit = if (x in bx.toFloat()..(bx + bw).toFloat() &&
                            y in by.toFloat()..(by + bh).toFloat()
                        ) {
                            "koreya"
                        } else {
                            shapes.at(x, y)
                        }
                        hit?.let(tap)
                    }
                }
        ) {
            val s = size.width / map.width
            scale(s, s, pivot = Offset.Zero) {
                drawRect(Sea, size = Size(map.width.toFloat(), map.height.toFloat()))
                shapes.neighbours.forEach {
                    drawPath(it, Land)
                    drawPath(it, LandEdge, style = Stroke(1.2f))
                }
            }
            if (wide) {
                map.names.forEach { (k, xy) ->
                    val sea = k == "caspian"
                    label(
                        measurer,
                        countryNames[k].orEmpty(),
                        xy[0] * s,
                        xy[1] * s,
                        15f * s,
                        if (sea) SeaName else Country,
                        if (sea) Sea else Land,
                        italic = true
                    )
                }
            }
            scale(s, s, pivot = Offset.Zero) {
                translate(3f, 5f) { drawPath(shapes.outline, Shadow) }
                regions.forEach { r ->
                    val p = shapes.regions[r.id] ?: return@forEach
                    if (r.id in stamped) {
                        drawPath(p, inkColor(r.fill))
                        drawPath(p, Color.White, style = Stroke(2.4f, join = StrokeJoin.Round))
                    } else {
                        drawPath(p, RegionFill)
                        drawPath(p, RegionEdge, style = Stroke(1.6f, join = StrokeJoin.Round))
                    }
                }
                drawPath(shapes.outline, Border, style = Stroke(3.2f, join = StrokeJoin.Round))
                selected?.let { shapes.regions[it] }?.let { drawPath(it, Selected, style = Stroke(4f, join = StrokeJoin.Round)) }
                star(shapes.star, map.capital[0].toFloat(), map.capital[1].toFloat(), 1.1f)
                // stamped places get a tick in their ink
                regions.forEach { r ->
                    val g = map.regions[r.id] ?: return@forEach
                    if (r.id !in stamped) return@forEach
                    val (x, y) = g.label.map { it.toFloat() }
                    drawCircle(inkColor(r.ink), 11f, Offset(x, y - 25))
                    drawCircle(Color.White, 11f, Offset(x, y - 25), style = Stroke(2.6f))
                    val tick = Path().apply {
                        moveTo(x - 5, y - 25)
                        relativeLineTo(3.6f, 3.8f)
                        relativeLineTo(6.6f, -7.4f)
                    }
                    drawPath(tick, Color.White, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            regions.forEach { r ->
                val g = map.regions[r.id] ?: return@forEach
                val on = r.id == selected
                if (!wide && !on) return@forEach
                val px = when {
                    !wide -> 38f
                    on -> 20f
                    else -> 17f
                }
                label(
                    measurer,
                    names[r.id].orEmpty(),
                    g.label[0] * s,
                    (g.label[1] + 5) * s,
                    px * s,
                    if (on) NameOn else Name,
                    Color.White,
                    haloWidth = if (wide) 4f else 8f
                )
            }
            // Korea in its own box, and the way there from Tashkent
            val korea = byId["koreya"] ?: return@Canvas
            val (bx, by, bw, bh) = map.korea.box.map { it.toFloat() }
            val (sx, sy) = map.korea.seoul.map { it.toFloat() }
            val (tx, ty) = map.capital.map { it.toFloat() }
            val kOn = "koreya" in stamped
            val kSel = selected == "koreya"
            scale(s, s, pivot = Offset.Zero) {
                drawRoundRect(Inset, Offset(bx, by), Size(bw, bh), CornerRadius(16f))
                drawRoundRect(
                    if (kSel) Selected else InsetEdge,
                    Offset(bx, by),
                    Size(bw, bh),
                    CornerRadius(16f),
                    style = Stroke(2.4f, pathEffect = if (kSel) null else PathEffect.dashPathEffect(floatArrayOf(9f, 6f)))
                )
                drawPath(shapes.north, Land)
                drawPath(shapes.north, LandEdge, style = Stroke(1.2f))
                drawPath(shapes.south, if (kOn) inkColor(korea.fill) else RegionFill)
                drawPath(shapes.south, if (kOn) Color.White else RegionEdge, style = Stroke(if (kOn) 2.4f else 1.6f))
                star(shapes.star, sx, sy, 0.8f)
                val flight = Path().apply {
                    moveTo(tx + 4, ty - 12)
                    quadraticTo(tx + 64, (ty + sy) / 2 + 20, sx + 9, sy + 9)
                }
                drawPath(
                    flight,
                    Flight.copy(alpha = 0.75f),
                    style = Stroke(2.6f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 8f)))
                )
            }
            val titlePx = if (wide) 17f else 30f
            label(
                measurer,
                names["koreya"].orEmpty(),
                (bx + bw / 2) * s,
                (by + 24 + if (wide) 0 else 14) * s,
                titlePx * s,
                if (kSel) NameOn else Name,
                Inset
            )
            if (wide) {
                val city = if (ko) korea.koCity.orEmpty() else text(korea.city.orEmpty())
                label(measurer, city, (sx - 12) * s, (sy + 4) * s, 14f * s, CityText, Inset, end = true)
                val km = "✈ " + map.seoulKm.toString().reversed().chunked(3).joinToString(" ").reversed() + " km"
                label(measurer, km, ((tx + sx) / 2 + 52) * s, ((ty + sy) / 2 + 4) * s, 15f * s, Flight, Land)
            }
        }
    }
}

private fun DrawScope.star(star: Path, x: Float, y: Float, k: Float) {
    translate(x, y) {
        scale(k, k, pivot = Offset.Zero) {
            drawPath(star, Color.White, style = Stroke(2.4f * 2, join = StrokeJoin.Round))
            drawPath(star, Star)
        }
    }
}

/**
 * Map text centred on ([x], [baseline]) with a halo so it reads over any colour;
 * lines split at "\n" stack 16 units apart around it. [end] anchors its right edge.
 */
private fun DrawScope.label(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    baseline: Float,
    px: Float,
    color: Color,
    halo: Color,
    italic: Boolean = false,
    haloWidth: Float = 4f,
    end: Boolean = false
) {
    if (text.isEmpty()) return
    val lines = text.split('\n')
    val step = px * 16f / 17f
    val first = baseline - (lines.size - 1) * step / 2
    lines.forEachIndexed { i, line ->
        val style = TextStyle(
            fontFamily = Nunito,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            fontSize = px.toSp()
        )
        // One layout drawn twice: the halo as a stroke, then the letters.
        val layout = measurer.measure(line, style)
        val left = if (end) x - layout.size.width else x - layout.size.width / 2f
        val top = first + i * step - layout.firstBaseline
        drawText(
            layout,
            color = halo,
            topLeft = Offset(left, top),
            drawStyle = Stroke(width = haloWidth * px / 17f, join = StrokeJoin.Round)
        )
        drawText(layout, color = color, topLeft = Offset(left, top))
    }
}
