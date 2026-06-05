package com.rooler.data

import android.content.Context

data class RollerGroup(val size: String, val from: Int, val to: Int) {
    fun rollers(): List<Int> = (from..to).toList()
}

class RollerGroups(context: Context) {
    private val settings = AdminSettings(context)

    fun load(): List<RollerGroup> = settings.loadRollerGroups()
    fun save(groups: List<RollerGroup>) = settings.saveRollerGroups(groups)

    fun sizeOf(rollerId: Int): String =
        load().firstOrNull { rollerId in it.from..it.to }?.size ?: ""
}
