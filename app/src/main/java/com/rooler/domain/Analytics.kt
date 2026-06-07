package com.rooler.domain

import com.rooler.data.models.Shift
import com.rooler.data.models.Transaction

data class DayAnalytics(
    val totalRevenue: Int,
    val forgivenExtra: Int,
    val netProfit: Int,
    val rollerUsage: List<Pair<Int, Int>>,
    val clientsCount: Int = 0,        // кол-во завершённых транзакций
    val totalHours: Double = 0.0,     // суммарные часы проката
    val sizeUsage: List<Pair<String, Int>> = emptyList()  // выдач по размеру ролика
)

object AnalyticsLogic {
    /**
     * Транзакции, относящиеся к конкретной смене: по дате И по временно́му окну смены
     * (старт сессии между открытием и закрытием смены). Если смена ещё открыта
     * (closeTime<=0) — берём всё с момента открытия. Это разделяет выручку между
     * сменами одного дня (например, смена Айжан и затем смена Сыймыка).
     */
    fun transactionsForShift(all: List<Transaction>, shift: Shift): List<Transaction> {
        if (shift.openTime <= 0) return emptyList()
        val end = if (shift.closeTime > 0) shift.closeTime else Long.MAX_VALUE
        return all.filter { it.dateKey == shift.dateKey && it.startTime in shift.openTime..end }
    }

    fun compute(
        transactions: List<Transaction>,
        salary: Int,
        otherExpenses: Int
    ): DayAnalytics {
        val finished = transactions.filter { !it.isActive }
        val totalRevenue = finished.sumOf { it.totalAmount }
        val forgivenExtra = transactions.filter { it.isExtraForgiven }.sumOf { it.extraAmount }
        val netProfit = totalRevenue - salary - otherExpenses
        val usage = transactions
            .groupingBy { it.rollerId }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
        val clientsCount = finished.size
        val totalHours = transactions.sumOf { it.durationMins } / 60.0
        val sizeUsage = transactions
            .filter { it.rollerSize.isNotEmpty() }
            .groupingBy { it.rollerSize }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
        return DayAnalytics(totalRevenue, forgivenExtra, netProfit, usage, clientsCount, totalHours, sizeUsage)
    }
}
