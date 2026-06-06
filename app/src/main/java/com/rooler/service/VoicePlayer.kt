package com.rooler.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

/**
 * Озвучка истёкших сессий и объявлений о закрытии.
 *
 * Групповая озвучка: когда подряд (в течение [BATCH_WINDOW_MS]) истекает несколько
 * роликов, бейджи собираются в пачку и проговариваются друг за другом —
 * "num_1, num_2, num_3, ..." — а общая фраза "у вас заканчивается время"
 * звучит ОДИН раз в конце, а не после каждого бейджа.
 *
 * Несколько пачек/объявлений никогда не накладываются: всё идёт строго по очереди.
 */
class VoicePlayer(
    private val context: Context,
    private val onDuckStart: () -> Unit = {},
    private val onDuckEnd: () -> Unit = {}
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val queue = Channel<Int>(Channel.UNLIMITED)
    private val announcementQueue = Channel<Int>(Channel.UNLIMITED)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: Any? = null

    init {
        // Единый потребитель — гарантирует, что озвучка бейджей и объявления
        // никогда не звучат одновременно.
        scope.launch {
            for (firstBadge in queue) {
                // Собираем пачку: первый бейдж + всё, что прилетело за окно batch.
                val batch = linkedSetOf(firstBadge)
                while (true) {
                    val next = withTimeoutOrNull(BATCH_WINDOW_MS) { queue.receive() } ?: break
                    batch.add(next)
                }
                acquireFocus()
                onDuckStart()
                playBatch(batch.sorted())
                drainAnnouncements()
                if (queue.isEmpty && announcementQueue.isEmpty) {
                    onDuckEnd()
                    releaseFocus()
                }
            }
        }
    }

    /** Проигрывает объявления, накопившиеся пока шла озвучка бейджей. */
    private suspend fun drainAnnouncements() {
        while (true) {
            val minutesBefore = announcementQueue.tryReceive().getOrNull() ?: break
            playOne("announce_$minutesBefore")
            playOne("closing_reminder")
        }
    }

    private fun acquireFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { }
                .build()
            focusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun releaseFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest is AudioFocusRequest) {
            audioManager.abandonAudioFocusRequest(focusRequest as AudioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    fun enqueue(badgeId: Int) { queue.trySend(badgeId) }
    fun enqueueAnnouncement(minutesBefore: Int) { announcementQueue.trySend(minutesBefore) }

    /** Все номера бейджей подряд, затем общая фраза один раз. */
    private suspend fun playBatch(badges: List<Int>) {
        for (badge in badges) playOne("num_$badge")
        playOne("time_ended")
    }

    private suspend fun playOne(name: String) = suspendCancellableCoroutine<Unit> { cont ->
        val file = voiceFile(context, name)
        if (!file.exists()) { cont.resume(Unit); return@suspendCancellableCoroutine }
        val player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
            }
        }.getOrNull()
        if (player == null) { cont.resume(Unit); return@suspendCancellableCoroutine }
        // Сообщаем UI, что идёт озвучка — для «волны» снизу экрана.
        VoiceBus.startSpeaking(name)
        player.setOnCompletionListener {
            it.release()
            VoiceBus.stopSpeaking()
            if (cont.isActive) cont.resume(Unit)
        }
        player.setOnErrorListener { mp, _, _ ->
            mp.release()
            VoiceBus.stopSpeaking()
            if (cont.isActive) cont.resume(Unit)
            true
        }
        cont.invokeOnCancellation { runCatching { player.release() }; VoiceBus.stopSpeaking() }
        player.start()
    }

    fun release() { queue.close(); announcementQueue.close() }

    companion object {
        /** Окно сбора пачки бейджей: всё, что истекло за это время, озвучивается вместе. */
        private const val BATCH_WINDOW_MS = 3_000L

        fun voicesDir(context: Context): File =
            File(context.filesDir, "voices").apply { mkdirs() }

        fun voiceFile(context: Context, name: String): File =
            File(voicesDir(context), "$name.m4a")
    }
}
