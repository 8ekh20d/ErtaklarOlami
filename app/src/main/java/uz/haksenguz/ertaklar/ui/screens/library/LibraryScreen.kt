package uz.haksenguz.ertaklar.ui.screens.library

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.core.levels.Levels
import uz.haksenguz.ertaklar.data.models.Category
import uz.haksenguz.ertaklar.ui.components.AssetImage
import uz.haksenguz.ertaklar.ui.components.CoverThumb
import uz.haksenguz.ertaklar.ui.components.ScriptToggle
import uz.haksenguz.ertaklar.ui.components.StarRow
import uz.haksenguz.ertaklar.ui.components.TagChip
import uz.haksenguz.ertaklar.ui.components.ToastHost
import uz.haksenguz.ertaklar.ui.components.parseHue
import uz.haksenguz.ertaklar.ui.components.rememberToastState
import uz.haksenguz.ertaklar.ui.screens.profiles.ProfilesSheet
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.Amber400
import uz.haksenguz.ertaklar.ui.theme.Amber700
import uz.haksenguz.ertaklar.ui.theme.Amber950
import uz.haksenguz.ertaklar.ui.theme.AppBackground
import uz.haksenguz.ertaklar.ui.theme.Brand100
import uz.haksenguz.ertaklar.ui.theme.Brand500
import uz.haksenguz.ertaklar.ui.theme.Brand600
import uz.haksenguz.ertaklar.ui.theme.Brand700
import uz.haksenguz.ertaklar.ui.theme.Emerald500
import uz.haksenguz.ertaklar.ui.theme.Emerald700
import uz.haksenguz.ertaklar.ui.theme.Gray500
import uz.haksenguz.ertaklar.ui.theme.Gray600
import uz.haksenguz.ertaklar.ui.theme.Gray800
import uz.haksenguz.ertaklar.ui.theme.HeroGradient
import uz.haksenguz.ertaklar.ui.theme.Orange100
import uz.haksenguz.ertaklar.ui.theme.Orange200
import uz.haksenguz.ertaklar.ui.theme.Sky100
import uz.haksenguz.ertaklar.ui.theme.Sky800

private val Orange500 = Color(0xFFF97316)
private val EveningGlow = Color(0xFFFDE68A)

/** The library: banner, category tabs, age shelves and the book cards. */
@Composable
fun LibraryScreen(
    onOpenBook: (key: String, resume: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val display = LocalDisplay.current
    val toast = rememberToastState()
    var profilesOpen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val later: () -> Unit = { toast.show(display.ui("Bu bo'lim tez orada qo'shiladi")) }
    val share: () -> Unit = {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, display.ui("Ertaklar Olami — o'zbek ertaklari, rasmli va o'qib beriladigan kitoblar."))
        }
        context.startActivity(Intent.createChooser(send, display.ui("Ulashish")))
    }

    Box(
        modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(Modifier.fillMaxSize()) {
            Header(
                points = state.profile?.points ?: 0,
                avatar = state.profile?.avatar ?: "🦊",
                choice = display.choice,
                onChoice = viewModel::setChoice,
                onProfile = { profilesOpen = true }
            )
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Brand500)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(340.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 16.dp,
                        bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    libraryContent(
                        state = state,
                        showKorean = display.korean,
                        onHero = { state.hero?.let { onOpenBook(it.key, it.continueReading) } },
                        onBedtime = later,
                        onDictionary = later,
                        onPassport = later,
                        onShare = share,
                        onCategory = viewModel::selectCategory,
                        onShelf = viewModel::selectShelf,
                        onBook = { card -> if (card.locked) viewModel.showLocked(card.key) else onOpenBook(card.key, false) }
                    )
                }
            }
        }
        ToastHost(
            toast,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
        )
    }

    if (profilesOpen) ProfilesSheet(onDismiss = { profilesOpen = false })

    state.lockedTwinOf?.let { twin ->
        AlertDialog(
            onDismissRequest = viewModel::dismissLocked,
            confirmButton = { TextButton(onClick = viewModel::dismissLocked) { Text(uiText("Tushunarli")) } },
            title = { Text(uiText("🔒 Hali yopiq")) },
            text = { Text(uiText("Avval «$twin» ertagini oxirigacha o'qing. Shunda uning Koreyadagi egizagi ochiladi!")) },
            containerColor = Color.White
        )
    }
}

