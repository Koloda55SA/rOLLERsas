package com.rooler.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooler.data.AdminSettings
import com.rooler.data.RollerGroup

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
    var adsEnabled by remember { mutableStateOf(settings.adsEnabled) }
    var saved by remember { mutableStateOf(false) }

    var groups by remember { mutableStateOf(settings.loadRollerGroups()) }
    var announcementMins by remember { mutableStateOf(settings.loadAnnouncementMinutes()) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Админ-настройки") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") } }
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Основные", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            NumField("Количество роликов", rollers) { rollers = it }
            NumField("Количество сотрудниц", staff) { staff = it }
            NumField("Зарплата на сотрудницу (сом)", salary) { salary = it }
            OutlinedTextField(openT, { openT = it }, label = { Text("Открытие (ЧЧ:ММ)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), singleLine = true)
            OutlinedTextField(closeT, { closeT = it }, label = { Text("Закрытие (ЧЧ:ММ)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), singleLine = true)
            NumField("PIN-код", pin) { pin = it.take(4) }

            Spacer(Modifier.height(8.dp))
            Text("Зарплата за смену: ${(staff.toIntOrNull() ?: 0) * (salary.toIntOrNull() ?: 0)} с",
                fontSize = 14.sp, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("Группы роликов по размерам", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Добавьте группы, чтобы не листать 50 роликов. Пример: рз.31 = ролики 1-10",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            groups.forEachIndexed { i, g ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(g.size, { groups = groups.toMutableList().apply { this[i] = RollerGroup(it, g.from, g.to) } },
                            label = { Text("Размер") }, modifier = Modifier.weight(1f), singleLine = true)
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(g.from.toString(), {
                            val n = it.toIntOrNull() ?: g.from
                            groups = groups.toMutableList().apply { this[i] = RollerGroup(g.size, n, g.to) }
                        }, label = { Text("От") }, modifier = Modifier.weight(0.7f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(g.to.toString(), {
                            val n = it.toIntOrNull() ?: g.to
                            groups = groups.toMutableList().apply { this[i] = RollerGroup(g.size, g.from, n) }
                        }, label = { Text("До") }, modifier = Modifier.weight(0.7f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        IconButton(onClick = { groups = groups.toMutableList().apply { removeAt(i) } }) {
                            Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            OutlinedButton(onClick = { groups = groups + RollerGroup("", groups.size + 1, groups.size + 10) },
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить группу")
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("Объявления перед закрытием", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("За сколько минут до закрытия объявлять. Голос записывается в «Озвучка».",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            announcementMins.forEachIndexed { i, m ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    OutlinedTextField(m.toString(), {
                        val n = it.toIntOrNull() ?: m
                        announcementMins = announcementMins.toMutableList().apply { this[i] = n }
                    }, label = { Text("Минут до закрытия") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = { announcementMins = announcementMins.toMutableList().apply { removeAt(i) } }) {
                        Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            OutlinedButton(onClick = { announcementMins = announcementMins + 30 },
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить время объявления")
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("Реклама", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Показывать рекламу", modifier = Modifier.weight(1f))
                Switch(checked = adsEnabled, onCheckedChange = { adsEnabled = it })
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Разраб: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Рахманов Сыймыкбек", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(4.dp))
                Text("\uD83D\uDCF8 @rahmanov_", fontSize = 12.sp, color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    settings.totalRollers = rollers.toIntOrNull()?.coerceIn(1, 200) ?: 50
                    settings.staffCount = staff.toIntOrNull() ?: 2
                    settings.salaryPerStaff = salary.toIntOrNull() ?: 0
                    settings.openTimeStr = openT
                    settings.closeTimeStr = closeT
                    settings.pin = if (pin.length == 4) pin else "7777"
                    settings.adsEnabled = adsEnabled
                    settings.saveRollerGroups(groups.filter { it.size.isNotEmpty() })
                    groups = settings.loadRollerGroups()
                    settings.saveAnnouncementMinutes(announcementMins.filter { it > 0 }.distinct().sortedDescending())
                    announcementMins = settings.loadAnnouncementMinutes()
                    saved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("\uD83D\uDCBE Сохранить все настройки", fontWeight = FontWeight.Bold) }

            if (saved) Text("Сохранено \u2713", color = MaterialTheme.colorScheme.primary,
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
