package com.rooler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.data.AdminSettings
import com.rooler.service.VoiceRecorder

private data class VI(val key: String, val title: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSetupScreen(totalBadges: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val rec = remember { VoiceRecorder(ctx) }
    val admin = remember { AdminSettings(ctx) }
    DisposableEffect(Unit) { onDispose { rec.release() } }
    var recKey by remember { mutableStateOf<String?>(null) }
    var ver by remember { mutableStateOf(0) }
    val annMins = remember { admin.loadAnnouncementMinutes() }
    val items = remember(totalBadges, annMins) { buildList {
        add(VI("time_ended", "\u23F0 Общая фраза"))
        add(VI("closing_reminder", "\uD83D\uDD34 Фраза закрытия + __rahmanov___"))
        for (m in annMins) add(VI("announce_$m", "\uD83D\uDCE2 За $m мин до закрытия"))
        for (i in 1..totalBadges) add(VI("num_$i", "Бейдж $i"))
    }}

    var toast by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = R.BG, topBar = { TopAppBar(title = { Text("Озвучка", color = R.T1) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = R.T2) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = R.S1)) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(horizontal = 10.dp)) {
            items(items, key = { it.key }) { item ->
                key(ver) { VRow(item.title, rec.exists(item.key), recKey == item.key,
                    { if (recKey == item.key) { rec.stop(); recKey = null; ver++ }
                      else { try { rec.start(item.key); recKey = item.key } catch (_: Exception) { toast = "Нет доступа к микрофону" } } },
                    { rec.playback(item.key) }) }
            }
        }
    }
    toast?.let { t -> Snackbar(Modifier.padding(10.dp), containerColor = R.RD.copy(alpha = 0.9f), contentColor = Color.White) { Text(t, fontWeight = FontWeight.Medium) }; LaunchedEffect(t) { kotlinx.coroutines.delay(2500); toast = null } }
}

@Composable private fun VRow(title: String, done: Boolean, rec: Boolean, onRec: () -> Unit, onPlay: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = R.S2), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = R.T1)
                if (done) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, null, tint = R.GR, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(3.dp)); Text("Записано", fontSize = 11.sp, color = R.GR) }
                else Text("Нет записи", fontSize = 11.sp, color = R.T3)
            }
            if (done && !rec) IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, null, tint = R.SC, modifier = Modifier.size(20.dp)) }
            Button(onClick = onRec, colors = if (rec) ButtonDefaults.buttonColors(containerColor = R.RD) else ButtonDefaults.buttonColors(containerColor = R.PR),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Icon(if (rec) Icons.Default.Stop else Icons.Default.Mic, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(3.dp))
                Text(if (rec) "Стоп" else if (done) "Перезаписать" else "Записать", fontSize = 12.sp)
            }
        }
    }
}
