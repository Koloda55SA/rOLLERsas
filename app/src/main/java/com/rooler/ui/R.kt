package com.rooler.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Дизайн-токены приложения. Премиальная тёмная тема:
 * глубокий сине-фиолетовый фон, неоновый индиго-акцент, мягкие «стеклянные» поверхности.
 * Обновление цветов здесь автоматически освежает все экраны.
 */
object R {
    // Фон и поверхности (от самого тёмного к светлому)
    val BG = Color(0xFF0B0B14)     // фон экрана
    val S1 = Color(0xFF15151F)     // топбар / панели
    val S2 = Color(0xFF1E1E2D)     // карточки
    val S3 = Color(0xFF2A2A3D)     // границы / неактивные чипы

    // Акценты
    val PR = Color(0xFF7C5CFF)     // основной (индиго)
    val PR2 = Color(0xFFB9A5FF)    // светлый индиго (текст на тёмном)
    val SC = Color(0xFF22E0C8)     // бирюзовый (вторичные действия)

    // Статусы
    val GR = Color(0xFF34D399)     // зелёный — катаются / ок
    val GR_BG = Color(0xFF10231C)
    val YL = Color(0xFFFFC44D)     // жёлтый — скоро
    val YL_BG = Color(0xFF2A2110)
    val RD = Color(0xFFFF5C72)     // красный — истекло
    val RD_BG = Color(0xFF2A1018)

    // Текст
    val T1 = Color(0xFFF4F4F8)     // основной
    val T2 = Color(0xFFA8A8BC)     // вторичный
    val T3 = Color(0xFF6C6C82)     // приглушённый

    // Прочее
    val DV = Color(0xFF26263A)     // разделители
    val WM = Color(0xFFB0B0C0)
    val IG = Color(0xFFE1306C)
    val CH = Color(0xFF15151F)

    // Градиенты
    val GradPrimary = Brush.linearGradient(listOf(Color(0xFF7C5CFF), Color(0xFF9D7BFF)))
    val GradGreen = Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF22C594)))
    val GradHeader = Brush.linearGradient(listOf(Color(0xFF1A1430), Color(0xFF120F22)))
}
