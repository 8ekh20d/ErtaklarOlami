package uz.haksenguz.ertaklar.ui.screens.reader.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.haksenguz.ertaklar.core.scoring.Scoring
import uz.haksenguz.ertaklar.data.models.AnswerState
import uz.haksenguz.ertaklar.data.models.Page
import uz.haksenguz.ertaklar.data.models.Question
import uz.haksenguz.ertaklar.ui.theme.Brand700
import uz.haksenguz.ertaklar.ui.theme.ChoiceBorder
import uz.haksenguz.ertaklar.ui.theme.ChoiceText
import uz.haksenguz.ertaklar.ui.theme.Ink
import uz.haksenguz.ertaklar.ui.theme.QuestionBorder
import uz.haksenguz.ertaklar.ui.theme.QuestionFrom
import uz.haksenguz.ertaklar.ui.theme.QuestionTo
import uz.haksenguz.ertaklar.ui.theme.RightBg
import uz.haksenguz.ertaklar.ui.theme.RightBorder
import uz.haksenguz.ertaklar.ui.theme.Rust
import uz.haksenguz.ertaklar.ui.theme.WordTerm
import uz.haksenguz.ertaklar.ui.theme.WrongBg
import uz.haksenguz.ertaklar.ui.theme.WrongBorder
import uz.haksenguz.ertaklar.ui.theme.WrongText

/** A page's question for the children: answers, then praise or "think again". */
@Composable
fun QuestionBlock(page: Page, question: Question, state: AnswerState?, onAnswer: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val done = state?.done == true
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(QuestionFrom, QuestionTo)))
            .border(2.dp, QuestionBorder, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("💡 BOLAJONLAR UCHUN SAVOL", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, color = Brand700)
        Text(question.q, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
        question.a.forEachIndexed { i, text ->
            val right = done && (i == question.ok || question.ok < 0) && i == state.pick
            val wrong = state?.wrong?.contains(i) == true
            Choice(text, right = right, wrong = wrong, enabled = !done, onClick = { onAnswer(i) })
        }
        when {
            done -> {
                Text(
                    "⭐ ${state.praise ?: "Barakalla!"} +${Scoring.ANSWER} ball",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Brand700
                )
                page.reveal?.let { Text("🎉 Javob: ${it.answer}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Rust) }
            }

            !state?.wrong.isNullOrEmpty() -> Text(
                "🤔 Yana bir o'ylab ko'ring!",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Brand700
            )
        }
    }
}

@Composable
private fun Choice(text: String, right: Boolean, wrong: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val (border, bg, fg) = when {
        right -> Triple(RightBorder, RightBg, WordTerm)
        wrong -> Triple(WrongBorder, WrongBg, WrongText)
        else -> Triple(ChoiceBorder, Color.White, ChoiceText)
    }
    Text(
        text,
        color = fg.copy(alpha = if (!enabled && !right) 0.6f else 1f),
        fontSize = 15.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.ExtraBold,
        textDecoration = if (wrong) TextDecoration.LineThrough else null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(bg)
            .border(2.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    )
}
