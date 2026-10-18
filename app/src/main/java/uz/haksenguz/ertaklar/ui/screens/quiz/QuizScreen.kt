package uz.haksenguz.ertaklar.ui.screens.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.ui.screens.reader.pages.KoLine
import uz.haksenguz.ertaklar.ui.screens.reader.pages.KoreanToggle
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.AppBackground
import uz.haksenguz.ertaklar.ui.theme.Brand700
import uz.haksenguz.ertaklar.ui.theme.Emerald700
import uz.haksenguz.ertaklar.ui.theme.Gray500
import uz.haksenguz.ertaklar.ui.theme.Gray800
import uz.haksenguz.ertaklar.ui.theme.Orange100

private val EmeraldBg = Color(0xFFD1FAE5)
private val EmeraldChip = Color(0xFFECFDF5)

/** "Bilimdon testi": the book's lesson as one question. */
@Composable
fun QuizScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(viewModel) { viewModel.doneEvents.collect { done() } }

    Box(
        modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        val q = state.question ?: return@Box
        val ko = state.korean.takeIf { state.koShown }
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .padding(16.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, Orange100, RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(EmeraldBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Emerald700, modifier = Modifier.size(34.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    uiText("Bilimdon Testi").uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Emerald700,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EmeraldChip)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
                if (state.korean != null) KoreanToggle(state.koShown, viewModel::toggleKorean)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(storyText(q.q), style = MaterialTheme.typography.headlineSmall, color = Gray800, textAlign = TextAlign.Center)
                ko?.getOrNull(0)?.let { KoLine(it) }
            }
            Text(
                uiText("To'g'ri javobni tanlab 50 ball qo'lga kiriting!"),
                style = MaterialTheme.typography.bodyMedium,
                color = Gray500,
                textAlign = TextAlign.Center
            )
            state.order.forEach { i ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(2.dp, Orange100, RoundedCornerShape(16.dp))
                        .clickable { viewModel.answer(i) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(storyText(q.a[i]), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Gray800)
                        ko?.getOrNull(i + 1)?.let { KoLine(it) }
                    }
                    Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null, tint = Gray500)
                }
            }
            TextButton(onClick = onBack) {
                Text(uiText("← Kitobga qaytish"), color = Brand700, fontWeight = FontWeight.Bold)
            }
        }
    }

    when (val result = state.result) {
        QuizResult.Wrong -> AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            confirmButton = { TextButton(onClick = viewModel::dismissResult) { Text(uiText("Tushunarli")) } },
            title = { Text(uiText("Boshqatdan urinib ko'ring! 💪")) },
            text = { Text(uiText("Bu xulosa asar g'oyasiga mos kelmaydi.")) },
            containerColor = Color.White
        )

        is QuizResult.Right -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(uiText("Tabriklaymiz! 🎉")) },
            text = {
                Text(
                    uiText(
                        if (result.first) {
                            "Siz to'g'ri xulosa topdingiz va ${Scoring.QUIZ} ball qo'shildi!"
                        } else {
                            "Siz to'g'ri xulosa topdingiz! Bu kitob uchun ballni avvalroq olgansiz."
                        }
                    )
                )
            },
            containerColor = Color.White
        )

        null -> Unit
    }
}
