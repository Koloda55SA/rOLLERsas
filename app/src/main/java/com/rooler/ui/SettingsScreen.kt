package com.rooler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.data.AdminSettings
import com.rooler.data.RollerRepository
import com.rooler.domain.ReportPdf
import com.rooler.service.AnnouncementService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PinDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val ctx = LocalContext.current
    val pin = remember { AdminSettings(ctx).pin }
    var input by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = R.S1,
        title = { Text("PIN-код", fontWeight = FontWeight.Bold, color = R.T1) },
        text = { Column {
            OutlinedTextField(input, { input = it.filter { c -> c.isDigit() }.take(4); err = false }, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), isError = err, singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = R.PR, cursorColor = R.PR, unfocusedBorderColor = R.S3))
            if (err) Text("Неверный PIN", color = R.RD, fontSize = 13.sp)
        }},
        confirmButton = { Button(onClick = { if (input == pin) onSuccess() else err = true }, colors = ButtonDefaults.buttonColors(containerColor = R.PR)) { Text("Войти") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = R.T2) } }
    )
}

private fun tf(ms: Long) = if (ms <= 0) "—" else SimpleDateFormat("HH:mm", Locale.US).format(Date(ms))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onOpenVoiceSetup: () -> Unit, onOpenAdmin: () -> Unit, onOpenShiftHistory: () -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val admin = remember { AdminSettings(ctx) }
    val dk by vm.selectedDate.collectAsState()
    val exp by vm.expense.collectAsState()
    val an by vm.analytics.collectAsState()
    val sh by vm.shift.collectAsState()
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dk) { vm.loadAccounting(dk); vm.loadShift(dk) }
    var salary by remember(exp) { mutableStateOf((if (exp.salary > 0) exp.salary else admin.defaultDailySalary()).toString()) }
    var other by remember(exp) { mutableStateOf(exp.otherExpenses.toString()) }
    var comment by remember(exp) { mutableStateOf(exp.comment) }
    var showDP by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { AnnouncementService.start(ctx) }

    Scaffold(containerColor = R.BG, topBar = { TopAppBar(title = { Text("Бухгалтерия", color = R.T1) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = R.T2) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = R.S1)) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            LazyColumn(Modifier.weight(1f).padding(14.dp)) {
                item {
                    OutlinedButton(onClick = { showDP = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = R.T2)) { Text("\uD83D\uDCC5 $dk") }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onOpenVoiceSetup, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = R.SC)) { Text("\uD83C\uDFA4 Озвучка") }
                        Spacer(Modifier.width(6.dp))
                        OutlinedButton(onClick = onOpenAdmin, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = R.PR)) { Text("\u2699 Админ") }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onOpenShiftHistory, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = R.T2)) { Text("\uD83D\uDCDC История смен") }

                    Spacer(Modifier.height(14.dp))
                    Text("Смена", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = R.T1)
                    if (sh.cashierName.isNotEmpty()) Text("\uD83D\uDC64 ${sh.cashierName}", fontSize = 14.sp, color = R.PR2, fontWeight = FontWeight.Medium)
                    Text("${tf(sh.openTime)} — ${tf(sh.closeTime)}", fontSize = 13.sp, color = R.T2)
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        if (sh.openTime <= 0 || sh.closeTime > 0) Button(onClick = { vm.openShift(dk, sh.cashierName); toast = "Смена открыта" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = R.GR)) { Text("Открыть") }
                        if (sh.openTime > 0 && sh.closeTime <= 0) { Spacer(Modifier.width(6.dp)); OutlinedButton(onClick = { vm.closeShift(dk); toast = "Смена закрыта" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = R.YL)) { Text("Закрыть") } }
                    }
                    if (sh.openTime > 0 && sh.closeTime <= 0) { Spacer(Modifier.height(4.dp)); OutlinedButton(onClick = { vm.forceCloseAll(); toast = "Все закрыты" }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = R.RD)) { Text("\u26A0 Закрыть ВСЕ активные", fontWeight = FontWeight.Bold) } }

                    Spacer(Modifier.height(14.dp))
                    Text("Расходы", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = R.T1)
                    OF("Зарплата", salary) { salary = it }
                    OF("Прочие", other) { other = it }
                    OutlinedTextField(comment, { comment = it }, label = { Text("Коммент") }, modifier = Modifier.fillMaxWidth(), colors = tfColors())
                    Button(onClick = { vm.saveExpense(dk, salary.toIntOrNull() ?: 0, other.toIntOrNull() ?: 0, comment); toast = "Сохранено" }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), colors = ButtonDefaults.buttonColors(containerColor = R.PR)) { Text("\uD83D\uDCBE Сохранить") }

                    Spacer(Modifier.height(14.dp))
                    Text("Аналитика", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = R.T1)
                }
                an?.let { a ->
                    item {
                        SR("Клиентов", "${a.clientsCount}"); SR("Часы", "%.1f ч".format(a.totalHours)); SR("Выручка", "${a.totalRevenue} с"); SR("Прощено", "${a.forgivenExtra} с")
                        SR("Зарплата", "${exp.salary} с"); SR("Прочие", "${exp.otherExpenses} с")
                        HorizontalDivider(color = R.DV, modifier = Modifier.padding(vertical = 4.dp))
                        SR("Чистая прибыль", "${a.netProfit} с", true)
                        Button(onClick = { ReportPdf.share(ctx, ReportPdf.generate(ctx, dk, sh, a, exp.salary, admin.staffCount, exp.otherExpenses)); toast = "PDF создан" }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = R.SC)) { Text("\uD83D\uDCC4 PDF") }
                        Spacer(Modifier.height(10.dp)); Text("Износ роликов", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = R.T1)
                    }
                    items(a.rollerUsage) { (r, c) -> SR("#$r", "$c выдач") }
                } ?: item { Text("Нет данных", color = R.T3) }
            }
            Watermark()
        }
    }
    if (showDP) { val dp = rememberDatePickerState(); DatePickerDialog(onDismissRequest = { showDP = false }, confirmButton = { TextButton(onClick = { dp.selectedDateMillis?.let { vm.selectDate(mdk(it)) }; showDP = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showDP = false }) { Text("Отмена") } }) { DatePicker(dp) } }
    toast?.let { t -> Snackbar(Modifier.padding(10.dp), containerColor = R.GR.copy(alpha = 0.9f), contentColor = Color.White) { Text("\u2705 $t", fontWeight = FontWeight.Medium) }; LaunchedEffect(t) { kotlinx.coroutines.delay(2500); toast = null } }
}

@Composable private fun SR(l: String, v: String, b: Boolean = false) = Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(l, fontSize = 14.sp, color = R.T2, modifier = Modifier.weight(1f), fontWeight = if (b) FontWeight.Bold else FontWeight.Normal); Text(v, fontSize = 14.sp, color = if (b) R.T1 else R.T2, fontWeight = if (b) FontWeight.Bold else FontWeight.Normal) }
@Composable private fun OF(label: String, v: String, cb: (String) -> Unit) = OutlinedTextField(v, { cb(it.filter { c -> c.isDigit() }) }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = tfColors())
@Composable private fun tfColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = R.PR, cursorColor = R.PR, unfocusedBorderColor = R.S3, focusedLabelColor = R.PR2, unfocusedLabelColor = R.T3)
private fun mdk(m: Long) = Calendar.getInstance().apply { timeInMillis = m }.run { "%04d-%02d-%02d".format(get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH)) }
