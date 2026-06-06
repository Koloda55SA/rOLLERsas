package com.rooler.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Тёмная тема приложения. Без неё Compose берёт светлую схему по умолчанию —
 * и текст в полях ввода/диалогах становится чёрным на тёмном фоне (не виден).
 */
private val RollerColors = darkColorScheme(
    primary = R.PR,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = R.S2,
    onPrimaryContainer = R.T1,
    secondary = R.SC,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    background = R.BG,
    onBackground = R.T1,
    surface = R.S1,
    onSurface = R.T1,
    surfaceVariant = R.S2,
    onSurfaceVariant = R.T2,
    outline = R.S3,
    error = R.RD,
    onError = androidx.compose.ui.graphics.Color.White,
)

@Composable
fun RollerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RollerColors,
        typography = Typography(),
        content = content
    )
}
