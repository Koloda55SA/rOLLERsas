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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.data.RollerGroup
import com.rooler.data.models.Shift
import com.rooler.domain.SessionView

private val GREEN = Color(0xFF2E7D32)
private val YELLOW = Color(0xFFF9A825)
private val RED = Color(0xFFC62828)
private val FREE_BG = Color(0xFFECEFF1)
private val GROUP_HEADER_BG = Color(0xFF37474F)
private val WATERMARK_COLOR = Color(0xFF90A4AE)

fun fmt(ms: Long): String {
    val abs = kotlin.math.abs(ms) / 1000
    val m = abs / 60
    val s = abs % 60
    val sign = if (ms < 0) "-" else ""
    return "%s%02d:%02d".format(sign, m, s)
}

@Composable
fun KanbanScreen(
    vm: MainViewModel,
    totalRollers: Int,
    groups: List<RollerGroup>,
    onOpenSettings: () -> Unit
) {
    val state by vm.kanban.collectAsState()
    val now by vm.currentDateTime.collectAsState()
    val shift by vm.shift.collectAsState()
    var giveOutRoller by remember { mutableStateOf<Int?>(null) }
    var returnSession by remember { mutableStateOf<SessionView?>(null) }
    var showForceCloseDialog by remember { mutableStateOf(false) }
    var showShiftDialog by remember { mutableStateOf(false) }

    val busyRollers = state.riding.map { it.tx.rollerId } +
            state.ending.map { it.tx.rollerId } +
            state.expired.map { it.tx.rollerId }

    val sizeOfRoller: (Int) -> String = { rollerId ->
        groups.firstOrNull { rollerId in it.from..it.to }?.size ?: ""
    }

    val shiftOpen = shift.openTime > 0
    val shiftClosed = shift.closeTime > 0
    val shiftActive = shiftOpen && !shiftClosed

    LaunchedEffect(shiftOpen, shiftClosed) {
        if (!shiftOpen || shiftClosed) showShiftDialog = true
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF263238)).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\uD83D\uDEF9 Роллердром", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text(now, color = Color(0xFFB0BEC5), fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            if (shiftActive && shift.cashierName.isNotEmpty()) {
                Surface(color = Color(0xFF00695C), shape = RoundedCornerShape(4.dp)) {
                    Text("\uD83D\uDC64 ${shift.cashierName}", color = Color.White, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(Modifier.width(6.dp))
            }
            Text("Своб: ${state.freeRollers.size} | Прокат: ${busyRollers.size}",
                color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            if (shiftActive && busyRollers.isNotEmpty()) {
                IconButton(onClick = { showForceCloseDialog = true }) {
                    Icon(Icons.Default.Close, "Закрыть все", tint = Color(0xFFFF8A80))
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, "Настройки", tint = Color.White)
            }
        }

        if (!shiftActive) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Смена закрыта", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { showShiftDialog = true }) { Text("Открыть смену") }
                }
            }
        } else {
            Row(Modifier.fillMaxSize().padding(4.dp)) {
                GroupsColumn(
                    groups = groups,
                    freeRollers = state.freeRollers,
                    busyRollers = busyRollers,
                    modifier = Modifier.weight(1.2f),
                    onRollerClick = { giveOutRoller = it }
                )
                Spacer(Modifier.width(4.dp))
                SessionColumn("\uD83D\uDFE2 Катаются", state.riding, GREEN, Modifier.weight(1f), null, sizeOfRoller)
                Spacer(Modifier.width(4.dp))
                SessionColumn("\uD83D\uDFE1 Заканчивается", state.ending, YELLOW, Modifier.weight(1f), null, sizeOfRoller)
                Spacer(Modifier.width(4.dp))
                SessionColumn("\uD83D\uDD34 ИСТЕКЛО", state.expired, RED, Modifier.weight(1f), { returnSession = it }, sizeOfRoller)
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Color(0xFF263238)).padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Разраб: Рахманов Сыймыкбек", color = WATERMARK_COLOR, fontSize = 10.sp)
            Spacer(Modifier.width(6.dp))
            Text("\uD83D\uDCF8 @rahmanov_", color = Color(0xFFE91E63), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("v1.2-groups-announcements", color = WATERMARK_COLOR, fontSize = 9.sp)
        }
    }

    giveOutRoller?.let { roller ->
        GiveOutDialog(
            rollerId = roller,
            rollerSize = sizeOfRoller(roller),
            onDismiss = { giveOutRoller = null },
            onStart = { badge, mins ->
                vm.startSession(roller, badge, mins, sizeOfRoller(roller))
                giveOutRoller = null
            }
        )
    }

    returnSession?.let { sv ->
        ReturnDialog(
            session = sv,
            onDismiss = { returnSession = null },
            onConfirm = { forgiven ->
                vm.returnSession(sv.tx, sv.extraAmount, forgiven)
                returnSession = null
            }
        )
    }

    if (showForceCloseDialog) {
        ForceCloseDialog(
            activeCount = busyRollers.size,
            onConfirm = { vm.forceCloseAll(); showForceCloseDialog = false },
            onDismiss = { showForceCloseDialog = false }
        )
    }

    if (showShiftDialog) {
        ShiftOpenDialog(
            shift = shift,
            onOpen = { name ->
                vm.openShift(RollerRepository.dateKey(), name)
                vm.loadShift(RollerRepository.dateKey())
                showShiftDialog = false
            },
            onDismiss = {
                if (shiftActive) showShiftDialog = false
            }
        )
    }
}

