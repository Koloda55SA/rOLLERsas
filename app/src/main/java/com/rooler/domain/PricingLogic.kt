package com.rooler.domain

import com.rooler.data.models.Transaction

enum class Column { FREE, RIDING, ENDING, EXPIRED }

object PricingLogic {
    const val EXTRA_PER_MIN = 10
    private const val ENDING_THRESHOLD_MS = 5 * 60_000L

    fun baseAmount(mins: Int): Int = if (mins >= 60) 400 else 200

    fun remainingMs(endTime: Long, now: Long): Long = endTime - now

    fun column(remainingMs: Long): Column = when {
        remainingMs > ENDING_THRESHOLD_MS -> Column.RIDING
        remainingMs > 0                   -> Column.ENDING
        else                              -> Column.EXPIRED
    }

    fun overdueMinutes(endTime: Long, now: Long): Int =
        (((now - endTime).coerceAtLeast(0) + 59_999) / 60_000).toInt()

    fun extraAmount(endTime: Long, now: Long): Int =
        overdueMinutes(endTime, now) * EXTRA_PER_MIN

    fun totalOnReturn(base: Int, extra: Int, forgiven: Boolean): Int =
        if (forgiven) base else base + extra
}

data class SessionView(
    val tx: Transaction,
    val remainingMs: Long,
    val column: Column,
    val overdueMins: Int,
    val extraAmount: Int
)

fun Transaction.toSessionView(now: Long): SessionView {
    val rem = PricingLogic.remainingMs(endTime, now)
    return SessionView(
        tx = this,
        remainingMs = rem,
        column = PricingLogic.column(rem),
        overdueMins = PricingLogic.overdueMinutes(endTime, now),
        extraAmount = PricingLogic.extraAmount(endTime, now)
    )
}
