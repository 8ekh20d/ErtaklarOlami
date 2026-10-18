package uz.haksenguz.ertaklar.ui.screens.bedtime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.core.reading.Bedtime
import uz.haksenguz.ertaklar.ui.components.AssetImage
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText

private val Night = Color(0xFF111833)
private val NightCard = Color(0xFF1E2A5A)
private val NightBorder = Color(0xFF34407A)
private val Moonlight = Color(0xFFFDE68A)
private val Lavender = Color(0xFFC7D2FE)
private val Periwinkle = Color(0xFFA5B4FC)

/** "🌙 Uxlash vaqti": a grown-up picks tonight's pages and a calm book. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BedtimeSheet(
    onStart: (key: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BedtimeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Night,
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(uiText("🌙 Uxlash vaqti"), style = MaterialTheme.typography.headlineSmall, color = Moonlight)
            Text(uiText("Bugun kechqurun qaysi ertakni o'qiymiz?"), style = MaterialTheme.typography.bodyMedium, color = Lavender)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Text(uiText("Nechta sahifa?"), style = MaterialTheme.typography.labelLarge, color = Periwinkle)
                Bedtime.PAGES.forEach { n ->
                    val on = n == state.pages
                    Text(
                        uiText(if (n > 0) "$n sahifa" else "Butun kitob"),
                        fontWeight = FontWeight.Bold,
                        color = if (on) Night else Lavender,
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(CircleShape)
                            .background(if (on) Moonlight else NightCard)
                            .border(1.dp, if (on) Moonlight else NightBorder, CircleShape)
                            .clickable { viewModel.setPages(n) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
            state.books.forEach { b ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NightCard)
                        .border(1.dp, NightBorder, RoundedCornerShape(16.dp))
                        .clickable { onStart(b.key) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AssetImage(
                        b.cover,
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            storyText(b.title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(uiText(b.status), style = MaterialTheme.typography.labelMedium, color = Periwinkle)
                    }
                }
            }
        }
    }
}
