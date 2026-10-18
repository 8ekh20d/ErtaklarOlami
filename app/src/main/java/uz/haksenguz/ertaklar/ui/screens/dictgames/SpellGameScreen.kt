package uz.haksenguz.ertaklar.ui.screens.dictgames

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.ui.components.AssetImage
import uz.haksenguz.ertaklar.ui.components.PlayScreen
import uz.haksenguz.ertaklar.ui.components.ResultLine
import uz.haksenguz.ertaklar.ui.components.StepDots
import uz.haksenguz.ertaklar.ui.components.rememberShake
import uz.haksenguz.ertaklar.ui.components.rememberToastState
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.GowunDodum

private val Slot = Color(0xFFF59E0B)
private val TileBg = Color(0xFFFFF7ED)
private val TileRing = Color(0xFFFDBA74)
private val TileText = Color(0xFF9A3412)
private val MeanText = Color(0xFF4B5563)
private val KoText = Color(0xFF1E40AF)

/** "🔤 So'zni yig'ing": the word's picture and meaning; tap its letters in order. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellGameScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SpellGameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val display = LocalDisplay.current
    val toast = rememberToastState()
    LaunchedEffect(viewModel) { viewModel.toastEvents.collect { toast.show(display.ui(it)) } }

    PlayScreen(
        title = uiText("🔤 So'zni yig'ing"),
        sub = uiText("Rasmga qarang va harflarni to'g'ri tartibda bosing."),
        onClose = onBack,
        toast = toast,
        modifier = modifier
    ) {
        val word = state.word ?: return@PlayScreen
        AssetImage(
            word.image,
            Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 260.dp)
                .fillMaxWidth(0.75f)
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(16.dp)),
            alignment = Alignment.Center
        )
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(storyText(word.meaning), fontSize = 14.sp, color = MeanText, textAlign = TextAlign.Center)
            word.ko?.let {
                Text(
                    "🇰🇷 ${it.split(" — ").first()}",
                    fontFamily = GowunDodum,
                    fontWeight = FontWeight.Bold,
                    color = KoText,
                    textAlign = TextAlign.Center
                )
            }
        }
        // The letters stay Latin in Cyrillic too: the game is about these tiles.
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            state.letters.forEachIndexed { i, l ->
                Box(
                    Modifier
                        .size(40.dp, 46.dp)
                        .drawBehind {
                            val y = size.height - 2.dp.toPx()
                            drawLine(Slot, Offset(0f, y), Offset(size.width, y), strokeWidth = 4.dp.toPx())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (i < state.next) Text(l, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Words.Term)
                }
            }
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.tiles.forEach { tile ->
                LetterTile(tile, used = tile.index in state.used, shakes = state.shakes[tile.index] ?: 0, onTap = viewModel::tap)
            }
        }
        StepDots(state.rounds, done = { it < state.round })
        ResultLine(uiText(state.result), won = state.won)
    }
}

@Composable
private fun LetterTile(tile: Tile, used: Boolean, shakes: Int, onTap: (Tile) -> Unit) {
    val shake = rememberShake(shakes)
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .graphicsLayer { translationX = shake.value.dp.toPx() }
            .alpha(if (used) 0f else 1f)
            .height(46.dp)
            .widthIn(min = 46.dp)
            .clip(shape)
            .background(TileBg)
            .border(2.dp, if (shake.isRunning) Words.Wrong else TileRing, shape)
            .clickable(enabled = !used, role = Role.Button) { onTap(tile) }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(tile.letter, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TileText)
    }
}
