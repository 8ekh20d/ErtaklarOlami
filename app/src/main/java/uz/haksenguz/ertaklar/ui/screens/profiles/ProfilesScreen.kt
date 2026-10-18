package uz.haksenguz.ertaklar.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.haksenguz.ertaklar.core.levels.Levels
import uz.haksenguz.ertaklar.data.models.Profile
import uz.haksenguz.ertaklar.ui.text.storyText
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.Brand500
import uz.haksenguz.ertaklar.ui.theme.Brand700
import uz.haksenguz.ertaklar.ui.theme.FairyRose
import uz.haksenguz.ertaklar.ui.theme.Gray500
import uz.haksenguz.ertaklar.ui.theme.Gray700
import uz.haksenguz.ertaklar.ui.theme.Gray800
import uz.haksenguz.ertaklar.ui.theme.Orange100
import uz.haksenguz.ertaklar.ui.theme.Orange200
import uz.haksenguz.ertaklar.ui.theme.Sky800

private val Selected = Color(0xFFFFF7ED)

/** "Kim o'qiyapti?": the children on this device; choose one, add, change or remove. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(viewModel) { viewModel.closeEvents.collect { dismiss() } }
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
                form == null -> "Kim o'qiyapti?"
                form.id == null -> "Yangi kitobxon"
                else -> "Tahrirlash"
            }
            Text(uiText(title), style = MaterialTheme.typography.headlineSmall, color = Gray800)
            if (form == null) {
                ProfileGrid(state.profiles, state.activeId, onUse = viewModel::use, onEdit = viewModel::edit)
            } else {
                ProfileFormView(
                    form = form,
                    onName = viewModel::setName,
                    onAvatar = viewModel::setAvatar,
                    onAge = viewModel::toggleAge,
                    onSave = viewModel::save,
                    onCancel = viewModel::cancel,
                    onDelete = { viewModel.askDelete(true) }
                )
                if (form.confirmDelete) {
                    AlertDialog(
                        onDismissRequest = { viewModel.askDelete(false) },
                        text = { Text(uiText("${form.name} o'chirilsinmi? Uning ballari va o'qigan kitoblari ham o'chib ketadi.")) },
                        confirmButton = {
                            TextButton(onClick = viewModel::delete) { Text(uiText("Ha, o'chirish"), color = FairyRose) }
                        },
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
private fun ProfileGrid(profiles: List<Profile>, activeId: String?, onUse: (String) -> Unit, onEdit: (String?) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 2
    ) {
        profiles.forEach { p ->
            val on = p.id == activeId
            Box(Modifier.weight(1f)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 128.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) Selected else Color.White)
                        .border(2.dp, if (on) Brand500 else Orange100, RoundedCornerShape(16.dp))
                        .clickable { onUse(p.id) }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    Text(p.avatar, fontSize = 38.sp)
                    Text(storyText(p.name), fontWeight = FontWeight.Bold, color = Gray800, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(uiText("⭐ ${p.points} ball"), style = MaterialTheme.typography.labelMedium, color = Color(0xFFD97706))
                    p.age?.let { Text(uiText(Levels.ageLabel(it)), style = MaterialTheme.typography.labelMedium, color = Sky800) }
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .clickable(onClickLabel = uiText("${p.name}: tahrirlash")) { onEdit(p.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = uiText("${p.name}: tahrirlash"),
                        tint = Gray500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .heightIn(min = 128.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Orange200, RoundedCornerShape(16.dp))
                .clickable { onEdit(null) }
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Text("＋", fontSize = 30.sp, color = Brand700)
            Text(uiText("Bola qo'shish"), fontWeight = FontWeight.Bold, color = Brand700)
        }
        if (profiles.size % 2 == 0) Box(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileFormView(
    form: ProfileForm,
    onName: (String) -> Unit,
    onAvatar: (String) -> Unit,
    onAge: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Label("Ism")
        OutlinedTextField(
            value = form.name,
            onValueChange = onName,
            singleLine = true,
            placeholder = { Text(uiText("Masalan: Asal")) },
            textStyle = MaterialTheme.typography.titleLarge,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand500, unfocusedBorderColor = Orange100),
            modifier = Modifier.fillMaxWidth()
        )
        Label("Rasm tanlang")
        Choices(Profile.AVATARS, form.avatar, onAvatar) { Text(it, fontSize = 30.sp) }
        Label("Yoshi")
        Choices(Levels.AGES, form.age, onAge) {
            Text(if (it >= 11) "11+" else "$it", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gray700)
        }
        Text(uiText("Kutubxona shu yoshga mos kitoblarni ko'rsatadi."), style = MaterialTheme.typography.bodySmall, color = Gray500)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSave,
                enabled = form.name.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand500),
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
        if (form.canDelete) {
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text(uiText("Kitobxonni o'chirish"), color = FairyRose, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(uiText(text), style = MaterialTheme.typography.labelLarge, color = Gray700)
}

/** Four to a row; the chosen one is ringed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choices(items: List<T>, chosen: T?, onPick: (T) -> Unit, content: @Composable (T) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 4
    ) {
        items.forEach { item ->
            val on = item == chosen
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) Selected else Color.White)
                    .border(2.dp, if (on) Brand500 else Orange100, RoundedCornerShape(16.dp))
                    .clickable { onPick(item) },
                contentAlignment = Alignment.Center
            ) { content(item) }
        }
    }
}
