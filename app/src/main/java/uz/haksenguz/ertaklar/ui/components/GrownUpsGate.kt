package uz.haksenguz.ertaklar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import uz.haksenguz.ertaklar.ui.text.uiText
import uz.haksenguz.ertaklar.ui.theme.Gray600
import uz.haksenguz.ertaklar.ui.theme.Gray800

private val Rose = Color(0xFFE11D48)
private val RoseBg = Color(0xFFFFE4E6)

/** "Kattalar uchun": a small sum keeps young children out of the recording studio. */
@Composable
fun GrownUpsGateDialog(onPass: () -> Unit, onDismiss: () -> Unit) {
    fun newSum() = (3 + Random.nextInt(7)) to (3 + Random.nextInt(7))
    var sum by remember { mutableStateOf(newSum()) }
    var answer by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf<String?>(null) }
    val check = {
        if (answer.trim().toIntOrNull() == sum.first * sum.second) {
            onPass()
        } else {
            hint = "Yana bir urinib ko'ring"
            sum = newSum()
            answer = ""
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        icon = {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(RoseBg),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Shield, contentDescription = null, tint = Rose) }
        },
        title = { Text(uiText("Kattalar uchun"), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(uiText("Ovoz yozish bo'limiga kirish uchun hisoblang:"), color = Gray600, textAlign = TextAlign.Center)
                Text("${sum.first} × ${sum.second} = ?", style = MaterialTheme.typography.headlineMedium, color = Gray800)
                OutlinedTextField(
                    value = answer,
                    onValueChange = { v -> answer = v.filter { it.isDigit() }.take(3) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { check() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Rose),
                    modifier = Modifier.width(140.dp)
                )
                hint?.let { Text(uiText(it), color = Rose, fontWeight = FontWeight.Bold) }
            }
        },
        confirmButton = { TextButton(onClick = check) { Text(uiText("Kirish"), color = Rose, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(uiText("Bekor qilish")) } }
    )
}
