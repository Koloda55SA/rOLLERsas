package com.rooler.service

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Запись голоса с микрофона в filesDir/voices/<name>.m4a (AAC/MPEG4).
 * Используется для записи озвучки бейджей и общей фразы.
 */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentTarget: File? = null

    /** Начать запись во временный файл. */
    fun start(name: String) {
        stopPlayback()
        val target = VoicePlayer.voiceFile(context, name)
        currentTarget = target
        val tmp = File(target.parentFile, "$name.tmp")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        rec.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(tmp.absolutePath)
            prepare()
            start()
        }
        recorder = rec
    }

    /** Остановить запись и сохранить файл (заменяя старый). Возвращает true при успехе. */
    fun stop(): Boolean {
        val rec = recorder ?: return false
        return runCatching {
            rec.stop()
            rec.release()
            recorder = null
            val target = currentTarget!!
            val tmp = File(target.parentFile, "${target.nameWithoutExtension}.tmp")
            if (target.exists()) target.delete()
            tmp.renameTo(target)
            true
        }.getOrElse {
            runCatching { rec.release() }
            recorder = null
            false
        }
    }

    /** Прослушать запись. */
    fun playback(name: String) {
        stopPlayback()
        val file = VoicePlayer.voiceFile(context, name)
        if (!file.exists()) return
        player = runCatching {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { it.release() }
                prepare()
                start()
            }
        }.getOrNull()
    }

    fun stopPlayback() {
        player?.runCatching { release() }
        player = null
    }

    fun exists(name: String): Boolean = VoicePlayer.voiceFile(context, name).exists()

    fun release() {
        runCatching { recorder?.release() }
        recorder = null
        stopPlayback()
    }
}
