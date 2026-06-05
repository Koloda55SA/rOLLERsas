package com.rooler.domain

import com.rooler.data.models.Transaction

data class DayAnalytics(
    val totalRevenue: Int,
    val forgivenExtra: Int,
    val netProfit: Int,
    val rollerUsage: List<Pair<Int, Int>>,
    val clientsCount: Int = 0,        // кол-во завершённых транзакций
    val totalHours: Double = 0.0      // суммарные часы проката
)

object AnalyticsLogic {
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
        return DayAnalytics(totalRevenue, forgivenExtra, netProfit, usage, clientsCount, totalHours)
    }
}