private fun LazyGridScope.libraryContent(
    state: LibraryUiState,
    showKorean: Boolean,
    onHero: () -> Unit,
    onBedtime: () -> Unit,
    onDictionary: () -> Unit,
    onPassport: () -> Unit,
    onShare: () -> Unit,
    onCategory: (Category) -> Unit,
    onShelf: (String) -> Unit,
    onBook: (BookCard) -> Unit
) {
    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    state.hero?.let { hero ->
        item(span = full, key = "hero") {
            HeroBanner(hero, state.evening, onHero, onBedtime, onDictionary, onPassport, onShare)
        }
    }
    item(span = full, key = "tabs") { CategoryTabs(state.tabs, state.category, onCategory) }
    item(span = full, key = "shelves") { AgeShelves(state.shelf, state.myShelf, state.profile?.avatar, onShelf) }
    item(span = full, key = "heading") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = Brand500)
            Text(uiText("${state.books.size} ta interaktiv kitob"), style = MaterialTheme.typography.titleLarge, color = Gray800)
        }
    }
    if (state.books.isEmpty()) {
        item(span = full, key = "empty") { EmptyShelf(onShowAll = { onShelf(Levels.ALL) }) }
    }
    items(state.books, key = { it.key }) { card -> BookCardView(card, showKorean, onClick = { onBook(card) }) }
}

