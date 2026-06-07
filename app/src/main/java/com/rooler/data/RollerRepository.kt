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

    suspend fun startSession(rollerId: Int, badgeId: Int, durationMins: Int, rollerSize: String = "", durationSeconds: Int = durationMins * 60) {
        val now = System.currentTimeMillis()
        val base = PricingLogic.baseAmount(durationMins)
        // Пишем через Map: поле id помечено @DocumentId и его нельзя сериализовать.
        // endTime считаем по точным секундам — это поддерживает «своё время» (например 10 сек).
        val data = mapOf(
            "dateKey" to dateKey(),
            "rollerId" to rollerId,
            "rollerSize" to rollerSize,
            "badgeId" to badgeId,
            "durationMins" to durationMins,
            "startTime" to now,
            "endTime" to now + durationSeconds * 1000L,
            "baseAmount" to base,
            "extraAmount" to 0,
            "totalAmount" to base,
            "isActive" to true,
            "isExtraForgiven" to false
        )
        db.collection(TRANSACTIONS).add(data).await()
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

    /**
     * Продлевает активную сессию на [addMins] минут от текущего момента.
     * Просрочка обнуляется (новое время отсчитывается заново), к базовой
     * стоимости добавляется цена доп. времени.
     */
    suspend fun extendSession(txId: String, addMins: Int) {
        val ref = db.collection(TRANSACTIONS).document(txId)
        val tx = ref.get().await().toObject(Transaction::class.java) ?: return
        val now = System.currentTimeMillis()
        val newBase = tx.baseAmount + PricingLogic.baseAmount(addMins)
        ref.update(
            mapOf(
                "endTime" to now + addMins * 60_000L,
                "durationMins" to tx.durationMins + addMins,
                "baseAmount" to newBase,
                "totalAmount" to newBase,
                "extraAmount" to 0,
                "isExtraForgiven" to false
            )
        ).await()
    }

    suspend fun forceCloseActive() {
        val snap = db.collection(TRANSACTIONS).whereEqualTo("isActive", true).get().await()
        val now = System.currentTimeMillis()
        for (doc in snap.documents) {
            doc.reference.update(mapOf(
                "isActive" to false,
                "endTime" to now,
                "isExtraForgiven" to true
            )).await()
        }
    }

    suspend fun saveExpense(dateKey: String, expense: DailyExpense) {
        db.collection(DAILY_EXPENSES).document(dateKey).set(expense).await()
    }

    // --- Смены ---
    fun shiftFlow(dateKey: String): Flow<com.rooler.data.models.Shift> = callbackFlow {
        val reg = db.collection(SHIFTS)
            .whereEqualTo("dateKey", dateKey)
            .orderBy("openTime", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(com.rooler.data.models.Shift(dateKey = dateKey)); return@addSnapshotListener }
                val shift = snap?.documents?.firstOrNull()?.toObject(com.rooler.data.models.Shift::class.java)
                    ?: com.rooler.data.models.Shift(dateKey = dateKey)
                trySend(shift)
            }
        awaitClose { reg.remove() }
    }

    suspend fun openShift(dateKey: String, cashierName: String, staffCount: Int = 1) {
        val now = System.currentTimeMillis()
        val openShifts = db.collection(SHIFTS)
            .whereEqualTo("dateKey", dateKey)
            .whereEqualTo("closeTime", 0L)
            .get().await()
        for (doc in openShifts.documents) {
            doc.reference.update("closeTime", now).await()
        }
        db.collection(SHIFTS).add(mapOf(
            "dateKey" to dateKey,
            "cashierName" to cashierName,
            "staffCount" to staffCount,
            "openTime" to now,
            "closeTime" to 0L,
            "comment" to ""
        )).await()
    }

    suspend fun closeShift(shiftId: String) {
        db.collection(SHIFTS).document(shiftId)
            .update("closeTime", System.currentTimeMillis()).await()
    }

    suspend fun loadShiftHistory(limit: Int = 30): List<Pair<String, com.rooler.data.models.Shift>> {
        val snap = db.collection(SHIFTS)
            .orderBy("openTime", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get().await()
        return snap.documents.mapNotNull { doc ->
            val shift = doc.toObject(com.rooler.data.models.Shift::class.java) ?: return@mapNotNull null
            doc.id to shift
        }
    }

    /** Все завершённые транзакции (для полного отчёта А-Я). */
    suspend fun loadAllTransactions(): List<Transaction> {
        val snap = db.collection(TRANSACTIONS).get().await()
        return snap.toObjects(Transaction::class.java)
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
