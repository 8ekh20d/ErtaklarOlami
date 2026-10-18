package uz.haksenguz.ertaklar.ui.screens.reader

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import uz.haksenguz.ertaklar.ui.components.GrownUpsGateDialog
import uz.haksenguz.ertaklar.ui.components.PointsChip
import uz.haksenguz.ertaklar.ui.components.ToastHost
import uz.haksenguz.ertaklar.ui.components.rememberToastState
import uz.haksenguz.ertaklar.ui.screens.reader.pages.CoverPage
import uz.haksenguz.ertaklar.ui.screens.reader.pages.FinaleActions
import uz.haksenguz.ertaklar.ui.screens.reader.pages.FinalePage
import uz.haksenguz.ertaklar.ui.screens.reader.pages.PageHelpers
import uz.haksenguz.ertaklar.ui.screens.reader.pages.StoryPage
import uz.haksenguz.ertaklar.ui.screens.reader.pages.TitlePage
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.Amber500
import uz.haksenguz.ertaklar.ui.theme.AppBackground
import uz.haksenguz.ertaklar.ui.theme.Brand500
import uz.haksenguz.ertaklar.ui.theme.Brand700
import uz.haksenguz.ertaklar.ui.theme.GowunDodum
import uz.haksenguz.ertaklar.ui.theme.Gray700
import uz.haksenguz.ertaklar.ui.theme.Gray800
import uz.haksenguz.ertaklar.ui.theme.HeadingFont
import uz.haksenguz.ertaklar.ui.theme.Orange100

private val PrevBg = Color(0xFFFEF3C7)
private val PrevText = Color(0xFF78350F)
private val TrackBg = Color(0xFFE5E7EB)
private val GlossBorder = Color(0xFF6EE7B7)
private val GlossTerm = Color(0xFF065F46)
private val GlossSay = Color(0xFFD1FAE5)
private val StudioBg = Color(0xFFFFF1F2)
private val StudioText = Color(0xFFBE123C)

