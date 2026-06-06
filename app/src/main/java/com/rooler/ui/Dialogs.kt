package com.rooler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rooler.domain.PricingLogic
import com.rooler.domain.SessionView
import kotlinx.coroutines.delay

/**
 * Диалог с прокручиваемым телом и ЗАКРЕПЛЁННЫМ футером кнопок.
 * В ландшафте тело скроллится, но кнопки действий всегда видны и нажимаемы —
 * это решает проблему «кнопка не нажимается».
 */
@Composable
private fun SheetDialog(onDismiss: () -> Unit, body: @Composable ColumnScope.() -> Unit, footer: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth(0.7f)) {
            GlassCard(Modifier.fillMaxWidth(), color = R.S1) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Column(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 320.dp).verticalScroll(rememberScrollState()), content = body)
                    Spacer(Modifier.height(14.dp))
                    footer()
                }
            }
        }
    }
}

@Composable
fun GiveOutDialog(rollerId: Int, rollerSize: String, onDismiss: () -> Unit, onStart: (badgeId: Int, mins: Int, seconds: Int) -> Unit) {
    var badge by remember { mutableStateOf("") }
    var mins by remember { mutableStateOf(30) }
    var customMin by remember { mutableStateOf("") }
    var customSec by remember { mutableStateOf("") }
    var customOn by remember { mutableStateOf(false) }

    val customTotalSec = (customMin.toIntOrNull() ?: 0) * 60 + (customSec.toIntOrNull() ?: 0)
    val ok = (badge.toIntOrNull()?.let { it > 0 } ?: false) &&
        (if (customOn) customTotalSec > 0 else true)

    fun start() {
        val b = badge.toIntOrNull() ?: return
        if (customOn) {
            // Точные секунды: цена по тарифу из минут (округление вниз), endTime по секундам.
            val m = customTotalSec / 60
            onStart(b, m, customTotalSec)
        } else onStart(b, mins, mins * 60)
    }

    SheetDialog(
        onDismiss = onDismiss,
        body = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Выдача ролика #$rollerId", fontWeight = FontWeight.Bold, color = R.T1, fontSize = 18.sp)
                if (rollerSize.isNotEmpty()) { Spacer(Modifier.width(8.dp)); Pill("рз.$rollerSize", R.PR2) }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                badge, { badge = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("Номер бейджа") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = R.PR, cursorColor = R.PR, unfocusedBorderColor = R.S3, focusedLabelColor = R.PR2, focusedTextColor = R.T1, unfocusedTextColor = R.T1)
            )
            Spacer(Modifier.height(14.dp))
            Text("Время аренды:", color = R.T2, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                DurBtn("30 мин\n200 сом", mins == 30 && !customOn, Modifier.weight(1f)) { mins = 30; customOn = false }
                Spacer(Modifier.width(10.dp))
                DurBtn("60 мин\n400 сом", mins == 60 && !customOn, Modifier.weight(1f)) { mins = 60; customOn = false }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (customOn) R.PR.copy(alpha = 0.18f) else R.S2)
                    .clickable { customOn = true }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏱ Своё время", color = if (customOn) R.PR2 else R.T2, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            if (customOn) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(customMin, { customMin = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("Мин") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = R.PR, cursorColor = R.PR, unfocusedBorderColor = R.S3, focusedTextColor = R.T1, unfocusedTextColor = R.T1))
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(customSec, { customSec = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Сек") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = R.PR, cursorColor = R.PR, unfocusedBorderColor = R.S3, focusedTextColor = R.T1, unfocusedTextColor = R.T1))
                }
                Text("Длительность: ${customTotalSec / 60} мин ${customTotalSec % 60} сек · цена ${PricingLogic.baseAmount(customTotalSec / 60)} сом", color = R.T3, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        },
        footer = {
            Row(Modifier.fillMaxWidth()) {
                OutlineButton("Отмена", color = R.T2, modifier = Modifier.weight(1f)) { onDismiss() }
                Spacer(Modifier.width(10.dp))
                GradientButton("▶  СТАРТ", enabled = ok, modifier = Modifier.weight(1.4f)) { start() }
            }
        }
    )
}

