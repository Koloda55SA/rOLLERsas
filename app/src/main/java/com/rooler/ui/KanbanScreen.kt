package com.rooler.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.data.RollerGroup
import com.rooler.data.RollerRepository
import com.rooler.data.models.Shift
import com.rooler.domain.PricingLogic
import com.rooler.domain.SessionView

fun fmt(ms: Long): String {
    val abs = kotlin.math.abs(ms) / 1000
    val m = abs / 60
    val s = abs % 60
    val sign = if (ms < 0) "-" else ""
    return "%s%02d:%02d".format(sign, m, s)
}

@Composable
fun Watermark() {
    Row(Modifier.fillMaxWidth().background(R.CH).padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("Рахманов Сыймыкбек", color = R.WM, fontSize = 10.sp)
        Spacer(Modifier.width(4.dp))
        Text("\uD83D\uDCF8 __rahmanov___", color = R.IG, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun KanbanScreen(vm: MainViewModel, totalRollers: Int, groups: List<RollerGroup>, onOpenSettings: () -> Unit) {
    val state by vm.kanban.collectAsState()
    val now by vm.currentDateTime.collectAsState()
    val shift by vm.shift.collectAsState()
    var giveOutRoller by remember { mutableStateOf<Int?>(null) }
    var returnSession by remember { mutableStateOf<SessionView?>(null) }
    var earlyReturn by remember { mutableStateOf<SessionView?>(null) }
    var showForceClose by remember { mutableStateOf(false) }
    var showShiftDialog by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    val allActive = state.riding + state.ending + state.expired
    val busyIds = allActive.map { it.tx.rollerId }.toSet()
    val sizeOf: (Int) -> String = { r -> groups.firstOrNull { r in it.from..it.to }?.size ?: "" }
    val shiftOn = shift.openTime > 0 && shift.closeTime <= 0L

    fun onSessionClick(sv: SessionView) {
        if (sv.tx.isActive) {
            if (sv.remainingMs <= 0) returnSession = sv
            else earlyReturn = sv
        }
    }

    Column(Modifier.fillMaxSize().background(R.BG)) {
        Row(Modifier.fillMaxWidth().background(R.S1).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("\uD83D\uDEF9 Роллердром", color = R.T1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Text(now, color = R.T3, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            if (shiftOn && shift.cashierName.isNotEmpty()) {
                Surface(color = R.PR.copy(alpha = 0.3f), shape = RoundedCornerShape(20.dp)) {
                    Text("\uD83D\uDC64 ${shift.cashierName}", color = R.PR2, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.Medium)
                }
            }
            if (!shiftOn) {
                Spacer(Modifier.width(6.dp))
                Button(onClick = { showShiftDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = R.GR),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("Открыть смену", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (shiftOn && allActive.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { showForceClose = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null, tint = R.RD, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Settings, null, tint = R.T2, modifier = Modifier.size(20.dp))
            }
        }

        if (!shiftOn) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDD12 Смена закрыта", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = R.T2)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { showShiftDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = R.GR)) {
                        Text("Открыть смену", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onOpenSettings, colors = ButtonDefaults.outlinedButtonColors(contentColor = R.T2)) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Настройки", fontSize = 14.sp)
                    }
                }
            }
        } else {
            Row(Modifier.weight(1f).fillMaxWidth().padding(4.dp)) {
                FreePanel(groups, state.freeRollers, busyIds, Modifier.weight(1.2f)) { giveOutRoller = it }
                Spacer(Modifier.width(4.dp))
                ActColumn("\uD83D\uDFE2 Катаются", state.riding, R.GR, R.GR_BG, Modifier.weight(1f), ::onSessionClick, sizeOf)
                Spacer(Modifier.width(4.dp))
                ActColumn("\uD83D\uDFE1 Скоро", state.ending, R.YL, R.YL_BG, Modifier.weight(1f), ::onSessionClick, sizeOf)
                Spacer(Modifier.width(4.dp))
                ActColumn("\uD83D\uDD34 Истекло", state.expired, R.RD, R.RD_BG, Modifier.weight(1f), ::onSessionClick, sizeOf)
            }
        }
        Watermark()
    }

    giveOutRoller?.let { r ->
        GiveOutDialog(r, sizeOf(r), { giveOutRoller = null }, { b, m ->
            vm.startSession(r, b, m, sizeOf(r)); giveOutRoller = null; toast = "Ролик #$r выдан"
        })
    }
    returnSession?.let { sv ->
        ReturnDialog(sv, { returnSession = null }, { f ->
            val extra = if (f) 0 else PricingLogic.extraAmount(sv.tx.endTime, System.currentTimeMillis())
            vm.returnSession(sv.tx, extra, f); returnSession = null; toast = "Ролик #${sv.tx.rollerId} возвращён"
        })
    }
    earlyReturn?.let { sv ->
        EarlyReturnDialog(sv, { earlyReturn = null }, { f ->
            val extra = if (f) 0 else PricingLogic.extraAmount(sv.tx.endTime, System.currentTimeMillis())
            vm.returnSession(sv.tx, extra, f); earlyReturn = null; toast = "Досрочный возврат #${sv.tx.rollerId}"
        })
    }
    if (showForceClose) ForceCloseDialog(allActive.size, { vm.forceCloseAll(); showForceClose = false; toast = "Все закрыты" }, { showForceClose = false })
    if (showShiftDialog && !shiftOn) ShiftDialog({ n -> vm.openShift(RollerRepository.dateKey(), n); showShiftDialog = false; toast = "Смена открыта: $n" }, { showShiftDialog = false })

    toast?.let { t ->
        Snackbar(Modifier.padding(10.dp), containerColor = R.GR.copy(alpha = 0.9f), contentColor = Color.White) { Text("\u2705 $t", fontWeight = FontWeight.Medium) }
        LaunchedEffect(t) { kotlinx.coroutines.delay(2500); toast = null }
    }
}

@Composable fun ShiftDialog(onOpen: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = R.S1,
        title = { Text("\uD83D\uDD13 Открытие смены", fontWeight = FontWeight.Bold, color = R.T1) },
        text = { Column {
            Text("Введите имя — оно попадёт в отчёт.", color = R.T2, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(name, { name = it }, label = { Text("Имя кассира") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = R.PR, unfocusedBorderColor = R.S3, cursorColor = R.PR, focusedLabelColor = R.PR2))
        }},
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onOpen(name.trim()) }, enabled = name.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = R.GR)) { Text("Открыть", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = R.T2) } }
    )
}

