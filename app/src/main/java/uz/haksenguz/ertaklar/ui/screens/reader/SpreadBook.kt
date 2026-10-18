package uz.haksenguz.ertaklar.ui.screens.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.reading.Reading
import uz.haksenguz.ertaklar.ui.screens.reader.pages.PageHalf

/** How long a sheet takes to turn, and the heavier cover. */
private const val FLIP_MS = 900
private const val COVER_MS = 1150

/** A drag that ends this fast (px per event) completes the turn whatever its distance. */
private const val FLICK = 12f

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

/**
 * The open book on a tablet: where it lies open ([view]: -1 closed, 0 the
 * title, 1..N the story, then the end) and the sheet being turned, if any.
 */
@Stable
class SpreadState(initial: Int) {
    /** A sheet on its way from [from] to [to]. */
    data class Turn(val from: Int, val to: Int) {
        val forward: Boolean get() = to > from

        /** The front board itself: opening or closing the book. */
        val cover: Boolean get() = from == Reading.COVER || to == Reading.COVER
    }

    var view by mutableIntStateOf(initial)
        private set
    var turn by mutableStateOf<Turn?>(null)
        private set

    /** How far the sheet has turned, 0..1. */
    val progress = Animatable(0f)

    /** The last view the child may turn to (at bedtime, tonight's last page). */
    var last by mutableIntStateOf(initial)

    /** Called as a sheet starts to turn (for its sound); true for the cover. */
    var onTurnStart: (Boolean) -> Unit = {}

    private val mutex = MutatorMutex()

    val canNext: Boolean get() = view < last
    val canPrev: Boolean get() = view > Reading.COVER

    /** Turns to [target]: one sheet with the animation, further at once. A turn in flight finishes first; taps meanwhile are ignored. */
    suspend fun turnTo(target: Int) {
        if (turn != null) return
        val to = target.coerceIn(Reading.COVER, max(last, Reading.COVER))
        if (to == view) return
        if (abs(to - view) != 1) {
            jump(to)
            return
        }
        mutex.mutate(MutatePriority.UserInput) {
            begin(view, to)
            try {
                progress.animateTo(1f, tween(if (turn?.cover == true) COVER_MS else FLIP_MS, easing = EaseInOutCubic))
                end(true)
            } catch (e: CancellationException) {
                // Interrupted (the screen closed or changed size): land the sheet where it is nearer.
                end(progress.value > 0.5f)
                throw e
            }
        }
    }

    /** Opens the book at [to] without turning (a jump, or a change of screen size). */
    suspend fun jump(to: Int) {
        mutex.mutate(MutatePriority.PreventUserInput) {
            turn = null
            progress.snapTo(0f)
            view = to.coerceIn(Reading.COVER, max(last, Reading.COVER))
        }
    }

    internal suspend fun begin(from: Int, to: Int) {
        turn = Turn(from, to)
        progress.snapTo(0f)
        onTurnStart(turn!!.cover)
    }

    internal suspend fun end(completed: Boolean) {
        val t = turn ?: return
        if (completed) view = t.to
        turn = null
        progress.snapTo(0f)
    }

    // ---------- dragging a page by hand ----------

    private var dragging = false

    internal suspend fun dragStart(forward: Boolean): Boolean {
        if (turn != null) return false
        val to = if (forward) view + 1 else view - 1
        if (to > last || to < Reading.COVER) return false
        dragging = true
        begin(view, to)
        return true
    }

    internal suspend fun dragTo(p: Float) {
        if (dragging) progress.snapTo(p.coerceIn(0f, 1f))
    }

    /** The finger let go: past halfway (or flicked) the sheet completes its turn, else it falls back. */
    internal suspend fun dragEnd(flick: Boolean) {
        if (!dragging) return
        dragging = false
        val p = progress.value
        val complete = flick || p > 0.5f
        val target = if (complete) 1f else 0f
        progress.animateTo(target, tween(max(160, (FLIP_MS * abs(target - p)).toInt()), easing = EaseOutCubic))
        end(complete)
    }
}

/**
 * The book lying open: two pages on their boards, the spine between them,
 * and a sheet that lifts around the spine to turn. Forward, its front is the
 * right page and its back the next left one; the next right page waits
 * underneath and the old left page stays until the sheet lands on it.
 * [page] draws a view's left or right side (the cover is view -1's right side,
 * the endpaper view 0's left side). Drag a page sideways to turn it by hand.
 */
