package uz.haksenguz.ertaklar.ui.screens.dictgames

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.core.dictionary.Dictionary
import uz.haksenguz.ertaklar.ui.components.AssetImage
import uz.haksenguz.ertaklar.ui.components.PlayScreen
import uz.haksenguz.ertaklar.ui.components.ResultLine
import uz.haksenguz.ertaklar.ui.components.StepDots
import uz.haksenguz.ertaklar.ui.components.rememberShake
import uz.haksenguz.ertaklar.ui.components.rememberToastState
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.HeadingFont

/** "🔊 Eshit va top": hear the word (or read it), tap its picture. */
@Composable
fun ListenGameScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ListenGameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val display = LocalDisplay.current
    val toast = rememberToastState()
    LaunchedEffect(viewModel) { viewModel.toastEvents.collect { toast.show(display.ui(it)) } }

    PlayScreen(
        title = uiText("🔊 Eshit va top"),
        sub = uiText(if (state.voice) "So'zni tinglang va uning rasmini toping." else "So'zni o'qing va uning rasmini toping."),
        onClose = onBack,
        toast = toast,
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.voice) {
                Text(
                    uiText("🔊 Yana eshitish"),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E3A8A),
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Words.SayBg)
                        .border(2.dp, Words.SayRing, RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClick = viewModel::again)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
            if (state.word.isNotEmpty()) {
                Text(storyText(state.word), fontFamily = HeadingFont, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Words.Term)
            }
        }
        state.options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { e ->
                    Picture(
                        e,
                        right = state.right == e.key,
                        shakes = state.shakes[e.key] ?: 0,
                        onClick = { viewModel.pick(e) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
        StepDots(state.rounds, done = { it < state.round })
        ResultLine(uiText(state.result), won = state.won)
    }
}

@Composable
private fun Picture(e: Dictionary.Entry, right: Boolean, shakes: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shake = rememberShake(shakes)
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .graphicsLayer { translationX = shake.value.dp.toPx() }
            .aspectRatio(4f / 3f)
            .then(if (right) Modifier.border(4.dp, Words.RightGlow, shape) else Modifier)
            .padding(if (right) 4.dp else 0.dp)
            .clip(shape)
            .border(3.dp, Words.ring(right, shake.isRunning), shape)
            .background(Words.CardBg)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        AssetImage(e.image, Modifier.fillMaxSize(), contentDescription = uiText("Rasm"), alignment = Alignment.Center)
    }
}

/** The word games' colours. */
internal object Words {
    val Term = Color(0xFF065F46)
    val SayBg = Color(0xFFEFF6FF)
    val SayRing = Color(0xFF93C5FD)
    val CardBg = Color(0xFFFEF3C7)
    val Ring = Color(0xFFFDE68A)
    val Right = Color(0xFF34D399)
    val RightGlow = Color(0xFFD1FAE5)
    val Wrong = Color(0xFFF87171)

    fun ring(right: Boolean, shaking: Boolean): Color = when {
        right -> Right
        shaking -> Wrong
        else -> Ring
    }
}