@Composable private fun DurBtn(label: String, sel: Boolean, m: Modifier, onClick: () -> Unit) {
    Box(
        m.height(72.dp).clip(RoundedCornerShape(14.dp))
            .background(if (sel) R.GradPrimary else Brush.linearGradient(listOf(R.S2, R.S2)))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 15.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = if (sel) Color.White else R.T2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** Тап по катающемуся ролику — «Закрыть досрочно? Да/Нет». */
@Composable
fun EarlyReturnDialog(session: SessionView, onDismiss: () -> Unit, onConfirm: (forgiven: Boolean) -> Unit) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); now = System.currentTimeMillis() } }
    val extra = PricingLogic.extraAmount(session.tx.endTime, now)
    val overdueMins = PricingLogic.overdueMinutes(session.tx.endTime, now)
    SheetDialog(
        onDismiss = onDismiss,
        body = {
            Text("Закрыть досрочно?", fontWeight = FontWeight.Bold, color = R.T1, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text("Ролик #${session.tx.rollerId} · бейдж ${session.tx.badgeId}", color = R.T2, fontSize = 14.sp)
            Text("Тариф: ${session.tx.durationMins} мин · ${session.tx.baseAmount} сом", color = R.T2, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            GlassCard(Modifier.fillMaxWidth(), color = R.S2, radius = 12.dp) {
                Column(Modifier.padding(12.dp)) {
                    if (extra > 0) Text("Просрочка: $overdueMins мин · доплата $extra сом", color = R.YL, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    else Text("Время ещё не истекло — доплаты нет.", color = R.GR, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Итого: ${session.tx.baseAmount + extra} сом", color = R.T1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        footer = {
            Row(Modifier.fillMaxWidth()) {
                OutlineButton("Нет", color = R.T2, modifier = Modifier.weight(1f)) { onDismiss() }
                Spacer(Modifier.width(10.dp))
                GradientButton("Да, закрыть", brush = R.GradGreen, modifier = Modifier.weight(1.4f)) { onConfirm(extra <= 0) }
            }
        }
    )
}

/** Тап по истёкшему ролику — «Закрыть» / «Простить» / «Продлить». */
@Composable
fun ReturnDialog(session: SessionView, onClose: (forgiven: Boolean) -> Unit, onExtend: (addMins: Int) -> Unit, onDismiss: () -> Unit) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); now = System.currentTimeMillis() } }
    val liveExtra = PricingLogic.extraAmount(session.tx.endTime, now)
    val liveOverdue = PricingLogic.overdueMinutes(session.tx.endTime, now)
    var mode by remember { mutableStateOf(0) }
    var addMin by remember { mutableStateOf(30) }

    SheetDialog(
        onDismiss = onDismiss,
        body = {
            Text("Ролик #${session.tx.rollerId} — время истекло", fontWeight = FontWeight.Bold, color = R.RD, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            Text("Бейдж ${session.tx.badgeId}${if (session.tx.rollerSize.isNotEmpty()) " · рз.${session.tx.rollerSize}" else ""}", color = R.T2, fontSize = 14.sp)
            Text("Просрочка: $liveOverdue мин", color = R.RD, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            GlassCard(Modifier.fillMaxWidth(), color = R.S2, radius = 12.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text("Осн. ${session.tx.baseAmount} сом · доплата $liveExtra сом", color = R.T2, fontSize = 13.sp)
                    Text("Итого: ${session.tx.baseAmount + liveExtra} сом", color = R.T1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (mode == 1) {
                Spacer(Modifier.height(10.dp))
                Text("На сколько продлить?", color = R.T2, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    DurBtn("30 мин\n+200 сом", addMin == 30, Modifier.weight(1f)) { addMin = 30 }
                    Spacer(Modifier.width(10.dp))
                    DurBtn("60 мин\n+400 сом", addMin == 60, Modifier.weight(1f)) { addMin = 60 }
                }
            }
        },
        footer = {
            if (mode == 0) {
                GradientButton("✅ Закрыть (с доплатой)", modifier = Modifier.fillMaxWidth()) { onClose(false) }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlineButton("Простить", color = R.SC, modifier = Modifier.weight(1f)) { onClose(true) }
                    Spacer(Modifier.width(10.dp))
                    OutlineButton("⏱ Продлить", color = R.PR2, modifier = Modifier.weight(1f)) { mode = 1 }
                }
            } else {
                Row(Modifier.fillMaxWidth()) {
                    OutlineButton("Назад", color = R.T2, modifier = Modifier.weight(1f)) { mode = 0 }
                    Spacer(Modifier.width(10.dp))
                    GradientButton("Продлить $addMin мин", brush = R.GradGreen, modifier = Modifier.weight(1.6f)) { onExtend(addMin) }
                }
            }
        }
    )
}
