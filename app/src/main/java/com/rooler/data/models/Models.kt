package com.rooler.data.models

import com.google.firebase.firestore.DocumentId

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
    val isActive: Boolean = false,
    val isExtraForgiven: Boolean = false
)

data class DailyExpense(
    val salary: Int = 0,
    val otherExpenses: Int = 0,
    val comment: String = ""
)

data class Shift(
    val dateKey: String = "",
    val cashierName: String = "",
    val openTime: Long = 0L,
    val closeTime: Long = 0L,
    val comment: String = ""
)
