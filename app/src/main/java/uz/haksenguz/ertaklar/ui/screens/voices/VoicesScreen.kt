package uz.haksenguz.ertaklar.ui.screens.voices

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.data.models.Voice
import uz.haksenguz.ertaklar.ui.components.shareFile
import uz.haksenguz.ertaklar.ui.text.LocalDisplay
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.Emerald700
import uz.haksenguz.ertaklar.ui.theme.FairyRose
import uz.haksenguz.ertaklar.ui.theme.Gray500
import uz.haksenguz.ertaklar.ui.theme.Gray700
import uz.haksenguz.ertaklar.ui.theme.Gray800
import uz.haksenguz.ertaklar.ui.theme.Orange100

private val Rose = Color(0xFFF43F5E)
private val RoseBorder = Color(0xFFFDA4AF)
private val RoseBg = Color(0xFFFFF1F2)
private val SkyBg = Color(0xFFF0F9FF)
private val SkyBorder = Color(0xFFE0F2FE)
private val Sky = Color(0xFF0EA5E9)
private val SkyText = Color(0xFF0C4A6E)

/** "Kimning ovozi?": choose who is reading, add a voice, or add one sent as a file. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesSheet(
    currentId: String?,
    onUse: (Voice) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoicesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val display = LocalDisplay.current
    val use by rememberUpdatedState(onUse)
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { e ->
            when (e) {
                is VoicesEvent.Use -> use(e.voice)
                is VoicesEvent.Share -> context.shareFile(e.file, "application/json", e.text, display.ui("Yuborish"))
            }
        }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::import) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val form = state.form
            val title = when {
                form == null -> "Kimning ovozi?"
                form.id == null -> "Yangi ovoz"
                else -> "Ovozni tahrirlash"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(uiText(title), style = MaterialTheme.typography.headlineSmall, color = Gray800, modifier = Modifier.weight(1f))
                if (state.busy) CircularProgressIndicator(Modifier.size(24.dp), color = Rose, strokeWidth = 3.dp)
            }
            if (form == null) {
                VoiceGrid(state.voices, currentId, onUse = viewModel::use, onEdit = viewModel::edit)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SkyBg)
                        .border(1.dp, SkyBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { pickFile.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Sky),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(uiText("Fayldan qo'shish"), fontWeight = FontWeight.Bold)
                    }
                    Text(
                        uiText(
                            "Buvijon yoki bobojon Telegram orqali yuborgan ovoz faylini (.json) telefoningizga saqlang, keyin shu tugma bilan tanlang."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyText
                    )
                }
                state.note?.let { Text(uiText(it), fontWeight = FontWeight.Bold, color = Emerald700) }
            } else {
                VoiceFormView(
                    form = form,
                    onName = viewModel::setName,
                    onAvatar = viewModel::setAvatar,
                    onSave = viewModel::save,
                    onCancel = viewModel::cancel,
                    onSendAll = viewModel::sendAll,
                    onDelete = { viewModel.askDelete(true) }
                )
                if (form.confirmDelete) {
                    AlertDialog(
                        onDismissRequest = { viewModel.askDelete(false) },
                        text = { Text(uiText("${form.name} ovozi va uning barcha yozuvlari o'chirilsinmi?")) },
                        confirmButton = { TextButton(onClick = viewModel::delete) { Text(uiText("Ha, o'chirish"), color = FairyRose) } },
                        dismissButton = { TextButton(onClick = { viewModel.askDelete(false) }) { Text(uiText("Bekor qilish")) } },
                        containerColor = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceGrid(voices: List<Voice>, currentId: String?, onUse: (Voice) -> Unit, onEdit: (Voice?) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 2
    ) {
        voices.forEach { v ->
            val on = v.id == currentId
            Box(Modifier.weight(1f)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 104.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) RoseBg else Color.White)
                        .border(2.dp, if (on) RoseBorder else Orange100, RoundedCornerShape(16.dp))
                        .clickable { onUse(v) }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    Text(v.avatar, fontSize = 36.sp)
                    Text(storyText(v.name), fontWeight = FontWeight.Bold, color = Gray800, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .clickable { onEdit(v) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = uiText("${v.name}: tahrirlash"),
                        tint = Gray500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .heightIn(min = 104.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, RoseBorder, RoundedCornerShape(16.dp))
                .clickable { onEdit(null) }
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Text("＋", fontSize = 30.sp, color = Rose)
            Text(uiText("Yangi ovoz"), fontWeight = FontWeight.Bold, color = Rose)
        }
        if (voices.size % 2 == 0) Box(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceFormView(
    form: VoiceForm,
    onName: (String) -> Unit,
    onAvatar: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onSendAll: () -> Unit,
    onDelete: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(uiText("Kim o'qiydi?"), style = MaterialTheme.typography.labelLarge, color = Gray700)
        OutlinedTextField(
            value = form.name,
            onValueChange = onName,
            singleLine = true,
            placeholder = { Text(uiText("Masalan: Buvijon")) },
            textStyle = MaterialTheme.typography.titleLarge,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Rose, unfocusedBorderColor = Orange100),
            modifier = Modifier.fillMaxWidth()
        )
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 4
        ) {
            Voice.AVATARS.forEach { a ->
                val on = a == form.avatar
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) RoseBg else Color.White)
                        .border(2.dp, if (on) RoseBorder else Orange100, RoundedCornerShape(16.dp))
                        .clickable { onAvatar(a) },
                    contentAlignment = Alignment.Center
                ) { Text(a, fontSize = 30.sp) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSave,
                enabled = form.name.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rose),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) { Text(uiText("Saqlash"), fontWeight = FontWeight.Bold) }
            Button(
                onClick = onCancel,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Gray700),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(uiText("Bekor qilish"), fontWeight = FontWeight.Bold) }
        }
        if (form.id != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onSendAll) {
                    Icon(Icons.Rounded.Share, contentDescription = null, tint = Sky, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(uiText("Barcha yozuvlarni yuborish"), color = Sky, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = FairyRose, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(uiText("Ovozni o'chirish"), color = FairyRose, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
