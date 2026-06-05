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
    val context = LocalContext.current
    val expected = remember { AdminSettings(context).pin }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Введите PIN-код") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(4); error = false },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                    singleLine = true
                )
                if (error) Text("Неверный PIN", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = { if (pin == expected) onSuccess() else error = true }) { Text("Войти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private fun timeFmt(ms: Long): String =
    if (ms <= 0) "\u2014" else SimpleDateFormat("HH:mm", Locale.US).format(Date(ms))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onOpenVoiceSetup: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenShiftHistory: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val admin = remember { AdminSettings(context) }
    val dateKey by vm.selectedDate.collectAsState()
    val expense by vm.expense.collectAsState()
    val analytics by vm.analytics.collectAsState()
    val shift by vm.shift.collectAsState()
    var successMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dateKey) { vm.loadAccounting(dateKey); vm.loadShift(dateKey) }

    var salary by remember(expense) {
        mutableStateOf((if (expense.salary > 0) expense.salary else admin.defaultDailySalary()).toString())
    }
    var other by remember(expense) { mutableStateOf(expense.otherExpenses.toString()) }
    var comment by remember(expense) { mutableStateOf(expense.comment) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { AnnouncementService.start(context) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Настройки и Бухгалтерия") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") }
            }
        )
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp)) {
            item {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("\uD83D\uDCC5 Дата: $dateKey")
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onOpenVoiceSetup, modifier = Modifier.weight(1f)) {
                        Text("\uD83C\uDFA4 Озвучка")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onOpenAdmin, modifier = Modifier.weight(1f)) {
                        Text("\u2699 Админ")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenShiftHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("\uD83D\uDCDC История смен")
                }

                Spacer(Modifier.height(12.dp))
                Text("Смена", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (shift.cashierName.isNotEmpty()) {
                    Text("\uD83D\uDC64 Кассир: ${shift.cashierName}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Text("Открыта: ${timeFmt(shift.openTime)}    Закрыта: ${timeFmt(shift.closeTime)}", fontSize = 14.sp)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    if (shift.openTime <= 0 || shift.closeTime > 0) {
                        Button(onClick = { vm.openShift(dateKey, shift.cashierName); successMsg = "Смена открыта!" },
                            modifier = Modifier.weight(1f)) { Text("Открыть") }
                    }
                    if (shift.openTime > 0 && shift.closeTime <= 0) {
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { vm.closeShift(dateKey); successMsg = "Смена закрыта!" },
                            modifier = Modifier.weight(1f)) { Text("Закрыть") }
                    }
                }
                if (shift.openTime > 0 && shift.closeTime <= 0) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = { vm.forceCloseAll(); successMsg = "Все ролики закрыты!" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))) {
                        Text("\u26A0 Закрыть ВСЕ активные ролики", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Расходы за день", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedTextField(salary, { salary = it.filter { c -> c.isDigit() } },
                    label = { Text("Зарплата (все сотрудницы)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(other, { other = it.filter { c -> c.isDigit() } },
                    label = { Text("Прочие расходы") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(comment, { comment = it },
                    label = { Text("Комментарий") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        vm.saveExpense(dateKey, salary.toIntOrNull() ?: 0, other.toIntOrNull() ?: 0, comment)
                        successMsg = "Расходы сохранены!"
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("\uD83D\uDCBE Сохранить расходы") }

                Spacer(Modifier.height(16.dp))
                Text("Аналитика", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            analytics?.let { a ->
                item {
                    StatRow("Клиентов за день", "${a.clientsCount}")
                    StatRow("Суммарные часы", "%.1f ч".format(a.totalHours))
                    StatRow("Общая выручка", "${a.totalRevenue} с")
                    StatRow("Прощено доплат", "${a.forgivenExtra} с")
                    StatRow("Зарплата", "${expense.salary} с")
                    StatRow("Прочие расходы", "${expense.otherExpenses} с")
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    StatRow("Чистая прибыль", "${a.netProfit} с", bold = true)

                    Button(
                        onClick = {
                            val file = ReportPdf.generate(
                                context, dateKey, shift, a,
                                salary = expense.salary,
                                staffCount = admin.staffCount,
                                otherExpenses = expense.otherExpenses
                            )
                            ReportPdf.share(context, file)
                            successMsg = "PDF отчёт создан!"
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) { Text("\uD83D\uDCC4 Экспорт в PDF") }

                    Spacer(Modifier.height(12.dp))
                    Text("Износ роликов", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                items(a.rollerUsage) { (rollerId, count) ->
                    StatRow("Ролик #$rollerId", "$count выдач")
                }
            } ?: item { Text("Нет данных за этот день") }
        }

        WatermarkBar()
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { vm.selectDate(millisToDateKey(it)) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = dpState) }
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

@Composable
private fun StatRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontSize = 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun millisToDateKey(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
    )
}
