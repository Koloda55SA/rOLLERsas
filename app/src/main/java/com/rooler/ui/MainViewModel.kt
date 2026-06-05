package com.rooler.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rooler.data.RollerRepository
import com.rooler.data.models.DailyExpense
import com.rooler.data.models.Shift
import com.rooler.data.models.Transaction
import com.rooler.domain.AnalyticsLogic
import com.rooler.domain.Column
import com.rooler.domain.DayAnalytics
import com.rooler.domain.SessionView
import com.rooler.domain.toSessionView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val TOTAL_ROLLERS_DEFAULT = 50

data class KanbanState(
    val freeRollers: List<Int> = emptyList(),
    val riding: List<SessionView> = emptyList(),
    val ending: List<SessionView> = emptyList(),
    val expired: List<SessionView> = emptyList()
)

class MainViewModel(
    private val repo: RollerRepository = RollerRepository()
) : ViewModel() {

    private var totalRollers = TOTAL_ROLLERS_DEFAULT
    fun setTotalRollers(n: Int) { totalRollers = n.coerceAtLeast(1) }

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    val currentDateTime: StateFlow<String> = ticker
        .map { now -> SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(now)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val kanban: StateFlow<KanbanState> =
        combine(repo.activeTransactionsFlow(), ticker) { active, now ->
            buildState(active, now)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KanbanState())

    private fun buildState(active: List<Transaction>, now: Long): KanbanState {
        val views = active.map { it.toSessionView(now) }
        val busyRollers = active.map { it.rollerId }.toSet()
        val free = (1..totalRollers).filter { it !in busyRollers }

        val riding = views.filter { it.column == Column.RIDING }.sortedBy { it.remainingMs }
        val ending = views.filter { it.column == Column.ENDING }.sortedBy { it.remainingMs }
        val expired = views.filter { it.column == Column.EXPIRED }.sortedByDescending { it.overdueMins }

        return KanbanState(free, riding, ending, expired)
    }

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    fun clearError() { _error.value = null }

    fun startSession(rollerId: Int, badgeId: Int, durationMins: Int, rollerSize: String = "") = viewModelScope.launch {
        try {
            repo.startSession(rollerId, badgeId, durationMins, rollerSize)
        } catch (e: Exception) {
            _error.value = "Не удалось выдать ролик: ${e.message}"
        }
    }

    fun returnSession(tx: Transaction, extra: Int, forgiven: Boolean) = viewModelScope.launch {
        try {
            repo.returnSession(tx.id, tx.baseAmount, extra, forgiven)
        } catch (e: Exception) {
            _error.value = "Не удалось оформить возврат: ${e.message}"
        }
    }

    private val _selectedDate = MutableStateFlow(RollerRepository.dateKey())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()
    fun selectDate(dateKey: String) { _selectedDate.value = dateKey }

    private val _expense = MutableStateFlow(DailyExpense())
    val expense: StateFlow<DailyExpense> = _expense.asStateFlow()

    private val _analytics = MutableStateFlow<DayAnalytics?>(null)
    val analytics: StateFlow<DayAnalytics?> = _analytics.asStateFlow()

    fun loadAccounting(dateKey: String) {
        viewModelScope.launch { repo.expenseFlow(dateKey).collect { _expense.value = it } }
        viewModelScope.launch {
            combine(repo.transactionsForDayFlow(dateKey), repo.expenseFlow(dateKey)) { txs, exp ->
                AnalyticsLogic.compute(txs, exp.salary, exp.otherExpenses)
            }.collect { _analytics.value = it }
        }
    }

    fun saveExpense(dateKey: String, salary: Int, other: Int, comment: String) =
        viewModelScope.launch {
            try {
                repo.saveExpense(dateKey, DailyExpense(salary, other, comment))
            } catch (e: Exception) { _error.value = e.message }
        }

    private val _shift = MutableStateFlow(Shift())
    val shift: StateFlow<Shift> = _shift.asStateFlow()

    fun loadShift(dateKey: String) = viewModelScope.launch {
        repo.shiftFlow(dateKey).collect { _shift.value = it }
    }

    fun openShift(dateKey: String) = viewModelScope.launch {
        try { repo.openShift(dateKey) } catch (e: Exception) { _error.value = e.message }
    }
    fun closeShift(dateKey: String) = viewModelScope.launch {
        try { repo.closeShift(dateKey) } catch (e: Exception) { _error.value = e.message }
    }
}
