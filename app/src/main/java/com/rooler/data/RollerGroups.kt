package com.rooler.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Группа роликов одного размера: размер и диапазон номеров роликов.
 * Например: size="31", from=1, to=10 — ролики 1..10 имеют размер 31.
 */
data class RollerGroup(val size: String, val from: Int, val to: Int) {
    fun rollers(): List<Int> = (from..to).toList()
}

/**
 * Хранилище групп роликов по размерам (локально, SharedPreferences в JSON).
 * Настраивается один раз в админке.
 */
class RollerGroups(context: Context) {
    private val prefs = context.getSharedPreferences("roller_groups", Context.MODE_PRIVATE)

    fun load(): List<RollerGroup> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                RollerGroup(o.getString("size"), o.getInt("from"), o.getInt("to"))
            }
        }.getOrDefault(emptyList())
    }

    fun save(groups: List<RollerGroup>) {
        val arr = JSONArray()
        groups.forEach { g ->
            arr.put(JSONObject().put("size", g.size).put("from", g.from).put("to", g.to))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    /** Найти размер для номера ролика. */
    fun sizeOf(rollerId: Int): String =
        load().firstOrNull { rollerId in it.from..it.to }?.size ?: ""

    companion object {
        private const val KEY = "groups_json"
    }
}
