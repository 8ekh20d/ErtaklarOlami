package uz.haksenguz.ertaklar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/** A picture that hops when tapped, so the characters seem to come alive. */
@Composable
fun PokeableArt(
    path: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onPoke: () -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onPoke()
                scope.launch {
                    scale.animateTo(0.94f, tween(110))
                    scale.animateTo(1.05f, tween(170))
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
            }
    ) {
        AssetImage(
            path,
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
            contentDescription = contentDescription
        )
        overlay()
    }
}
