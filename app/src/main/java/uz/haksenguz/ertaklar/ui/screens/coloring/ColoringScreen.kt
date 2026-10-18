package uz.haksenguz.ertaklar.ui.screens.coloring

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.print.PrintHelper
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.ui.components.Play
import uz.haksenguz.ertaklar.ui.components.PlayScreen
import uz.haksenguz.ertaklar.ui.components.rememberToastState
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.uiText

private val SwatchRing = Color(0xFFE5D3A8)
private val EraserStripe = Color(0xFFFDA4AF)

/** The most a picture can be zoomed in, for the small places. */
private const val MAX_ZOOM = 4f

/** "🎨 Bo'yash": pick a crayon, tap a place in the picture; pinch to zoom in. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColoringScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ColoringViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val display = LocalDisplay.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toast = rememberToastState()
    val heading = "${uiText("🎨 Bo'yash")} — ${display.content(state.title)}"

    val save: () -> Unit = {
        scope.launch {
            toast.show(display.ui(if (viewModel.save()) "💾 Rasm galereyaga saqlandi" else "Rasmni saqlab bo'lmadi"))
        }
    }
    // Before Android 10 the gallery needs the storage permission.
    val askStorage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) save() else toast.show(display.ui("Rasmni saqlab bo'lmadi"))
    }

    PlayScreen(
        title = heading,
        sub = uiText("Rangni tanlang va rasmning istalgan joyiga bosing."),
        onClose = onBack,
        scroll = false,
        toast = toast,
        modifier = modifier
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1000f / 960f)
                .clip(RoundedCornerShape(18.dp))
                .border(3.dp, Play.Ring, RoundedCornerShape(18.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            val lines = viewModel.lines
            val layer = viewModel.layer
            if (state.ready && lines != null && layer != null) {
                Picture(lines, layer, state.version, onTap = viewModel::fill)
            } else {
                CircularProgressIndicator(color = Play.Hint)
            }
        }
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Palette.forEachIndexed { i, c ->
                val label = if (c == ERASER) uiText("O'chirg'ich") else "${uiText("Rang")} ${i + 1}"
                Swatch(c, on = c == state.color, label = label, onClick = { viewModel.pick(c) })
            }
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Tool(uiText("↶ Orqaga"), enabled = state.canUndo, onClick = viewModel::undo)
            Tool(uiText("🧹 Tozalash"), onClick = viewModel::clear)
            Tool(uiText("💾 Rasmni saqlash")) {
                val needs = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED
                if (needs) askStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else save()
            }
            Tool(uiText("🖨 Chop etish")) {
                val title = heading.removePrefix("🎨").trim()
                viewModel.picture(title)?.let {
                    PrintHelper(context).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }.printBitmap(title, it)
                }
            }
        }
    }
}

/** The picture: colours under the lines; a tap paints, two fingers zoom and move it. */
@Composable
private fun Picture(lines: Bitmap, layer: Bitmap, version: Int, onTap: (x: Float, y: Float) -> Unit) {
    val linesImage = remember(lines) { lines.asImageBitmap() }
    val layerImage = remember(layer) { layer.asImageBitmap() }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, move, factor, _ ->
                    val next = (zoom * factor).coerceIn(1f, MAX_ZOOM)
                    val raw = (pan - centroid) * (next / zoom) + centroid + move
                    // Keep the picture filling the frame.
                    pan = Offset(raw.x.coerceIn(size.width * (1 - next), 0f), raw.y.coerceIn(size.height * (1 - next), 0f))
                    zoom = next
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val x = (tap.x - pan.x) / zoom / size.width * lines.width
                    val y = (tap.y - pan.y) / zoom / size.height * lines.height
                    onTap(x, y)
                }
            }
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = zoom
                    scaleY = zoom
                    translationX = pan.x
                    translationY = pan.y
                }
        ) {
            // Reading the version redraws the picture after each fill.
            if (version < 0) return@Canvas
            val dst = IntSize(size.width.toInt(), size.height.toInt())
            drawImage(layerImage, srcOffset = IntOffset.Zero, dstSize = dst)
            drawImage(linesImage, srcOffset = IntOffset.Zero, dstSize = dst, blendMode = BlendMode.Multiply)
        }
    }
}

@Composable
private fun Swatch(color: Int, on: Boolean, label: String, onClick: () -> Unit) {
    val c = Color(color)
    val fill = if (color == ERASER) {
        Brush.linearGradient(
            0f to Color.White,
            0.45f to Color.White,
            0.45f to EraserStripe,
            0.55f to EraserStripe,
            0.55f to Color.White,
            1f to Color.White
        )
    } else {
        Brush.linearGradient(listOf(c, c))
    }
    Box(
        Modifier
            .size(44.dp)
            .scale(if (on) 1.15f else 1f)
            .border(if (on) 3.dp else 2.dp, if (on) Play.Hint else SwatchRing, CircleShape)
            .padding(if (on) 3.dp else 2.dp)
            .border(3.dp, Color.White, CircleShape)
            .clip(CircleShape)
            .background(fill)
            .semantics {
                contentDescription = label
                selected = on
            }
            .clickable(role = Role.RadioButton, onClick = onClick)
    )
}

@Composable
private fun Tool(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        fontWeight = FontWeight.ExtraBold,
        color = if (enabled) Play.ToolText else Play.ToolText.copy(alpha = 0.4f),
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Play.Close)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
