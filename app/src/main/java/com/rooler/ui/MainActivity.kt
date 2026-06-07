package com.rooler.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rooler.data.AdminSettings
import com.rooler.data.RollerGroup
import com.rooler.data.RollerGroups
import com.rooler.data.RollerRepository
import com.rooler.service.TimerService

enum class Screen { KANBAN, SETTINGS, VOICE_SETUP, ADMIN, SHIFT_HISTORY }

class MainActivity : ComponentActivity() {

    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded()
        TimerService.start(this)

        setContent {
            RollerTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onDone = { showSplash = false })
                    return@RollerTheme
                }
                val vm: MainViewModel = viewModel()
                val admin = remember { AdminSettings(this) }
                val rollerGroups = remember { RollerGroups(this) }
                var groups by remember { mutableStateOf(rollerGroups.load()) }
                var screen by remember { mutableStateOf(Screen.KANBAN) }
                var showPin by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    vm.setTotalRollers(admin.totalRollers)
                    vm.loadShift(RollerRepository.dateKey())
                    // Подтягиваем записи озвучки из облака (нужно при переносе на новое устройство).
                    runCatching { com.rooler.service.VoiceSync.syncDown(this@MainActivity) }
                }

                when (screen) {
                    Screen.KANBAN -> KanbanScreen(
                        vm = vm,
                        totalRollers = admin.totalRollers,
                        groups = groups,
                        onOpenSettings = { showPin = true }
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        vm = vm,
                        onOpenVoiceSetup = { screen = Screen.VOICE_SETUP },
                        onOpenAdmin = { screen = Screen.ADMIN },
                        onOpenShiftHistory = { screen = Screen.SHIFT_HISTORY },
                        onBack = { screen = Screen.KANBAN }
                    )
                    Screen.VOICE_SETUP -> VoiceSetupScreen(
                        totalBadges = admin.badgeCount,
                        onBack = { screen = Screen.SETTINGS }
                    )
                    Screen.ADMIN -> AdminScreen(
                        onBack = {
                            groups = rollerGroups.load()
                            vm.setTotalRollers(admin.totalRollers)
                            screen = Screen.SETTINGS
                        }
                    )
                    Screen.SHIFT_HISTORY -> ShiftHistoryScreen(
                        onBack = { screen = Screen.SETTINGS }
                    )
                }

                if (showPin) {
                    PinDialog(
                        onDismiss = { showPin = false },
                        onSuccess = { showPin = false; screen = Screen.SETTINGS }
                    )
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        // Микрофон — для записи озвучки. Без явного запроса запись не работает.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needed.isNotEmpty()) permissions.launch(needed.toTypedArray())
    }
}
