package uz.haksenguz.ertaklar.ui.screens.coloring

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.haksenguz.ertaklar.R
import uz.haksenguz.ertaklar.core.games.Regions
import uz.haksenguz.ertaklar.data.audio.Sfx
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.navigation.ColoringRoute

/** The crayons: 13 colours and the eraser (white). */
val Palette: List<Int> = listOf(
    0xFFEF476F, 0xFFF94144, 0xFFF8961E, 0xFFFFD166, 0xFFFFF3B0, 0xFF90BE6D, 0xFF43AA8B,
    0xFF4CC9F0, 0xFF277DA1, 0xFF7B2CBF, 0xFFF6CDA5, 0xFF8D5524, 0xFFADB5BD, 0xFFFFFFFF
).map { it.toInt() }

const val ERASER = 0xFFFFFFFF.toInt()

data class ColoringUiState(
    val title: String = "",
    val ready: Boolean = false,
    val color: Int = Palette[3],
    /** Goes up with every change to the colours, so the picture redraws. */
    val version: Int = 0,
    val canUndo: Boolean = false
)

/**
 * "🎨 Bo'yash": the page's line drawing; a tap fills the area under the finger
 * with the chosen colour. The colours sit under the lines, which are multiplied
 * over them, so the lines stay crisp.
 */
@HiltViewModel
class ColoringViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    @ApplicationContext private val context: Context,
    private val sfx: Sfx
) : ViewModel() {

    private val route = savedState.toRoute<ColoringRoute>()
    private val _state = MutableStateFlow(ColoringUiState())
    val state: StateFlow<ColoringUiState> = _state.asStateFlow()

    /** The line drawing. */
    var lines: Bitmap? = null
        private set

    /** The colours, under the lines. */
    var layer: Bitmap? = null
        private set

    private var regions: Regions? = null
    private var colors = IntArray(0)
    private var pixels = IntArray(0)

    /** Each fill: the area and its colour before. */
    private val history = ArrayDeque<Pair<Int, Int>>()

    init {
        viewModelScope.launch {
            val book = content.library()[route.key] ?: return@launch
            val v = route.view.coerceIn(1, book.pages.size)
            val page = book.pages[v - 1]
            val path = (if (route.reveal) page.revealLineArt else null) ?: page.lineArt ?: return@launch
            val title = if (route.view > book.pages.size) book.title else page.title
            _state.update { it.copy(title = title) }
            withContext(Dispatchers.Default) {
                val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                val bmp = context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) } ?: return@withContext
                val w = bmp.width
                val h = bmp.height
                val px = IntArray(w * h)
                bmp.getPixels(px, 0, w, 0, 0, w, h)
                val found = Regions.of(w, h, IntArray(px.size) { Regions.luminance(px[it]) })
                colors = IntArray(found.count) { ERASER }
                pixels = IntArray(w * h)
                found.paint(colors, ERASER, pixels)
                layer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { setPixels(pixels, 0, w, 0, 0, w, h) }
                lines = bmp
                regions = found
            }
            _state.update { it.copy(ready = lines != null) }
        }
    }

    fun pick(color: Int) = _state.update { it.copy(color = color) }

    /** A tap at ([x], [y]) in the picture's pixels. Returns whether something was painted. */
    fun fill(x: Float, y: Float): Boolean {
        val r = regions ?: return false
        // A finger is wide: a tap on a line fills the area beside it.
        val area = r.near(x.toInt(), y.toInt(), radius = r.width / 100)
        if (area < 0) return false
        val color = _state.value.color
        if (colors[area] == color) return false
        history.addLast(area to colors[area])
        colors[area] = color
        redraw()
        sfx.chime()
        return true
    }

    fun undo() {
        val (area, before) = history.removeLastOrNull() ?: return
        colors[area] = before
        redraw()
    }

    fun clear() {
        history.clear()
        colors.fill(ERASER)
        redraw()
    }

    private fun redraw() {
        val r = regions ?: return
        val bmp = layer ?: return
        r.paint(colors, ERASER, pixels)
        bmp.setPixels(pixels, 0, r.width, 0, 0, r.width, r.height)
        _state.update { it.copy(version = it.version + 1, canUndo = history.isNotEmpty()) }
    }

    /** The coloured picture as one image; with [heading], a title band above it (for printing). */
    fun picture(heading: String? = null): Bitmap? {
        val l = lines ?: return null
        val c = layer ?: return null
        val band = if (heading != null) l.height / 9 else 0
        val out = Bitmap.createBitmap(l.width, l.height + band, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(android.graphics.Color.WHITE)
        if (heading != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF7C2D12.toInt()
                textAlign = Paint.Align.CENTER
                textSize = band * 0.5f
                typeface = ResourcesCompat.getFont(context, R.font.nunito_extrabold)
            }
            canvas.drawText(heading, l.width / 2f, band * 0.68f, paint)
        }
        canvas.drawBitmap(c, 0f, band.toFloat(), null)
        canvas.drawBitmap(l, 0f, band.toFloat(), Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY) })
        return out
    }

    /** Saves the picture to the phone's gallery (Pictures/Ertaklar Olami). Returns whether it worked. */
    suspend fun save(): Boolean = withContext(Dispatchers.IO) {
        val bmp = picture() ?: return@withContext false
        val name = "ertaklar-olami-" + _state.value.title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Ertaklar Olami")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        runCatching {
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
            val ok = resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } == true
            if (!ok) {
                resolver.delete(uri, null, null)
                return@runCatching false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            }
            true
        }.getOrDefault(false)
    }
}
