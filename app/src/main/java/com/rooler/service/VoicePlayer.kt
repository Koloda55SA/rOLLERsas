package com.rooler.service

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Озвучка с ОЧЕРЕДЬЮ.
 *
 * Голос ЦЕЛИКОМ записывается кассиршей через интерфейс приложения и хранится
 * в filesDir/voices/. В коде никаких встроенных файлов озвучки нет.
 *   - num_<badge>.m4a  — запись номера бейджа (1..50)
 *   - time_ended.m4a   — общая фраза «время закончилось, обратитесь на кассу»
 *
 * При истечении времени у нескольких сессий одновременно фразы идут СТРОГО
 * ПО ОЧЕРЕДИ, не накладываясь. Каждая фраза = num_<badge> -> time_ended.
 *
 * Перед началом озвучки вызывается onDuckStart (приглушить музыку),
 * после опустошения очереди — onDuckEnd (вернуть громкость).
 */
class VoicePlayer(
    private val context: Context,
    private val onDuckStart: () -> Unit = {},
    private val onDuckEnd: () -> Unit = {}
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val queue = Channel<Int>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (badgeId in queue) {
                onDuckStart()
                playPhrase(badgeId)
                if (queue.isEmpty) onDuckEnd()
            }
        }
    }

    /** Добавить бейдж в очередь озвучки. */
    fun enqueue(badgeId: Int) { queue.trySend(badgeId) }

    private suspend fun playPhrase(badgeId: Int) {
        playOne("num_$badgeId")
        playOne("time_ended")
    }

    private suspend fun playOne(name: String) = suspendCancellableCoroutine<Unit> { cont ->
        val file = voiceFile(context, name)
        if (!file.exists()) { cont.resume(Unit); return@suspendCancellableCoroutine }
        val player = runCatching {
            MediaPlayer().apply {
                // Поток ALARM: звук играет даже в тихом режиме и при заблокированном экране.
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
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

    fun release() { queue.close() }

    companion object {
        fun voicesDir(context: Context): File =
            File(context.filesDir, "voices").apply { mkdirs() }

        fun voiceFile(context: Context, name: String): File =
            File(voicesDir(context), "$name.m4a")
    }
}
