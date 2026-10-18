package uz.haksenguz.ertaklar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** A short message that shows for a moment and fades ("⭐ Barakalla! +10 ball"). */
@Stable
class ToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var visible by mutableStateOf(false)
        private set
    internal var serial by mutableIntStateOf(0)
        private set

    fun show(text: String) {
        message = text
        visible = true
        serial++
    }

    internal fun hide() {
        visible = false
    }
}

@Composable
fun rememberToastState() = remember { ToastState() }

@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    LaunchedEffect(state.serial) {
        if (state.visible) {
            delay(1800)
            state.hide()
        }
    }
    AnimatedVisibility(
        visible = state.visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
        Text(
            state.message.orEmpty(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xE61F2937))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}
