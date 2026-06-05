package com.rooler.data.models

data class Shift(
    val dateKey: String = "",
    val cashierName: String = "",
    val openTime: Long = 0L,
    val closeTime: Long = 0L,
    val comment: String = ""
)
