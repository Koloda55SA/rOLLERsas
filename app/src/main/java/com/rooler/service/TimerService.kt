package com.rooler.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rooler.data.RollerRepository
import com.rooler.domain.PricingLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// Foreground Service: держит таймеры в памяти. Каждую секунду проверяет
// активные сессии и при переходе через 00:00 ставит бейдж в очередь озвучки.
// Фоновая музыка играет всё время и приглушается на время озвучки.
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repo = RollerRepository()
    private lateinit var voice: VoicePlayer
    private lateinit var music: BackgroundMusic

    private val endTimes = MutableStateFlow<Map<String, Pair<Int, Long>>>(emptyMap())
    private val announced = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        music = BackgroundMusic(this)
        voice = VoicePlayer(
            context = this,
            onDuckStart = { music.duck() },
            onDuckEnd = { music.unduck() }
        )
        startForeground(NOTIF_ID, buildNotification())
        music.start()
        observeSessions()
        startTicker()
    }

    private fun observeSessions() = scope.launch {
        repo.activeTransactionsFlow().collect { list ->
            val map = list.associate { it.id to (it.badgeId to it.endTime) }
            endTimes.value = map
            announced.retainAll(map.keys)
        }
    }

    private fun startTicker() = scope.launch {
        while (true) {
            val now = System.currentTimeMillis()
            endTimes.value.forEach { (id, badgeAndEnd) ->
                val (badge, end) = badgeAndEnd
                if (PricingLogic.remainingMs(end, now) <= 0 && id !in announced) {
                    announced.add(id)
                    voice.enqueue(badge)
                }
            }
            delay(1_000)
        }
    }

    private fun buildNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL, "Roller timers", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Rollerdrome")
            .setContentText("Таймеры сессий активны")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        voice.release()
        music.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "roller_timers"
        private const val NOTIF_ID = 1
        fun start(ctx: Context) {
            val i = Intent(ctx, TimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }
    }
}
