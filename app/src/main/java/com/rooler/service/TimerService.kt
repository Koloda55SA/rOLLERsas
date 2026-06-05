package com.rooler.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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
// активные сессии и при переходе через 00:00 ставит бейдж в очередь озвучки
// и показывает heads-up уведомление (поверх приложений и на экране блокировки).
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repo = RollerRepository()
    private lateinit var voice: VoicePlayer
    private lateinit var music: BackgroundMusic
    private var wakeLock: PowerManager.WakeLock? = null

    private val endTimes = MutableStateFlow<Map<String, Pair<Int, Long>>>(emptyMap())
    private val announced = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        music = BackgroundMusic(this)
        voice = VoicePlayer(
            context = this,
            onDuckStart = { music.duck(); acquireWake() },
            onDuckEnd = { music.unduck(); releaseWake() }
        )
        createChannels()
        startForeground(NOTIF_ID, buildOngoingNotification())
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
                    showExpiredNotification(badge)
                }
            }
            delay(1_000)
        }
    }

    // WakeLock на время озвучки, чтобы CPU не уснул при выключенном экране.
    private fun acquireWake() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rollerdrome:voice").apply {
            acquire(60_000L)
        }
    }

    private fun releaseWake() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL, "Таймеры", NotificationManager.IMPORTANCE_LOW)
            )
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERT, "Истекло время", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Уведомление о конце сессии" }
            )
        }
    }

    private fun buildOngoingNotification(): android.app.Notification =
        NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Rollerdrome")
            .setContentText("Таймеры сессий активны")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .build()

    // Уведомление поверх других приложений и на экране блокировки.
    private fun showExpiredNotification(badge: Int) {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setContentTitle("Время истекло!")
            .setContentText("Бейдж $badge — примите возврат")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()
        mgr.notify(1000 + badge, n)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        voice.release()
        music.release()
        releaseWake()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "roller_timers"
        private const val CHANNEL_ALERT = "roller_alert"
        private const val NOTIF_ID = 1
        fun start(ctx: Context) {
            val i = Intent(ctx, TimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }
    }
}
