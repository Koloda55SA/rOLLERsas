package com.rooler.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rooler.data.models.DailyExpense
import com.rooler.data.models.Transaction
import com.rooler.domain.PricingLogic
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Единственный источник данных - Cloud Firestore.
class RollerRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        const val TRANSACTIONS = "transactions"
        const val DAILY_EXPENSES = "daily_expenses"
        const val SHIFTS = "shifts"
        fun dateKey(date: Date = Date()): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
    }

    fun activeTransactionsFlow(): Flow<List<Transaction>> = callbackFlow {
        val reg = db.collection(TRANSACTIONS)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.toObjects(Transaction::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    fun transactionsForDayFlow(dateKey: String): Flow<List<Transaction>> = callbackFlow {
        val reg = db.collection(TRANSACTIONS)
            .whereEqualTo("dateKey", dateKey)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.toObjects(Transaction::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun startSession(rollerId: Int, badgeId: Int, durationMins: Int) {
        val now = System.currentTimeMillis()
        val base = PricingLogic.baseAmount(durationMins)
        val tx = Transaction(
            dateKey = dateKey(),
            rollerId = rollerId,
            badgeId = badgeId,
            durationMins = durationMins,
            startTime = now,
            endTime = now + durationMins * 60_000L,
            baseAmount = base,
            extraAmount = 0,
            totalAmount = base,
            isActive = true,
            isExtraForgiven = false
        )
        db.collection(TRANSACTIONS).add(tx).await()
    }

    suspend fun returnSession(txId: String, base: Int, extra: Int, forgiven: Boolean) {
        val total = PricingLogic.totalOnReturn(base, extra, forgiven)
        db.collection(TRANSACTIONS).document(txId).update(
            mapOf(
                "isActive" to false,
                "endTime" to System.currentTimeMillis(),
                "extraAmount" to extra,
                "isExtraForgiven" to forgiven,
                "totalAmount" to total
            )
        ).await()
    }

    suspend fun saveExpense(dateKey: String, expense: DailyExpense) {
        db.collection(DAILY_EXPENSES).document(dateKey).set(expense).await()
    }

    // --- Смены ---
    fun shiftFlow(dateKey: String): Flow<com.rooler.data.models.Shift> = callbackFlow {
        val reg = db.collection(SHIFTS).document(dateKey)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(com.rooler.data.models.Shift(dateKey)); return@addSnapshotListener }
                trySend(snap?.toObject(com.rooler.data.models.Shift::class.java)
                    ?: com.rooler.data.models.Shift(dateKey))
            }
        awaitClose { reg.remove() }
    }

    suspend fun openShift(dateKey: String) {
        db.collection(SHIFTS).document(dateKey)
            .set(mapOf("dateKey" to dateKey, "openTime" to System.currentTimeMillis()),
                com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun closeShift(dateKey: String) {
        db.collection(SHIFTS).document(dateKey)
            .set(mapOf("dateKey" to dateKey, "closeTime" to System.currentTimeMillis()),
                com.google.firebase.firestore.SetOptions.merge()).await()
    }

    fun expenseFlow(dateKey: String): Flow<DailyExpense> = callbackFlow {
        val reg = db.collection(DAILY_EXPENSES).document(dateKey)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(DailyExpense()); return@addSnapshotListener }
                trySend(snap?.toObject(DailyExpense::class.java) ?: DailyExpense())
            }
        awaitClose { reg.remove() }
    }
}