@Composable
private fun Header(points: Int, avatar: String, choice: String, onChoice: (String) -> Unit, onProfile: () -> Unit) {
    Surface(color = Color.White.copy(alpha = 0.92f), shadowElevation = 2.dp) {
        Column {
            Row(
                Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Brand500)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Brand500, Amber400))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    uiText("Ertaklar Olami"),
                    style = MaterialTheme.typography.titleLarge.merge(
                        TextStyle(brush = Brush.horizontalGradient(listOf(Brand600, Orange500)))
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                ScriptToggle(choice, onChoice)
                Spacer(Modifier.width(6.dp))
                // Who is reading, and their points; opens "Kim o'qiyapti?".
                Row(
                    Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Orange200, RoundedCornerShape(16.dp))
                        .clickable(onClickLabel = uiText("Kim o'qiyapti?"), onClick = onProfile)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(avatar, fontSize = 24.sp)
                    Text("⭐$points", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Brand700)
                }
            }
            HorizontalDivider(color = Orange100)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroBanner(
    hero: Hero,
    evening: Boolean,
    onOpen: () -> Unit,
    onBedtime: () -> Unit,
    onDictionary: () -> Unit,
    onPassport: () -> Unit,
    onShare: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(HeroGradient)
            .padding(20.dp)
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                uiText("📚 Haqiqiy kitobdek varaqlanadi").uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Text(
                uiText("O'zbek xalq ertaklari, Navoiy dostonlari va sehrli sarguzashtlar"),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                uiText("Kitobni oching, sahifani surib varaqlang, rasmlarga bosib qahramonlarni jonlantiring va savollarga javob bering!"),
                color = Brand100,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Box(
                Modifier
                    .size(176.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .border(4.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onOpen)
            ) {
                AssetImage(hero.cover, Modifier.fillMaxSize(), contentDescription = storyText(hero.title))
            }
            Button(
                onClick = onOpen,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand700),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = Brand500)
                Spacer(Modifier.width(8.dp))
                Text(
                    uiText(if (hero.continueReading) "Davom ettirish" else "Kitobni Ochish"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            hero.resumePage?.let { page ->
                Text(
                    "📖 ${storyText(hero.title)} · $page / ${hero.pages}",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BannerButton("🌙", "Uxlash vaqti", glow = evening, onClick = onBedtime)
                BannerButton("📖", "Mening lug'atim", onClick = onDictionary)
                BannerButton("🗺️", "Pasportim", onClick = onPassport)
                BannerButton(null, "Ulashish", onClick = onShare)
            }
        }
    }
}

@Composable
private fun BannerButton(emoji: String?, label: String, glow: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (glow) 0.35f else 0.2f))
            .then(if (glow) Modifier.border(2.dp, EveningGlow, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (emoji !=
            null
        ) {
            Text(emoji, fontSize = 15.sp)
        } else {
            Icon(Icons.Rounded.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text(uiText(label), color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CategoryTabs(tabs: List<CategoryTab>, selected: Category, onSelect: (Category) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tabs, key = { it.category.id }) { tab ->
            val on = tab.category == selected
            Text(
                "${uiText(tab.category.label)} (${tab.count})",
                color = if (on) Color.White else Gray600,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                modifier = Modifier
                    .then(if (on) Modifier.shadow(2.dp, RoundedCornerShape(12.dp)) else Modifier)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) Brand500 else Color.White)
                    .then(if (on) Modifier else Modifier.border(1.dp, Orange100, RoundedCornerShape(12.dp)))
                    .clickable { onSelect(tab.category) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun AgeShelves(shelf: String, myShelf: String?, avatar: String?, onShelf: (String) -> Unit) {
    val options = listOf(Levels.ALL to "Barcha yoshlar") + Levels.SHELVES.map { it.id to it.label }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item { Text(uiText("🎈 Yoshi:"), color = Gray500, style = MaterialTheme.typography.labelLarge) }
        items(options, key = { it.first }) { (id, label) ->
            val on = id == shelf
            val face = if (id == myShelf && avatar != null) "$avatar " else ""
            Text(
                face + uiText(label),
                color = if (on) Amber950 else Gray600,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                modifier = Modifier
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(if (on) Amber400 else Color.White)
                    .then(if (on) Modifier else Modifier.border(1.dp, Orange100, CircleShape))
                    .clickable { onShelf(id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookCardView(card: BookCard, showKorean: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Orange100, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CoverThumb(card.cover, parseHue(card.hue), locked = card.locked)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (card.genre.isNotEmpty()) TagChip(uiText(card.genre), Orange100, Brand700)
                if (card.ageLabel.isNotEmpty()) TagChip(uiText(card.ageLabel), Sky100, Sky800)
                if (card.isNew) TagChip(uiText("Yangi"), Emerald500, Color.White)
            }
            Text(
                storyText(card.title),
                style = MaterialTheme.typography.titleMedium,
                color = Gray800,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (showKorean && card.koTitle != null) {
                Text(card.koTitle, style = MaterialTheme.typography.labelLarge, color = Gray500)
            }
            if (card.locked && card.twinTitle != null) {
                Text(uiText("🔒 «${card.twinTitle}»ni o'qib tugating"), style = MaterialTheme.typography.labelMedium, color = Sky800)
            } else {
                Text(uiText("📄 ${card.pageCount} sahifali rasmli kitob"), style = MaterialTheme.typography.bodySmall, color = Gray500)
            }
            when {
                card.progressPage != null -> ProgressLine(card.progressPage, card.pageCount)

                card.stars != null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StarRow(card.stars)
                    Text(uiText("O'qildi"), style = MaterialTheme.typography.labelMedium, color = Amber700)
                }
            }
        }
    }
}

@Composable
private fun ProgressLine(page: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(Orange100)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(page.toFloat() / total)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Emerald500)
            )
        }
        Text("📖 $page / $total", style = MaterialTheme.typography.labelSmall, color = Emerald700)
    }
}

@Composable
private fun EmptyShelf(onShowAll: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .border(BorderStroke(1.dp, Orange200), RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(uiText("📚 Bu yoshga mos kitob hali yo'q."), fontWeight = FontWeight.Bold, color = Gray600)
        Button(
            onClick = onShowAll,
            colors = ButtonDefaults.buttonColors(containerColor = Brand500),
            shape = RoundedCornerShape(12.dp)
        ) { Text(uiText("Barcha kitoblarni ko'rsatish")) }
    }
}
