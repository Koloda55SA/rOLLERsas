package com.rooler.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
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

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotifPermissionIfNeeded()
        TimerService.start(this)

        setContent {
            MaterialTheme {
                val vm: MainViewModel = viewModel()
                val admin = remember { AdminSettings(this) }
                val rollerGroups = remember { RollerGroups(this) }
                var groups by remember { mutableStateOf(rollerGroups.load()) }
                var screen by remember { mutableStateOf(Screen.KANBAN) }
                var showPin by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    vm.setTotalRollers(admin.totalRollers)
                    vm.loadShift(RollerRepository.dateKey())
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
                        totalBadges = admin.totalRollers,
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

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
