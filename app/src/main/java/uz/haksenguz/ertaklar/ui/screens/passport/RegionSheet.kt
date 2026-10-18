package uz.haksenguz.ertaklar.ui.screens.passport

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.haksenguz.ertaklar.core.passport.Passport.StickerState
import uz.haksenguz.ertaklar.data.models.Region
import uz.haksenguz.ertaklar.data.repository.PassportRepository
import uz.haksenguz.ertaklar.ui.components.AssetImage
import uz.haksenguz.ertaklar.ui.components.EmptyStamp
import uz.haksenguz.ertaklar.ui.components.StampImage
import uz.haksenguz.ertaklar.ui.components.StickerImage
import uz.haksenguz.ertaklar.ui.components.ToastHost
import uz.haksenguz.ertaklar.ui.components.ToastState
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.HeadingFont

private val CityText = Color(0xFF92400E)
private val FactText = Color(0xFF3F3A33)
private val FactKo = Color(0xFF1E40AF)
private val BookRing = Color(0xFFFDE7C4)
private val BookTitle = Color(0xFF3F2A14)
private val Status = Color(0xFF8A7A62)
private val Done = Color(0xFF15803D)
private val Reading = Color(0xFFB45309)
private val BuyFrom = Color(0xFFF59E0B)
private val BuyTo = Color(0xFFEA580C)
private val BuyEdge = Color(0xFF9A3412)
private val BuyOff = Color(0xFFE7DCC8)
private val NoteBg = Color(0xFFFEF3C7)
private val Sheet = Color(0xFFFFFDF7)

/** A place's card: its stamp, a fact, the books that go there and its two stickers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSheet(
    region: Region,
    snapshot: PassportRepository.Snapshot,
    fresh: String?,
    onBuy: (String) -> Unit,
    onBook: (String) -> Unit,
    onDismiss: () -> Unit,
    toast: ToastState,
    modifier: Modifier = Modifier
) {
    val display = LocalDisplay.current
    val p = snapshot.passport
    val stamp = snapshot.stamps[region.id]
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Sheet,
        modifier = modifier
    ) {
        // The sheet sits above the screen, so its messages show here.
        Box {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        storyText(region.name),
                        fontFamily = HeadingFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Pass.Title
                    )
                    Text(
                        region.ko,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pass.KoName,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val stampSize = Modifier.size(108.dp)
                    if (stamp != null) StampImage(region, stampSize, date = stamp) else EmptyStamp(region, stampSize)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val (label, city, koCity) = when {
                            region.capital -> Triple("📍 Poytaxti:", region.city, region.koCity)
                            region.city != null -> Triple("📍 Markazi:", region.city, region.koCity)
                            else -> Triple("📍 O'zbekistonning poytaxti", null, null)
                        }
                        Text(
                            uiText(label) +
                                (city?.let { " " + storyText(it) + if (display.korean && koCity != null) " ($koCity)" else "" } ?: ""),
                            fontWeight = FontWeight.ExtraBold,
                            color = CityText
                        )
                        Text(storyText(region.fact), fontSize = 15.sp, lineHeight = 23.sp, color = FactText)
                        if (display.korean) Text(region.koFact, fontSize = 14.sp, lineHeight = 21.sp, color = FactKo)
                    }
                }
                Heading(uiText("📚 Bu yerga olib boradigan kitoblar"))
                p.booksOf(snapshot.books, region.id).forEach { book ->
                    val rec = snapshot.records[book.key]
                    val twin = book.twin?.let { t -> snapshot.books.firstOrNull { it.key == t } }
                    val lockedTwin = twin?.takeIf { snapshot.records[it.key]?.finished != true }
                    val (status, color) = when {
                        lockedTwin != null -> uiText("🔒 «${lockedTwin.title}»ni o'qib tugating") to Status
                        rec?.finished == true -> uiText("✓ O'qildi") to Done
                        rec != null && rec.page > 0 -> "📖 ${rec.page} / ${book.pages.size}" to Reading
                        else -> uiText("Hali o'qilmagan") to Status
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(2.dp, BookRing, RoundedCornerShape(16.dp))
                            .clickable(role = Role.Button) { onBook(book.key) }
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AssetImage(
                            book.cover,
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column {
                            Text(storyText(book.title), fontWeight = FontWeight.Bold, color = BookTitle)
                            Text(status, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                    }
                }
                Heading(uiText("🎁 Stikerlar"))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    region.stickers.forEach { sticker ->
                        val state = p.stickerState(sticker.id, snapshot.stamps, snapshot.stickers, snapshot.profile.points)
                        val mine = state == StickerState.HAVE
                        val pop = remember(sticker.id) { Animatable(1f) }
                        LaunchedEffect(fresh) {
                            if (fresh == sticker.id) {
                                pop.snapTo(0.4f)
                                pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Pass.SlotBg)
                                .border(2.dp, Pass.SlotRing, RoundedCornerShape(18.dp))
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StickerImage(
                                sticker.image,
                                Modifier
                                    .size(104.dp)
                                    .graphicsLayer {
                                        scaleX = pop.value
                                        scaleY = pop.value
                                        rotationZ = (1f - pop.value) * -20f
                                    },
                                faded = if (mine) 0f else 0.85f
                            )
                            Text(
                                storyText(sticker.name),
                                fontWeight = FontWeight.Bold,
                                color = Pass.Brown,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Text(sticker.ko, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Pass.KoName)
                            if (mine) {
                                Text(uiText("✓ Sizniki"), fontWeight = FontWeight.ExtraBold, color = Done)
                            } else {
                                BuyButton(p.price, enabled = state == StickerState.OK, onClick = { onBuy(sticker.id) })
                                if (state == StickerState.POINTS) {
                                    Text(
                                        uiText("Yana ${p.price - snapshot.profile.points} ball kerak"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Status
                                    )
                                }
                            }
                        }
                    }
                }
                if (stamp == null) {
                    Text(
                        uiText("🔒 Stikerlar muhr bosilgach ochiladi: bu yerga olib boradigan kitobni oxirigacha o'qing!"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pass.AmberText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NoteBg)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
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
}

@Composable
private fun Heading(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Pass.Title, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun BuyButton(price: Int, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (enabled) BuyEdge else BuyOff)
            .padding(bottom = if (enabled) 3.dp else 0.dp)
            .clip(shape)
            .background(if (enabled) Brush.linearGradient(listOf(BuyFrom, BuyTo)) else Brush.linearGradient(listOf(BuyOff, BuyOff)))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val color = if (enabled) Color.White else Status
        Text(uiText("🎁 Olish"), fontWeight = FontWeight.ExtraBold, color = color)
        Text(uiText("⭐ $price ball"), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}
