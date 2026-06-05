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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

const val DEFAULT_PIN = "7777"

// Диалог PIN перед входом в настройки.
@Composable
fun PinDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
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
            Button(onClick = { if (pin == DEFAULT_PIN) onSuccess() else error = true }) { Text("Войти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val dateKey by vm.selectedDate.collectAsState()
    val expense by vm.expense.collectAsState()
    val analytics by vm.analytics.collectAsState()

    LaunchedEffect(dateKey) { vm.loadAccounting(dateKey) }

    var salary by remember(expense) { mutableStateOf(expense.salary.toString()) }
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
                Spacer(Modifier.height(16.dp))
                Text("Расходы за день", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedTextField(salary, { salary = it.filter { c -> c.isDigit() } },
                    label = { Text("Зарплата кассира") }, modifier = Modifier.fillMaxWidth(),
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
                    StatRow("Общая выручка", "${a.totalRevenue} сом")
                    StatRow("Прощено доплат", "${a.forgivenExtra} сом")
                    StatRow("Зарплата", "${expense.salary} сом")
                    StatRow("Прочие расходы", "${expense.otherExpenses} сом")
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    StatRow("Чистая прибыль", "${a.netProfit} сом", bold = true)
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
