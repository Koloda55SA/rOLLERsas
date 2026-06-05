package com.rooler.data.models

data class Announcement(
    val minutesBeforeClose: Int = 0,
    val text: String = "",
    val voiceKey: String = "announce_$minutesBeforeClose"
)
