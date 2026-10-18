package uz.haksenguz.ertaklar.ui.screens.studio

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.ui.components.AssetImage
import uz.haksenguz.ertaklar.ui.components.shareFile
import uz.haksenguz.ertaklar.ui.screens.voices.VoicesSheet
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.Amber700
import uz.haksenguz.ertaklar.ui.theme.AppBackground
import uz.haksenguz.ertaklar.ui.theme.Gray500
import uz.haksenguz.ertaklar.ui.theme.Gray600
import uz.haksenguz.ertaklar.ui.theme.Gray700
import uz.haksenguz.ertaklar.ui.theme.Gray800
import uz.haksenguz.ertaklar.ui.theme.Orange100

private val Rose = Color(0xFFF43F5E)
private val RoseDark = Color(0xFFE11D48)
private val RoseRing = Color(0x33F43F5E)
private val RoseBorder = Color(0xFFFECDD3)
private val Reading = Color(0xFFFEF3C7)
private val DotDone = Color(0xFF10B981)
private val DotIdle = Color(0xFFF3F4F6)
private val Amber = Color(0xFFFEF3C7)
private val AmberText = Color(0xFF78350F)
private val EmeraldBg = Color(0xFFD1FAE5)
private val EmeraldText = Color(0xFF065F46)
private val Sky = Color(0xFF0EA5E9)

/** Ovoz yozish: record a book aloud page by page, then send it as one file. */
@Composable
fun StudioScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: StudioViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val display = LocalDisplay.current
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { e ->
            when (e) {
                is StudioEvent.Share -> context.shareFile(e.file, "application/json", e.text, display.ui("Yuborish"))
            }
        }
    }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.toggleRecord() else viewModel.micDenied()
    }
    val record = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted || state.recording || state.voice == null) viewModel.toggleRecord() else askMic.launch(Manifest.permission.RECORD_AUDIO)
    }
    val book = state.book

    Column(
        modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Surface(color = Color.White.copy(alpha = 0.92f), shadowElevation = 2.dp) {
            Row(
                Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onBack, enabled = !state.recording) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = uiText("Kitobga qaytish"), tint = Gray700)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiText("🎙️ Ovoz yozish").uppercase(), style = MaterialTheme.typography.labelSmall, color = RoseDark)
                    Text(
                        storyText(book?.title.orEmpty()),
                        style = MaterialTheme.typography.titleMedium,
                        color = Gray800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, RoseBorder, RoundedCornerShape(16.dp))
                        .clickable(onClickLabel = uiText("Kimning ovozi?")) { viewModel.openVoices(true) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(state.voice?.avatar ?: "🎙️", fontSize = 24.sp)
                    Text(
                        state.voice?.let { storyText(it.name) } ?: uiText("Ovoz tanlang"),
                        style = MaterialTheme.typography.labelLarge,
                        color = Gray700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 96.dp)
                    )
                }
            }
        }
        if (book == null) return@Column
        val page = book.pages[state.page - 1]
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(1.dp, Orange100, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AssetImage(
                        page.image,
                        Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(uiText("${state.page} / ${state.pages} sahifa"), style = MaterialTheme.typography.labelLarge, color = Amber700)
                        Text(
                            uiText("Sarlavhadan boshlab o'qing. Har gapdan keyin biroz to'xtang — bolalar o'qiyotgan gapni ko'rib boradi."),
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                }
                Segments(state.segments, state.highlight)
                RecordArea(state, onRecord = record, onPreview = viewModel::preview, onDelete = viewModel::askDelete)
                PageNav(state, onGo = viewModel::goTo)
            }
            SendBar(state, onSend = viewModel::send)
        }
    }

    if (state.voicesOpen) {
        VoicesSheet(currentId = state.voice?.id, onUse = viewModel::useVoice, onDismiss = { viewModel.openVoices(false) })
    }
    state.confirm?.let { what ->
        AlertDialog(
            onDismissRequest = { viewModel.confirm(false) },
            text = {
                Text(
                    uiText(
                        when (what) {
                            StudioConfirm.Rerecord -> "Bu sahifa yozilgan. Qaytadan yozasizmi?"
                            StudioConfirm.Delete -> "${state.page}-sahifadagi yozuv o'chirilsinmi?"
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirm(true) }) {
                    Text(uiText(if (what == StudioConfirm.Delete) "Ha, o'chirish" else "Ha"), color = Rose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.confirm(false) }) { Text(uiText("Bekor qilish")) } },
            containerColor = Color.White
        )
    }
}

