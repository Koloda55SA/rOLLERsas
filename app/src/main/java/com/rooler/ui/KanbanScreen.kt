package com.rooler.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.data.RollerGroup
import com.rooler.data.RollerRepository
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
    Row(
        Modifier.fillMaxWidth().background(R.CH).padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Рахманов Сыймыкбек", color = R.WM, fontSize = 10.sp)
        Spacer(Modifier.width(4.dp))
        Text("📸 __rahmanov___", color = R.IG, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun KanbanScreen(vm: MainViewModel, totalRollers: Int, groups: List<RollerGroup>, onOpenSettings: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val admin = remember { com.rooler.data.AdminSettings(ctx) }
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
    val freeCount = state.freeRollers.size

    fun onSessionClick(sv: SessionView) {
        if (sv.tx.isActive) {
            if (sv.remainingMs <= 0) returnSession = sv
            else earlyReturn = sv
        }
    }

    Column(Modifier.fillMaxSize().background(R.BG)) {
        // ── Хедер ──
        Row(
            Modifier.fillMaxWidth().background(R.GradHeader).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.rooler.R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Роллердром", color = R.T1, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(now, color = R.T3, fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))

            if (shiftOn) {
                StatChip("⚪", freeCount, R.GR)
                Spacer(Modifier.width(6.dp))
                StatChip("🟢", state.riding.size, R.GR)
                Spacer(Modifier.width(6.dp))
                StatChip("🟡", state.ending.size, R.YL)
                Spacer(Modifier.width(6.dp))
                StatChip("🔴", state.expired.size, R.RD)
                Spacer(Modifier.width(10.dp))
                if (shift.cashierName.isNotEmpty()) {
                    Pill("👤 ${shift.cashierName}", R.PR2)
                    Spacer(Modifier.width(6.dp))
                }
                if (allActive.isNotEmpty()) {
                    IconButton(onClick = { showForceClose = true }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, null, tint = R.RD, modifier = Modifier.size(19.dp))
                    }
                }
            } else {
                GradientButton("🔓 Открыть смену", brush = R.GradGreen, height = 38.dp, fontSize = 13.sp) { showShiftDialog = true }
                Spacer(Modifier.width(6.dp))
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Settings, null, tint = R.T2, modifier = Modifier.size(21.dp))
            }
        }

        if (!shiftOn) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔒", fontSize = 56.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Смена закрыта", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = R.T1)
                    Text("Откройте смену, чтобы начать работу", fontSize = 14.sp, color = R.T3)
                    Spacer(Modifier.height(20.dp))
                    GradientButton("🔓 Открыть смену", brush = R.GradGreen, modifier = Modifier.width(240.dp), height = 52.dp, fontSize = 16.sp) { showShiftDialog = true }
                    Spacer(Modifier.height(10.dp))
                    OutlineButton("⚙ Настройки", color = R.T2, modifier = Modifier.width(240.dp)) { onOpenSettings() }
                }
            }
        } else {
            Row(Modifier.weight(1f).fillMaxWidth().padding(6.dp)) {
                FreePanel(groups, state.freeRollers, busyIds, Modifier.weight(1.25f)) { giveOutRoller = it }
                Spacer(Modifier.width(6.dp))
                ActColumn("Катаются", "🟢", state.riding, R.GR, R.GR_BG, Modifier.weight(1f), ::onSessionClick, sizeOf)
                Spacer(Modifier.width(6.dp))
                ActColumn("Скоро", "🟡", state.ending, R.YL, R.YL_BG, Modifier.weight(1f), ::onSessionClick, sizeOf)
                Spacer(Modifier.width(6.dp))
                ActColumn("Истекло", "🔴", state.expired, R.RD, R.RD_BG, Modifier.weight(1f), ::onSessionClick, sizeOf)
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
    if (showShiftDialog && !shiftOn) ShiftDialog(admin.lastCashier, { n -> admin.lastCashier = n; vm.openShift(RollerRepository.dateKey(), n); showShiftDialog = false; toast = "Смена открыта: $n" }, { showShiftDialog = false })

    toast?.let { t ->
        Snackbar(Modifier.padding(10.dp), containerColor = R.GR, contentColor = Color.White, shape = RoundedCornerShape(12.dp)) { Text("✅ $t", fontWeight = FontWeight.Medium) }
        LaunchedEffect(t) { kotlinx.coroutines.delay(2500); toast = null }
    }
}

@Composable private fun StatChip(emoji: String, count: Int, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 11.sp)
        Spacer(Modifier.width(4.dp))
        Text("$count", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable fun ShiftDialog(initialName: String = "", onOpen: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = R.S1, shape = RoundedCornerShape(20.dp),
        title = { Text("🔓 Открытие смены", fontWeight = FontWeight.Bold, color = R.T1) },
        text = {
            Column {
                Text("Введите имя — оно попадёт в отчёт.", color = R.T2, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    name, { name = it }, label = { Text("Имя кассира") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = R.PR, unfocusedBorderColor = R.S3, cursorColor = R.PR, focusedLabelColor = R.PR2)
                )
            }
        },
        confirmButton = { GradientButton("Открыть", brush = R.GradGreen, enabled = name.isNotBlank()) { if (name.isNotBlank()) onOpen(name.trim()) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = R.T2) } }
    )
}

