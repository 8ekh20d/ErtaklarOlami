package uz.haksenguz.ertaklar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.AppBackground
import uz.haksenguz.ertaklar.ui.theme.HeadingFont

/** The play corner's colours. */
object Play {
    val Card = Color(0xFFFFFDF7)
    val Title = Color(0xFF7C2D12)
    val Sub = Color(0xFF6B5640)
    val Close = Color(0xFFFEF3C7)
    val Ring = Color(0xFFFDE68A)
    val Won = Color(0xFF047857)
    val Done = Color(0xFF10B981)
    val DoneShadow = Color(0xFF047857)
    val Wrong = Color(0xFFFDA4AF)
    val Hint = Color(0xFFF59E0B)
    val StepText = Color(0xFFA8762C)
    val ToolText = Color(0xFF92400E)
}

/**
 * A game's screen: a cream card with its title and a ✕ to close, a line on
 * what to do, then the game. [scroll] is off for games played by dragging.
 */
@Composable
fun PlayScreen(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    sub: String? = null,
    scroll: Boolean = true,
    toast: ToastState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .widthIn(max = 760.dp)
                .padding(12.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Play.Card)
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    title,
                    fontFamily = HeadingFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    color = Play.Title,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Play.Close)
                        .clickable(role = Role.Button, onClickLabel = uiText("Yopish"), onClick = onClose),
                    contentAlignment = Alignment.Center
                ) { Text("✕", fontSize = 18.sp, color = Play.Title) }
            }
            if (sub != null) Text(sub, fontWeight = FontWeight.Bold, color = Play.Sub, style = MaterialTheme.typography.bodyMedium)
            content()
        }
        toast?.let { ToastHost(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) }
    }
}

/** What the last move did ("🤔 Yana o'ylab ko'ring!"); green once the game is won. */
@Composable
fun ResultLine(text: String, won: Boolean, modifier: Modifier = Modifier) {
    Text(
        text,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = if (won) Play.Won else Play.Title,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp)
    )
}

/** Numbered steps, green once done. */
@Composable
fun StepDots(count: Int, done: (Int) -> Boolean, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
        repeat(count) { i ->
            val on = done(i)
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (on) Play.Done else Play.Close),
                contentAlignment = Alignment.Center
            ) {
                Text("${i + 1}", fontWeight = FontWeight.ExtraBold, color = if (on) Color.White else Play.StepText)
            }
        }
    }
}

/** A sideways shake, played each time [trigger] goes up; read its value in a graphics layer. */
@Composable
fun rememberShake(trigger: Int): Animatable<Float, AnimationVector1D> {
    val x = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            for (v in listOf(-8f, 8f, -8f, 8f, 0f)) x.animateTo(v, tween(90))
        }
    }
    return x
}
