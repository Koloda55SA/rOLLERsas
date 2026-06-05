package com.rooler.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// После перезагрузки/включения телефона снова запускаем TimerService.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> TimerService.start(context)
        }
    }
}
