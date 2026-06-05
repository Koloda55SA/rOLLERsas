package com.rooler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.data.AdminSettings
import com.rooler.domain.ReportPdf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Диалог PIN перед входом в настройки. PIN берётся из AdminSettings (дефолт 7777).
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
    if (ms <= 0) "—" else SimpleDateFormat("HH:mm", Locale.US).format(Date(ms))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onOpenVoiceSetup: () -> Unit,
    onOpenAdmin: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val admin = remember { AdminSettings(context) }
    val dateKey by vm.selectedDate.collectAsState()
    val expense by vm.expense.collectAsState()
    val analytics by vm.analytics.collectAsState()
    val shift by vm.shift.collectAsState()

    LaunchedEffect(dateKey) { vm.loadAccounting(dateKey); vm.loadShift(dateKey) }

    // Если расход по зарплате не задан — подставляем значение по умолчанию из админки.
    var salary by remember(expense) {
        mutableStateOf((if (expense.salary > 0) expense.salary else admin.defaultDailySalary()).toString())
    }
    var other by remember(expense) { mutableStateOf(expense.otherExpenses.toString()) }
    var comment by remember(expense) { mutableStateOf(expense.comment) }
    var showDatePicker by remember { mutableStateOf(false) }

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
                    Text("📅 Дата: $dateKey")
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onOpenVoiceSetup, modifier = Modifier.weight(1f)) {
                        Text("🎙 Озвучка")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onOpenAdmin, modifier = Modifier.weight(1f)) {
                        Text("⚙ Админ")
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Смена", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Открыта: ${timeFmt(shift.openTime)}    Закрыта: ${timeFmt(shift.closeTime)}",
                    fontSize = 14.sp)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Button(onClick = { vm.openShift(dateKey) }, modifier = Modifier.weight(1f)) {
                        Text("Открыть смену")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { vm.closeShift(dateKey) }, modifier = Modifier.weight(1f)) {
                        Text("Закрыть смену")
                    }
                }

                Spacer(Modifier.height(16.dp))
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
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("💾 Сохранить расходы") }

                Spacer(Modifier.height(20.dp))
                Text("Аналитика", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            analytics?.let { a ->
                item {
                    StatRow("Клиентов за день", "${a.clientsCount}")
                    StatRow("Суммарные часы проката", "%.1f ч".format(a.totalHours))
                    StatRow("Общая выручка", "${a.totalRevenue} сом")
                    StatRow("Прощено доплат", "${a.forgivenExtra} сом")
                    StatRow("Зарплата", "${expense.salary} сом")
                    StatRow("Прочие расходы", "${expense.otherExpenses} сом")
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    StatRow("Чистая прибыль", "${a.netProfit} сом", bold = true)

                    Button(
                        onClick = {
                            val file = ReportPdf.generate(
                                context, dateKey, shift, a,
                                salary = expense.salary,
                                staffCount = admin.staffCount,
                                otherExpenses = expense.otherExpenses
                            )
                            ReportPdf.share(context, file)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) { Text("📄 Экспорт смены в PDF") }

                    Spacer(Modifier.height(16.dp))
                    Text("Износ роликов (по частоте)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                items(a.rollerUsage) { (rollerId, count) ->
                    StatRow("Ролик #$rollerId", "$count выдач")
                }
            } ?: item { Text("Нет данных за этот день") }
        }
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
}

@Composable
private fun StatRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontSize = 15.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun millisToDateKey(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
    )
}
