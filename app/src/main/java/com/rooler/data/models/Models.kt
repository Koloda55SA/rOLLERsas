package com.rooler.data.models

import com.google.firebase.firestore.DocumentId

// Транзакция проката одной сессии. Коллекция `transactions`.
data class Transaction(
    @DocumentId val id: String = "",
    val dateKey: String = "",          // "yyyy-MM-dd"
    val rollerId: Int = 0,             // номер ролика 1..50
    val badgeId: Int = 0,
    val durationMins: Int = 30,        // 30 или 60
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val baseAmount: Int = 200,         // 200 / 400 сом
    val extraAmount: Int = 0,
    val totalAmount: Int = 0,
    val isActive: Boolean = false,
    val isExtraForgiven: Boolean = false
)

// Расходы за день. ID документа = дата "yyyy-MM-dd". Коллекция `daily_expenses`.
data class DailyExpense(
    val salary: Int = 0,
    val otherExpenses: Int = 0,
    val comment: String = ""
)
