package com.rooler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.data.AdminSettings

// Экран админ-настроек: кол-во роликов, сотрудниц, ЗП, время смены, PIN.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AdminSettings(context) }

    var rollers by remember { mutableStateOf(settings.totalRollers.toString()) }
    var staff by remember { mutableStateOf(settings.staffCount.toString()) }
    var salary by remember { mutableStateOf(settings.salaryPerStaff.toString()) }
    var openT by remember { mutableStateOf(settings.openTimeStr) }
    var closeT by remember { mutableStateOf(settings.closeTimeStr) }
    var pin by remember { mutableStateOf(settings.pin) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Настройки администратора") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") } }
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            NumField("Количество роликов", rollers) { rollers = it }
            NumField("Количество сотрудниц", staff) { staff = it }
            NumField("Зарплата на сотрудницу (сом)", salary) { salary = it }
            OutlinedTextField(openT, { openT = it }, label = { Text("Время открытия (ЧЧ:ММ)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), singleLine = true)
            OutlinedTextField(closeT, { closeT = it }, label = { Text("Время закрытия (ЧЧ:ММ)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), singleLine = true)
            NumField("PIN-код входа", pin) { pin = it.take(4) }

            Spacer(Modifier.height(12.dp))
            Text("Зарплата за смену по умолчанию: " +
                "${(staff.toIntOrNull() ?: 0) * (salary.toIntOrNull() ?: 0)} сом",
                fontSize = 14.sp, fontWeight = FontWeight.Medium)

            Button(
                onClick = {
                    settings.totalRollers = rollers.toIntOrNull()?.coerceIn(1, 200) ?: 50
                    settings.staffCount = staff.toIntOrNull() ?: 2
                    settings.salaryPerStaff = salary.toIntOrNull() ?: 0
                    settings.openTimeStr = openT
                    settings.closeTimeStr = closeT
                    settings.pin = if (pin.length == 4) pin else "7777"
                    saved = true
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) { Text("💾 Сохранить настройки") }

            if (saved) Text("Сохранено ✓", color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