/** The page's title and numbered sentences; the one being played back is lit up. */
@Composable
private fun Segments(segments: List<String>, highlight: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        segments.forEachIndexed { i, text ->
            val on = i == highlight
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) Reading else Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (i == 0) {
                    Text(storyText(text), style = MaterialTheme.typography.titleLarge, color = AmberText)
                } else {
                    Text(
                        "$i",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Rose.copy(alpha = 0.75f))
                    )
                    Text(storyText(text), style = MaterialTheme.typography.bodyLarge, color = Gray800)
                }
            }
        }
    }
}

@Composable
private fun RecordArea(state: StudioUiState, onRecord: () -> Unit, onPreview: () -> Unit, onDelete: () -> Unit) {
    val ring by animateFloatAsState(1f + state.level * 0.7f, label = "ring")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            Box(
                Modifier
                    .size(84.dp)
                    .scale(if (state.recording) ring else 1f)
                    .clip(CircleShape)
                    .background(RoseRing)
            )
            Box(
                Modifier
                    .size(84.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        if (state.busy || state.voice == null) {
                            Rose.copy(alpha = 0.5f)
                        } else if (state.recording) {
                            RoseDark
                        } else {
                            Rose
                        }
                    )
                    .clickable(
                        enabled = !state.busy,
                        onClickLabel = uiText(if (state.recording) "Yozishni to'xtatish" else "Yozishni boshlash"),
                        onClick = onRecord
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.recording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        val status = when {
            state.recording -> "🔴 ${state.seconds / 60}:${(state.seconds % 60).toString().padStart(2, '0')} — o'qing..."
            state.status != null -> state.status
            state.voice == null -> "Avval ovozni tanlang"
            state.hasRecording -> "✓ Bu sahifa yozilgan"
            else -> "Qizil tugmani bosing va sahifani o'qing"
        }
        Text(uiText(status), style = MaterialTheme.typography.labelLarge, color = Gray600, textAlign = TextAlign.Center)
        if (state.hasRecording && !state.recording && !state.busy) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPreview,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldBg, contentColor = EmeraldText)
                ) {
                    Icon(
                        if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(uiText(if (state.playing) "To'xtatish" else "Tinglab ko'rish"), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onDelete,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DotIdle, contentColor = Gray600)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(uiText("O'chirish"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PageNav(state: StudioUiState, onGo: (Int) -> Unit) {
    val locked = state.recording || state.busy
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NavButton(Icons.Rounded.ChevronLeft, uiText("Oldingi"), enabled = !locked && state.page > 1) { onGo(state.page - 1) }
        FlowRow(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            (1..state.pages).forEach { k ->
                val done = k in state.recorded
                val current = k == state.page
                Text(
                    "$k",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (done) Color.White else Gray700,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (done) DotDone else DotIdle)
                        .border(if (current) 2.dp else 0.dp, if (current) Rose else Color.Transparent, CircleShape)
                        .clickable(enabled = !locked, onClickLabel = uiText(if (done) "$k-sahifa (yozilgan)" else "$k-sahifa")) { onGo(k) }
                        .padding(top = 5.dp)
                )
            }
        }
        NavButton(Icons.Rounded.ChevronRight, uiText("Keyingi"), enabled = !locked && state.page < state.pages) { onGo(state.page + 1) }
    }
}

@Composable
private fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Amber else Amber.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = description, tint = AmberText.copy(alpha = if (enabled) 1f else 0.4f)) }
}

@Composable
private fun SendBar(state: StudioUiState, onSend: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .border(1.dp, Orange100, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                uiText("${state.recorded.size} / ${state.pages} sahifa yozildi"),
                style = MaterialTheme.typography.labelLarge,
                color = Gray700
            )
            Text(
                uiText("Tayyor bo'lgach, bitta fayl qilib Telegram orqali yuboring."),
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
        Button(
            onClick = onSend,
            enabled = state.recorded.isNotEmpty() && !state.recording && !state.busy,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Sky),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(uiText("Yuborish"), fontWeight = FontWeight.Bold)
        }
    }
}
