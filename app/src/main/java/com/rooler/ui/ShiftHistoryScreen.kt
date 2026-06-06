package com.rooler.ui

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

private fun tf(ms: Long) = if (ms <= 0) "—" else SimpleDateFormat("HH:mm", Locale.US).format(Date(ms))
private fun df(dk: String): String {
    val parts = dk.split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else dk
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftHistoryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { RollerRepository() }
    var shifts by remember { mutableStateOf<List<Pair<String, Shift>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            shifts = repo.loadShiftHistory(60)
        } catch (e: Exception) {
            toast = "Ошибка загрузки: ${e.message}"
        }
        loading = false
    }

    Scaffold(containerColor = R.BG, topBar = { TopAppBar(title = { Text("История смен", color = R.T1) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = R.T2) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = R.S1)) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (loading) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = R.PR) }
            else Column(Modifier.weight(1f).padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Смен: ${shifts.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = R.T1, modifier = Modifier.weight(1f))
                    Button(onClick = { ReportPdf.share(ctx, ReportPdf.generateShiftHistory(ctx, shifts)); toast = "PDF создан" }, colors = ButtonDefaults.buttonColors(containerColor = R.SC)) { Text("\uD83D\uDCC4 PDF", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(shifts) { (_, sh) ->
                        Card(colors = CardDefaults.cardColors(containerColor = R.S2), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(df(sh.dateKey), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = R.T1)
                                    if (sh.cashierName.isNotEmpty()) Text("\uD83D\uDC64 ${sh.cashierName}", fontSize = 13.sp, color = R.PR2)
                                    Text("${tf(sh.openTime)} — ${tf(sh.closeTime)}", fontSize = 12.sp, color = R.T2)
                                    if (sh.closeTime > 0 && sh.openTime > 0) Text("%.1f ч".format((sh.closeTime - sh.openTime) / 3_600_000.0), fontSize = 11.sp, color = R.T3)
                                }
                                Surface(color = if (sh.closeTime > 0) R.GR_BG else R.YL_BG, shape = RoundedCornerShape(4.dp)) {
                                    Text(if (sh.closeTime > 0) "Закрыта" else "Открыта", fontSize = 11.sp, color = if (sh.closeTime > 0) R.GR else R.YL, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
            Watermark()
        }
    }
    toast?.let { t -> Snackbar(Modifier.padding(10.dp), containerColor = R.GR.copy(alpha = 0.9f), contentColor = Color.White) { Text("\u2705 $t", fontWeight = FontWeight.Medium) }; LaunchedEffect(t) { kotlinx.coroutines.delay(2500); toast = null } }
}
