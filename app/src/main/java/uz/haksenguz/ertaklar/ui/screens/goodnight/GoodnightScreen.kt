package uz.haksenguz.ertaklar.ui.screens.goodnight

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.haksenguz.ertaklar.ui.components.AssetImage
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText

private val NightTop = Color(0xFF1E2A5A)
private val NightDeep = Color(0xFF070B1D)
private val Moonlight = Color(0xFFFDE68A)
private val Lavender = Color(0xFFC7D2FE)
private val Periwinkle = Color(0xFFA5B4FC)
private val ButtonBorder = Color(0xFF4C5AA0)
private val ButtonText = Color(0xFFE0E7FF)

/** After this long the screen fades nearly to dark; a tap wakes it. */
private const val FADE_AFTER_MS = 15_000L

/**
 * "Xayrli tun": after tonight's last page, a sleeping village under the moon,
 * good night to the child, and where tomorrow begins. It slowly fades to dark.
 */
@Composable
fun GoodnightScreen(name: String, next: String, onLeave: () -> Unit, modifier: Modifier = Modifier) {
    var faded by remember { mutableStateOf(false) }
    var wakes by remember { mutableIntStateOf(0) }
    LaunchedEffect(wakes) {
        faded = false
        delay(FADE_AFTER_MS)
        faded = true
    }
    val alpha by animateFloatAsState(if (faded) 0.05f else 1f, tween(if (faded) 6_000 else 400), label = "fade")
    BackHandler(onBack = onLeave)

    Column(
        modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(NightTop, NightDeep), radius = 1400f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { wakes++ }
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
    ) {
        AssetImage(
            "img/goodnight.webp",
            Modifier
                .alpha(alpha)
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.86f)
                .aspectRatio(400f / 320f)
                .shadow(30.dp, RoundedCornerShape(24.dp), ambientColor = Moonlight, spotColor = Moonlight)
                .clip(RoundedCornerShape(24.dp))
        )
        Text(
            uiText("Xayrli tun, ${storyText(name)}! 🌙"),
            style = MaterialTheme.typography.headlineLarge,
            color = Moonlight,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(alpha)
                .padding(top = 8.dp)
        )
        Text(
            uiText("Shirin tushlar ko'ring!"),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Lavender,
            modifier = Modifier.alpha(alpha)
        )
        Text(
            uiText(next),
            fontSize = 15.sp,
            color = Periwinkle,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(alpha)
                .widthIn(max = 480.dp)
        )
        Text(
            uiText("📚 Kutubxonaga qaytish"),
            color = ButtonText,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .alpha(alpha)
                .padding(top = 8.dp)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NightTop)
                .border(2.dp, ButtonBorder, RoundedCornerShape(16.dp))
                .clickable(onClick = onLeave)
                .padding(horizontal = 20.dp, vertical = 13.dp)
        )
    }
}
