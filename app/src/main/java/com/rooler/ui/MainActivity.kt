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
import com.rooler.service.TimerService

// Количество роликов. По умолчанию 50.
private const val TOTAL_ROLLERS_DEFAULT = 50

class MainActivity : ComponentActivity() {

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* неважно при отказе: сервис всё равно работает */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotifPermissionIfNeeded()
        TimerService.start(this)

        setContent {
            MaterialTheme {
                val vm: MainViewModel = viewModel()
                var screen by remember { mutableStateOf(Screen.KANBAN) }
                var showPin by remember { mutableStateOf(false) }

                when (screen) {
                    Screen.KANBAN -> KanbanScreen(
                        vm = vm,
                        totalRollers = TOTAL_ROLLERS_DEFAULT,
                        onOpenSettings = { showPin = true }
                    )
                    Screen.SETTINGS -> SettingsScreen(vm) { screen = Screen.KANBAN }
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

import androidx.compose.material3.MaterialTheme

enum class Screen { KANBAN, SETTINGS }