@Composable
private fun ShiftOpenDialog(
    shift: Shift,
    onOpen: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val canDismiss = shift.openTime > 0 && shift.closeTime <= 0L

    AlertDialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        title = { Text("\uD83D\uDD13 Открытие смены", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Введите ваше имя — оно попадёт в отчёт смены.", fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя кассира") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onOpen(name.trim()) },
                enabled = name.isNotBlank()) {
                Text("Открыть смену", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun GroupsColumn(
    groups: List<RollerGroup>,
    freeRollers: List<Int>,
    busyRollers: List<Int>,
    modifier: Modifier,
    onRollerClick: (Int) -> Unit
) {
    Column(modifier.fillMaxHeight().background(FREE_BG, RoundedCornerShape(8.dp)).padding(6.dp)) {
        Text("\u2B55 Свободны (${freeRollers.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 4.dp))

        if (groups.isEmpty()) {
            LazyVerticalGrid(columns = GridCells.Adaptive(56.dp), modifier = Modifier.weight(1f)) {
                items(freeRollers) { roller -> RollerChip(roller, true, onRollerClick) }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(groups, key = { it.size }) { group ->
                    val groupFree = group.rollers().filter { it in freeRollers }
                    val groupTotal = group.rollers().size

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = GROUP_HEADER_BG, shape = RoundedCornerShape(6.dp)) {
                                    Text("Рз.${group.size}", color = Color.White,
                                        fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("${groupFree.size}/$groupTotal",
                                    fontSize = 12.sp, color = if (groupFree.isEmpty()) RED else GREEN,
                                    fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(group.rollers()) { roller ->
                                    RollerChip(roller, roller in freeRollers, onRollerClick)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RollerChip(roller: Int, isFree: Boolean, onClick: (Int) -> Unit) {
    Surface(
        color = if (isFree) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.size(48.dp).clickable(enabled = isFree) { onClick(roller) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("$roller", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = if (isFree) Color(0xFF1B5E20) else Color(0xFFB71C1C))
        }
    }
}

@Composable
private fun SessionColumn(
    title: String,
    sessions: List<SessionView>,
    color: Color,
    modifier: Modifier,
    onReturn: ((SessionView) -> Unit)?,
    sizeOfRoller: (Int) -> String
) {
    Column(modifier.fillMaxHeight().background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(6.dp)) {
        Text("$title (${sessions.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color,
            modifier = Modifier.padding(bottom = 4.dp))
        LazyColumn {
            items(sessions, key = { it.tx.id }) { sv ->
                SessionCard(sv, color, onReturn, sizeOfRoller)
            }
        }
    }
}

@Composable
private fun SessionCard(
    sv: SessionView, color: Color, onReturn: ((SessionView) -> Unit)?,
    sizeOfRoller: (Int) -> String
) {
    val expired = onReturn != null
    var blinkAlpha = 1f
    if (expired) {
        val transition = rememberInfiniteTransition(label = "blink")
        blinkAlpha = transition.animateFloat(
            initialValue = 1f, targetValue = 0.4f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "a"
        ).value
    }
    Surface(
        color = if (expired) color.copy(alpha = blinkAlpha) else color,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)) {
                    Text("Бейдж ${sv.tx.badgeId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("#${sv.tx.rollerId}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                if (sv.tx.rollerSize.isNotEmpty()) {
                    Spacer(Modifier.width(2.dp))
                    Text("рз.${sv.tx.rollerSize}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
            Text("${sv.tx.durationMins} мин", color = Color.White, fontSize = 11.sp)
            Text(fmt(sv.remainingMs), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (expired) {
                Text("Просрочка: ${sv.overdueMins} мин · +${sv.extraAmount} с", color = Color.White, fontSize = 11.sp)
                Button(
                    onClick = { onReturn?.invoke(sv) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RED),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) { Text("Принять возврат", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun ForceCloseDialog(
    activeCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val inputInt = input.toIntOrNull()
    val confirmed = inputInt == activeCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u26A0 Закрыть ВСЕ активные?", fontWeight = FontWeight.Bold, color = RED) },
        text = {
            Column {
                Text("Активных роликов: $activeCount. Все будут принудительно возвращены (доплата прощена).", fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Text("Введите число $activeCount для подтверждения:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() } },
                    label = { Text("Количество активных") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = input.isNotEmpty() && !confirmed
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = confirmed,
                colors = ButtonDefaults.buttonColors(containerColor = RED)) {
                Text("Закрыть все $activeCount", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
