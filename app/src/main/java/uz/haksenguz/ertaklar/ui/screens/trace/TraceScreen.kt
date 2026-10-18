package uz.haksenguz.ertaklar.ui.screens.trace

import android.graphics.Bitmap
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.R
import uz.haksenguz.ertaklar.core.games.TraceGrid
import uz.haksenguz.ertaklar.ui.components.Play
import uz.haksenguz.ertaklar.ui.components.PlayScreen
import uz.haksenguz.ertaklar.ui.components.ResultLine
import uz.haksenguz.ertaklar.ui.components.rememberToastState
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.uiText

private val Guide = 0xFFF6E7C8.toInt()
private val GuideEdge = 0xFFD4A65A.toInt()
private val InkColor = Color(0xFFF97316)
private val NowRing = Color(0xFFF97316)
private val NowBg = Color(0xFFFFEDD5)
private val StepBg = Color(0xFFFFFBEB)
private val MeterFrom = Color(0xFFFBBF24)
private val ClearRing = Color(0xFFE5E7EB)

/** "✍️ Harfni yozing": go over the big pale letter with a finger, capital then small. */
@Composable
fun TraceScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: TraceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val display = LocalDisplay.current
    val toast = rememberToastState()
    LaunchedEffect(viewModel) { viewModel.toastEvents.collect { toast.show(display.ui(it)) } }

    PlayScreen(
        title = uiText("✍️ Harfni yozing"),
        sub = uiText("Barmog'ingiz bilan harf ustidan yurgizing."),
        onClose = onBack,
        scroll = false,
        toast = toast,
        modifier = modifier
    ) {
        if (state.letters.isEmpty()) return@PlayScreen
        val shown = state.letters.map { display.content(it) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            shown.forEachIndexed { i, l ->
                val now = i == state.step && !state.won
                Text(
                    if (state.done(i)) "$l ⭐" else l,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Play.ToolText,
                    modifier = Modifier
                        .widthIn(min = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (now) NowBg else StepBg)
                        .border(2.dp, if (now) NowRing else Play.Ring, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side = min(340.dp, maxWidth * 0.86f)
            Box(
                Modifier
                    .size(side)
                    .clip(RoundedCornerShape(22.dp))
                    .border(3.dp, if (state.won) Play.Done else Play.Ring, RoundedCornerShape(22.dp))
                    .background(Play.Card)
            ) {
                val px = with(LocalDensity.current) { side.roundToPx() }
                Pad(
                    letter = shown[state.step],
                    size = px,
                    round = state.round,
                    enabled = !state.won && !state.waiting,
                    onStrokeEnd = viewModel::check
                )
            }
        }
        val meter by animateFloatAsState(state.meter / 100f, label = "meter")
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 340.dp)
                .fillMaxWidth(0.86f)
                .height(10.dp)
                .clip(CircleShape)
                .background(Play.Close)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(meter)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(MeterFrom, InkColor)))
            )
        }
        Text(
            uiText("🔄 Qaytadan"),
            fontWeight = FontWeight.ExtraBold,
            color = Play.ToolText,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(2.dp, ClearRing, RoundedCornerShape(14.dp))
                .clickable(enabled = !state.won, role = Role.Button, onClick = viewModel::restart)
                .padding(horizontal = 16.dp, vertical = 9.dp)
        )
        ResultLine(uiText(state.result), won = state.won)
    }
}

/** The letter drawn to fit the pad, its shape on the scoring grid, and the brush width. */
private class Sheet(val guide: Bitmap, val grid: TraceGrid, val brush: Float)

/** Draws [letter] big and pale with a dashed edge on a [size]-pixel pad, and samples its shape. */
private fun sheet(letter: String, size: Int, typeface: Typeface?): Sheet {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textAlign = Paint.Align.CENTER
    }
    val bounds = Rect()
    var px = size * 0.78f
    while (px > 20) {
        paint.textSize = px
        paint.getTextBounds(letter, 0, letter.length, bounds)
        if (paint.measureText(letter) <= size * 0.84f && bounds.height() <= size * 0.8f) break
        px -= 4
    }
    paint.getTextBounds(letter, 0, letter.length, bounds)
    val baseline = size / 2f + (-bounds.top - bounds.bottom) / 2f
    val guide = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(guide).apply {
        drawText(letter, size / 2f, baseline, Paint(paint).apply { color = Guide })
        drawText(
            letter,
            size / 2f,
            baseline,
            Paint(paint).apply {
                style = Paint.Style.STROKE
                color = GuideEdge
                strokeWidth = maxOf(2f, size / 220f)
                pathEffect = DashPathEffect(floatArrayOf(size / 60f, size / 60f), 0f)
            }
        )
    }
    val mask = Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8)
    android.graphics.Canvas(mask).drawText(letter, size / 2f, baseline, Paint(paint).apply { color = android.graphics.Color.BLACK })
    val grid = TraceGrid.of(size) { x, y -> android.graphics.Color.alpha(mask.getPixel(x, y)) > 100 }
    mask.recycle()
    return Sheet(guide, grid, maxOf(10f, px * 0.13f))
}

@Composable
private fun Pad(letter: String, size: Int, round: Int, enabled: Boolean, onStrokeEnd: (TraceGrid.Score) -> Unit) {
    val context = LocalContext.current
    val typeface = remember { ResourcesCompat.getFont(context, R.font.nunito_extrabold) }
    val sheet = remember(letter, size, round) { sheet(letter, size, typeface) }
    val guide = remember(sheet) { sheet.guide.asImageBitmap() }
    val strokes = remember(sheet) { mutableStateListOf<List<Offset>>() }
    val isEnabled by rememberUpdatedState(enabled)
    val end by rememberUpdatedState(onStrokeEnd)
    val label = uiText("Harf yozish maydoni")
    Canvas(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = label }
            .pointerInput(sheet) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (!isEnabled) return@awaitEachGesture
                    down.consume()
                    var last = down.position
                    sheet.grid.line(last.x, last.y, last.x, last.y, sheet.brush)
                    strokes.add(listOf(last))
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val p = change.position
                        change.consume()
                        sheet.grid.line(last.x, last.y, p.x, p.y, sheet.brush)
                        strokes[strokes.lastIndex] = strokes.last() + p
                        last = p
                    }
                    if (isEnabled) end(sheet.grid.score())
                }
            }
    ) {
        drawImage(guide)
        val ink = Stroke(width = sheet.brush, cap = StrokeCap.Round, join = StrokeJoin.Round)
        strokes.forEach { points ->
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                if (points.size == 1) lineTo(points[0].x + 0.01f, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, InkColor, style = ink)
        }
    }
}
