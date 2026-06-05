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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.domain.SessionView

private val GREEN = Color(0xFF2E7D32)
private val YELLOW = Color(0xFFF9A825)
private val RED = Color(0xFFC62828)
private val FREE_BG = Color(0xFFECEFF1)

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
    onOpenSettings: () -> Unit
) {
    val state by vm.kanban.collectAsState()
    var giveOutRoller by remember { mutableStateOf<Int?>(null) }
    var returnSession by remember { mutableStateOf<SessionView?>(null) }

    Column(Modifier.fillMaxSize()) {
        // Верхняя панель
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF263238)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛹 Роллердром", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("Свободно: ${state.freeRollers.size}  |  На прокате: ${state.riding.size + state.ending.size + state.expired.size}",
                color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, "Настройки", tint = Color.White)
            }
        }

        Row(Modifier.fillMaxSize().padding(6.dp)) {
            FreeColumn(state.freeRollers, Modifier.weight(1f)) { giveOutRoller = it }
            Spacer(Modifier.width(6.dp))
            SessionColumn("🟢 Катаются", state.riding, GREEN, Modifier.weight(1f), null)
            Spacer(Modifier.width(6.dp))
            SessionColumn("🟡 Заканчивается", state.ending, YELLOW, Modifier.weight(1f), null)
            Spacer(Modifier.width(6.dp))
            SessionColumn("🔴 ИСТЕКЛО", state.expired, RED, Modifier.weight(1f)) { returnSession = it }
        }
    }

    giveOutRoller?.let { roller ->
        GiveOutDialog(
            rollerId = roller,
            onDismiss = { giveOutRoller = null },
            onStart = { badge, mins ->
                vm.startSession(roller, badge, mins)
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
}

@Composable
private fun FreeColumn(free: List<Int>, modifier: Modifier, onClick: (Int) -> Unit) {
    Column(modifier.fillMaxHeight().background(FREE_BG, RoundedCornerShape(8.dp)).padding(6.dp)) {
        Text("⭕ Свободны (${free.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 6.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(64.dp)) {
            items(free) { roller ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(4.dp).size(60.dp).clickable { onClick(roller) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$roller", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionColumn(
    title: String,
    sessions: List<SessionView>,
    color: Color,
    modifier: Modifier,
    onReturn: ((SessionView) -> Unit)?
) {
    Column(modifier.fillMaxHeight().background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(6.dp)) {
        Text("$title (${sessions.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color,
            modifier = Modifier.padding(bottom = 6.dp))
        LazyColumn {
            items(sessions, key = { it.tx.id }) { sv ->
                SessionCard(sv, color, onReturn)
            }
        }
    }
}

@Composable
private fun SessionCard(sv: SessionView, color: Color, onReturn: ((SessionView) -> Unit)?) {
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
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text("Ролик #${sv.tx.rollerId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Бейдж ${sv.tx.badgeId} · ${sv.tx.durationMins} мин", color = Color.White, fontSize = 13.sp)
            Text(fmt(sv.remainingMs), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            if (expired) {
                Text("Просрочка: ${sv.overdueMins} мин · доплата ${sv.extraAmount} сом",
                    color = Color.White, fontSize = 13.sp)
                Button(
                    onClick = { onReturn?.invoke(sv) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RED),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) { Text("Принять возврат", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
