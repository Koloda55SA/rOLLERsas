package com.rooler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.domain.SessionView

@Composable
fun GiveOutDialog(
    rollerId: Int,
    rollerSize: String,
    onDismiss: () -> Unit,
    onStart: (badgeId: Int, mins: Int) -> Unit
) {
    var badge by remember { mutableStateOf("") }
    var mins by remember { mutableStateOf(1) }
    val badgeInt = badge.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Выдача ролика #$rollerId", fontWeight = FontWeight.Bold)
                if (rollerSize != "—" && rollerSize.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = Color(0xFF37474F), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)) {
                        Text("рз.$rollerSize", color = Color.White, fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = badge,
                    onValueChange = { badge = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Номер бейджа") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Длительность:", fontSize = 14.sp)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    DurationButton("1 мин\nТЕСТ", mins == 1, Modifier.weight(1f)) { mins = 1 }
                    Spacer(Modifier.width(4.dp))
                    DurationButton("30 мин\n200 с", mins == 30, Modifier.weight(1f)) { mins = 30 }
                    Spacer(Modifier.width(4.dp))
                    DurationButton("60 мин\n400 с", mins == 60, Modifier.weight(1f)) { mins = 60 }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { badgeInt?.let { onStart(it, mins) } },
                enabled = badgeInt != null && badgeInt > 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text("\u25B6 СТАРТ", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun DurationButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier.height(56.dp)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.height(56.dp)) {
            Text(label, fontSize = 13.sp)
        }
    }
}

@Composable
fun ReturnDialog(
    session: SessionView,
    onDismiss: () -> Unit,
    onConfirm: (forgiven: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Возврат ролика #${session.tx.rollerId}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Бейдж: ${session.tx.badgeId}", fontSize = 15.sp)
                if (session.tx.rollerSize.isNotEmpty()) Text("Размер: ${session.tx.rollerSize}", fontSize = 15.sp)
                Text("Просрочка: ${session.overdueMins} мин", fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text("Осн.: ${session.tx.baseAmount} с | Доплата: ${session.extraAmount} с", fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text("Итого: ${session.tx.baseAmount + session.extraAmount} с",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(false) }) { Text("Провести с доплатой", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(onClick = { onConfirm(true) }) { Text("Простить доплату") }
        }
    )
}
