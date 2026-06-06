package com.rooler.service

import android.content.Context
import android.media.MediaPlayer

/**
 * Фоновая музыка (зацикленная). Играет всё время.
 * Во время озвучки громкость приглушается до 20% (duck), потом обратно 100%.
 *
 * Файл музыки: сначала пользовательский filesDir/voices/background_music.m4a,
 * иначе встроенный res/raw/background_music. Если нет ничего — молчит.
 */
class BackgroundMusic(private val context: Context) {
    private var player: MediaPlayer? = null
    private val full = 1.0f
    private val ducked = 0.1f

    fun start() {
        if (player != null) return
        player = createPlayer()?.apply {
            isLooping = true
            setVolume(full, full)
            start()
        }
    }

    private fun createPlayer(): MediaPlayer? {
        val userFile = com.rooler.service.VoicePlayer.voiceFile(context, "background_music")
        if (userFile.exists()) {
            return runCatching {
                MediaPlayer().apply { setDataSource(userFile.absolutePath); prepare() }
            }.getOrNull()
        }
        val resId = context.resources.getIdentifier("background_music", "raw", context.packageName)
        if (resId == 0) return null
        return MediaPlayer.create(context, resId)
    }

    fun duck() { player?.setVolume(ducked, ducked) }
    fun unduck() { player?.setVolume(full, full) }

    fun release() {
        player?.release()
        player = null
    }
}
