package uz.haksenguz.ertaklar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.haksenguz.ertaklar.ui.theme.Brand100
import uz.haksenguz.ertaklar.ui.theme.Brand500
import uz.haksenguz.ertaklar.ui.theme.Brand700
import uz.haksenguz.ertaklar.ui.theme.ChapterChipBg

/** Lot / Кир / 한: Uzbek in Latin or Cyrillic, or the menus in Korean. Its labels never change script. */
@Composable
fun ScriptToggle(choice: String, onChoice: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brand100)
            .border(1.dp, ChapterChipBg, RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("lat" to "Lot", "cyr" to "Кир", "ko" to "한").forEach { (id, label) ->
            val on = id == choice
            Text(
                label,
                color = if (on) Color.White else Brand700,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) Brand500 else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onChoice(id) }
                    .padding(horizontal = 9.dp, vertical = 8.dp)
            )
        }
    }
}
