package com.rooler.data

import android.content.Context

/**
 * Локальные настройки администратора (SharedPreferences).
 * Используются в расчётах бухгалтерии и отчётах.
 */
class AdminSettings(context: Context) {
    private val prefs = context.getSharedPreferences("admin_settings", Context.MODE_PRIVATE)

    var totalRollers: Int
        get() = prefs.getInt(KEY_ROLLERS, 50)
        set(v) = prefs.edit().putInt(KEY_ROLLERS, v).apply()

    var staffCount: Int
        get() = prefs.getInt(KEY_STAFF, 2)
        set(v) = prefs.edit().putInt(KEY_STAFF, v).apply()

    var salaryPerStaff: Int
        get() = prefs.getInt(KEY_SALARY, 1000)
        set(v) = prefs.edit().putInt(KEY_SALARY, v).apply()

    var openTimeStr: String
        get() = prefs.getString(KEY_OPEN, "10:00") ?: "10:00"
        set(v) = prefs.edit().putString(KEY_OPEN, v).apply()

    var closeTimeStr: String
        get() = prefs.getString(KEY_CLOSE, "22:00") ?: "22:00"
        set(v) = prefs.edit().putString(KEY_CLOSE, v).apply()

    var pin: String
        get() = prefs.getString(KEY_PIN, "7777") ?: "7777"
        set(v) = prefs.edit().putString(KEY_PIN, v).apply()

    /** Зарплата по умолчанию за смену = кол-во сотрудниц * ЗП на сотрудницу. */
    fun defaultDailySalary(): Int = staffCount * salaryPerStaff

    companion object {
        private const val KEY_ROLLERS = "total_rollers"
        private const val KEY_STAFF = "staff_count"
        private const val KEY_SALARY = "salary_per_staff"
        private const val KEY_OPEN = "open_time"
        private const val KEY_CLOSE = "close_time"
        private const val KEY_PIN = "pin"
    }
}
