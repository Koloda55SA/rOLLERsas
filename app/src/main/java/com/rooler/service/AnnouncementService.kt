package com.rooler.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rooler.data.AdminSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnnouncementService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var voice: VoicePlayer
    private val announced = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        voice = VoicePlayer(context = this)
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        startChecker()
    }

    private fun startChecker() = scope.launch {
        while (true) {
            val admin = AdminSettings(this@AnnouncementService)
            val closeTime = parseTime(admin.closeTimeStr)
            if (closeTime > 0L) {
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val closeToday = cal.timeInMillis + closeTime
                val minutesLeft = ((closeToday - now) / 60_000).toInt()

                for (m in admin.loadAnnouncementMinutes()) {
                    val key = "announce_${m}_${SimpleDateFormat("yyyyMMdd", Locale.US).format(now)}"
                    if (minutesLeft in (m - 1)..m && key !in announced) {
                        announced.add(key)
                        voice.enqueueAnnouncement(m)
                        showAnnouncementNotification(m)
                    }
                }
            }
            delay(30_000)
        }
    }

    private fun parseTime(timeStr: String): Long {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0L
        val h = parts[0].toIntOrNull() ?: return 0L
        val m = parts[1].toIntOrNull() ?: return 0L
        return (h * 60 + m) * 60_000L
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL, "Объявления", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL)
        .setContentTitle("Rollerdrome")
        .setContentText("Объявления активны")
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setOngoing(true)
        .build()

    private fun showAnnouncementNotification(minutesBefore: Int) {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = NotificationCompat.Builder(this, "roller_alert")
            .setContentTitle("Закрываемся через $minutesBefore мин!")
            .setContentText("Подписывайтесь: __rahmanov___")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .build()
        mgr.notify(2000 + minutesBefore, n)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        voice.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "announcements"
        private const val NOTIF_ID = 2
        fun start(ctx: Context) {
            val i = Intent(ctx, AnnouncementService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }
    }
}
