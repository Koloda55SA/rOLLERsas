package com.rooler.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Transaction(
    @DocumentId val id: String = "",
    val dateKey: String = "",
    val rollerId: Int = 0,
    val rollerSize: String = "",
    val badgeId: Int = 0,
    val durationMins: Int = 30,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val baseAmount: Int = 200,
    val extraAmount: Int = 0,
    val totalAmount: Int = 0,
    // @get:PropertyName обязателен: иначе Firestore ищет поле "active"/"extraForgiven"
    // (отбрасывает префикс is) и булево всегда читается как false — ломает клики и учёт.
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = false,
    @get:PropertyName("isExtraForgiven") @set:PropertyName("isExtraForgiven")
    var isExtraForgiven: Boolean = false
)

data class DailyExpense(
    val salary: Int = 0,
    val otherExpenses: Int = 0,
    val comment: String = ""
)

data class Shift(
    @DocumentId val id: String = "",
    val dateKey: String = "",
    val cashierName: String = "",
    val staffCount: Int = 1,
    val openTime: Long = 0L,
    val closeTime: Long = 0L,
    val comment: String = ""
)
