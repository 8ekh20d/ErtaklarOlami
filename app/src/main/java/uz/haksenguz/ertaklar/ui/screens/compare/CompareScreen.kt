package uz.haksenguz.ertaklar.ui.screens.compare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import uz.haksenguz.ertaklar.ui.components.Play
import uz.haksenguz.ertaklar.ui.components.PlayScreen
import uz.haksenguz.ertaklar.ui.components.ResultLine
import uz.haksenguz.ertaklar.ui.components.dashedBorder
import uz.haksenguz.ertaklar.ui.components.rememberShake
import uz.haksenguz.ertaklar.ui.components.rememberToastState
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText

private val CardBg = Color(0xFFFFFBEB)
private val CardText = Color(0xFF1F2937)

/** Each column's button colours and background. */
private data class Look(val from: Color, val to: Color, val shadow: Color, val column: Color)

private val Looks = mapOf(
    Where.UZ to Look(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFF9A3412), Color(0xFFFFF7ED)),
    Where.BOTH to Look(Color(0xFF8B5CF6), Color(0xFF6D28D9), Color(0xFF4C1D95), Color(0xFFF5F3FF)),
    Where.KO to Look(Color(0xFF0EA5E9), Color(0xFF1D4ED8), Color(0xFF1E3A8A), Color(0xFFEFF6FF))
)

/** "🔍 Farqlarni toping": the two tales side by side, and one card at a time to sort. */
@Composable
fun CompareScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: CompareViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val display = LocalDisplay.current
    val toast = rememberToastState()
    LaunchedEffect(viewModel) { viewModel.toastEvents.collect { toast.show(display.ui(it)) } }
    val left = state.left
    val right = state.right

    PlayScreen(title = uiText("🔍 Farqlarni toping"), onClose = onBack, toast = toast, modifier = modifier) {
        if (left == null || right == null) return@PlayScreen
        val leftName = "${left.icon} ${storyText(left.title)}"
        val rightName = "${right.icon} ${storyText(right.title)}"
        val both = "✨ ${uiText("Ikkalasida ham")}"
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(left to leftName, right to rightName).forEach { (t, name) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    AssetImage(
                        t.cover,
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(400f / 260f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(3.dp, Play.Ring, RoundedCornerShape(14.dp))
                    )
                    Text(
                        name,
                        fontWeight = FontWeight.ExtraBold,
                        color = Play.Title,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        val card = state.card
        if (card != null) {
            Text(uiText("Bu qaysi ertakda bor?"), fontWeight = FontWeight.Bold, color = Play.Sub)
            val shake = rememberShake(state.shakes)
            Row(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shake.value.dp.toPx() }
                    .heightIn(min = 72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardBg)
                    .dashedBorder(3.dp, if (shake.isRunning) Play.Wrong else Play.Hint, 18.dp, dash = 8.dp, gap = 6.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(card.emoji, fontSize = 40.sp)
                Text(
                    storyText(card.text),
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CardText,
                    modifier = Modifier.weight(1f)
                )
                Text("${state.index + 1} / ${state.deck.size}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Play.ToolText)
            }
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice(leftName, Where.UZ, viewModel::answer, Modifier.weight(1f))
                Choice(both, Where.BOTH, viewModel::answer, Modifier.weight(1f))
                Choice(rightName, Where.KO, viewModel::answer, Modifier.weight(1f))
            }
        }
        Row(Modifier.height(IntrinsicSize.Min).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BoardColumn(Where.UZ, leftName, state.sorted(Where.UZ), Modifier.weight(1f))
            BoardColumn(Where.BOTH, both, state.sorted(Where.BOTH), Modifier.weight(1f))
            BoardColumn(Where.KO, rightName, state.sorted(Where.KO), Modifier.weight(1f))
        }
        ResultLine(uiText(state.result), won = state.won)
    }
}

@Composable
private fun Choice(text: String, where: Where, onClick: (Where) -> Unit, modifier: Modifier = Modifier) {
    val look = Looks.getValue(where)
    Box(
        modifier
            .fillMaxHeight()
            .heightIn(min = 56.dp)
            // A coloured edge under the button, so it looks pressable.
            .clip(RoundedCornerShape(16.dp))
            .background(look.shadow)
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(look.from, look.to)))
            .clickable(role = Role.Button) { onClick(where) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

/** A column of the board: the cards sorted into it, each popping in. */
@Composable
private fun BoardColumn(where: Where, head: String, cards: List<CompareCard>, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxHeight()
            .heightIn(min = 80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Looks.getValue(where).column)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            head,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Play.Title,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
        )
        cards.forEach { c ->
            val shown = remember(c) { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(shown, enter = scaleIn(initialScale = 0.6f) + fadeIn()) {
                Text(
                    "${c.emoji} ${storyText(c.text)}",
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}
