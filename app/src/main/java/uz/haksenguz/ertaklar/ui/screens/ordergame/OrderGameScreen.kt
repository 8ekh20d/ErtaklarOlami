package uz.haksenguz.ertaklar.ui.screens.ordergame

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import uz.haksenguz.ertaklar.ui.components.StepDots
import uz.haksenguz.ertaklar.ui.components.rememberShake
import uz.haksenguz.ertaklar.ui.components.rememberToastState
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText

/** "🧩 Voqealar tartibi": four pictures from the story, tapped in the order they happen. */
@Composable
fun OrderGameScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: OrderGameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val display = LocalDisplay.current
    val toast = rememberToastState()
    LaunchedEffect(viewModel) { viewModel.toastEvents.collect { toast.show(display.ui(it)) } }

    PlayScreen(
        title = uiText("🧩 Voqealar tartibi"),
        sub = uiText("Rasmlarni ertakda bo'lgan tartibda bosing. Avval nima bo'ldi?"),
        onClose = onBack,
        toast = toast,
        modifier = modifier
    ) {
        state.cards.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { card ->
                    PictureCard(
                        card = card,
                        placed = card.order < state.step,
                        shakes = state.shakes[card.order] ?: 0,
                        hint = state.hint == card.order,
                        onClick = { viewModel.pick(card) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
        StepDots(state.cards.size, done = { it < state.step }, modifier = Modifier.padding(top = 4.dp))
        ResultLine(uiText(state.result), won = state.won)
    }
}

@Composable
private fun PictureCard(card: OrderCard, placed: Boolean, shakes: Int, hint: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shake = rememberShake(shakes)
    val pulse = if (hint) {
        rememberInfiniteTransition(
            label = "hint"
        ).animateFloat(1f, 1.04f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "pulse").value
    } else {
        1f
    }
    val ring = when {
        placed -> Play.Done
        hint -> Play.Hint
        shake.isRunning -> Play.Wrong
        else -> Play.Ring
    }
    Box(
        modifier.graphicsLayer {
            translationX = shake.value.dp.toPx()
            scaleX = pulse
            scaleY = pulse
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(3.dp, ring, RoundedCornerShape(18.dp))
                .clickable(enabled = !placed, role = Role.Button, onClick = onClick)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AssetImage(
                card.image,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(400f / 320f)
                    .clip(RoundedCornerShape(12.dp))
            )
            // The caption shows once the picture is in place, so it is the picture that tells.
            Text(
                if (placed) storyText(card.title) else "",
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Play.Title,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (placed) {
            Box(
                Modifier
                    .offset((-8).dp, (-8).dp)
                    .size(40.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Play.Done),
                contentAlignment = Alignment.Center
            ) {
                Text("${card.order + 1}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}
