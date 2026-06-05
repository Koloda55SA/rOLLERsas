package com.rooler.service

import android.content.Context
import android.media.MediaPlayer
import com.rooler.R

// num_<badge>.mp3 -> (onCompletion) -> time_ended.mp3 без паузы.
// time_ended.mp3: "...номерүү бейджик, убактыңыз бүттү. Кассага кайрылыңыз".
class VoicePlayer(private val context: Context) {

    fun announce(badgeId: Int) {
        val numRes = context.resources.getIdentifier(
            "num_$badgeId", "raw", context.packageName
        )
        if (numRes == 0) { playTimeEnded(); return }
        MediaPlayer.create(context, numRes)?.apply {
            setOnCompletionListener { mp ->
                mp.release()
                playTimeEnded()
            }
            start()
        } ?: playTimeEnded()
    }

    private fun playTimeEnded() {
        MediaPlayer.create(context, R.raw.time_ended)?.apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }
}