@Composable
fun SpreadBook(state: SpreadState, board: Color, page: @Composable (view: Int, half: PageHalf) -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val content by rememberUpdatedState(page)
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        // Each page about as tall as a sheet of paper, the whole within the screen.
        val bookWidth = min(min(maxWidth, 1000.dp), maxHeight * 1.45f)
        val t = state.turn
        val p = state.progress.value
        val closed = when {
            t?.cover == true -> if (t.forward) 1 - p else p
            state.view == Reading.COVER -> 1f
            else -> 0f
        }
        Box(
            Modifier
                .widthIn(max = bookWidth)
                .fillMaxWidth()
                .height(min(maxHeight, bookWidth / 1.45f))
                // Closed, the book lies with its front cover in the middle.
                .graphicsLayer { translationX = -size.width / 4 * closed }
                .pointerInput(state) {
                    var total = 0f
                    var last = 0f
                    var started = false
                    detectHorizontalDragGestures(
                        onDragStart = {
                            total = 0f
                            started = false
                        },
                        onDragEnd = {
                            if (started) {
                                scope.launch {
                                    state.dragEnd(
                                        flick =
                                            abs(last) > FLICK && (last < 0) == (state.turn?.forward == true)
                                    )
                                }
                            }
                        },
                        onDragCancel = { if (started) scope.launch { state.dragEnd(flick = false) } }
                    ) { change, dx ->
                        total += dx
                        last = dx
                        if (!started && total != 0f) {
                            started = true
                            scope.launch { if (!state.dragStart(forward = total < 0)) started = false }
                        }
                        if (started) {
                            change.consume()
                            scope.launch { state.dragTo(abs(total) / size.width) }
                        }
                    }
                }
        ) {
            // The boards: the cover's cloth around the pages (only the front board while closed).
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterStart)
                    // Only once the cover has landed on it (its inside then lies on top).
                    .graphicsLayer { alpha = if (closed > 0f) 0f else 1f }
                    .offset(x = (-10).dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp, 4.dp, 4.dp, 16.dp))
                    .background(board, RoundedCornerShape(16.dp, 4.dp, 4.dp, 16.dp))
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp)
                    .shadow(16.dp, RoundedCornerShape(4.dp, 16.dp, 16.dp, 4.dp))
                    .background(board, RoundedCornerShape(4.dp, 16.dp, 16.dp, 4.dp))
            )
            val view = state.view
            // What lies beneath the turning sheet (or the book at rest).
            val under: Pair<Int?, Int?> = when {
                t == null -> (view.takeIf { it >= 0 }) to view
                t.forward -> (t.from.takeIf { it >= 0 }) to t.to
                else -> (t.to.takeIf { it >= 0 }) to t.from
            }
            val sweep = sin(p * PI).toFloat()
            val past = p > 0.5f
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    under.first?.let { content(it, PageHalf.LEFT) }
                    val shade = when {
                        t == null -> 0f
                        t.forward -> if (past) sweep * 0.7f else 0f
                        else -> if (past) sweep * 0.25f else sweep * 0.7f
                    }
                    PageShade(shade, towardsRight = true)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    under.second?.let { v -> content(v, PageHalf.RIGHT) }
                    val shade = when {
                        t == null -> 0f
                        t.forward -> if (past) sweep * 0.25f else sweep * 0.7f
                        else -> if (past) sweep * 0.7f else 0f
                    }
                    PageShade(shade, towardsRight = false)
                }
            }
            if (closed < 1f) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(SpineDark, SpineLight, SpineDark)))
                )
            }
            if (t != null) Leaf(t, p, content)
        }
    }
}

/** The sheet in the air: its front until it stands upright, then its back (drawn the right way round). */
@Composable
private fun BoxScope.Leaf(t: SpreadState.Turn, p: Float, page: @Composable (Int, PageHalf) -> Unit) {
    val forward = t.forward
    Box(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.5f)
            .padding(vertical = 4.dp)
            .align(if (forward) Alignment.CenterEnd else Alignment.CenterStart)
            .graphicsLayer {
                transformOrigin = TransformOrigin(if (forward) 0f else 1f, 0.5f)
                rotationY = if (forward) -180f * p else 180f * p
                cameraDistance = 32f * density
            }
    ) {
        val front = p <= 0.5f
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { if (!front) scaleX = -1f }
        ) {
            // Forward the right page lifts, the next left page on its back; backward the
            // mirror image. The cover is view -1's right side, its inside view 0's left.
            val (shown, half) = when {
                forward && front -> t.from to PageHalf.RIGHT
                forward -> t.to to PageHalf.LEFT
                front -> t.from to PageHalf.LEFT
                else -> t.to to PageHalf.RIGHT
            }
            page(shown, half)
            // The sheet darkens as it tilts away from the light.
            val shade = if (front) min(1f, p * 2) * 0.6f else min(1f, (1 - p) * 2) * 0.6f
            if (shade > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ShadeColor.copy(alpha = shade * 0.55f))
                )
            }
        }
    }
}

/** The shadow a turning sheet casts on a page, darkest at the spine. */
@Composable
private fun PageShade(alpha: Float, towardsRight: Boolean) {
    if (alpha <= 0f) return
    val dark = ShadeColor.copy(alpha = 0.55f * alpha)
    val mid = ShadeColor.copy(alpha = 0.12f * alpha)
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    if (towardsRight) {
                        listOf(Color.Transparent, mid, dark)
                    } else {
                        listOf(dark, mid, Color.Transparent)
                    }
                )
            )
    )
}

private val ShadeColor = Color(0xFF281405)
private val SpineDark = Color(0x593C230A)
private val SpineLight = Color(0x80FFFFFF)
