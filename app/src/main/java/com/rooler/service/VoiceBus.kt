package com.rooler.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Общая шина состояния озвучки: позволяет UI (Канбан) показывать «волну»,
 * пока сервис проговаривает бейджи/объявления. Сервис обновляет state,
 * экран его слушает.
 */
object VoiceBus {
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    /** Текущий «уровень» голоса 0..1 — для анимации волны. */
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    /** Что сейчас проговаривается (для подписи снизу). */
    private val _label = MutableStateFlow("")
    val label: StateFlow<String> = _label.asStateFlow()

    fun startSpeaking(label: String) { _label.value = label; _speaking.value = true }
    fun setLevel(v: Float) { _level.value = v.coerceIn(0f, 1f) }
    fun stopSpeaking() { _speaking.value = false; _level.value = 0f; _label.value = "" }
}