@Composable private fun ForceCloseDialog(cnt: Int, onOk: () -> Unit, onNo: () -> Unit) {
    var inp by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onNo, containerColor = R.S1,
        title = { Text("\u26A0 Закрыть ВСЕ?", fontWeight = FontWeight.Bold, color = R.RD) },
        text = { Column {
            Text("Активных: $cnt. Доплата прощена.", color = R.T2, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Text("Введите $cnt для подтверждения:", color = R.T1, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(inp, { inp = it.filter { c -> c.isDigit() } }, label = { Text("Число") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = inp.isNotEmpty() && inp.toIntOrNull() != cnt,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = R.RD, unfocusedBorderColor = R.S3, cursorColor = R.RD))
        }},
        confirmButton = { Button(onClick = onOk, enabled = inp.toIntOrNull() == cnt, colors = ButtonDefaults.buttonColors(containerColor = R.RD)) { Text("Закрыть $cnt", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onNo) { Text("Отмена", color = R.T2) } }
    )
}

@Composable private fun FreePanel(groups: List<RollerGroup>, free: List<Int>, busy: Set<Int>, m: Modifier, onClick: (Int) -> Unit) {
    Column(m.fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(R.S1).padding(8.dp)) {
        Text("\u2B55 Свободны (${free.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = R.T1, modifier = Modifier.padding(bottom = 6.dp))
        if (groups.isEmpty()) {
            LazyVerticalGrid(columns = GridCells.Adaptive(52.dp), modifier = Modifier.weight(1f)) { items(free) { RChip(it, true, onClick) } }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(groups, key = { it.size }) { g ->
                    val gf = g.rollers().filter { it in free }
                    Card(colors = CardDefaults.cardColors(containerColor = R.S2), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = R.PR, shape = RoundedCornerShape(6.dp)) { Text("Рз.${g.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) }
                                Spacer(Modifier.width(8.dp))
                                Text("${gf.size}/${g.rollers().size}", fontSize = 12.sp, color = if (gf.isEmpty()) R.RD else R.GR, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) { items(g.rollers()) { RChip(it, it in free, onClick) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun RChip(n: Int, free: Boolean, onClick: (Int) -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(if (free) R.GR.copy(alpha = 0.2f) else R.S3)
        .clickable(enabled = free) { onClick(n) }, contentAlignment = Alignment.Center) {
        Text("$n", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (free) R.GR else R.T3)
    }
}

@Composable private fun ActColumn(title: String, sessions: List<SessionView>, accent: Color, bg: Color, m: Modifier, onClick: (SessionView) -> Unit, sizeOf: (Int) -> String) {
    Column(m.fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(bg).padding(6.dp)) {
        Text("$title (${sessions.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = accent, modifier = Modifier.padding(bottom = 4.dp))
        LazyColumn { items(sessions, key = { it.tx.id }) { sv -> SCard(sv, accent, onClick, sizeOf) } }
    }
}

@Composable private fun SCard(sv: SessionView, accent: Color, onClick: (SessionView) -> Unit, sizeOf: (Int) -> String) {
    val expired = sv.remainingMs <= 0
    var alpha by remember { mutableFloatStateOf(1f) }
    if (expired) {
        val t = rememberInfiniteTransition(label = "b")
        alpha = t.animateFloat(1f, 0.35f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "a").value
    }
    Surface(color = accent.copy(alpha = alpha), shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { onClick(sv) }) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White.copy(alpha = 0.25f), shape = RoundedCornerShape(4.dp)) {
                    Text("Бейдж ${sv.tx.badgeId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("#${sv.tx.rollerId}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                if (sv.tx.rollerSize.isNotEmpty()) Text(" рз.${sv.tx.rollerSize}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            }
            Text("${sv.tx.durationMins} мин · ${sv.tx.baseAmount} с", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Text(fmt(sv.remainingMs), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (expired) {
                Text("Доплата: ${sv.extraAmount} с · Итого: ${sv.tx.baseAmount + sv.extraAmount} с", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("Нажмите для возврата", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            } else {
                Text("Нажмите для досрочного возврата", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            }
        }
    }
}
