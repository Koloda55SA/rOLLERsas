package com.rooler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.data.RollerRepository
import com.rooler.data.models.Shift
import com.rooler.domain.ReportPdf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun timeFmt(ms: Long): String =
    if (ms <= 0) "\u2014" else SimpleDateFormat("HH:mm", Locale.US).format(Date(ms))

private fun dateFmt(dateKey: String): String {
    return runCatching {
        val parts = dateKey.split("-")
        "${parts[2]}.${parts[1]}.${parts[0]}"
    }.getOrDefault(dateKey)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { RollerRepository() }
    var shifts by remember { mutableStateOf<List<Pair<String, Shift>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        shifts = repo.loadShiftHistory(60)
        loading = false
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("История смен") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") } }
        )
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(Modifier.weight(1f).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Всего смен: ${shifts.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        modifier = Modifier.weight(1f))
                    Button(onClick = {
                        val file = ReportPdf.generateShiftHistory(context, shifts)
                        ReportPdf.share(context, file)
                        successMsg = "PDF истории смен создан!"
                    }) { Text("\uD83D\uDCC4 Экспорт PDF", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(8.dp))

                LazyColumn(Modifier.weight(1f)) {
                    items(shifts) { (dateKey, shift) ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(dateFmt(dateKey), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (shift.cashierName.isNotEmpty()) {
                                        Text("\uD83D\uDC64 Кассир: ${shift.cashierName}", fontSize = 14.sp, color = Color(0xFF00695C))
                                    }
                                    Text("${timeFmt(shift.openTime)} — ${timeFmt(shift.closeTime)}", fontSize = 13.sp, color = Color.Gray)
                                    if (shift.closeTime > 0 && shift.openTime > 0) {
                                        val hours = (shift.closeTime - shift.openTime) / 3_600_000.0
                                        Text("Длительность: ${"%.1f".format(hours)} ч", fontSize = 12.sp, color = Color(0xFF37474F))
                                    }
                                }
                                if (shift.closeTime > 0) {
                                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                                        Text("Закрыта", fontSize = 12.sp, color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Medium)
                                    }
                                } else if (shift.openTime > 0) {
                                    Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(4.dp)) {
                                        Text("Открыта", fontSize = 12.sp, color = Color(0xFFF57F17),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        WatermarkBar()
        }
    }

    successMsg?.let { msg ->
        Snackbar(
            modifier = Modifier.padding(12.dp),
            containerColor = Color(0xFF1B5E20),
            contentColor = Color.White
        ) { Text("\u2705 $msg", fontWeight = FontWeight.Medium) }
        LaunchedEffect(msg) { kotlinx.coroutines.delay(2500); successMsg = null }
    }
}
