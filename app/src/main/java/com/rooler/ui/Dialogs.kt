package com.rooler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.domain.SessionView

// Диалог выдачи: вводим бейдж, выбираем 30/60, жмём Старт.
@Composable
fun GiveOutDialog(
    rollerId: Int,
    onDismiss: () -> Unit,
    onStart: (badgeId: Int, mins: Int) -> Unit
) {
    var badge by remember { mutableStateOf("") }
    var mins by remember { mutableStateOf(30) }
    val badgeInt = badge.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выдача ролика #$rollerId", fontWeight = FontWeight.Bold) },
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
                Spacer(Modifier.height(16.dp))
                Text("Длительность:", fontSize = 14.sp)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    DurationButton("30 мин\n200 сом", mins == 30, Modifier.weight(1f)) { mins = 30 }
                    Spacer(Modifier.width(8.dp))
                    DurationButton("60 мин\n400 сом", mins == 60, Modifier.weight(1f)) { mins = 60 }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { badgeInt?.let { onStart(it, mins) } },
                enabled = badgeInt != null && badgeInt > 0
            ) { Text("▶ СТАРТ", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun DurationButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = if (selected)
        ButtonDefaults.buttonColors()
    else
        ButtonDefaults.outlinedButtonColors()
    if (selected) {
        Button(onClick = onClick, modifier = modifier.height(64.dp), colors = colors) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.height(64.dp)) {
            Text(label, fontSize = 15.sp)
        }
    }
}

// Диалог возврата: расчёт доплаты, Провести / Простить.
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
                Text("Просрочка: ${session.overdueMins} мин", fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Text("Основная сумма: ${session.tx.baseAmount} сом", fontSize = 15.sp)
                Text("Доплата: ${session.extraAmount} сом", fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text("Итого к оплате: ${session.tx.baseAmount + session.extraAmount} сом",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(false) }) {
                Text("Провести с доплатой", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onConfirm(true) }) { Text("Простить доплату") }
        }
    )
}
