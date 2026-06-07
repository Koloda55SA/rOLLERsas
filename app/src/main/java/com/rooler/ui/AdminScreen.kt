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
    val ctx = LocalContext.current
    val s = remember { AdminSettings(ctx) }
    var rollers by remember { mutableStateOf(s.totalRollers.toString()) }
    var badges by remember { mutableStateOf(s.badgeCount.toString()) }
    var staff by remember { mutableStateOf(s.staffCount.toString()) }
    var salary by remember { mutableStateOf(s.salaryPerStaff.toString()) }
    var openT by remember { mutableStateOf(s.openTimeStr) }
    var closeT by remember { mutableStateOf(s.closeTimeStr) }
    var pin by remember { mutableStateOf(s.pin) }
    var ads by remember { mutableStateOf(s.adsEnabled) }
    var groups by remember { mutableStateOf(s.loadRollerGroups()) }
    var annMins by remember { mutableStateOf(s.loadAnnouncementMinutes()) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(containerColor = R.BG, topBar = { TopAppBar(title = { Text("Админ", color = R.T1) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = R.T2) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = R.S1)) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.weight(1f).padding(14.dp).verticalScroll(rememberScrollState())) {
                Text("Основные", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = R.T1)
                Spacer(Modifier.height(4.dp))
                NF("Роликов", rollers) { rollers = it }; NF("Бейджей (озвучка)", badges) { badges = it }; NF("Сотрудниц", staff) { staff = it }; NF("ЗП/сотр.", salary) { salary = it }
                OutlinedTextField(openT, { openT = it }, label = { Text("Открытие ЧЧ:ММ") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = tfc())
                OutlinedTextField(closeT, { closeT = it }, label = { Text("Закрытие ЧЧ:ММ") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = tfc())
                NF("PIN", pin) { pin = it.take(4) }
                Text("ЗП за смену: ${(staff.toIntOrNull() ?: 0) * (salary.toIntOrNull() ?: 0)} с", fontSize = 13.sp, color = R.T2)
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Реклама", modifier = Modifier.weight(1f), color = R.T2); Switch(ads, { ads = it }, colors = SwitchDefaults.colors(checkedTrackColor = R.PR)) }

                Spacer(Modifier.height(14.dp)); HorizontalDivider(color = R.DV); Spacer(Modifier.height(10.dp))
                Text("Группы роликов", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = R.T1)
                Text("Добавьте группы по размерам", fontSize = 12.sp, color = R.T3)
                groups.forEachIndexed { i, g ->
                    Card(colors = CardDefaults.cardColors(containerColor = R.S2), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(g.size, { groups = groups.toMutableList().apply { this[i] = RollerGroup(it, g.from, g.to) } }, label = { Text("Разм") }, singleLine = true, modifier = Modifier.weight(0.8f), colors = tfc())
                            Spacer(Modifier.width(4.dp))
                            OutlinedTextField(g.from.toString(), { val n = it.toIntOrNull() ?: g.from; groups = groups.toMutableList().apply { this[i] = RollerGroup(g.size, n, g.to) } }, label = { Text("От") }, singleLine = true, modifier = Modifier.weight(0.6f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = tfc())
                            Spacer(Modifier.width(4.dp))
                            OutlinedTextField(g.to.toString(), { val n = it.toIntOrNull() ?: g.to; groups = groups.toMutableList().apply { this[i] = RollerGroup(g.size, g.from, n) } }, label = { Text("До") }, singleLine = true, modifier = Modifier.weight(0.6f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = tfc())
                            IconButton(onClick = { groups = groups.toMutableList().apply { removeAt(i) } }) { Icon(Icons.Default.Delete, null, tint = R.RD, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
                OutlinedButton(onClick = { groups = groups + RollerGroup("", groups.size + 1, groups.size + 10) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = R.PR)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Добавить") }

                Spacer(Modifier.height(14.dp)); HorizontalDivider(color = R.DV); Spacer(Modifier.height(10.dp))
                Text("Объявления", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = R.T1)
                annMins.forEachIndexed { i, m ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(m.toString(), { val n = it.toIntOrNull() ?: m; annMins = annMins.toMutableList().apply { this[i] = n } }, label = { Text("Мин до закрытия") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = tfc())
                        IconButton(onClick = { annMins = annMins.toMutableList().apply { removeAt(i) } }) { Icon(Icons.Default.Delete, null, tint = R.RD, modifier = Modifier.size(18.dp)) }
                    }
                }
                OutlinedButton(onClick = { annMins = annMins + 30 }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = R.PR)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Добавить") }

                Spacer(Modifier.height(20.dp))
                Button(onClick = {
                    s.totalRollers = rollers.toIntOrNull()?.coerceIn(1, 200) ?: 50; s.badgeCount = badges.toIntOrNull()?.coerceIn(1, 500) ?: 50; s.staffCount = staff.toIntOrNull() ?: 2; s.salaryPerStaff = salary.toIntOrNull() ?: 0
                    s.openTimeStr = openT; s.closeTimeStr = closeT; s.pin = if (pin.length == 4) pin else "7777"; s.adsEnabled = ads
                    s.saveRollerGroups(groups.filter { it.size.isNotEmpty() }); groups = s.loadRollerGroups()
                    s.saveAnnouncementMinutes(annMins.filter { it > 0 }.distinct().sortedDescending()); annMins = s.loadAnnouncementMinutes(); saved = true
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = R.PR)) { Text("\uD83D\uDCBE Сохранить всё", fontWeight = FontWeight.Bold) }
                if (saved) Text("\u2705 Сохранено", color = R.GR, modifier = Modifier.padding(top = 6.dp))
            }
            Watermark()
        }
    }
}

@Composable private fun NF(l: String, v: String, cb: (String) -> Unit) = OutlinedTextField(v, { cb(it.filter { c -> c.isDigit() }) }, label = { Text(l) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = tfc())
@Composable private fun tfc() = OutlinedTextFieldDefaults.colors(focusedBorderColor = R.PR, cursorColor = R.PR, unfocusedBorderColor = R.S3, focusedLabelColor = R.PR2, unfocusedLabelColor = R.T3)
