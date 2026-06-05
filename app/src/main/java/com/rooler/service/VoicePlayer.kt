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
import java.io.File
import kotlin.coroutines.resume

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
        scope.launch {
            for (badgeId in queue) {
                acquireFocus()
                onDuckStart()
                playPhrase(badgeId)
                if (queue.isEmpty && announcementQueue.isEmpty) {
                    onDuckEnd()
                    releaseFocus()
                }
            }
        }
        scope.launch {
            for (minutesBefore in announcementQueue) {
                acquireFocus()
                onDuckStart()
                playOne("announce_$minutesBefore")
                playOne("closing_reminder")
                if (queue.isEmpty && announcementQueue.isEmpty) {
                    onDuckEnd()
                    releaseFocus()
                }
            }
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

    private suspend fun playPhrase(badgeId: Int) {
        playOne("num_$badgeId")
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
        player.setOnCompletionListener {
            it.release()
            if (cont.isActive) cont.resume(Unit)
        }
        player.setOnErrorListener { mp, _, _ ->
            mp.release()
            if (cont.isActive) cont.resume(Unit)
            true
        }
        cont.invokeOnCancellation { runCatching { player.release() } }
        player.start()
    }

    fun release() { queue.close(); announcementQueue.close() }

    companion object {
        fun voicesDir(context: Context): File =
            File(context.filesDir, "voices").apply { mkdirs() }

        fun voiceFile(context: Context, name: String): File =
            File(voicesDir(context), "$name.m4a")
    }
}
