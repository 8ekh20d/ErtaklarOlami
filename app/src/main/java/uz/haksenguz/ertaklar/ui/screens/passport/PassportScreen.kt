package uz.haksenguz.ertaklar.ui.screens.passport

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.ui.components.EmptyStamp
import uz.haksenguz.ertaklar.ui.components.StampImage
import uz.haksenguz.ertaklar.ui.components.StickerImage
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

internal object Pass {
    val Brown = Color(0xFF5B3A1A)
    val Title = Color(0xFF7C2D12)
    val Hint = Color(0xFF8A6D45)
    val SlotBg = Color(0xFFFFFAF0)
    val SlotRing = Color(0xFFF6E7C8)
    val Meta = Color(0xFFA16207)
    val KoName = Color(0xFF6B7280)
    val AlbumRing = Color(0xFFECDCB8)
    val AlbumBg = Color(0xFFFFFDF7)
    val MineRing = Color(0xFFFDE68A)
    val MineBg = Color(0xFFFFFBEB)
    val Small = Color(0xFF9A7B4F)
    val AmberChip = Color(0xFFFEF3C7)
    val AmberText = Color(0xFF92400E)
    val SkyChip = Color(0xFFE0F2FE)
    val SkyText = Color(0xFF075985)
    val BackBg = Color(0xFFF3F4F6)
}

/** "🗺️ Mening pasportim": the map, a stamp (or an empty place) for each place, and the sticker album. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PassportScreen(
    onBack: () -> Unit,
    onOpenBook: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PassportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val display = LocalDisplay.current
    val toast = rememberToastState()
    val open by rememberUpdatedState(onOpenBook)
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { e ->
            when (e) {
                is PassportViewModel.Event.Toast -> toast.show(display.ui(e.text))
                is PassportViewModel.Event.OpenBook -> open(e.key)
            }
        }
    }
    val s = state.snapshot
    val map = state.map
    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

    Box(
        modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        if (s != null && map != null) {
            val p = s.passport
            LazyVerticalGrid(
                columns = GridCells.Adaptive(112.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                item(span = full) { Header(s.stamps.size, p.regions.size, s.stickers.size, p.stickers.size, onBack) }
                item(span = full) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White)
                            .border(1.dp, Orange100, RoundedCornerShape(28.dp))
                            .padding(12.dp)
                    ) {
                        UzMap(
                            map = map,
                            regions = p.regions,
                            countries = p.file.names,
                            stamped = s.stamps.keys,
                            selected = state.selected,
                            ko = display.korean,
                            text = display::content,
                            onRegion = viewModel::select
                        )
                        Text(
                            uiText("👆 Viloyatni bosing: u yerning muhri, kitoblari va stikerlari ochiladi."),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Pass.Hint,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
                        )
                    }
                }
                item(span = full) { SectionTitle(uiText("🛂 Muhrlar")) }
                item(span = full) {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 3
                    ) {
                        p.regions.forEach { r ->
                            val have = r.stickers.count { it.id in s.stickers }
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Pass.SlotBg)
                                    .border(2.dp, Pass.SlotRing, RoundedCornerShape(22.dp))
                                    .clickable(role = Role.Button) { viewModel.select(r.id) }
                                    .padding(horizontal = 4.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val stamp = s.stamps[r.id]
                                if (stamp != null) {
                                    StampImage(r, Modifier.size(96.dp), date = stamp)
                                } else {
                                    EmptyStamp(r, Modifier.size(96.dp))
                                }
                                Text(
                                    storyText(r.name),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    color = Pass.Brown,
                                    textAlign = TextAlign.Center
                                )
                                Text(r.ko, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Pass.KoName)
                                Text(
                                    "📚 ${p.booksOf(s.books, r.id).size} · 🎁 $have/${r.stickers.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Pass.Meta
                                )
                            }
                        }
                        // keep the last row's slots the same width
                        repeat((3 - p.regions.size % 3) % 3) { Spacer(Modifier.weight(1f)) }
                    }
                }
                item(span = full) {
                    Column {
                        SectionTitle(uiText("🎁 Stikerlar albomi"))
                        Text(
                            uiText("Har bir stiker — ${p.price} ball. Viloyat stikerlari uning muhri bosilgach ochiladi."),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Pass.Hint,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                items(p.stickers, key = { it.first.id }) { (sticker, region) ->
                    val mine = sticker.id in s.stickers
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (mine) Pass.MineBg else Pass.AlbumBg)
                            .border(2.dp, if (mine) Pass.MineRing else Pass.AlbumRing, RoundedCornerShape(18.dp))
                            .clickable(role = Role.Button) { viewModel.select(region.id) }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StickerImage(sticker.image, Modifier.size(84.dp), faded = if (mine) 0f else 1f)
                        Text(
                            storyText(sticker.name),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Pass.Brown,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            if (display.korean) sticker.ko else storyText(region.name),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Pass.Small,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                item(span = full) { Spacer(Modifier.navigationBarsPadding()) }
            }
            state.selected?.let { id ->
                p.region(id)?.let { r ->
                    RegionSheet(
                        region = r,
                        snapshot = s,
                        fresh = state.fresh,
                        onBuy = viewModel::buy,
                        onBook = viewModel::openBook,
                        onDismiss = { viewModel.select(null) },
                        toast = toast
                    )
                }
            }
        }
        state.lockedTwinOf?.let { twin ->
            AlertDialog(
                onDismissRequest = viewModel::dismissLocked,
                confirmButton = { TextButton(onClick = viewModel::dismissLocked) { Text(uiText("Tushunarli")) } },
                title = { Text(uiText("🔒 Hali yopiq")) },
                text = { Text(uiText("Avval «$twin» ertagini oxirigacha o'qing. Shunda uning Koreyadagi egizagi ochiladi!")) },
                containerColor = Color.White
            )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(stamped: Int, places: Int, owned: Int, stickers: Int, onBack: () -> Unit) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, Orange100, RoundedCornerShape(24.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Pass.BackBg)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Gray700, modifier = Modifier.size(18.dp))
            Text(uiText("Kutubxona"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Gray700)
        }
        Text(uiText("🗺️ Mening pasportim"), fontFamily = HeadingFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Gray800)
        Chip(uiText("$stamped / $places muhr"), Pass.AmberChip, Pass.AmberText)
        Chip(uiText("$owned / $stickers stiker"), Pass.SkyChip, Pass.SkyText)
    }
}

@Composable
private fun Chip(text: String, bg: Color, color: Color) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = color,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Pass.Title,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}
