package com.rooler.service

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentTarget: File? = null

    fun start(name: String) {
        stopPlayback()
        stop()
        val target = VoicePlayer.voiceFile(context, name)
        target.parentFile?.mkdirs()
        currentTarget = target
        val tmp = File(target.parentFile, "$name.tmp")
        if (tmp.exists()) tmp.delete()
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            // Меньше задержка старта и стабильнее качество речи.
            rec.setAudioSamplingRate(44100)
            rec.setAudioEncodingBitRate(96000)
            rec.setOutputFile(tmp.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
        } catch (e: Exception) {
            runCatching { rec.release() }
            recorder = null
            currentTarget = null
            throw e
        }
    }

    fun stop(): Boolean {
        val rec = recorder ?: return false
        return runCatching {
            rec.stop()
            rec.release()
            recorder = null
            val target = currentTarget ?: return false
            val tmp = File(target.parentFile, "${target.nameWithoutExtension}.tmp")
            if (target.exists()) target.delete()
            tmp.renameTo(target)
            currentTarget = null
            true
        }.getOrElse {
            runCatching { rec.release() }
            recorder = null
            currentTarget = null
            false
        }
    }

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
