package uz.haksenguz.ertaklar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Light only; bedtime gets its own dark theme. */
private val LightColors = lightColorScheme(
    primary = Brand500,
    onPrimary = Color.White,
    primaryContainer = Brand100,
    onPrimaryContainer = Brand700,
    secondary = FairySky,
    onSecondary = Color.White,
    tertiary = FairyMint,
    onTertiary = Color.White,
    background = Brand50,
    onBackground = Gray800,
    surface = Color.White,
    onSurface = Gray800,
    surfaceVariant = Orange100,
    onSurfaceVariant = Gray600,
    outline = Orange200,
    outlineVariant = Orange100,
    error = FairyRose
)

/** The screen background: brand-50 → amber-50 → orange-100. */
val AppBackground = Brush.linearGradient(listOf(Brand50, Amber50, Orange100))

/** The banner: orange-400 → brand-500 → amber-500. */
val HeroGradient = Brush.horizontalGradient(listOf(Orange400, Brand500, Amber500))

@Composable
fun ErtaklarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
