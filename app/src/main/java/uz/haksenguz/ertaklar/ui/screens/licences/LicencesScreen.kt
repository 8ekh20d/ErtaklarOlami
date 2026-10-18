package uz.haksenguz.ertaklar.ui.screens.licences

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.ui.components.Play
import uz.haksenguz.ertaklar.ui.components.PlayScreen
import uz.haksenguz.ertaklar.ui.text.uiText

private val CreditBg = Color(0xFFFFFBEB)
private val CreditText = Color(0xFF78350F)
private val CardRing = Color(0xFFF6E7C8)
private val Body = Color(0xFF4B5563)

/** "📜 Litsenziyalar": who made the fonts, the emoji, the icon's book and the map, and on what terms. */
@Composable
fun LicencesScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: LicencesViewModel = hiltViewModel()) {
    val licences by viewModel.licences.collectAsStateWithLifecycle()
    PlayScreen(
        title = uiText("📜 Litsenziyalar"),
        sub = uiText("Ilovadagi shriftlar, emoji, belgi va xarita quyidagi litsenziyalar asosida ishlatiladi."),
        onClose = onBack,
        modifier = modifier
    ) {
        // The credit the emoji licence asks for, in plain view.
        Text(
            "Emoji graphics: Twemoji, by Twitter, Inc and other contributors, CC-BY 4.0",
            fontWeight = FontWeight.Bold,
            color = CreditText,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CreditBg)
                .border(1.dp, Play.Ring, RoundedCornerShape(14.dp))
                .padding(12.dp)
        )
        licences.forEach { LicenceCard(it) }
    }
}

@Composable
private fun LicenceCard(licence: Licence) {
    var open by rememberSaveable(licence.title) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, CardRing, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = uiText(if (open) "Yopish" else "Ochish")) { open = !open }
            .animateContentSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(licence.title, fontWeight = FontWeight.ExtraBold, color = Play.Title)
                if (licence.about.isNotEmpty()) Text(uiText(licence.about), fontSize = 13.sp, color = Play.Sub)
            }
            Text(if (open) "▲" else "▼", color = Play.Sub)
        }
        if (open) {
            Text(licence.text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp, color = Body)
        }
    }
}
