package com.rooler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.domain.PricingLogic
import com.rooler.domain.SessionView
import kotlinx.coroutines.delay

@Composable
fun GiveOutDialog(rollerId: Int, rollerSize: String, onDismiss: () -> Unit, onStart: (badgeId: Int, mins: Int) -> Unit) {
    var badge by remember { mutableStateOf("") }
    var mins by remember { mutableStateOf(30) }
    val ok = badge.toIntOrNull()?.let { it > 0 } ?: false
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = R.S1, shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Выдача #$rollerId", fontWeight = FontWeight.Bold, color = R.T1)
                if (rollerSize.isNotEmpty()) { Spacer(Modifier.width(8.dp)); Pill("рз.$rollerSize", R.PR2) }
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    badge, { badge = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("Бейдж") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = R.PR, cursorColor = R.PR, unfocusedBorderColor = R.S3, focusedLabelColor = R.PR2)
                )
                Spacer(Modifier.height(12.dp))
                Text("Длительность:", color = R.T2, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    DurBtn("1 мин\nТЕСТ", mins == 1, Modifier.weight(1f)) { mins = 1 }
                    Spacer(Modifier.width(6.dp))
                    DurBtn("30 мин\n200 с", mins == 30, Modifier.weight(1f)) { mins = 30 }
                    Spacer(Modifier.width(6.dp))
                    DurBtn("60 мин\n400 с", mins == 60, Modifier.weight(1f)) { mins = 60 }
                }
            }
        },
        confirmButton = { GradientButton("▶  СТАРТ", enabled = ok, modifier = Modifier.fillMaxWidth()) { badge.toIntOrNull()?.let { onStart(it, mins) } } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = R.T2) } }
    )
}

@Composable private fun DurBtn(label: String, sel: Boolean, m: Modifier, onClick: () -> Unit) {
    Box(
        m.height(56.dp).clip(RoundedCornerShape(13.dp))
            .background(if (sel) R.GradPrimary else Brush.linearGradient(listOf(R.S2, R.S2)))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) Color.White else R.T2)
    }
}

@Composable
fun ReturnDialog(session: SessionView, onDismiss: () -> Unit, onConfirm: (forgiven: Boolean) -> Unit) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); now = System.currentTimeMillis() } }
    val liveExtra = PricingLogic.extraAmount(session.tx.endTime, now)
    val liveOverdue = PricingLogic.overdueMinutes(session.tx.endTime, now)
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = R.S1, shape = RoundedCornerShape(20.dp),
        title = { Text("Возврат #${session.tx.rollerId}", fontWeight = FontWeight.Bold, color = R.T1) },
        text = {
            Column {
                Text("Бейдж: ${session.tx.badgeId}", color = R.T2, fontSize = 14.sp)
                if (session.tx.rollerSize.isNotEmpty()) Text("Размер: ${session.tx.rollerSize}", color = R.T2, fontSize = 14.sp)
                Text("Просрочка: $liveOverdue мин", color = R.RD, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                GlassCard(Modifier.fillMaxWidth(), color = R.S2, radius = 12.dp) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Осн.: ${session.tx.baseAmount} с  ·  Доплата: $liveExtra с", color = R.T2, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Итого: ${session.tx.baseAmount + liveExtra} с", color = R.T1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = { GradientButton("С доплатой") { onConfirm(false) } },
        dismissButton = { OutlineButton("Простить", color = R.SC) { onConfirm(true) } }
    )
}

@Composable
fun EarlyReturnDialog(session: SessionView, onDismiss: () -> Unit, onConfirm: (forgiven: Boolean) -> Unit) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); now = System.currentTimeMillis() } }
    val extra = PricingLogic.extraAmount(session.tx.endTime, now)
    val overdueMins = PricingLogic.overdueMinutes(session.tx.endTime, now)
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = R.S1, shape = RoundedCornerShape(20.dp),
        title = { Text("Досрочный возврат #${session.tx.rollerId}", fontWeight = FontWeight.Bold, color = R.SC) },
        text = {
            Column {
                Text("Бейдж: ${session.tx.badgeId} · ${session.tx.durationMins} мин", color = R.T2, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                GlassCard(Modifier.fillMaxWidth(), color = R.S2, radius = 12.dp) {
                    Column(Modifier.padding(12.dp)) {
                        if (extra > 0) {
                            Text("Уже просрочка: $overdueMins мин", color = R.YL, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Доплата: $extra с", color = R.T2, fontSize = 14.sp)
                        } else {
                            Text("Время ещё не истекло — доплаты нет.", color = R.GR, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Итого: ${session.tx.baseAmount + extra} с", color = R.T1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = { GradientButton("Вернуть", brush = R.GradGreen) { onConfirm(extra <= 0) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = R.T2) } }
    )
}
