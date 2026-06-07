package com.rooler.data

import android.content.Context
import org.json.JSONArray

class AdminSettings(context: Context) {
    private val prefs = context.getSharedPreferences("admin_settings", Context.MODE_PRIVATE)

    var totalRollers: Int
        get() = prefs.getInt(KEY_ROLLERS, 50)
        set(v) = prefs.edit().putInt(KEY_ROLLERS, v).apply()

    /** Кол-во бейджей (номеров для озвучки). По умолчанию = кол-ву роликов. */
    var badgeCount: Int
        get() = prefs.getInt(KEY_BADGES, prefs.getInt(KEY_ROLLERS, 50))
        set(v) = prefs.edit().putInt(KEY_BADGES, v).apply()

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

    var adsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ADS, false)
        set(v) = prefs.edit().putBoolean(KEY_ADS, v).apply()

    /** Имя последнего кассира — подставляется при открытии новой смены. */
    var lastCashier: String
        get() = prefs.getString(KEY_LAST_CASHIER, "") ?: ""
        set(v) = prefs.edit().putString(KEY_LAST_CASHIER, v).apply()

    fun defaultDailySalary(): Int = staffCount * salaryPerStaff

    fun loadAnnouncementMinutes(): List<Int> {
        val raw = prefs.getString(KEY_ANNOUNCEMENTS, null) ?: return listOf(30, 15, 5)
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getInt(it) }
        }.getOrDefault(listOf(30, 15, 5))
    }

    fun saveAnnouncementMinutes(minutes: List<Int>) {
        val arr = JSONArray()
        minutes.forEach { arr.put(it) }
        prefs.edit().putString(KEY_ANNOUNCEMENTS, arr.toString()).apply()
    }

    fun loadRollerGroups(): List<RollerGroup> {
        val raw = prefs.getString(KEY_GROUPS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                RollerGroup(o.getString("size"), o.getInt("from"), o.getInt("to"))
            }
        }.getOrDefault(emptyList())
    }

    fun saveRollerGroups(groups: List<RollerGroup>) {
        val arr = JSONArray()
        groups.forEach { g ->
            arr.put(org.json.JSONObject().put("size", g.size).put("from", g.from).put("to", g.to))
        }
        prefs.edit().putString(KEY_GROUPS, arr.toString()).apply()
    }

    companion object {
        private const val KEY_ROLLERS = "total_rollers"
        private const val KEY_BADGES = "badge_count"
        private const val KEY_STAFF = "staff_count"
        private const val KEY_SALARY = "salary_per_staff"
        private const val KEY_OPEN = "open_time"
        private const val KEY_CLOSE = "close_time"
        private const val KEY_PIN = "pin"
        private const val KEY_ADS = "ads_enabled"
        private const val KEY_ANNOUNCEMENTS = "announcement_minutes"
        private const val KEY_GROUPS = "roller_groups"
        private const val KEY_LAST_CASHIER = "last_cashier"
    }
}
