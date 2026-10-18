package uz.haksenguz.ertaklar.ui.screens.dictgames

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.core.dictionary.Dictionary
import uz.haksenguz.ertaklar.ui.components.PlayScreen
import uz.haksenguz.ertaklar.ui.components.ResultLine
import uz.haksenguz.ertaklar.ui.components.StepDots
import uz.haksenguz.ertaklar.ui.components.rememberShake
import uz.haksenguz.ertaklar.ui.components.rememberToastState
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.GowunDodum

private val UzBg = Color(0xFFFFFBEB)
private val UzText = Color(0xFF78350F)
private val KoBg = Color(0xFFEFF6FF)
private val KoRing = Color(0xFFBFDBFE)
private val KoText = Color(0xFF1E3A8A)
private val PickedBg = Color(0xFFFFEDD5)
private val PickedRing = Color(0xFFF97316)

/** "🇰🇷 Juftini top": Uzbek words on the left, their Korean meanings on the right. */
@Composable
fun MatchGameScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: MatchGameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val display = LocalDisplay.current
    val toast = rememberToastState()
    LaunchedEffect(viewModel) { viewModel.toastEvents.collect { toast.show(display.ui(it)) } }

    PlayScreen(
        title = uiText("🇰🇷 Juftini top"),
        sub = uiText("O'zbekcha so'zni bosing, keyin uning koreyscha ma'nosini toping."),
        onClose = onBack,
        toast = toast,
        modifier = modifier
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.uz.forEach { e -> Item(storyText(e.term), Side.UZ, e, state, viewModel::tap) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.ko.forEach { e -> Item(Dictionary.shortKo(e.ko.orEmpty()), Side.KO, e, state, viewModel::tap) }
            }
        }
        StepDots(state.uz.size, done = { it < state.matched.size })
        ResultLine(uiText(state.result), won = state.won)
    }
}

@Composable
private fun Item(text: String, side: Side, e: Dictionary.Entry, state: MatchUiState, onTap: (Side, String) -> Unit) {
    val shake = rememberShake(state.shakes[side to e.key] ?: 0)
    val matched = e.key in state.matched
    val picked = state.chosen == (side to e.key)
    val ring = when {
        matched -> Words.Right
        shake.isRunning -> Words.Wrong
        picked -> PickedRing
        side == Side.KO -> KoRing
        else -> Words.Ring
    }
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shake.value.dp.toPx() }
            .alpha(if (matched) 0.75f else 1f)
            .heightIn(min = 46.dp)
            .clip(shape)
            .background(
                if (picked) {
                    PickedBg
                } else if (side == Side.KO) {
                    KoBg
                } else {
                    UzBg
                }
            )
            .border(2.dp, ring, shape)
            .clickable(enabled = !matched, role = Role.Button) { onTap(side, e.key) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            fontFamily = if (side == Side.KO) GowunDodum else null,
            color = if (side == Side.KO) KoText else UzText,
            textAlign = TextAlign.Center
        )
    }
}