@Composable private fun ForceCloseDialog(cnt: Int, onOk: () -> Unit, onNo: () -> Unit) {
    var inp by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onNo, containerColor = R.S1, shape = RoundedCornerShape(20.dp),
        title = { Text("⚠ Закрыть ВСЕ?", fontWeight = FontWeight.Bold, color = R.RD) },
        text = {
            Column {
                Text("Активных: $cnt. Доплата прощена.", color = R.T2, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                Text("Введите $cnt для подтверждения:", color = R.T1, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    inp, { inp = it.filter { c -> c.isDigit() } }, label = { Text("Число") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(),
                    isError = inp.isNotEmpty() && inp.toIntOrNull() != cnt, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = R.RD, unfocusedBorderColor = R.S3, cursorColor = R.RD)
                )
            }
        },
        confirmButton = { GradientButton("Закрыть $cnt", brush = Brush.linearGradient(listOf(R.RD, Color(0xFFFF8095))), enabled = inp.toIntOrNull() == cnt) { onOk() } },
        dismissButton = { TextButton(onClick = onNo) { Text("Отмена", color = R.T2) } }
    )
}

@Composable private fun FreePanel(groups: List<RollerGroup>, free: List<Int>, busy: Set<Int>, m: Modifier, onClick: (Int) -> Unit) {
    GlassCard(m.fillMaxHeight(), color = R.S1) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Text("⚪ Свободны", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = R.T1)
                Spacer(Modifier.width(8.dp))
                Pill("${free.size}", R.GR)
            }
            if (groups.isEmpty()) {
                LazyVerticalGrid(columns = GridCells.Adaptive(54.dp), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(free) { RChip(it, true, onClick) }
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(groups, key = { it.size }) { g ->
                        val gf = g.rollers().filter { it in free }
                        GlassCard(Modifier.fillMaxWidth(), color = R.S2, radius = 12.dp) {
                            Column(Modifier.padding(9.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.clip(RoundedCornerShape(7.dp)).background(R.GradPrimary).padding(horizontal = 9.dp, vertical = 3.dp)) {
                                        Text("Рз.${g.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("${gf.size}/${g.rollers().size}", fontSize = 12.sp, color = if (gf.isEmpty()) R.RD else R.GR, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { items(g.rollers()) { RChip(it, it in free, onClick) } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun RChip(n: Int, free: Boolean, onClick: (Int) -> Unit) {
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(13.dp))
            .background(if (free) R.GR.copy(alpha = 0.16f) else R.S3)
            .then(if (free) Modifier.border(1.dp, R.GR.copy(alpha = 0.4f), RoundedCornerShape(13.dp)) else Modifier)
            .clickable(enabled = free) { onClick(n) },
        contentAlignment = Alignment.Center
    ) {
        Text("$n", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (free) R.GR else R.T3)
    }
}

@Composable private fun ActColumn(title: String, emoji: String, sessions: List<SessionView>, accent: Color, bg: Color, m: Modifier, onClick: (SessionView) -> Unit, sizeOf: (Int) -> String) {
    GlassCard(m.fillMaxHeight(), color = bg) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                Text("$emoji $title", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accent)
                Spacer(Modifier.width(6.dp))
                Pill("${sessions.size}", accent)
            }
            if (sessions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("—", color = accent.copy(alpha = 0.3f), fontSize = 28.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sessions, key = { it.tx.id }) { sv -> SCard(sv, accent, onClick, sizeOf) }
                }
            }
        }
    }
}

@Composable private fun SCard(sv: SessionView, accent: Color, onClick: (SessionView) -> Unit, sizeOf: (Int) -> String) {
    val expired = sv.remainingMs <= 0
    var alpha by remember { mutableFloatStateOf(1f) }
    if (expired) {
        val t = rememberInfiniteTransition(label = "b")
        alpha = t.animateFloat(1f, 0.5f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "a").value
    }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
            .background(accent.copy(alpha = if (expired) alpha else 0.92f))
            .clickable { onClick(sv) }
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎫${sv.tx.badgeId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(6.dp))
                Text("#${sv.tx.rollerId}${if (sv.tx.rollerSize.isNotEmpty()) " · ${sv.tx.rollerSize}" else ""}", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Text(fmt(sv.remainingMs), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            if (expired) {
                Text("Доплата ${sv.extraAmount}с · Итого ${sv.tx.baseAmount + sv.extraAmount}с · 👆 возврат", color = Color.White.copy(alpha = 0.92f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
