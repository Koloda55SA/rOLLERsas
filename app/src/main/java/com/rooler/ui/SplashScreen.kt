package com.rooler.ui

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * Заставка при запуске: проигрывает видео splash.mp4 ~2.5 сек, затем входит в приложение.
 * Если видео по какой-то причине не стартует — всё равно уходим дальше по таймеру.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    // Подстраховка: уходим максимум через 3.5 сек, даже если видео не доиграло/не стартовало.
    LaunchedEffect(Unit) {
        delay(3500)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(R.BG), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    val uri = android.net.Uri.parse("android.resource://${ctx.packageName}/${com.rooler.R.raw.splash}")
                    setVideoURI(uri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = false
                        runCatching { mp.setVolume(1f, 1f) }
                        start()
                    }
                    // Когда видео доиграло раньше таймера — входим сразу.
                    setOnCompletionListener { onDone() }
                    setOnErrorListener { _, _, _ -> onDone(); true }
                }
            }
        )
    }
}