/**
 * The book reader. Pages turn with a swipe or the buttons below:
 * cover, title page, the story pages, then the end.
 */
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onOpenBook: (String) -> Unit,
    onOpenQuiz: (String) -> Unit,
    onOpenStudio: (key: String, page: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val display = LocalDisplay.current
    val toast = rememberToastState()
    val openQuiz by rememberUpdatedState { state.book?.key?.let(onOpenQuiz) }
    val openStudio by rememberUpdatedState { page: Int -> state.book?.key?.let { onOpenStudio(it, page) } }
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ReaderEvent.Toast -> toast.show(display.ui(event.text))
                ReaderEvent.OpenQuiz -> openQuiz()
                is ReaderEvent.OpenStudio -> openStudio(event.page)
            }
        }
    }
    BackHandler(enabled = state.glossCard != null) { viewModel.closeGloss() }
    val gateOpen by viewModel.gateOpen.collectAsStateWithLifecycle()
    if (gateOpen) GrownUpsGateDialog(onPass = viewModel::gatePassed, onDismiss = { viewModel.gateOpen.value = false })

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
                    putExtra(Intent.EXTRA_TEXT, display.ui("«${book.title}» — o'zbek ertagi: rasmli, varaqlanadigan kitob"))
                }
                context.startActivity(Intent.createChooser(send, display.ui("Ulashish")))
            }
            val actions = FinaleActions(
                onTwin = { state.twin?.let { onOpenBook(it.first) } },
                onCompare = viewModel::comingSoon,
                onQuiz = viewModel::openQuiz,
                onOrder = viewModel::comingSoon,
                onRestart = { goTo(1) },
                onShare = share
            )
            val view = pager.currentPage - 1

            Column(Modifier.fillMaxSize()) {
                TopBar(view = view, pages = state.pages, points = state.points, onBack = onBack, onStudio = viewModel::openStudio)
                HorizontalPager(
                    state = pager,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.weight(1f),
                    key = { it }
                ) { index ->
                    when (val shown = index - 1) {
                        Reading.COVER -> CoverPage(book, onOpen = { goTo(Reading.TITLE) })

                        Reading.TITLE -> TitlePage(book, state.resumePage, onResume = { goTo(state.resumePage) })

                        state.lastView -> FinalePage(
                            book = book,
                            score = state.score!!,
                            twinTitle = state.twin?.second,
                            actions = actions,
                            koMoral = state.korean?.moral?.takeIf { it.isNotEmpty() },
                            koShown = state.koView == shown,
                            onToggleKorean = { viewModel.toggleKorean(shown) }
                        )

                        else -> {
                            val koShown = state.koView == shown
                            StoryPage(
                                book = book,
                                view = shown,
                                answer = state.record.answers[shown.toString()],
                                helpers = PageHelpers(
                                    korean = state.korean,
                                    gloss = state.gloss,
                                    koShown = koShown,
                                    readsOut = state.reading && !koShown,
                                    hasKorean = state.hasKorean(shown),
                                    reading = state.reading,
                                    onToggleKorean = { viewModel.toggleKorean(shown) },
                                    onToggleReading = viewModel::toggleReading,
                                    onGloss = viewModel::openGloss
                                ),
                                onAnswer = { idx -> viewModel.answer(shown, idx) }
                            )
                        }
                    }
                }
                BottomBar(
                    view = view,
                    pages = state.pages,
                    onPrev = { goTo(view - 1) },
                    onNext = { if (view == state.lastView) viewModel.openQuiz() else goTo(view + 1) }
                )
            }
        }
        AnimatedVisibility(
            visible = state.glossCard != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 84.dp, start = 16.dp, end = 16.dp)
        ) {
            state.glossCard?.let { (term, meaning) ->
                GlossCard(term, meaning, onSay = viewModel::sayGloss, onClose = viewModel::closeGloss)
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

/** A tapped word: its Korean meaning, which the phone's Korean voice can say. */
@Composable
private fun GlossCard(term: String, meaning: String, onSay: () -> Unit, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, GlossBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🇰🇷", fontSize = 22.sp)
        Column(Modifier.weight(1f)) {
            Text(storyText(term), fontFamily = HeadingFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = GlossTerm)
            Text(meaning, fontFamily = GowunDodum, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp, color = Gray800)
        }
        FilledTonalIconButton(
            onClick = onSay,
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = GlossSay, contentColor = GlossTerm),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = uiText("Koreyschasini eshitish"))
        }
        FilledTonalIconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = uiText("Yopish"))
        }
    }
}

/** "Muqova", "Sarlavha", "3 / 12 sahifa" or "Tamom" (in Uzbek; shown through the display). */
private fun label(view: Int, pages: Int) = when {
    view == Reading.COVER -> "Muqova"
    view == Reading.TITLE -> "Sarlavha"
    view > pages -> "Tamom"
    else -> "$view / $pages sahifa"
}

@Composable
private fun TopBar(view: Int, pages: Int, points: Int, onBack: () -> Unit, onStudio: () -> Unit) {
    Surface(color = Color.White.copy(alpha = 0.92f), shadowElevation = 2.dp) {
        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back to the library; on a phone only the arrow fits next to the progress.
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = uiText("Kutubxona"), tint = Gray700)
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
                uiText(label(view, pages)),
                style = MaterialTheme.typography.labelMedium,
                color = Brand700,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Orange100)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            // Ovoz yozish: for grown-ups (behind a small sum).
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioBg)
                    .clickable(onClickLabel = uiText("Ovoz yozish (kattalar uchun)"), onClick = onStudio),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Mic,
                    contentDescription = uiText("Ovoz yozish (kattalar uchun)"),
                    tint = StudioText,
                    modifier = Modifier.size(20.dp)
                )
            }
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
                Text(uiText("Oldingi"), fontWeight = FontWeight.Bold)
            }
            Text(
                uiText(if (view in 1..pages) "Sahifa $view / $pages" else label(view, pages)),
                style = MaterialTheme.typography.labelMedium,
                color = PrevText
            )
            val (text, icon) = when {
                view == Reading.COVER -> "Kitobni ochish" to Icons.AutoMirrored.Rounded.MenuBook
                view > pages -> "Bilimdon Testi" to Icons.Rounded.EmojiEvents
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
                    Text(uiText(text), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
