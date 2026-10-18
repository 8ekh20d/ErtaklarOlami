package uz.haksenguz.ertaklar.ui.screens.reader

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.core.reading.Reading
import uz.haksenguz.ertaklar.ui.components.PointsChip
import uz.haksenguz.ertaklar.ui.components.ToastHost
import uz.haksenguz.ertaklar.ui.components.rememberToastState
import uz.haksenguz.ertaklar.ui.screens.reader.pages.CoverPage
import uz.haksenguz.ertaklar.ui.screens.reader.pages.FinaleActions
import uz.haksenguz.ertaklar.ui.screens.reader.pages.FinalePage
import uz.haksenguz.ertaklar.ui.screens.reader.pages.StoryPage
import uz.haksenguz.ertaklar.ui.screens.reader.pages.TitlePage
import uz.haksenguz.ertaklar.ui.theme.Amber500
import uz.haksenguz.ertaklar.ui.theme.AppBackground
import uz.haksenguz.ertaklar.ui.theme.Brand500
import uz.haksenguz.ertaklar.ui.theme.Brand700
import uz.haksenguz.ertaklar.ui.theme.Gray700
import uz.haksenguz.ertaklar.ui.theme.Orange100

private val PrevBg = Color(0xFFFEF3C7)
private val PrevText = Color(0xFF78350F)
private val TrackBg = Color(0xFFE5E7EB)

/**
 * The book reader. Pages turn with a swipe or the buttons below:
 * cover, title page, the story pages, then the end.
 */
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onOpenBook: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberToastState()
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ReaderEvent.Toast -> toast.show(event.text)
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        val book = state.book
        if (state.loading || book == null) {
            CircularProgressIndicator(color = Brand500, modifier = Modifier.align(Alignment.Center))
        } else {
            // Pager page i shows view i - 1: the cover is page 0.
            val pager = rememberPagerState(initialPage = state.startView + 1) { state.lastView + 2 }
            LaunchedEffect(pager) {
                snapshotFlow { pager.settledPage }.distinctUntilChanged().collect { viewModel.onPageSettled(it - 1) }
            }
            val scope = rememberCoroutineScope()
            val goTo: (Int) -> Unit = { view -> scope.launch { pager.animateScrollToPage(view + 1) } }
            val context = LocalContext.current
            val share = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "«${book.title}» — Ertaklar Olami ilovasidagi rasmli ertak.")
                }
                context.startActivity(Intent.createChooser(send, "Ulashish"))
            }
            val actions = FinaleActions(
                onTwin = { state.twin?.let { onOpenBook(it.first) } },
                onCompare = viewModel::comingSoon,
                onQuiz = viewModel::comingSoon,
                onOrder = viewModel::comingSoon,
                onRestart = { goTo(1) },
                onShare = share
            )
            val view = pager.currentPage - 1

            Column(Modifier.fillMaxSize()) {
                TopBar(view = view, pages = state.pages, points = state.points, onBack = onBack)
                HorizontalPager(
                    state = pager,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.weight(1f),
                    key = { it }
                ) { index ->
                    when (val shown = index - 1) {
                        Reading.COVER -> CoverPage(book, onOpen = { goTo(Reading.TITLE) })

                        Reading.TITLE -> TitlePage(book, state.resumePage, onResume = { goTo(state.resumePage) })

                        state.lastView -> FinalePage(book, state.score!!, state.twin?.second, actions)

                        else -> StoryPage(
                            book = book,
                            view = shown,
                            answer = state.record.answers[shown.toString()],
                            onAnswer = { idx -> viewModel.answer(shown, idx) }
                        )
                    }
                }
                BottomBar(
                    view = view,
                    pages = state.pages,
                    onPrev = { goTo(view - 1) },
                    onNext = { if (view == state.lastView) viewModel.comingSoon() else goTo(view + 1) }
                )
            }
        }
        ToastHost(
            toast,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp, start = 24.dp, end = 24.dp)
        )
    }
}

/** "Muqova", "Sarlavha", "3 / 12 sahifa" or "Tamom". */
private fun label(view: Int, pages: Int) = when {
    view == Reading.COVER -> "Muqova"
    view == Reading.TITLE -> "Sarlavha"
    view > pages -> "Tamom"
    else -> "$view / $pages sahifa"
}

@Composable
private fun TopBar(view: Int, pages: Int, points: Int, onBack: () -> Unit) {
    Surface(color = Color.White.copy(alpha = 0.92f), shadowElevation = 2.dp) {
        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Gray700)
                Spacer(Modifier.width(4.dp))
                Text("Kutubxona", color = Gray700, fontWeight = FontWeight.Bold)
            }
            val progress = if (view > pages) 1f else (view.coerceAtLeast(0).toFloat() / pages).coerceIn(0f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(TrackBg)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Amber500, Brand500)))
                )
            }
            Text(
                label(view, pages),
                style = MaterialTheme.typography.labelMedium,
                color = Brand700,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Orange100)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            PointsChip(points)
        }
    }
}

@Composable
private fun BottomBar(view: Int, pages: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Surface(color = Color.White.copy(alpha = 0.95f), shadowElevation = 6.dp) {
        Row(
            Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onPrev,
                enabled = view > Reading.COVER,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrevBg, contentColor = PrevText),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
                Text("Oldingi", fontWeight = FontWeight.Bold)
            }
            Text(
                if (view in 1..pages) "Sahifa $view / $pages" else label(view, pages),
                style = MaterialTheme.typography.labelMedium,
                color = PrevText
            )
            val (text, icon) = when {
                view == Reading.COVER -> "Kitobni ochish" to Icons.AutoMirrored.Rounded.MenuBook
                view > pages -> "Bilimdon testi" to Icons.Rounded.EmojiEvents
                else -> "Keyingi" to Icons.Rounded.ChevronRight
            }
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Amber500, Brand500)))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
