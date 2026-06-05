package com.rooler.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rooler.service.VoiceRecorder

// Экран настройки озвучки: общая фраза + номера бейджей 1..N.
// Каждую запись можно записать, прослушать и перезаписать.

private data class VoiceItem(val key: String, val title: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSetupScreen(totalBadges: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    DisposableEffect(Unit) { onDispose { recorder.release() } }

    var hasMic by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val askMic = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasMic = it }

    var recordingKey by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableStateOf(0) }

    val items = remember(totalBadges) {
        buildList {
            add(VoiceItem("time_ended", "Общая фраза: «...убактыңыз бүттү, кассага кайрылыңыз»"))
            for (i in 1..totalBadges) add(VoiceItem("num_$i", "Номер бейджа $i"))
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Озвучка (запись голоса)") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") } }
        )
    }) { pad ->
        Column(Modifier.padding(pad)) {
            if (!hasMic) {
                Surface(color = Color(0xFFFFF3E0), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Нужен доступ к микрофону", Modifier.weight(1f))
                        Button(onClick = { askMic.launch(Manifest.permission.RECORD_AUDIO) }) {
                            Text("Разрешить")
                        }
                    }
                }
            }
            Text(
                "Сначала запишите общую фразу, затем номера бейджей.",
                Modifier.padding(12.dp), fontSize = 13.sp, color = Color.Gray
            )
            LazyColumn(Modifier.padding(horizontal = 8.dp)) {
                items(items, key = { it.key }) { item ->
                    key(version) {
                        VoiceRow(
                            title = item.title,
                            recorded = recorder.exists(item.key),
                            isRecording = recordingKey == item.key,
                            enabled = hasMic,
                            onToggleRecord = {
                                if (recordingKey == item.key) {
                                    recorder.stop()
                                    recordingKey = null
                                    version++
                                } else {
                                    recorder.start(item.key)
                                    recordingKey = item.key
                                }
                            },
                            onPlay = { recorder.playback(item.key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceRow(
    title: String,
    recorded: Boolean,
    isRecording: Boolean,
    enabled: Boolean,
    onToggleRecord: () -> Unit,
    onPlay: () -> Unit
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                if (recorded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Записано", fontSize = 12.sp, color = Color(0xFF2E7D32))
                    }
                } else {
                    Text("Нет записи", fontSize = 12.sp, color = Color.Gray)
                }
            }
            if (recorded && !isRecording) {
                IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, "Прослушать") }
            }
            Button(
                onClick = onToggleRecord,
                enabled = enabled,
                colors = if (isRecording)
                    ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)) else ButtonDefaults.buttonColors()
            ) {
                Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, null)
                Spacer(Modifier.width(4.dp))
                Text(if (isRecording) "Стоп" else if (recorded) "Перезаписать" else "Записать")
            }
        }
    }
}
