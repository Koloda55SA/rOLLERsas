package com.rooler.ui

import android.widget.FrameLayout
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
 * Заставка при запуске: видео splash.mp4 на ВЕСЬ экран (заполняет, обрезая края),
 * ~2.5-3.5 сек, затем вход. Если видео не стартует — уходим по таймеру.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3500)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(R.BG), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // Контейнер на весь экран; видео масштабируем так, чтобы заполнить целиком.
                val container = FrameLayout(ctx)
                val video = VideoView(ctx)
                val lp = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply { gravity = android.view.Gravity.CENTER }
                video.layoutParams = lp
                container.addView(video)

                val uri = android.net.Uri.parse("android.resource://${ctx.packageName}/${com.rooler.R.raw.splash}")
                video.setVideoURI(uri)
                video.setOnPreparedListener { mp ->
                    mp.isLooping = false
                    runCatching { mp.setVolume(1f, 1f) }
                    // Масштаб «cover»: растягиваем VideoView, чтобы видео заполнило экран без полей.
                    val vw = mp.videoWidth.toFloat()
                    val vh = mp.videoHeight.toFloat()
                    if (vw > 0 && vh > 0) {
                        val cw = container.width.toFloat().coerceAtLeast(1f)
                        val ch = container.height.toFloat().coerceAtLeast(1f)
                        val scale = maxOf(cw / vw, ch / vh)
                        val newLp = FrameLayout.LayoutParams((vw * scale).toInt(), (vh * scale).toInt())
                        newLp.gravity = android.view.Gravity.CENTER
                        video.layoutParams = newLp
                    }
                    video.start()
                }
                video.setOnCompletionListener { onDone() }
                video.setOnErrorListener { _, _, _ -> onDone(); true }
                container
            }
        )
    }
}
