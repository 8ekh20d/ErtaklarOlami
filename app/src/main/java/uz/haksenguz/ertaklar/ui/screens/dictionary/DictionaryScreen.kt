package uz.haksenguz.ertaklar.ui.screens.dictionary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.core.dictionary.Dictionary
import uz.haksenguz.ertaklar.data.repository.DictionaryRepository
import uz.haksenguz.ertaklar.ui.components.AssetImage
import uz.haksenguz.ertaklar.ui.components.ToastHost
import uz.haksenguz.ertaklar.ui.components.rememberToastState
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.AppBackground
import uz.haksenguz.ertaklar.ui.theme.Gray700
import uz.haksenguz.ertaklar.ui.theme.Gray800
import uz.haksenguz.ertaklar.ui.theme.HeadingFont
import uz.haksenguz.ertaklar.ui.theme.Orange100

private val Amber50 = Color(0xFFFFFBEB)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber200 = Color(0xFFFDE68A)
private val Amber800 = Color(0xFF92400E)
private val Amber900 = Color(0xFF78350F)
private val Orange50 = Color(0xFFFFF7ED)
private val Orange200 = Color(0xFFFED7AA)
private val Term = Color(0xFF065F46)
private val Meaning = Color(0xFF4B5563)
private val KoText = Color(0xFF1E40AF)
private val ToolBg = Color(0xFFF9FAFB)
private val ToolRing = Color(0xFFE5E7EB)
private val BackBg = Color(0xFFF3F4F6)

/** The three games, as (game, emoji, name, what it is). */
private val GAMES = listOf(
    Triple(DictionaryRepository.LISTEN, "🔊", "Eshit va top" to "So'zni eshitib, rasmini toping"),
    Triple(DictionaryRepository.MATCH, "🇰🇷", "Juftini top" to "O'zbekcha va koreyscha juftlar"),
    Triple(DictionaryRepository.SPELL, "🔤", "So'zni yig'ing" to "Harflardan so'z tuzing")
)

/** "📖 Mening lug'atim": the child's words as picture cards in Uzbek alphabet order, and games with them. */
@Composable
fun DictionaryScreen(
    onBack: () -> Unit,
    onOpenWord: (book: String, page: Int) -> Unit,
    onGame: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictionaryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val display = LocalDisplay.current
    val toast = rememberToastState()
    LaunchedEffect(viewModel) { viewModel.toastEvents.collect { toast.show(display.ui(it)) } }

    Box(
        modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(300.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { Header(state.entries.size, onBack) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GAMES.forEach { (game, emoji, text) ->
                        GameButton(emoji, uiText(text.first), uiText(text.second), enabled = state.opens(game), onClick = { onGame(game) })
                    }
                }
            }
            if (!state.loading && state.short) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val n = Dictionary.MIN_WORDS
                    Text(
                        uiText("🎮 O'yinlar uchun kamida $n ta so'z kerak. Yana ${n - state.entries.size} ta so'z yig'ing!"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Amber800,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Amber50)
                            .border(1.dp, Amber200, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
            if (!state.loading && state.entries.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        uiText("📚 Hali so'z yo'q. Kitob o'qing — yangi so'zlar shu yerga yig'iladi!"),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                    )
                }
            }
            items(state.entries, key = { it.key }) { e ->
                WordCard(e, onSay = { viewModel.say(e.term) }, onOpen = { onOpenWord(e.book, e.view) })
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.navigationBarsPadding()) }
        }
        ToastHost(
            toast,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun Header(count: Int, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, Orange100, RoundedCornerShape(24.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BackBg)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Gray700, modifier = Modifier.size(18.dp))
            Text(uiText("Kutubxona"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Gray700)
        }
        Text(
            uiText("📖 Mening lug'atim"),
            fontFamily = HeadingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Gray800,
            modifier = Modifier.weight(1f)
        )
        Text(
            uiText("$count ta so'z"),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Amber800,
            modifier = Modifier
                .clip(CircleShape)
                .background(Amber100)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun GameButton(emoji: String, name: String, about: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Amber50, Orange50)))
            .border(2.dp, Amber200, RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 24.sp)
        Column {
            Text(name, fontWeight = FontWeight.ExtraBold, color = Amber900)
            Text(about, fontSize = 12.sp, color = Amber800.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun WordCard(e: Dictionary.Entry, onSay: () -> Unit, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Orange200, RoundedCornerShape(20.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AssetImage(
            e.image,
            Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Amber100),
            alignment = Alignment.Center
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(storyText(e.term), fontFamily = HeadingFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Term)
            Text(storyText(e.meaning), fontSize = 13.sp, lineHeight = 17.sp, color = Meaning)
            e.ko?.let { Text("🇰🇷 $it", fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, color = KoText) }
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val sayLabel = uiText("Eshitish")
                Tool("🔊", onSay, Modifier.semantics { contentDescription = sayLabel })
                Tool(uiText("📖 Kitobda"), onOpen)
            }
        }
    }
}

@Composable
private fun Tool(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = Gray700,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ToolBg)
            .border(1.dp, ToolRing, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    )
}
